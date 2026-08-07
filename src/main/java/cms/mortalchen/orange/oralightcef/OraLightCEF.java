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
import java.util.Base64;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;
import static org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

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
        browser.createImmediately(url);
        // Process creation immediately so browser + URL are ready
        OraLightCEFRuntime.doMessageLoopWork();
        pendingUrl = url; // 供 onLoadEnd 的 about:blank 兜底重试
        browser.setFocus(true);
        browser.resizeBrowser(Math.max(1, width), Math.max(1, height));

        paintBuffer = ByteBuffer.allocateDirect(width * height * 4);

        ipc.attach(client, browser);
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

        // Consume pending paint frame from CEF
        if (browser.consumeFrame(paintBuffer, outW, outH)) {
            uploadPaintToTexture(outW[0], outH[0]);
        } else if (outW[0] > 0 && outH[0] > 0) {
            int needed = outW[0] * outH[0] * 4;
            paintBuffer = ByteBuffer.allocateDirect(needed);
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
            browser.injectKeyRelease(vkCode, keyChar, glfwMods);
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
        if (browser != null) {
            try {
                browser.close(true);
            } catch (Exception ignored) {}
        }
        closed = true;
    }

    // --- Internal ---

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
        } else if (width != lastPaintWidth || height != lastPaintHeight) {
            // Size changed: recreate GL texture, mark NVG image dirty
            nvgImageDirty = true;
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                    GL_BGRA, GL_UNSIGNED_BYTE, 0L);
            lastPaintWidth = width;
            lastPaintHeight = height;
        } else {
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
        paintBuffer.clear();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height,
                GL_BGRA, GL_UNSIGNED_BYTE, paintBuffer);
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
            if (params != null && params.sampleRate > 0) {
                lastSampleRate = params.sampleRate;
            }
            return true; // 接受 CEF 提供的采样率/缓冲配置
        }

        @Override
        public void onAudioStreamStarted(CefBrowser browser, CefAudioParameters params, int channels) {
            AudioManager am = OrangeEngine.audioManager;
            if (am == null || !am.isAvailable()) return;
            int rate = lastSampleRate;
            int ch = Math.max(1, channels);
            if (stream != null) {
                am.stopStream(audioStreamKey); // 流重启（loop/seek）时清理旧流
                stream = null;
            }
            stream = am.startStream(audioStreamKey, rate, ch);
        }

        @Override
        public void onAudioStreamPacket(CefBrowser browser, DataPointer data, int frames, long pts) {
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
        }

        @Override
        public void onAudioStreamStopped(CefBrowser browser) {
            stop();
        }

        @Override
        public void onAudioStreamError(CefBrowser browser, String text) {
            stop();
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
            if (loading) {
                ul.isLoading = true;
                ul.loadProgress.set(0.10f);
            } else {
                ul.isLoading = false;
            }
        }

        @Override
        public void onLoadStart(CefBrowser browser, CefFrame frame,
                                TransitionType transitionType) {
            if (frame.isMain()) {
                ul.isLoading = true;
                ul.loadProgress.to(0.70f, 3000, Motion.Easing.SINE_IN_OUT);
                ipc.installJavaScriptBridge();
            }
        }

        @Override
        public void onLoadEnd(CefBrowser browser, CefFrame frame,
                              int httpStatusCode) {
            if (frame.isMain()) {
                ul.isLoading = false;
                ul.loadProgress.to(1.0f, 300, Motion.Easing.QUAD_OUT);
                ipc.installJavaScriptBridge();
                ul.retryIfBlank(browser); // about:blank 兜底重试（创建竞态丢导航）
            }
        }

        @Override
        public void onLoadError(CefBrowser browser, CefFrame frame,
                                ErrorCode errorCode, String errorText,
                                String failedUrl) {
            if (frame.isMain()) {
                ul.isLoading = false;
                ul.loadProgress.to(1.0f, 300, Motion.Easing.QUAD_OUT);
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
            if (frame.isMain()) {
                ul.isLoading = true;
                ul.loadProgress.set(0.0f);
                ul.loadProgress.to(0.70f, 3000, Motion.Easing.SINE_IN_OUT);
            }
        }
    }

    @SuppressWarnings("unused")
    private static String buildUserAgent() {
        // Handled by CEF defaults now
        return "";
    }
}
