package cms.mortalchen.orange.oralightcef;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.audio.AudioManager;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.Motion;
import cms.mortalchen.orange.utils.render.anim.animatednumber.AnimatedFloat;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.JcefBrowser;
import org.cef.handler.*;
import org.cef.misc.CefAudioParameters;
import org.cef.misc.DataPointer;
import org.cef.network.CefRequest.TransitionType;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import javax.swing.*;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;
import static org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

public class OraLightCEF implements AutoCloseable {
    private static final int NVG_IMAGE_NODELETE = 1 << 16;

    private final JcefBrowser browser;
    private final CefClient client;
    private final OraLightCEFIpc ipc;
    private final ResourceLocation textureLocation = new ResourceLocation("__jcef_browser__");
    private final CefAudioSink audioSink = new CefAudioSink();
    /** 本实例音频流唯一 key（多 CEF 实例不互相覆盖） */
    private final String audioStreamKey = "__cef_audio_" + System.identityHashCode(this);

    private ByteBuffer paintBuffer;
    private final int[] outW = new int[1];
    private final int[] outH = new int[1];

    private int nvgImage;
    private int textureId;
    private int lastPaintWidth, lastPaintHeight;
    private boolean nvgImageDirty = true;

    /** PBO 双缓冲：CPU 写入 GPU 可见内存，glTexSubImage2D 只排一次异步 DMA，不阻塞渲染线程 */
    private int[] pboIds;
    private int pboIndex;
    private int pboCapacity;

    /** 纹理重建/尺寸变化后首帧必须整帧上传，不能信 CEF 首帧脏区 */
    private boolean needsFullUpload = true;

    /** peekDirtyStats 输出（避免每帧分配） */
    private final int[] dirtyCount = new int[1];
    private final long[] dirtyArea = new long[1];
    /** 脏区局部上传的暂存缓冲（行连续打包）与每块几何/偏移（[x,y,w,h,off]*n） */
    private ByteBuffer stagingBuffer;
    private int stagingCapacity;
    private final int[] rectData = new int[MAX_DIRTY_RECTS * 5];

    private float lastMouseX;
    private float lastMouseY;
    private boolean closed;
    /** 期望加载的 URL：浏览器创建未完成时导航可能丢失（停在 about:blank），onLoadEnd 兜底重试 */
    private volatile String pendingUrl;

    boolean isLoading;
    final AnimatedFloat loadProgress = new AnimatedFloat(0f);
    private boolean progressBarVisible = true;

    public OraLightCEF(int width, int height) {
        this(width, height, true, "about:blank");
    }

    public OraLightCEF(int width, int height, String url) {
        this(width, height, true, url);
    }

    public OraLightCEF(int width, int height, boolean transparent) {
        this(width, height, transparent, "about:blank");
    }

    public OraLightCEF(int width, int height, boolean transparent, String url) {
        if (MainGame.instance == null) {
            throw new IllegalStateException(
                    "OraLightCEF must be created after MainGame has initialized");
        }

        ipc = new OraLightCEFIpc();
        OraLightCEFRuntime.initialize();

        client = OraLightCEFRuntime.app().createClient();
        client.addLoadHandler(new LoadHandler(this, ipc));
        client.addLifeSpanHandler(new LifeSpanHandler(this));
        client.addDisplayHandler(new DisplayHandler(this));
        client.addAudioHandler(audioSink); // 网页音频传出 → OpenAL 流式播放

        CefBrowserSettings settings = new CefBrowserSettings();
        var vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        settings.windowless_frame_rate = vidMode != null ? vidMode.refreshRate() : 60;

        browser = new JcefBrowser(client, url, transparent, settings);
        // 必须在 createImmediately 之前 attach：native 创建浏览器时会把
        // message router 配置塞进 extra_info 传给 renderer 进程，
        // 晚了的话 renderer 侧没有 CefMessageRouterRendererSide，
        // window.cefQuery 就不会被挂载，IPC 静默失效。
        ipc.attach(client, browser);
        browser.createImmediately(url);
        // MTML：CEF 消息循环在独立线程，无需（也不应）调用 doMessageLoopWork
        pendingUrl = url; // 供 onLoadEnd 的 about:blank 兜底重试
        browser.setFocus(true);
        browser.resizeBrowser(Math.max(1, width), Math.max(1, height));

        paintBuffer = ByteBuffer.allocateDirect(width * height * 4);
    }

    public void loadURL(String url) {
        if (browser == null) return;
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("chrome://")
                && !url.startsWith("data:") && !url.startsWith("about:")) {
            url = "https://" + url;
        }
        pendingUrl = url;
        browser.loadURL(url);
    }

    /**
     * 兜底：浏览器创建与 loadURL 存在时序竞态（createImmediately 是异步的），
     * 偶发会丢失导航停在 about:blank。主框架加载完成后若仍是 about:blank 且
     * 有目标 URL 未确认加载，则重新导航。
     */
    private void retryIfBlank(CefBrowser b) {
        String target = pendingUrl;
        if (target == null || target.startsWith("about:")) return;
        String cur;
        try {
            cur = b.getURL();
        } catch (Throwable t) {
            return;
        }
        if (cur == null || cur.isEmpty()) return;       // 浏览器尚未创建完成，等下一次回调
        if (cur.equals("about:blank") || cur.equals("about:")) {
            b.loadURL(target);
        }
    }

    public void loadHTML(String html) {
        String encoded = Base64.getEncoder().encodeToString(
                html.getBytes(StandardCharsets.UTF_8));
        loadURL("data:text/html;charset=utf-8;base64," + encoded);
    }

    public void reload() {
        if (browser != null) browser.reload();
    }

    public String getUrl() {
        return browser != null ? browser.getURL() : "";
    }

    public boolean isLoading() {
        return isLoading;
    }

    public float getLoadProgress() {
        return loadProgress.get();
    }

    public void setProgressBarVisible(boolean visible) {
        this.progressBarVisible = visible;
    }

    public boolean isProgressBarVisible() {
        return progressBarVisible;
    }

    public void resize(int width, int height) {
        if (browser != null) {
            browser.resizeBrowser(Math.max(1, width), Math.max(1, height));
        }
        nvgImageDirty = true;
    }

    public void update() {}

    public void render(long vg, float x, float y, float viewWidth, float viewHeight) {
        if (viewWidth <= 0f || viewHeight <= 0f) return;

        // Consume pending paint frame from CEF（无新帧时跳过，避免空闲帧的 PBO map/unmap 抖动）
        if (browser.peekDirtyStats(outW, outH, dirtyCount, dirtyArea)) {
            uploadPaintToTexture(outW[0], outH[0]);
            int needed = outW[0] * outH[0] * 4;
            if (paintBuffer == null || paintBuffer.capacity() < needed) {
                paintBuffer = ByteBuffer.allocateDirect(needed);
            }
        }

        // Recreate NVG image only when needed
        if (nvgImageDirty && textureId != 0) {
            if (nvgImage != 0) {
                nvgDeleteImage(vg, nvgImage);
                nvgImage = 0;
            }
            nvgImage = nvglCreateImageFromHandle(vg, textureId,
                    lastPaintWidth, lastPaintHeight, NVG_IMAGE_NODELETE);
            textureLocation.setId(nvgImage);
            nvgImageDirty = false;
        }

        if (nvgImage != 0) {
            RenderUtils.drawImage(vg, textureLocation, x, y, viewWidth, viewHeight,
                    255, Color.WHITE);
        }

        if (!closed) {
            renderProgressBar(vg, x, y, viewWidth);
        }
    }

    public void on(String channel, OraLightCEFBackend backend) {
        ipc.on(channel, backend);
    }

    public void emit(String channel, String payload) {
        ipc.emitToJavaScript(channel, payload);
    }

    public String evaluateScript(String script) {
        if (browser != null) browser.executeJavaScript(script, "evaluate", 0);
        return null;
    }

    public void mouseMove(float x, float y) {
        lastMouseX = x;
        lastMouseY = y;
        if (browser == null) return;
        if (heldMouseButton != 0) {
            browser.injectMouseButton((int) x, (int) y, currentModifiers(),
                    heldMouseButton, true); // drag = press + move
        } else {
            browser.injectMouseMove((int) x, (int) y, 0);
        }
    }

    public void mouseButton(int button, int action, Vector2f mousePos) {
        if (browser == null) return;

        int cefButton = switch (button) {
            case GLFW_MOUSE_BUTTON_LEFT -> 0;
            case GLFW_MOUSE_BUTTON_MIDDLE -> 1;
            case GLFW_MOUSE_BUTTON_RIGHT -> 2;
            default -> -1;
        };
        if (cefButton == -1) return;

        lastMouseX = mousePos.x;
        lastMouseY = mousePos.y;
        boolean pressed = (action == GLFW_PRESS);
        heldMouseButton = pressed ? cefButton : 0;
        browser.injectMouseButton((int) lastMouseX, (int) lastMouseY,
                currentModifiers(), cefButton, pressed);
    }

    public void scroll(double xOffset, double yOffset) {
        if (browser != null) {
            browser.injectMouseWheel((int) lastMouseX, (int) lastMouseY,
                    currentModifiers(), yOffset * 4);
        }
    }

    private int heldMouseButton;

    public void key(int key, int scancode, int action, int mods) {
        if (action != GLFW_PRESS && action != GLFW_RELEASE && action != GLFW_REPEAT) return;
        if (browser == null) return;

        boolean shiftHeld = (mods & GLFW_MOD_SHIFT) != 0;
        if (action == GLFW_PRESS) {
            if (shiftHeld && key == GLFW_KEY_F5) {
                reload();
                return;
            }
            if (shiftHeld && key == GLFW_KEY_F12) {
                String input = JOptionPane.showInputDialog(null,
                        "输入网址:", browser.getURL(),
                        JOptionPane.QUESTION_MESSAGE);
                if (input != null && !input.trim().isEmpty()) {
                    loadURL(input.trim());
                }
                return;
            }
        }

        int vkCode = OraLightCEFKeyboard.keyCode(key);
        int glfwMods = OraLightCEFKeyboard.glfwModsToAwt(mods);

        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            browser.injectKeyPress(vkCode, '￿', glfwMods, scancode);
        } else {
            char keyChar = mapKeyChar(key, mods);
            browser.injectKeyRelease(vkCode, keyChar, glfwMods, scancode);
        }
    }

    public void character(int codepoint) {
        if (browser != null) {
            browser.injectKeyTyped((char) codepoint, 0);
        }
    }

    public boolean hasInputFocus() {
        return browser != null;
    }

    @Override
    public void close() {
        if (closed) return;
        audioSink.stop(); // 释放本实例音频流（source/buffer）
        if (nvgImage != 0 && MainGame.instance != null) {
            nvgDeleteImage(MainGame.instance.vg, nvgImage);
            nvgImage = 0;
        }
        if (pboIds != null) {
            for (int pbo : pboIds) {
                glDeleteBuffers(pbo);
            }
            pboIds = null;
            pboCapacity = 0;
        }
        if (browser != null) {
            try {
                browser.close(true);
            } catch (Exception ignored) {}
        }
        closed = true;
    }

    // --- Internal ---

    private static final int MAX_DIRTY_RECTS = 16;
    /** 脏区面积占比超过该值走 PBO 整帧异步上传（碎小 rect 多/大面积时逐块更贵） */
    private static final float FULL_UPLOAD_AREA_RATIO = 0.25f;

    private void uploadPaintToTexture(int width, int height) {
        if (textureId == 0) {
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                    GL_BGRA, GL_UNSIGNED_BYTE, 0L);
            lastPaintWidth = width;
            lastPaintHeight = height;
            needsFullUpload = true;
            browser.requestFullCopyNextPaint(); // 纹理全新，下一帧必须整帧
        } else if (width != lastPaintWidth || height != lastPaintHeight) {
            // Size changed: recreate GL texture, mark NVG image dirty
            nvgImageDirty = true;
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                    GL_BGRA, GL_UNSIGNED_BYTE, 0L);
            lastPaintWidth = width;
            lastPaintHeight = height;
            needsFullUpload = true;
            browser.requestFullCopyNextPaint();
        } else {
            glBindTexture(GL_TEXTURE_2D, textureId);
        }

        // 路径决策：脏区小 → 只拷脏区像素 + 同步局部上传（空闲页/时钟文本等
        // 周期小重绘，避免整帧 8MB 拷贝）；脏区大/首帧 → PBO 整帧异步上传。
        int count = dirtyCount[0];
        long area = dirtyArea[0];
        boolean useFull = needsFullUpload || count == 0 || count > MAX_DIRTY_RECTS
                || area > (long) ((long) width * height * FULL_UPLOAD_AREA_RATIO);
        if (useFull) {
            uploadFullPbo(width, height);
        } else {
            uploadRegions(width, height);
        }
    }

    /** PBO 双缓冲整帧异步上传：帧数据直接写入映射内存（单次拷贝），DMA 不阻塞渲染线程 */
    private void uploadFullPbo(int width, int height) {
        int needed = width * height * 4;
        ensurePbo(needed);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboIds[pboIndex]);
        ByteBuffer mapped = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, needed,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        if (mapped != null) {
            if (browser.consumeFrameTo(mapped, outW, outH)) {
                glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
                browser.consumeDirtyRects(); // 整帧已上传，丢弃累积脏区
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, outW[0], outH[0],
                        GL_BGRA, GL_UNSIGNED_BYTE, 0L); // PBO 绑定中，0L = 偏移
                needsFullUpload = false;
            } else {
                // 帧尺寸在 peek 后变大（resize 竞态）：容量不足，本帧放弃，下帧重试
                glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
            }
        } else {
            // 映射失败兜底：退回同步整帧上传（经 paintBuffer）
            if (browser.consumeFrame(paintBuffer, outW, outH)) {
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height,
                        GL_BGRA, GL_UNSIGNED_BYTE, paintBuffer);
                needsFullUpload = false;
                browser.consumeDirtyRects(); // 已整帧上传，丢弃累积脏区
            }
        }
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        pboIndex ^= 1;
    }

    /** 脏区局部上传：只拷脏区像素到暂存缓冲，逐块同步上传（小块同步开销可忽略） */
    private void uploadRegions(int width, int height) {
        int needed = width * height * 4;
        if (stagingBuffer == null || stagingCapacity < needed) {
            stagingBuffer = ByteBuffer.allocateDirect(needed);
            stagingCapacity = needed;
        }
        // rectData 复用时清零：consumeFrameRegions 可能写入更少的块，残留的旧
        // 条目（w>0）会被误当真实 rect 从暂存缓冲错位读取 → 花屏
        Arrays.fill(rectData, 0);
        if (!browser.consumeFrameRegions(stagingBuffer, outW, outH, rectData)) {
            return; // 竞态后帧/脏区变化，保持 pending，下帧重试
        }
        for (int i = 0; i < rectData.length; i += 5) {
            int x = rectData[i], y = rectData[i + 1];
            int w = rectData[i + 2], h = rectData[i + 3];
            if (w <= 0) break; // 结束标记
            stagingBuffer.position(rectData[i + 4]);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h,
                    GL_BGRA, GL_UNSIGNED_BYTE, stagingBuffer);
        }
        needsFullUpload = false;
    }

    private void ensurePbo(int needed) {
        if (pboIds != null && pboCapacity >= needed) {
            return;
        }
        if (pboIds != null) {
            for (int pbo : pboIds) {
                glDeleteBuffers(pbo);
            }
        }
        pboIds = new int[2];
        pboIds[0] = glGenBuffers();
        pboIds[1] = glGenBuffers();
        pboIndex = 0;
        for (int pbo : pboIds) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
            glBufferData(GL_PIXEL_UNPACK_BUFFER, needed, GL_STREAM_DRAW);
        }
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        pboCapacity = needed;
    }

    private int currentModifiers() {
        int mods = 0;
        long window = MainGame.instance.window;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
            mods |= GLFW_MOD_SHIFT;
        if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS)
            mods |= GLFW_MOD_CONTROL;
        return mods;
    }

    private static char mapKeyChar(int key, int mods) {
        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
        return switch (key) {
            case GLFW_KEY_SPACE -> ' ';
            case GLFW_KEY_APOSTROPHE -> shift ? '"' : '\'';
            case GLFW_KEY_COMMA -> shift ? '<' : ',';
            case GLFW_KEY_MINUS -> shift ? '_' : '-';
            case GLFW_KEY_PERIOD -> shift ? '>' : '.';
            case GLFW_KEY_SLASH -> shift ? '?' : '/';
            case GLFW_KEY_0 -> shift ? ')' : '0';
            case GLFW_KEY_1 -> shift ? '!' : '1';
            case GLFW_KEY_2 -> shift ? '@' : '2';
            case GLFW_KEY_3 -> shift ? '#' : '3';
            case GLFW_KEY_4 -> shift ? '$' : '4';
            case GLFW_KEY_5 -> shift ? '%' : '5';
            case GLFW_KEY_6 -> shift ? '^' : '6';
            case GLFW_KEY_7 -> shift ? '&' : '7';
            case GLFW_KEY_8 -> shift ? '*' : '8';
            case GLFW_KEY_9 -> shift ? '(' : '9';
            case GLFW_KEY_SEMICOLON -> shift ? ':' : ';';
            case GLFW_KEY_EQUAL -> shift ? '+' : '=';
            case GLFW_KEY_LEFT_BRACKET -> shift ? '{' : '[';
            case GLFW_KEY_RIGHT_BRACKET -> shift ? '}' : ']';
            case GLFW_KEY_BACKSLASH -> shift ? '|' : '\\';
            case GLFW_KEY_GRAVE_ACCENT -> shift ? '~' : '`';
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> '\n';
            case GLFW_KEY_TAB -> '\t';
            case GLFW_KEY_KP_0 -> '0';
            case GLFW_KEY_KP_1 -> '1';
            case GLFW_KEY_KP_2 -> '2';
            case GLFW_KEY_KP_3 -> '3';
            case GLFW_KEY_KP_4 -> '4';
            case GLFW_KEY_KP_5 -> '5';
            case GLFW_KEY_KP_6 -> '6';
            case GLFW_KEY_KP_7 -> '7';
            case GLFW_KEY_KP_8 -> '8';
            case GLFW_KEY_KP_9 -> '9';
            case GLFW_KEY_KP_DECIMAL -> '.';
            case GLFW_KEY_KP_ADD -> '+';
            case GLFW_KEY_KP_SUBTRACT -> '-';
            case GLFW_KEY_KP_MULTIPLY -> '*';
            case GLFW_KEY_KP_DIVIDE -> '/';
            case GLFW_KEY_KP_EQUAL -> '=';
            default -> '￿';
        };
    }

    private void renderProgressBar(long vg, float x, float y, float viewWidth) {
        if (!progressBarVisible) return;
        float progress = Math.min(loadProgress.get(), 1.0f);
        if (!isLoading && progress >= 0.999f) return;
        float barHeight = 3f;
        RenderUtils.drawRect(vg, x, y, x + viewWidth, y + barHeight,
                new Color(0, 0, 0, 120));
        RenderUtils.drawRect(vg, x, y, x + viewWidth * progress, y + barHeight,
                new Color(255, 120, 0));
    }

    // --- Handlers ---

    /**
     * 网页音频传出：CEF 离屏渲染无系统音频设备，音频回调（CEF UI 线程）把
     * float32 交错 PCM 转发给 AudioManager 流式播放（OpenAL 提交在渲染线程）。
     */
    private class CefAudioSink implements CefAudioHandler {
        private AudioManager.AudioStream stream;
        /** 官方 JCEF 在 onAudioStreamStarted 传 null params，但从 getAudioParameters 可拿到真实采样率 */
        private volatile int lastSampleRate = 48000;

        @Override
        public boolean getAudioParameters(CefBrowser browser, CefAudioParameters params) {
        try {

            if (params != null && params.sampleRate > 0) {
                lastSampleRate = params.sampleRate;
            }
            return true; // 接受 CEF 提供的采样率/缓冲配置
                } catch (Throwable t) {
            // CEF 线程回调兜底
            return false;
        }
}

        @Override
        public void onAudioStreamStarted(CefBrowser browser, CefAudioParameters params, int channels) {
        try {

            AudioManager am = OrangeEngine.audioManager;
            if (am == null || !am.isAvailable()) return;
            int rate = lastSampleRate;
            int ch = Math.max(1, channels);
            if (stream != null) {
                am.stopStream(audioStreamKey); // 流重启（loop/seek）时清理旧流
                stream = null;
            }
            stream = am.startStream(audioStreamKey, rate, ch);
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onAudioStreamPacket(CefBrowser browser, DataPointer data, int frames, long pts) {
        try {

            if (stream == null || frames <= 0) return;
            int channels = stream.channels;
            // CEF 音频为 planar 布局：data 是指向各通道 float 缓冲的指针数组 (float**)
            long base = data.getAddress();
            long[] chPtrs = new long[channels];
            for (int c = 0; c < channels; c++) {
                chPtrs[c] = MemoryUtil.memGetAddress(base + c * 8L);
            }
            // planar -> interleaved float32（必须小端，OpenAL 期望 LE）
            ByteBuffer out = ByteBuffer.allocateDirect(frames * channels * 4)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN);
            float peak = 0f;
            for (int i = 0; i < frames; i++) {
                for (int c = 0; c < channels; c++) {
                    float s = MemoryUtil.memGetFloat(chPtrs[c] + i * 4L);
                    float a = Math.abs(s);
                    if (a > peak) peak = a;
                    out.putFloat(s);
                }
            }
            out.flip();
            // Chromium 输出可能超过 [-1,1]（响处），硬削顶会损失低频/产生失真：
            // 超范围时整体归一化（保持动态范围，避免削波）
            if (peak > 1.0f) {
                float scale = 1.0f / peak;
                out.rewind();
                while (out.hasRemaining()) {
                    out.putFloat(out.getFloat() * scale);
                }
                out.flip();
            }
            AudioManager am = OrangeEngine.audioManager;
            if (am != null) {
                am.streamQueue(stream, out);
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onAudioStreamStopped(CefBrowser browser) {
        try {

            stop();
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onAudioStreamError(CefBrowser browser, String text) {
        try {

            stop();
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        private void stop() {
            if (stream != null) {
                AudioManager am = OrangeEngine.audioManager;
                if (am != null) am.stopStream(audioStreamKey);
                stream = null;
            }
        }
    }

    private static class LoadHandler extends CefLoadHandlerAdapter {
        private final OraLightCEF ul;
        private final OraLightCEFIpc ipc;

        LoadHandler(OraLightCEF ul, OraLightCEFIpc ipc) {
            this.ul = ul;
            this.ipc = ipc;
        }

        @Override
        public void onLoadingStateChange(CefBrowser browser, boolean loading,
                                         boolean canGoBack, boolean canGoForward) {
        try {

            if (loading) {
                ul.isLoading = true;
                ul.loadProgress.set(0.10f);
            } else {
                ul.isLoading = false;
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onLoadStart(CefBrowser browser, CefFrame frame,
                                TransitionType transitionType) {
        try {

            if (frame.isMain()) {
                ul.isLoading = true;
                ul.loadProgress.to(0.70f, 3000, Motion.Easing.SINE_IN_OUT);
                ipc.installJavaScriptBridge();
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onLoadEnd(CefBrowser browser, CefFrame frame,
                              int httpStatusCode) {
        try {

            if (frame.isMain()) {
                ul.isLoading = false;
                ul.loadProgress.to(1.0f, 300, Motion.Easing.QUAD_OUT);
                ipc.installJavaScriptBridge();
                ul.retryIfBlank(browser); // about:blank 兜底重试（创建竞态丢导航）
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onLoadError(CefBrowser browser, CefFrame frame,
                                ErrorCode errorCode, String errorText,
                                String failedUrl) {
        try {

            if (frame.isMain()) {
                ul.isLoading = false;
                ul.loadProgress.to(1.0f, 300, Motion.Easing.QUAD_OUT);
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}
    }

    private static class LifeSpanHandler extends CefLifeSpanHandlerAdapter {
        private final OraLightCEF ul;

        LifeSpanHandler(OraLightCEF ul) {
            this.ul = ul;
        }

        @Override
        public boolean onBeforePopup(CefBrowser browser, CefFrame frame,
                                      String target_url, String target_frame_name) {
            ul.loadURL(target_url);
            return true;
        }

        @Override
        public boolean doClose(CefBrowser browser) {
            return false;
        }

        @Override
        public void onBeforeClose(CefBrowser browser) {
            ul.closed = true;
        }
    }

    private static class DisplayHandler extends CefDisplayHandlerAdapter {
        private final OraLightCEF ul;

        DisplayHandler(OraLightCEF ul) {
            this.ul = ul;
        }

        @Override
        public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
        try {

            if (frame.isMain()) {
                ul.isLoading = true;
                ul.loadProgress.set(0.0f);
                ul.loadProgress.to(0.70f, 3000, Motion.Easing.SINE_IN_OUT);
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}

        @Override
        public void onTitleChange(CefBrowser browser, String title) {
        try {

            if (title != null && title.startsWith("CODEC:")) {
                System.out.println("[CODEC] " + title);
            }
                } catch (Throwable t) {
            // CEF 线程回调兜底
        }
}
    }

    @SuppressWarnings("unused")
    private static String buildUserAgent() {
        // Handled by CEF defaults now
        return "";
    }
}
