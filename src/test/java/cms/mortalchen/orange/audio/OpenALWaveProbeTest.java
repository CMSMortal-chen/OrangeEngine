package cms.mortalchen.orange.audio;

import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 决定性实验：用 OpenAL Soft 自带的 "Wave File Writer" 伪设备直接抓取
 * 混音后的输出（绕过物理设备 / Windows 音频处理），量化 OpenAL 内部
 * 处理链路（混音/重采样/滤波器）对低频的影响。
 * <p>
 * 生成一组 30Hz~7kHz 等幅纯音，按游戏相同的流式播放方式（float32、
 * 24 buffer 循环池、cubic resampler、不空间化）播放，写出的 wav 即
 * OpenAL 内部最终输出。若低频纯音幅度保持，则 OpenAL 内部无问题，
 * 罪魁祸首是物理设备/Windows 音频链；若低频被衰减，则 OpenAL 内部
 * 处理（或流式路径）有问题。
 * <p>
 * 需要 %APPDATA%\\alsoft.ini（[wave] file=...）指向输出文件。
 */
class OpenALWaveProbeTest {

    static final int RATE = 44100;
    static final int CH = 2;
    // 等幅纯音序列（Hz）
    static final int[] TONES = {30, 45, 60, 80, 100, 130, 170, 220, 300, 400, 600, 1000, 2000, 4000, 7000};
    static final int TONE_FRAMES = (int) (RATE * 0.5);   // 0.5s 纯音
    static final int GAP_FRAMES = (int) (RATE * 0.15);   // 0.15s 静音
    static final double AMP = 0.4;

    @Test
    void listDevices() {
        // LWJGL alcGetString 在首个 \0 处截断，需直接读原生内存拿到完整列表
        long p = ALC10.nalcGetString(0L, 0x1013); // ALC_ALL_DEVICES_SPECIFIER
        int count = 0;
        if (p != 0L) {
            long a = p;
            while (true) {
                int len = 0;
                while (MemoryUtil.memGetByte(a + len) != 0) len++;
                if (len == 0) break;
                count++;
                a += len + 1;
            }
        }
        assertTrue(count > 0, "未枚举到任何 OpenAL 输出设备");
        long d = ALC10.alcOpenDevice((ByteBuffer) null);
        assertTrue(d != 0, "打开默认设备失败");
        ALC10.alcCloseDevice(d);
    }

    /**
     * 向真实默认设备播放等幅纯音序列（供外部循环回放录音做传递函数测量）。
     * 启动前等待 8 秒（配合外部 ffmpeg dshow 立体声混音录制）。
     */
    @Test
    void playProbeToDevice() throws Exception {
        sleep(8000);
        // 延长：重复 tone 序列 3 次 ≈ 29.25s，便于外部对齐
        int reps = Integer.parseInt(System.getProperty("gateway.alwave.reps", "3"));
        String mode = System.getProperty("gateway.alwave.mode", "stream");
        ByteBuffer base = generateProbe();
        int seqBytes = base.remaining();
        ByteBuffer pcm = ByteBuffer.allocateDirect(seqBytes * reps).order(ByteOrder.LITTLE_ENDIAN);
        for (int r = 0; r < reps; r++) {
            base.rewind();
            for (int i = 0; i < base.remaining(); i++) pcm.put(base.get(i));
        }
        pcm.flip();

        long device = openDeviceByProperty();
        assertTrue(device != 0, "打开设备失败");
        ALCCapabilities caps = ALC.createCapabilities(device);
        int ctxRate = Integer.parseInt(System.getProperty("gateway.alwave.ctxrate", "48000"));
        IntBuffer attrs = BufferUtils.createIntBuffer(5);
        attrs.put(ALC10.ALC_FREQUENCY).put(ctxRate);
        attrs.put(SOFTHRTF.ALC_HRTF_SOFT).put(SOFTHRTF.ALC_HRTF_DISABLED_SOFT);
        attrs.put(0);
        attrs.flip();
        long ctx = ALC10.alcCreateContext(device, attrs);
        ALC10.alcMakeContextCurrent(ctx);
        AL.createCapabilities(caps);

        int src = AL10.alGenSources();
        int spatial = Integer.parseInt(System.getProperty("gateway.alwave.spatialize", "0"));
        AL10.alSourcei(src, 0x1214, spatial); // 0=auto 3=force_stereo(永不空间化)
        AL10.alSource3f(src, AL10.AL_POSITION, 0f, 0f, 0f);
        AL10.alSourcef(src, AL10.AL_GAIN, 1f);
        AL10.alSourcei(src, AL10.AL_LOOPING, AL10.AL_FALSE);
        try { AL10.alSourcei(src, 0x1210, 2); } catch (Throwable ignored) { }

        int nbuf = 24;
        int[] bufs = new int[nbuf];
        for (int i = 0; i < nbuf; i++) bufs[i] = AL10.alGenBuffers();
        boolean[] inQ = new boolean[nbuf];
        int wIdx = 0;

        if ("static".equals(mode)) {
            // 静态路径：整个 PCM 放一个 buffer 直接播（与 playFile 一致）
            pcm.rewind();
            ByteBuffer all = ByteBuffer.allocateDirect(pcm.remaining()).order(ByteOrder.LITTLE_ENDIAN);
            all.put(pcm);
            all.flip();
            AL10.alBufferData(bufs[0], 0x10011, all, RATE);
            AL10.alSourceQueueBuffers(src, bufs[0]);
            AL10.alSourcePlay(src);
            while (AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) sleep(20);
            sleep(300);
            AL10.alSourceStop(src);
            for (int b : bufs) AL10.alDeleteBuffers(b);
            AL10.alDeleteSources(src);
            ALC10.alcDestroyContext(ctx);
            ALC10.alcCloseDevice(device);
            return;
        }

        int chunkFrames = 4096;
        long totalFrames = pcm.remaining() / (CH * 4L);
        long fed = 0;
        boolean playing = false;
        pcm.rewind();
        while (fed < totalFrames) {
            int processed = AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED);
            for (int p = 0; p < processed; p++) {
                IntBuffer one = BufferUtils.createIntBuffer(1);
                AL10.alSourceUnqueueBuffers(src, one);
                one.rewind();
                int id = one.get();
                for (int i = 0; i < nbuf; i++) {
                    if (bufs[i] == id) { inQ[i] = false; break; }
                }
            }
            boolean fedAny = false;
            for (int attempt = 0; attempt < nbuf && fed < totalFrames; attempt++) {
                int idx = wIdx;
                wIdx = (wIdx + 1) % nbuf;
                if (inQ[idx]) continue;
                long remain = totalFrames - fed;
                int thisFrames = (int) Math.min(chunkFrames, remain);
                ByteBuffer chunk = ByteBuffer.allocateDirect(thisFrames * CH * 4).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < thisFrames * CH; i++) chunk.putFloat(pcm.getFloat());
                chunk.flip();
                AL10.alBufferData(bufs[idx], 0x10011, chunk, RATE);
                AL10.alSourceQueueBuffers(src, bufs[idx]);
                inQ[idx] = true;
                fed += thisFrames;
                fedAny = true;
            }
            if (!playing && fed > 0) { AL10.alSourcePlay(src); playing = true; }
            if (!fedAny) sleep(10);
        }
        while (AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED) < nbuf) sleep(20);
        sleep(300);
        AL10.alSourceStop(src);
        for (int b : bufs) AL10.alDeleteBuffers(b);
        AL10.alDeleteSources(src);
        ALC10.alcDestroyContext(ctx);
        ALC10.alcCloseDevice(device);
    }

    @Test
    void probeInternalDsp() {
        // 用 -Dgateway.alwave.rate / -Dgateway.alwave.out 控制；每个 JVM 一次，
        // 因为 [wave] file 配置在进程内只读一次。默认 44100（无内部重采样）。
        int rate = Integer.parseInt(System.getProperty("gateway.alwave.rate", "44100"));
        String out = System.getProperty("gateway.alwave.out",
                outFile(rate == 48000 ? "al_probe_48.wav" : "al_probe_44.wav"));
        runAt(rate, out);
    }

    private void runAt(int ctxRate, String outWav) {
        // 生成 probe（float32 LE 交错，双声道同相）
        ByteBuffer pcm = generateProbe();
        long device = ALC10.alcOpenDevice("Wave File Writer");
        org.junit.jupiter.api.Assumptions.assumeTrue(device != 0,
                "Wave File Writer 后端未编译进 LWJGL 的 OpenAL Soft（需 alsound.ini [wave] file=），跳过");
        assertTrue(device != 0, "无法打开 Wave File Writer 设备（检查 alsound.ini 的 [wave] file=）");
        ALCCapabilities caps = ALC.createCapabilities(device);
        IntBuffer attrs = BufferUtils.createIntBuffer(3);
        attrs.put(0x1007).put(ctxRate).put(0); // ALC_FREQUENCY
        attrs.flip();
        long ctx = ALC10.alcCreateContext(device, attrs);
        assertTrue(ctx != 0, "创建上下文失败");
        ALC10.alcMakeContextCurrent(ctx);
        AL.createCapabilities(caps);

        // 与游戏流式路径一致：float32 立体声、24 buffer、cubic resampler、不空间化
        int src = AL10.alGenSources();
        AL10.alSourcei(src, 0x1214, 0); // AL_SOURCE_SPATIALIZE_SOFT=0
        AL10.alSource3f(src, AL10.AL_POSITION, 0f, 0f, 0f);
        AL10.alSourcef(src, AL10.AL_GAIN, 1f);
        AL10.alSourcei(src, AL10.AL_LOOPING, AL10.AL_FALSE);
        try {
            AL10.alSourcei(src, 0x1210, 2); // cubic/spline resampler
        } catch (Throwable ignored) {
        }

        int nbuf = 24;
        int[] bufs = new int[nbuf];
        for (int i = 0; i < nbuf; i++) bufs[i] = AL10.alGenBuffers();
        boolean[] inQ = new boolean[nbuf];
        int wIdx = 0;

        // 按 4096 帧/块喂给循环池（模拟 updateStream）
        int chunkFrames = 4096;
        int chunkBytes = chunkFrames * CH * 4;
        long totalFrames = pcm.remaining() / (CH * 4L);
        long fed = 0;
        boolean playing = false;
        pcm.rewind();
        ByteBuffer work = pcm;
        while (fed < totalFrames) {
            // 回收已播完 buffer
            int processed = AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED);
            for (int p = 0; p < processed; p++) {
                IntBuffer one = BufferUtils.createIntBuffer(1);
                AL10.alSourceUnqueueBuffers(src, one);
                one.rewind();
                int id = one.get();
                for (int i = 0; i < nbuf; i++) {
                    if (bufs[i] == id) { inQ[i] = false; break; }
                }
            }
            // 有位置就填一块
            boolean fedAny = false;
            for (int attempt = 0; attempt < nbuf && fed < totalFrames; attempt++) {
                int idx = wIdx;
                wIdx = (wIdx + 1) % nbuf;
                if (inQ[idx]) continue;
                long remain = totalFrames - fed;
                int thisFrames = (int) Math.min(chunkFrames, remain);
                ByteBuffer chunk = ByteBuffer.allocateDirect(thisFrames * CH * 4).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < thisFrames * CH; i++) {
                    chunk.putFloat(work.getFloat());
                }
                chunk.flip();
                AL10.alBufferData(bufs[idx], 0x10011, chunk, RATE); // AL_FORMAT_STEREO_FLOAT32
                AL10.alSourceQueueBuffers(src, bufs[idx]);
                inQ[idx] = true;
                fed += thisFrames;
                fedAny = true;
            }
            if (!playing && fed > 0) {
                AL10.alSourcePlay(src);
                playing = true;
            }
            if (!fedAny) {
                sleep(10);
            }
        }
        // 等播放结束
        while (true) {
            int processed = AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED);
            if (processed == nbuf) break;
            sleep(20);
        }
        sleep(200);
        AL10.alSourceStop(src);
        for (int b : bufs) AL10.alDeleteBuffers(b);
        AL10.alDeleteSources(src);
        ALC10.alcDestroyContext(ctx);
        ALC10.alcCloseDevice(device);
        assertTrue(new File(outWav).length() > 0, "输出文件为空: " + outWav);
    }

    private ByteBuffer generateProbe() {
        int total = 0;
        for (int ignored : TONES) total += TONE_FRAMES + GAP_FRAMES;
        ByteBuffer bb = ByteBuffer.allocateDirect(total * CH * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int f : TONES) {
            double w = 2 * Math.PI * f / RATE;
            for (int i = 0; i < TONE_FRAMES; i++) {
                float v = (float) (AMP * Math.sin(w * i));
                bb.putFloat(v);
                bb.putFloat(v);
            }
            for (int i = 0; i < GAP_FRAMES; i++) {
                bb.putFloat(0f);
                bb.putFloat(0f);
            }
        }
        bb.flip();
        return bb;
    }

    /** 按 -Dgateway.alDevice=<子串> 打开设备（复用 AudioManager 的匹配逻辑） */
    private static long openDeviceByProperty() {
        String wanted = System.getProperty("gateway.alDevice");
        if (wanted == null || wanted.isBlank()) {
            return ALC10.alcOpenDevice((ByteBuffer) null);
        }
        // 原生遍历完整设备列表
        java.util.List<String> devs = new java.util.ArrayList<>();
        long p = ALC10.nalcGetString(0L, 0x1013);
        if (p != 0L) {
            long a = p;
            while (true) {
                int len = 0;
                while (MemoryUtil.memGetByte(a + len) != 0) len++;
                if (len == 0) break;
                byte[] buf = new byte[len];
                for (int i = 0; i < len; i++) buf[i] = MemoryUtil.memGetByte(a + i);
                devs.add(new String(buf, java.nio.charset.StandardCharsets.UTF_8));
                a += len + 1;
            }
        }
        String match = null;
        for (String d : devs) {
            if (d.contains(wanted)) { match = d; break; }
        }
        if (match == null) return ALC10.alcOpenDevice((ByteBuffer) null);
        ByteBuffer nameBuf = BufferUtils.createByteBuffer(match.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1);
        nameBuf.put(match.getBytes(java.nio.charset.StandardCharsets.UTF_8)).put((byte) 0).flip();
        return ALC10.alcOpenDevice(nameBuf);
    }

    private static String outFile(String name) {
        String dir = System.getProperty("gateway.alwave.dir", "G:/codes/ProjectGatewayGame/tmp");
        return dir + "/" + name;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }
}
