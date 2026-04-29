package cms.mortalchen.orange.ultralight;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.animatednumber.AnimatedFloat;
import net.janrupf.ujr.api.UltralightKeyEventBuilder;
import net.janrupf.ujr.api.UltralightMouseEventBuilder;
import net.janrupf.ujr.api.UltralightScrollEventBuilder;
import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.UltralightViewConfigBuilder;
import net.janrupf.ujr.api.event.UlKeyEvent;
import net.janrupf.ujr.api.event.UlKeyEventType;
import net.janrupf.ujr.api.event.UlMouseButton;
import net.janrupf.ujr.api.event.UlScrollEventType;
import org.joml.Vector2f;

import java.awt.Color;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;
import static org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle;

public class UltraLight implements AutoCloseable {
    private static final int NVG_IMAGE_NODELETE = 1 << 16;

    private final UltralightView view;
    private final UltraLightIpc ipc;
    private final ResourceLocation textureLocation = new ResourceLocation("__ultralight_runtime__");

    private int nvgImage;
    private int wrappedTexture;
    private int wrappedWidth;
    private int wrappedHeight;
    private float lastMouseX;
    private float lastMouseY;
    private boolean closed;

    boolean isLoading;
    final AnimatedFloat loadProgress = new AnimatedFloat(0f);
    private boolean progressBarVisible = true;

    public UltraLight(int width, int height) {
        this(width, height, true);
    }

    public UltraLight(int width, int height, boolean transparent) {
        if (MainGame.instance == null) {
            throw new IllegalStateException("UltraLight must be created after MainGame has initialized the GLFW window");
        }

        UltraLightRuntime runtime = UltraLightRuntime.getOrCreate(MainGame.instance.window);
        view = runtime.renderer().createView(Math.max(1, width), Math.max(1, height),
                new UltralightViewConfigBuilder()
                        .userAgent(buildUserAgent())
                        .transparent(transparent)
                        .build());

        ipc = new UltraLightIpc();
        ipc.attach(view);

        UltraLightViewListener listener = new UltraLightViewListener(ipc, this);
        view.setViewListener(listener);
        view.setLoadListener(listener);
        view.focus();
    }

    public void loadURL(String url) {
        view.loadURL(url);
    }

    public void loadHTML(String html) {
        view.loadHTML(html);
    }

    UltralightView createChildView(int width, int height) {
        UltraLightRuntime runtime = UltraLightRuntime.getOrCreate(MainGame.instance.window);
        return runtime.renderer().createView(Math.max(1, width), Math.max(1, height),
                new UltralightViewConfigBuilder()
                        .transparent(true)
                        .build());
    }

    public void reload() {
        view.reload();
    }

    public String getUrl() {
        return view.url();
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
        view.resize(Math.max(1, width), Math.max(1, height));
        releaseNanoVGImage();
    }

    public void update() {
        UltraLightRuntime.updateAndRenderActive();
    }

    public void render(long vg, float x, float y, float width, float height) {
        OrangeUltralightSurface surface = surface();
        if (surface == null || surface.texture() == 0 || width <= 0f || height <= 0f) {
            return;
        }

        if (nvgImage == 0
                || wrappedTexture != surface.texture()
                || wrappedWidth != surface.width()
                || wrappedHeight != surface.height()) {
            releaseNanoVGImage();
            wrappedTexture = surface.texture();
            wrappedWidth = (int) surface.width();
            wrappedHeight = (int) surface.height();
            nvgImage = nvglCreateImageFromHandle(vg, wrappedTexture, wrappedWidth, wrappedHeight, NVG_IMAGE_NODELETE);
            textureLocation.setId(nvgImage);
        }

        RenderUtils.drawImage(vg, textureLocation, x, y, width, height, 255, Color.WHITE);

        if (!closed) {
            renderProgressBar(vg, x, y, width);
        }
    }

    public void on(String channel, UltraLightBackend backend) {
        ipc.on(channel, backend);
    }

    public void emit(String channel, String payload) {
        ipc.emitToJavaScript(channel, payload);
    }

    public String evaluateScript(String script) {
        try {
            return view.evaluateScript(script);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to evaluate Ultralight script", e);
        }
    }

    public void mouseMove(float x, float y) {
        lastMouseX = x;
        lastMouseY = y;
        view.fireMouseEvent(UltralightMouseEventBuilder.moved()
                .x((int) x)
                .y((int) y)
                .button(currentMouseButton())
                .build());
    }

    public void mouseButton(int button, int action, Vector2f mousePos) {
        UlMouseButton ulButton = switch (button) {
            case GLFW_MOUSE_BUTTON_LEFT -> UlMouseButton.LEFT;
            case GLFW_MOUSE_BUTTON_RIGHT -> UlMouseButton.RIGHT;
            case GLFW_MOUSE_BUTTON_MIDDLE -> UlMouseButton.MIDDLE;
            default -> UlMouseButton.NONE;
        };
        if (ulButton == UlMouseButton.NONE) {
            return;
        }

        lastMouseX = mousePos.x;
        lastMouseY = mousePos.y;
        UltralightMouseEventBuilder builder = action == GLFW_PRESS
                ? UltralightMouseEventBuilder.down(ulButton)
                : UltralightMouseEventBuilder.up(ulButton);
        view.fireMouseEvent(builder.x((int) lastMouseX).y((int) lastMouseY).build());
    }

    public void scroll(double xOffset, double yOffset) {
        view.fireScrollEvent(new UltralightScrollEventBuilder(UlScrollEventType.BY_PIXEL)
                .deltaX((int) (xOffset * 50))
                .deltaY((int) (yOffset * 50))
                .build());
    }

    public void key(int key, int scancode, int action, int mods) {
        if (action != GLFW_PRESS && action != GLFW_RELEASE && action != GLFW_REPEAT) {
            return;
        }

        boolean shiftHeld = (mods & GLFW_MOD_SHIFT) != 0;
        if (action == GLFW_PRESS) {
            if (shiftHeld && key == GLFW_KEY_F5) {
                view.reload();
                return;
            }
            if (shiftHeld && key == GLFW_KEY_F12) {
                String input = javax.swing.JOptionPane.showInputDialog(null,
                        "输入网址:", view.url(),
                        javax.swing.JOptionPane.PLAIN_MESSAGE);
                if (input != null && !input.trim().isEmpty()) {
                    view.loadURL(input.trim());
                }
                return;
            }
        }

        if (action != GLFW_PRESS && action != GLFW_RELEASE) {
            return;
        }

        UltralightKeyEventBuilder builder = action == GLFW_PRESS
                ? UltralightKeyEventBuilder.rawDown()
                : UltralightKeyEventBuilder.up();

        builder.nativeKeyCode(scancode)
                .virtualKeyCode(UltraLightKeyboard.keyCode(key))
                .keyIdentifier(UlKeyEvent.keyIdentifierFromVirtualKeyCode(builder.virtualKeyCode))
                .modifiers(UltraLightKeyboard.modifiers(mods));

        view.fireKeyEvent(builder.build());

        if (builder.type == UlKeyEventType.RAW_DOWN && (key == GLFW_KEY_ENTER || key == GLFW_KEY_TAB)) {
            String text = key == GLFW_KEY_ENTER ? "\n" : "\t";
            view.fireKeyEvent(UltralightKeyEventBuilder.character()
                    .unmodifiedText(text)
                    .text(text)
                    .build());
        }
    }

    public void character(int codepoint) {
        String text = new String(Character.toChars(codepoint));
        view.fireKeyEvent(UltralightKeyEventBuilder.character()
                .unmodifiedText(text)
                .text(text)
                .build());
    }

    public boolean hasInputFocus() {
        return view.hasInputFocus();
    }

    public UltralightView view() {
        return view;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        releaseNanoVGImage();
        closed = true;
    }

    private UlMouseButton currentMouseButton() {
        long window = MainGame.instance.window;
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            return UlMouseButton.LEFT;
        }
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            return UlMouseButton.RIGHT;
        }
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS) {
            return UlMouseButton.MIDDLE;
        }
        return UlMouseButton.NONE;
    }

    private OrangeUltralightSurface surface() {
        if (view.surface() instanceof OrangeUltralightSurface surface) {
            return surface;
        }
        return null;
    }

    private void releaseNanoVGImage() {
        if (nvgImage != 0 && MainGame.instance != null) {
            nvgDeleteImage(MainGame.instance.vg, nvgImage);
        }
        nvgImage = 0;
        wrappedTexture = 0;
        textureLocation.setId(0);
    }

    private void renderProgressBar(long vg, float x, float y, float viewWidth) {
        if (!progressBarVisible) {
            return;
        }
        float progress = Math.min(loadProgress.get(), 1.0f);
        if (!isLoading && progress >= 0.999f) {
            return;
        }
        float barHeight = 3f;
        RenderUtils.drawRect(vg, x, y, x + viewWidth, y + barHeight,
                new Color(0, 0, 0, 120));
        RenderUtils.drawRect(vg, x, y, x + viewWidth * progress, y + barHeight,
                new Color(255, 120, 0));
    }

    private static String buildUserAgent() {
        String osName = System.getProperty("os.name", "");
        String osVersion = System.getProperty("os.version", "");
        String osArch = System.getProperty("os.arch", "");

        String platform;
        if (osName.startsWith("Windows")) {
            String archToken = osArch.contains("64") ? "Win64; x64" : "Win32";
            platform = String.format("Windows NT %s; %s", osVersion.isEmpty() ? "10.0" : osVersion, archToken);
        } else if (osName.startsWith("Mac")) {
            String macVersion = osVersion.replace('.', '_');
            platform = String.format("Macintosh; Intel Mac OS X %s", macVersion.isEmpty() ? "10_15_7" : macVersion);
        } else if (osName.startsWith("Linux")) {
            String arch = osArch.contains("64") ? "x86_64" : "i686";
            platform = String.format("X11; Linux %s", arch);
        } else {
            platform = String.format("%s %s; %s", osName, osVersion, osArch);
        }

        return String.format(
                "Mozilla/5.0 (%s) AppleWebKit/605.1.15 (KHTML, like Gecko) Ultralight/1.3.0 %s/%s Safari/605.1.15 UltralightJava/0.0.1",
                platform, OrangeEngine.engineName.replace(" ",""), OrangeEngine.version);
    }
}
