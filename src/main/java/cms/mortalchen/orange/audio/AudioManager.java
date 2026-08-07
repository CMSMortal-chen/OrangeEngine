package cms.mortalchen.orange.audio;

import cms.mortalchen.orange.api.ILoggerImp;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 音效引擎（LWJGL OpenAL）。
 * <p>
 * 单例，懒初始化：首次 {@link #getInstance()} 时打开音频设备并创建上下文；
 * 设备不可用时静默降级（isAvailable() == false，所有播放调用为空操作）。
 * <p>
 * 资源加载走 classpath（与图片一致，路径如 /sounds/ui/move.wav），按路径缓存解码结果；
 * 一次性音效用固定大小的播放池轮换，循环音（环境/氛围）独占源。
 */
public class AudioManager implements ILoggerImp {

    private static AudioManager instance;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    private static final int POOL_SIZE = 16;
    /** 前 2 个源专用为可寻址通道（playChannel），其余为普通轮换池 */
    private static final int CHANNEL_COUNT = 2;
    /** 流式播放（网页音频等）的 OpenAL buffer 循环池大小（≈552ms 缓冲，吸收低帧率抖动避免断音） */
    private static final int STREAM_BUFFERS = 24;

    private long device;
    private long context;
    private boolean available = false;
    /** AL_EXT_FLOAT32 是否受支持（不支持时流式音频转 16-bit 播放） */
    private boolean float32Supported = false;
    /** AL_SOFT_source_resampler 是否可用（启用时用 cubic 重采样提升输出质量） */
    private boolean resamplerSoft = false;

    private final Map<String, Integer> buffers = new HashMap<>(); // path -> AL buffer
    private final int[] pool = new int[POOL_SIZE];                // 一次性音效源
    private int cursor = CHANNEL_COUNT;                           // 普通轮换游标（跳过通道源）
    private final Map<String, Integer> loops = new HashMap<>();   // path -> 循环源
    private float masterVolume = 1f;

    private AudioManager() {
        try {
            String wanted = resolveAudioDevice();
            String devList = deviceListText();
            if (wanted != null) {
                // 用子串匹配枚举列表，方便音频路由排查/指定（例：-Dgateway.alDevice=扬声器）
                String match = devList.contains(wanted) ? wanted : null;
                if (match == null) {
                    for (String d : enumerateDevices()) {
                        if (d.contains(wanted)) { match = d; break; }
                    }
                }
                if (match != null) {
                    ByteBuffer nameBuf = BufferUtils.createByteBuffer(
                            match.getBytes(StandardCharsets.UTF_8).length + 1);
                    nameBuf.put(match.getBytes(StandardCharsets.UTF_8)).put((byte) 0).flip();
                    device = ALC10.alcOpenDevice(nameBuf);
                    logger.info("OpenAL 按配置打开设备: {}", match);
                } else {
                    logger.warn("OpenAL 未找到匹配设备 \"{}\"，使用系统默认", wanted);
                }
            }
            if (device == 0L) {
                device = ALC10.alcOpenDevice((ByteBuffer) null);
            }
            if (device == 0L) {
                logger.warn("OpenAL 无法打开音频设备，音效已禁用");
                return;
            }
            ALCCapabilities caps = ALC.createCapabilities(device);
            // context 采样率跟随设备（ALC_FREQUENCY=设备混音速率，实测本机 48000）：
            // 源（CEF 44.1k / wav）由 OpenAL 高质量源重采样器（spline/bsinc）转到设备速率，
            // 输出阶段不再有设备转换器，链路最短。
            // 同时强制禁用 HRTF（ALC_HRTF_SOFT=ALC_FALSE）：耳机/头戴端点会被
            // OpenAL Soft 自动启用 HRTF 渲染，双耳滤波会削低频（实测 30Hz -9.5dB）
            // 并制造频点凹陷——普通立体声播放不需要 HRTF。
            IntBuffer ctxAttrs = org.lwjgl.BufferUtils.createIntBuffer(5);
            ctxAttrs.put(ALC10.ALC_FREQUENCY).put(48000);  // ALC_FREQUENCY = 设备速率
            ctxAttrs.put(SOFTHRTF.ALC_HRTF_SOFT).put(SOFTHRTF.ALC_HRTF_DISABLED_SOFT); // 禁用 HRTF
            ctxAttrs.put(0);                  // 终止符
            ctxAttrs.flip();
            context = ALC10.alcCreateContext(device, ctxAttrs);
            if (context == 0L) {
                logger.warn("OpenAL 无法创建上下文，音效已禁用");
                return;
            }
            ALC10.alcMakeContextCurrent(context);
            AL.createCapabilities(caps);
            // 显式设置 listener 位置（默认已是原点，避免任何未初始化状态）
            AL10.alListener3f(AL10.AL_POSITION, 0f, 0f, 0f);
            AL10.alListener3f(AL10.AL_VELOCITY, 0f, 0f, 0f);
            AL10.alListenerf(AL10.AL_GAIN, 1f);
            for (int i = 0; i < POOL_SIZE; i++) {
                pool[i] = AL10.alGenSources();
            }
            try {
                float32Supported = AL10.alIsExtensionPresent("AL_EXT_FLOAT32");
                resamplerSoft = AL10.alIsExtensionPresent("AL_SOFT_source_resampler");
            } catch (Throwable t) {
                float32Supported = false;
                resamplerSoft = false;
            }
            if (resamplerSoft) {
                for (int i = 0; i < POOL_SIZE; i++) {
                    applySourceQuality(pool[i]);
                }
            }
            available = true;
            // 只保留 OpenAL 版本日志
            logger.info("OpenAL AL: renderer={} version={}",
                    AL10.alGetString(AL10.AL_RENDERER),
                    AL10.alGetString(AL10.AL_VERSION));
        } catch (Throwable t) {
            available = false;
            logger.warn("OpenAL 初始化失败，音效已禁用", t);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** 播放一次性音效（classpath 路径，如 /sounds/ui/move.wav），素材缺失时静默跳过 */
    public void play(String path) {
        play(path, 1f);
    }

    public void play(String path, float volume) {
        if (!available) return;
        Integer buffer = getBuffer(path);
        if (buffer == null) return;
        int source = pool[CHANNEL_COUNT + cursor % (POOL_SIZE - CHANNEL_COUNT)];
        cursor = (cursor + 1) % (POOL_SIZE - CHANNEL_COUNT);
        try {
            AL10.alSourceStop(source);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, masterVolume * clamp(volume));
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcePlay(source);
        } catch (Throwable ignored) {
            // 单个源播放失败不影响其他音效
        }
    }

    /**
     * 指定通道播放：channel 0/1 固定映射到两个专用源，同一通道重复调用会从头截断覆盖。
     * 交替/覆盖策略由调用方（游戏侧）决定，引擎不实现具体算法。
     */
    public void playChannel(int channel, String path, float volume) {
        if (!available) return;
        if (channel < 0 || channel >= CHANNEL_COUNT) return;
        Integer buffer = getBuffer(path);
        if (buffer == null) return;
        int source = pool[channel];
        try {
            AL10.alSourceStop(source);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, masterVolume * clamp(volume));
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcePlay(source);
        } catch (Throwable ignored) {
        }
    }

    /** 播放磁盘 wav 文件（测试/调试用；一次性音效） */
    public void playFile(String filePath, float volume) {
        if (!available) return;
        String key = "file:" + filePath;
        Integer buffer = buffers.get(key);
        if (buffer == null) {
            try (InputStream in = new java.io.FileInputStream(filePath)) {
                buffer = load(in, filePath);
            } catch (Exception ignored) {
                return;
            }
            if (buffer == null) return;
            buffers.put(key, buffer);
        }
        int source = pool[CHANNEL_COUNT + cursor % (POOL_SIZE - CHANNEL_COUNT)];
        cursor = (cursor + 1) % (POOL_SIZE - CHANNEL_COUNT);
        try {
            AL10.alSourceStop(source);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, masterVolume * clamp(volume));
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcePlay(source);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 通过流式路径（float32、循环 buffer 池）播放磁盘 wav —— 与 {@link #playFile}（静态 S16 buffer）
     * 对照，用于定位低频损失到底在静态播放还是流式播放。
     */
    public void playFileStream(String filePath, float volume) {
        if (!available) return;
        try (InputStream in = new java.io.FileInputStream(filePath)) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            AudioFormat fmt = ais.getFormat();
            int rate = (int) fmt.getSampleRate();
            int channels = Math.max(1, fmt.getChannels());
            byte[] raw = ais.readAllBytes();
            int bits = fmt.getSampleSizeInBits();
            int bytesPerSample = bits / 8;
            if (bytesPerSample <= 0) return;
            int samples = raw.length / bytesPerSample;
            ByteBuffer f32 = ByteBuffer.allocateDirect(samples * 4).order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer rb = ByteBuffer.wrap(raw).order(fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < samples; i++) {
                float v;
                switch (bytesPerSample) {
                    case 1 -> v = ((rb.get() & 0xFF) - 128) / 128f;
                    case 2 -> v = rb.getShort() / 32768f;
                    case 3 -> {
                        int b0 = rb.get() & 0xFF, b1 = rb.get() & 0xFF, b2 = rb.get() & 0xFF;
                        int val = fmt.isBigEndian() ? (b0 << 16) | (b1 << 8) | b2 : (b2 << 16) | (b1 << 8) | b0;
                        v = val / 8388608f;
                    }
                    default -> v = rb.getInt() / 2147483648f;
                }
                f32.putFloat(v);
            }
            f32.flip();
            AudioStream stream = startStream("stream:" + filePath, rate, channels);
            if (stream == null) return;
            stream.gain = clamp(volume);
            streamQueue(stream, f32);
            logger.info("playFileStream 入队: {} ({}ch {}Hz {}s)", filePath, channels, rate,
                    Math.round(samples / (double) rate / channels));
        } catch (Throwable t) {
            logger.warn("playFileStream 失败: {}", filePath, t);
        }
    }

    /** 开始循环播放（环境/氛围音）；同路径幂等，只占用一个循环源 */    public void loop(String path) {
        loop(path, 1f);
    }

    public void loop(String path, float volume) {
        if (!available) return;
        if (loops.containsKey(path)) return;
        Integer buffer = getBuffer(path);
        if (buffer == null) return;
        try {
            int source = AL10.alGenSources();
            if (resamplerSoft) {
                applySourceQuality(source);
            }
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, masterVolume * clamp(volume));
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_TRUE);
            AL10.alSourcePlay(source);
            loops.put(path, source);
        } catch (Throwable ignored) {
        }
    }

    /** 停止指定循环音 */
    public void stopLoop(String path) {
        if (!available) return;
        Integer source = loops.remove(path);
        if (source != null) {
            AL10.alSourceStop(source);
            AL10.alDeleteSources(source);
        }
    }

    /** 停止全部声音（含循环音） */
    public void stopAll() {
        if (!available) return;
        for (int s : pool) {
            AL10.alSourceStop(s);
        }
        for (Integer s : loops.values()) {
            AL10.alSourceStop(s);
            AL10.alDeleteSources(s);
        }
        loops.clear();
    }

    public void setMasterVolume(float v) {
        masterVolume = clamp(v);
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    // ================= 流式 PCM 播放（网页音频 / CEF 音频传出） =================

    /**
     * 流式音源：float32 交错 PCM（AL_EXT_FLOAT32）。
     * 任意线程可 {@link #streamQueue} 入队；OpenAL 提交统一在渲染线程 {@link #updateStreams()} 执行
     * （OpenAL context 线程绑定，跨线程 alBufferData 不安全）。
     */
    public static class AudioStream {
        public final String key;
        public final int sampleRate;
        public final int channels;
        final int format;
        final ConcurrentLinkedQueue<ByteBuffer> pending = new ConcurrentLinkedQueue<>();
        volatile int source = 0;
        volatile boolean stopping = false;
        /** 该流独立音量（乘以主音量生效） */
        volatile float gain = 1f;
        int[] buffers = new int[0];
        /** 对应 buffer 是否仍在 OpenAL 源队列中（防止写入正在播放的 buffer） */
        boolean[] inQueue = new boolean[0];
        int writeIdx = 0;

        AudioStream(String key, int sampleRate, int channels, int format) {
            this.key = key;
            this.sampleRate = sampleRate;
            this.channels = Math.max(1, channels);
            this.format = format;
        }
    }

    private final ConcurrentHashMap<String, AudioStream> streams = new ConcurrentHashMap<>();

    /** 创建流式音源（可在 CEF UI 线程调用；OpenAL 资源延迟到渲染线程创建） */
    public AudioStream startStream(String key, int sampleRate, int channels) {
        if (!available) return null;
        // 优先 float32（AL_EXT_FLOAT32，无损），不支持时回退 16-bit PCM
        boolean useFloat = float32Supported;
        int format;
        if (channels >= 2) {
            format = useFloat ? 0x10011 : AL10.AL_FORMAT_STEREO16;
        } else {
            format = useFloat ? 0x10010 : AL10.AL_FORMAT_MONO16;
        }
        AudioStream stream = new AudioStream(key, sampleRate, channels, format);
        streams.put(key, stream);
        return stream;
    }

    /** 提交一段 float32 交错 PCM（任意线程；数据立即拷贝，可安全复用入队 Buffer） */
    public void streamQueue(AudioStream stream, ByteBuffer pcm) {
        if (stream == null || !available) return;
        int rem = pcm.remaining();
        if (rem <= 0) return;
        if (stream.format == 0x10010 || stream.format == 0x10011) {
            // float32 直接拷贝（字节序保持小端）
            ByteBuffer copy = ByteBuffer.allocateDirect(rem);
            copy.put(pcm);
            copy.flip();
            stream.pending.add(copy);
        } else {
            // 设备不支持 float32：转换 16-bit PCM（必须小端，OpenAL 期望 LE）
            int samples = rem / 4;
            ByteBuffer copy = ByteBuffer.allocateDirect(samples * 2)
                    .order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < samples; i++) {
                float f = pcm.getFloat();
                short v = (short) (Math.max(-1f, Math.min(1f, f)) * 32767f);
                copy.putShort(v);
            }
            copy.flip();
            stream.pending.add(copy);
        }
    }

    /** 停止并释放流（任意线程） */
    public void stopStream(String key) {
        AudioStream s = streams.get(key);
        if (s != null) s.stopping = true;
    }

    /** 渲染线程每帧调用：提交待播放 PCM、回收已播放 buffer、清理停止的流 */
    public void updateStreams() {
        if (!available) return;
        for (AudioStream s : streams.values()) {
            updateStream(s);
        }
        streams.entrySet().removeIf(e -> e.getValue().stopping && e.getValue().source == 0);
    }

    private void updateStream(AudioStream s) {
        try {
            if (s.source == 0) {
                if (s.pending.isEmpty()) return; // 无数据前不创建
                s.source = AL10.alGenSources();
                s.buffers = new int[STREAM_BUFFERS];
                s.inQueue = new boolean[STREAM_BUFFERS];
                for (int i = 0; i < s.buffers.length; i++) {
                    s.buffers[i] = AL10.alGenBuffers();
                }
                if (resamplerSoft) {
                    applySourceQuality(s.source);
                }
                AL10.alSourcef(s.source, AL10.AL_GAIN, masterVolume * s.gain);
                AL10.alSourcei(s.source, AL10.AL_LOOPING, AL10.AL_FALSE);
            } else {
                // 实时同步主音量（setMasterVolume 运行中调整生效）
                AL10.alSourcef(s.source, AL10.AL_GAIN, masterVolume * s.gain);
            }

            // 1. 回收已播放完的 buffer（标记可复用）
            int processed = AL10.alGetSourcei(s.source, AL10.AL_BUFFERS_PROCESSED);
            for (int p = 0; p < processed; p++) {
                IntBuffer one = org.lwjgl.BufferUtils.createIntBuffer(1);
                AL10.alSourceUnqueueBuffers(s.source, one);
                one.rewind();
                int id = one.get();
                for (int i = 0; i < s.inQueue.length; i++) {
                    if (s.buffers[i] == id) {
                        s.inQueue[i] = false;
                        break;
                    }
                }
            }

            // 2. 提交待播放数据（只写入不在队列中的 buffer，轮询分配）
            int attempts = 0;
            while (!s.pending.isEmpty() && attempts < STREAM_BUFFERS) {
                attempts++;
                int idx = s.writeIdx;
                s.writeIdx = (s.writeIdx + 1) % STREAM_BUFFERS;
                if (s.inQueue[idx]) continue; // 仍在播放，换下一个
                ByteBuffer pcm = s.pending.poll();
                AL10.alBufferData(s.buffers[idx], s.format, pcm, s.sampleRate);
                AL10.alSourceQueueBuffers(s.source, s.buffers[idx]);
                s.inQueue[idx] = true;
            }
            int dropped = s.pending.size();
            if (dropped > 0) {
                s.pending.clear(); // 缓冲池全占满，丢弃积压（实时音频丢包）
            }

            // 3. 源未播放且有数据时开始播放
            int state = AL10.alGetSourcei(s.source, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING && AL10.alGetSourcei(s.source, AL10.AL_BUFFERS_QUEUED) > 0) {
                AL10.alSourcePlay(s.source);
            }

            if (s.stopping) {
                AL10.alSourceStop(s.source);
                for (int b : s.buffers) {
                    AL10.alDeleteBuffers(b);
                }
                AL10.alDeleteSources(s.source);
                s.buffers = new int[0];
                s.inQueue = new boolean[0];
                s.pending.clear();
                s.source = 0;
            }
        } catch (Throwable ignored) {
            // 单个流异常不影响其余音频
        }
    }

    /** 释放所有 OpenAL 资源（游戏退出时调用） */
    public void dispose() {
        if (!available) return;
        stopAll();
        for (int s : pool) {
            AL10.alDeleteSources(s);
        }
        for (Integer b : buffers.values()) {
            AL10.alDeleteBuffers(b);
        }
        buffers.clear();
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
        available = false;
        instance = null;
        logger.info("OpenAL 已释放");
    }

    // ================= 内部 =================

    /**
     * 音频设备选择：-Dgateway.alDevice 优先，其次工作目录 audio_device.txt
     * （每行一个名字，首个非空行生效）。用于排查/固定输出设备。
     */
    private static String resolveAudioDevice() {
        String v = System.getProperty("gateway.alDevice");
        if (v != null && !v.isBlank()) return v.trim();
        try {
            java.io.File f = new java.io.File("audio_device.txt");
            if (f.isFile()) {
                for (String line : java.nio.file.Files.readAllLines(f.toPath())) {
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("#")) return t;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 枚举 OpenAL 全部输出设备名（原生内存遍历，LWJGL 的 alcGetString 会在首个 \\0 截断） */
    private static java.util.List<String> enumerateDevices() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            long p = ALC10.nalcGetString(0L, 0x1013); // ALC_ALL_DEVICES_SPECIFIER
            if (p != 0L) {
                long a = p;
                while (true) {
                    int len = 0;
                    while (MemoryUtil.memGetByte(a + len) != 0) len++;
                    if (len == 0) break;
                    byte[] buf = new byte[len];
                    for (int i = 0; i < len; i++) buf[i] = MemoryUtil.memGetByte(a + i);
                    out.add(new String(buf, StandardCharsets.UTF_8));
                    a += len + 1;
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String deviceListText() {
        java.util.List<String> list = enumerateDevices();
        return list.isEmpty() ? "" : String.join(" | ", list);
    }

    private Integer getBuffer(String path) {
        Integer id = buffers.get(path);
        if (id != null) return id;
        int buffer = load(path);
        if (buffer == 0) return null;
        buffers.put(path, buffer);
        return buffer;
    }

    private int load(String path) {
        try (InputStream in = AudioManager.class.getResourceAsStream(path)) {
            if (in == null) {
                logger.warn("音效不存在: {}", path);
                return 0;
            }
            return load(in, path);
        } catch (Throwable t) {
            logger.warn("音效加载失败: {}", path, t);
            return 0;
        }
    }

    private int load(InputStream in, String name) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            AudioFormat fmt = ais.getFormat();
            int alFormat = toOpenALFormat(fmt);
            if (alFormat == 0) {
                logger.warn("不支持的音频格式（需 8/16bit PCM WAV）: {} {}", name, fmt);
                return 0;
            }
            byte[] pcm = ais.readAllBytes();
            ByteBuffer data = ByteBuffer.allocateDirect(pcm.length).order(ByteOrder.nativeOrder());
            data.put(pcm).flip();
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, alFormat, data, (int) fmt.getSampleRate());
            return buf;
        } catch (Throwable t) {
            logger.warn("音效加载失败: {}", name, t);
            return 0;
        }
    }

    private static int toOpenALFormat(AudioFormat fmt) {
        int bits = fmt.getSampleSizeInBits();
        return switch (fmt.getChannels()) {
            case 1 -> bits == 16 ? AL10.AL_FORMAT_MONO16 : bits == 8 ? AL10.AL_FORMAT_MONO8 : 0;
            case 2 -> bits == 16 ? AL10.AL_FORMAT_STEREO16 : bits == 8 ? AL10.AL_FORMAT_STEREO8 : 0;
            default -> 0;
        };
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** 对源应用高质量重采样（cubic）+ 强制不空间化（立体声直接输出，避免 downmix 相位抵消削低频） */
    private void applySourceQuality(int source) {
        try {
            // AL_SOURCE_RESAMPLER_SOFT = 0x1210；2 = cubic
            AL10.alSourcei(source, 0x1210, 2);
        } catch (Throwable ignored) {
        }
        try {
            // AL_SOURCE_SPATIALIZE_SOFT = 0x1214；0 = 永不空间化（立体声源原样输出）
            AL10.alSourcei(source, 0x1214, 0);
            AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, 0f);
        } catch (Throwable ignored) {
        }
    }
}
