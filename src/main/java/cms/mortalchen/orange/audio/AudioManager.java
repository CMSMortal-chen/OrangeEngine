package cms.mortalchen.orange.audio;

import cms.mortalchen.orange.api.ILoggerImp;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

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

    private long device;
    private long context;
    private boolean available = false;

    private final Map<String, Integer> buffers = new HashMap<>(); // path -> AL buffer
    private final int[] pool = new int[POOL_SIZE];                // 一次性音效源
    private int cursor = CHANNEL_COUNT;                           // 普通轮换游标（跳过通道源）
    private final Map<String, Integer> loops = new HashMap<>();   // path -> 循环源
    private float masterVolume = 1f;

    private AudioManager() {
        try {
            device = ALC10.alcOpenDevice((ByteBuffer) null);
            if (device == 0L) {
                logger.warn("OpenAL 无法打开音频设备，音效已禁用");
                return;
            }
            ALCCapabilities caps = ALC.createCapabilities(device);
            context = ALC10.alcCreateContext(device, (IntBuffer) null);
            if (context == 0L) {
                logger.warn("OpenAL 无法创建上下文，音效已禁用");
                return;
            }
            ALC10.alcMakeContextCurrent(context);
            AL.createCapabilities(caps);
            for (int i = 0; i < POOL_SIZE; i++) {
                pool[i] = AL10.alGenSources();
            }
            available = true;
            logger.info("OpenAL 初始化完成，音效池 {} 源", POOL_SIZE);
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

    /** 开始循环播放（环境/氛围音）；同路径幂等，只占用一个循环源 */
    public void loop(String path) {
        loop(path, 1f);
    }

    public void loop(String path, float volume) {
        if (!available) return;
        if (loops.containsKey(path)) return;
        Integer buffer = getBuffer(path);
        if (buffer == null) return;
        try {
            int source = AL10.alGenSources();
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
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            AudioFormat fmt = ais.getFormat();
            int alFormat = toOpenALFormat(fmt);
            if (alFormat == 0) {
                logger.warn("不支持的音频格式（需 8/16bit PCM WAV）: {} {}", path, fmt);
                return 0;
            }
            byte[] pcm = ais.readAllBytes();
            ByteBuffer data = ByteBuffer.allocateDirect(pcm.length).order(ByteOrder.nativeOrder());
            data.put(pcm).flip();
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, alFormat, data, (int) fmt.getSampleRate());
            return buf;
        } catch (Throwable t) {
            logger.warn("音效加载失败: {}", path, t);
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
}
