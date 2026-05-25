package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.event.CefKeyEvent;
import org.cef.event.CefMouseEvent;
import org.cef.event.CefMouseWheelEvent;
import org.cef.handler.CefAcceleratedPaintInfo;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * JOGAMP-free off-screen CEF browser. Uses a plain AWT Canvas (not GLCanvas)
 * inside a hidden Frame to provide a valid native HWND for the standard
 * jcef.dll JAWT code.
 *
 * Must live in org.cef.browser to access package-private CefBrowser_N members.
 */
public class JcefBrowser extends CefBrowser_N implements CefRenderHandler {

    private final Rectangle viewRect = new Rectangle(0, 0, 1, 1);
    private final Point screenPoint = new Point(0, 0);
    private final Canvas canvas;
    private final Frame dummyFrame;
    private final boolean transparent;

    private final Object paintLock = new Object();
    private ByteBuffer pendingBuffer;
    private int pendingWidth;
    private int pendingHeight;
    private boolean hasPendingFrame;

    public JcefBrowser(CefClient client, String url, boolean transparent) {
        this(client, url, transparent, new CefBrowserSettings());
    }

    public JcefBrowser(CefClient client, String url, boolean transparent, CefBrowserSettings settings) {
        super(client, url, null, null, null, settings);
        this.transparent = transparent;

        // Frame must stay alive — Canvas peer depends on it for valid HWND.
        dummyFrame = new Frame("Orange-JCEF");
        canvas = new Canvas();
        dummyFrame.add(canvas);
        dummyFrame.pack();
    }

    @Override
    public void createImmediately() {
        createImmediately(getUrl());
    }

    public void createImmediately(String url) {
        if (getNativeRef("CefBrowser") == 0) {
            createBrowser(getClient(), 0, url, true, transparent,
                    getRequestContext());
        }
        setFocus(true);
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
                                                  CefRequestContext context,
                                                  CefBrowser_N parent, Point inspectAt) {
        return null;
    }


    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return viewRect;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        return new Point(viewPoint);
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        return false;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {}

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {}

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        if (popup) return;
        int size = width * height * 4;
        synchronized (paintLock) {
            if (buffer.limit() > size) return;
            if (pendingBuffer == null || size != pendingBuffer.capacity()) {
                pendingBuffer = ByteBuffer.allocateDirect(size);
            }
            pendingBuffer.clear();
            buffer.clear();
            pendingBuffer.put(buffer);
            pendingBuffer.clear();
            pendingWidth = width;
            pendingHeight = height;
            hasPendingFrame = true;
        }
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {}

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {}

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {}

    @Override
    public void onAcceleratedPaint(CefBrowser browser, boolean popup,
                                    Rectangle[] dirtyRects, CefAcceleratedPaintInfo info) {}

    @Override
    public void addOnAcceleratedPaintListener(Consumer<CefAcceleratedPaintEvent> listener) {}

    @Override
    public void setOnAcceleratedPaintListener(Consumer<CefAcceleratedPaintEvent> listener) {}

    @Override
    public void removeOnAcceleratedPaintListener(Consumer<CefAcceleratedPaintEvent> listener) {}

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        return false;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData,
                                  int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {}

    // --- Public API ---

    public boolean consumeFrame(ByteBuffer dst, int[] outWidth, int[] outHeight) {
        synchronized (paintLock) {
            if (!hasPendingFrame) return false;
            int size = pendingWidth * pendingHeight * 4;
            outWidth[0] = pendingWidth;
            outHeight[0] = pendingHeight;
            if (dst.capacity() < size) return false;
            dst.clear();
            pendingBuffer.clear();
            dst.limit(size);
            dst.put(pendingBuffer);
            dst.clear();
            hasPendingFrame = false;
            return true;
        }
    }

    public void resizeBrowser(int width, int height) {
        viewRect.setBounds(0, 0, width, height);
        wasResized(width, height);
    }

    public void injectMouseMove(int x, int y, int mods) {
        sendMouseEvent(new CefMouseEvent(CefMouseEvent.MOUSE_MOVED, x, y, 0, 0, mods));
    }

    public void injectMouseButton(int x, int y, int mods, int btn, boolean pressed) {
        int buttonFlag = switch (btn) {
            case 0 -> CefMouseEvent.BUTTON1_MASK;
            case 1 -> CefMouseEvent.BUTTON2_MASK;
            case 2 -> CefMouseEvent.BUTTON3_MASK;
            default -> 0;
        };
        sendMouseEvent(new CefMouseEvent(
                pressed ? 1 : 0, x, y, 1, btn, mods | buttonFlag));
    }

    public void injectMouseWheel(int x, int y, int mods, double delta) {
        sendMouseWheelEvent(new CefMouseWheelEvent(
                CefMouseWheelEvent.WHEEL_UNIT_SCROLL, x, y, delta, mods));
    }

    public void injectKeyPress(int vkCode, char keyChar, int mods) {
        injectKeyPress(vkCode, keyChar, mods, 0);
    }

    public void injectKeyPress(int vkCode, char keyChar, int mods, int scanCode) {
        setFocus(true);
        CefKeyEvent ev = new CefKeyEvent(CefKeyEvent.KEY_PRESS, vkCode, keyChar, mods);
        ev.scancode = scanCode;
        sendKeyEvent(ev);
    }

    public void injectKeyRelease(int vkCode, char keyChar, int mods) {
        sendKeyEvent(new CefKeyEvent(CefKeyEvent.KEY_RELEASE, vkCode, keyChar, mods));
    }

    public void injectKeyTyped(char c, int mods) {
        injectKeyTyped(0, c, mods);
    }

    public void injectKeyTyped(int vkCode, char c, int mods) {
        sendKeyEvent(new CefKeyEvent(CefKeyEvent.KEY_TYPE, vkCode, c, mods));
    }

    @Override
    public void close(boolean force) {
        try { super.close(force); } catch (Exception ignored) {}
        if (dummyFrame != null) {
            try { dummyFrame.dispose(); } catch (Exception ignored) {}
        }
    }
}
