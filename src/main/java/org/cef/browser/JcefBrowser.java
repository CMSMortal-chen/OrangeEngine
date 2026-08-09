package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.callback.CefTaskRunner;
import org.cef.event.CefKeyEvent;
import org.cef.event.CefMouseEvent;
import org.cef.event.CefMouseWheelEvent;
import org.cef.handler.CefAcceleratedPaintInfo;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    /**
     * 自上次消费以来累积的脏区（CEF 每次 onPaint 给出相对上一次 paint 的
     * dirtyRects；若只存最后一次 paint 的 rects，两次 paint 之间消费一次会漏帧，
     * 必须累积并集）。消费方按并集局部上传。
     */
    private final List<Rectangle> pendingDirtyRects = new ArrayList<>();

    /** 单次 paint 脏区面积占比超过该值则整帧拷贝（大区域逐行稀疏拷贝反而更慢） */
    private static final float SPARSE_FULL_RATIO = 0.25f;
    /** 首帧/纹理重建后下一次 paint 必须整帧拷贝（稀疏叠加的前提是缓冲区已有完整帧） */
    private boolean fullCopyNextPaint = true;

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
        final String target = url;
        runOnCefThread(() -> {
            if (getNativeRef("CefBrowser") == 0) {
                createBrowser(getClient(), 0, target, true, transparent,
                        getRequestContext());
            }
            setFocus(true);
        });
    }

    /**
     * MTML 下 CEF UI 线程是独立线程：CEF 触碰调用必须封送到 UI 线程执行；
     * 已在 UI 线程则直接执行（避免 postTask 自等待死锁）。任务异常被吞掉，
     * 只影响该次调用，不逃逸进 native CEF 触发 JVM fatal。
     */
    private void runOnCefThread(Runnable task) {
        Runnable guarded = () -> {
            try {
                task.run();
            } catch (Throwable t) {
                // CEF 线程回调兜底：记录但不让异常污染 JNI 环境
            }
        };
        if (CefTaskRunner.isCefThread()) {
            guarded.run();
        } else {
            CefTaskRunner.postTask(() -> guarded.run(), CefTaskRunner.TID_UI);
        }
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
        try {
            doPaint(browser, popup, dirtyRects, buffer, width, height);
        } catch (Throwable t) {
            // CEF 线程回调兜底：异常不逃逸进 native CEF
        }
    }

    private void doPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                         ByteBuffer buffer, int width, int height) {
        if (popup) return;
        int size = width * height * 4;
        synchronized (paintLock) {
            if (buffer.limit() > size) return;
            boolean bufferFresh = (pendingBuffer == null || size != pendingBuffer.capacity());
            if (bufferFresh) {
                pendingBuffer = ByteBuffer.allocateDirect(size);
            }
            // 首帧/缓冲区重建必须整帧拷贝；小脏区只稀疏拷贝脏区行（叠加在
            // 上一帧数据上仍是完整帧），避免 CEF 消息循环（主线程）里每帧
            // 8.3MB memcpy 造成的周期停顿
            boolean fullCopy = fullCopyNextPaint || bufferFresh
                    || dirtyRects == null || dirtyRects.length == 0;
            if (!fullCopy) {
                long area = 0;
                for (Rectangle r : dirtyRects) {
                    area += (long) Math.max(0, r.width) * Math.max(0, r.height);
                }
                fullCopy = area > (long) (width * height * SPARSE_FULL_RATIO);
            }
            pendingBuffer.clear();
            buffer.clear();
            if (fullCopy) {
                pendingBuffer.put(buffer);
                fullCopyNextPaint = false;
            } else {
                // 稀疏拷贝：逐行按整帧行宽步进，只写脏区
                for (Rectangle r : dirtyRects) {
                    int x = Math.max(0, Math.min(r.x, width - 1));
                    int y = Math.max(0, Math.min(r.y, height - 1));
                    int w = Math.min(r.width, width - x);
                    int h = Math.min(r.height, height - y);
                    if (w <= 0 || h <= 0) continue;
                    for (int row = 0; row < h; row++) {
                        int off = ((y + row) * width + x) * 4;
                        buffer.position(off);
                        buffer.limit(off + w * 4);
                        pendingBuffer.position(off);
                        pendingBuffer.put(buffer);
                        buffer.limit(buffer.capacity()); // 恢复供下一行
                    }
                }
                buffer.clear();
                pendingBuffer.clear();
            }
            pendingWidth = width;
            pendingHeight = height;
            hasPendingFrame = true;
            if (dirtyRects != null) {
                if (fullCopy) {
                    // 整帧已写：清掉旧脏区，仅保留整帧矩形（消费侧走整帧路径）
                    pendingDirtyRects.clear();
                    pendingDirtyRects.add(new Rectangle(0, 0, width, height));
                } else {
                    for (Rectangle r : dirtyRects) {
                        pendingDirtyRects.add(r);
                    }
                }
            } else if (fullCopy) {
                pendingDirtyRects.clear();
                pendingDirtyRects.add(new Rectangle(0, 0, width, height));
            }
            // 上传长期停滞时防止脏区列表无限增长
            if (pendingDirtyRects.size() > 64) {
                pendingDirtyRects.clear();
                pendingDirtyRects.add(new Rectangle(0, 0, width, height));
            }
        }
    }

    /**
     * 取出并清空自上次消费以来累积的脏区并集（与最近一次 consumeFrame 的帧
     * 对应）。无脏区时返回 null。
     */
    public List<Rectangle> consumeDirtyRects() {
        synchronized (paintLock) {
            if (pendingDirtyRects.isEmpty()) {
                return null;
            }
            List<Rectangle> out = new ArrayList<>(pendingDirtyRects);
            pendingDirtyRects.clear();
            return out;
        }
    }

    /** 请求下一次 paint 整帧拷贝（纹理重建/尺寸变化后由调用方设置，防稀疏叠加悬空） */
    public void requestFullCopyNextPaint() {
        synchronized (paintLock) {
            fullCopyNextPaint = true;
        }
    }

    /**
     * 只读当前待处理帧尺寸与累积脏区统计（不消费）。用于决定整帧/局部上传路径。
     * 返回是否有待处理帧（无帧时调用方应跳过上传，避免空闲帧的 PBO map/unmap 抖动）。
     */
    public boolean peekDirtyStats(int[] outWidth, int[] outHeight,
                                  int[] outCount, long[] outArea) {
        synchronized (paintLock) {
            outWidth[0] = pendingWidth;
            outHeight[0] = pendingHeight;
            outCount[0] = pendingDirtyRects.size();
            long area = 0;
            for (Rectangle r : pendingDirtyRects) {
                area += (long) Math.max(0, r.width) * Math.max(0, r.height);
            }
            outArea[0] = area;
            return hasPendingFrame;
        }
    }

    /**
     * 原子消费「帧 + 累积脏区」：把脏区矩形（逐行按整帧行宽步进）拷贝到 dst
     * 连续内存，供局部上传。脏区取自 pendingDirtyRects（自上次消费以来的并集），
     * 与帧在同一锁内快照，不存在「帧已换但脏区是旧帧」的错配。
     * dst 容量须 >= 脏区字节和；outRectData 按 [x,y,w,h,offset]*n 输出每块的
     * 几何与在 dst 内的字节偏移（w<=0 表示结束）。脏区数超过 outRectData
     * 容量时返回 false（保持 pending，调用方下帧走整帧路径）。
     */
    public boolean consumeFrameRegions(ByteBuffer dst, int[] outWidth, int[] outHeight,
                                       int[] outRectData) {
        synchronized (paintLock) {
            if (!hasPendingFrame || pendingDirtyRects.isEmpty()) {
                return false;
            }
            int cap = outRectData.length / 5;
            if (pendingDirtyRects.size() > cap) {
                return false;
            }
            outWidth[0] = pendingWidth;
            outHeight[0] = pendingHeight;
            int rowBytes = pendingWidth * 4;
            int offset = 0;
            int idx = 0;
            for (Rectangle r : pendingDirtyRects) {
                int x = Math.max(0, Math.min(r.x, pendingWidth - 1));
                int y = Math.max(0, Math.min(r.y, pendingHeight - 1));
                int w = Math.min(r.width, pendingWidth - x);
                int h = Math.min(r.height, pendingHeight - y);
                if (w <= 0 || h <= 0) continue;
                int bytes = w * h * 4;
                if (offset + bytes > dst.capacity()) {
                    return false; // 竞态后脏区变大：帧保持 pending，下帧重试
                }
                for (int row = 0; row < h; row++) {
                    int src = ((y + row) * pendingWidth + x) * 4;
                    pendingBuffer.position(src);
                    pendingBuffer.limit(src + w * 4);
                    dst.position(offset);
                    dst.put(pendingBuffer);
                    pendingBuffer.limit(pendingBuffer.capacity()); // 恢复整帧行宽，供下一行
                    offset += w * 4;
                }
                outRectData[idx++] = x;
                outRectData[idx++] = y;
                outRectData[idx++] = w;
                outRectData[idx++] = h;
                outRectData[idx++] = offset - bytes;
            }
            pendingBuffer.clear();
            dst.clear();
            hasPendingFrame = false;
            pendingDirtyRects.clear();
            return true;
        }
    }

    /**
     * 把待处理帧直接拷入 dst（如 PBO 映射内存），省掉中间 paintBuffer 副本。
     * dst 容量须 >= 帧大小；成功消费返回 true。返回的 outWidth/outHeight
     * 以实际消费的帧为准（peek 与 consume 之间可能有新帧到达）。
     */
    public boolean consumeFrameTo(ByteBuffer dst, int[] outWidth, int[] outHeight) {
        synchronized (paintLock) {
            if (!hasPendingFrame) {
                return false;
            }
            int size = pendingWidth * pendingHeight * 4;
            if (dst.capacity() < size) {
                return false;
            }
            outWidth[0] = pendingWidth;
            outHeight[0] = pendingHeight;
            dst.clear();
            pendingBuffer.clear();
            dst.put(pendingBuffer);
            dst.clear();
            hasPendingFrame = false;
            return true;
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
        runOnCefThread(() -> wasResized(width, height));
    }

    /** 鼠标移动低水位合并：位置是状态而非事件流，队列有任务时只更新最新值，丢弃中间帧 */
    private final Object mouseCoalesceLock = new Object();
    private boolean mouseMoveQueued;
    private int coalesceX;
    private int coalesceY;
    private int coalesceMods;

    public void injectMouseMove(int x, int y, int mods) {
        synchronized (mouseCoalesceLock) {
            coalesceX = x;
            coalesceY = y;
            coalesceMods = mods;
            if (mouseMoveQueued) {
                return; // 已有任务在队列：仅更新最新位置（低水位丢弃）
            }
            mouseMoveQueued = true;
        }
        runOnCefThread(() -> {
            int tx, ty, tm;
            synchronized (mouseCoalesceLock) {
                mouseMoveQueued = false;
                tx = coalesceX;
                ty = coalesceY;
                tm = coalesceMods;
            }
            sendMouseEvent(new CefMouseEvent(CefMouseEvent.MOUSE_MOVED, tx, ty, 0, 0, tm));
        });
    }

    public void injectMouseButton(int x, int y, int mods, int btn, boolean pressed) {
        int buttonFlag = switch (btn) {
            case 0 -> CefMouseEvent.BUTTON1_MASK;
            case 1 -> CefMouseEvent.BUTTON2_MASK;
            case 2 -> CefMouseEvent.BUTTON3_MASK;
            default -> 0;
        };
        CefMouseEvent ev = new CefMouseEvent(
                pressed ? 1 : 0, x, y, 1, btn, mods | buttonFlag);
        runOnCefThread(() -> sendMouseEvent(ev));
    }

    public void injectMouseWheel(int x, int y, int mods, double delta) {
        CefMouseWheelEvent ev = new CefMouseWheelEvent(
                CefMouseWheelEvent.WHEEL_UNIT_SCROLL, x, y, delta, mods);
        runOnCefThread(() -> sendMouseWheelEvent(ev));
    }

    public void injectKeyPress(int vkCode, char keyChar, int mods) {
        injectKeyPress(vkCode, keyChar, mods, 0);
    }

    public void injectKeyPress(int vkCode, char keyChar, int mods, int scanCode) {
        CefKeyEvent ev = new CefKeyEvent(CefKeyEvent.KEY_PRESS, vkCode, keyChar, mods);
        ev.scancode = scanCode;
        runOnCefThread(() -> {
            setFocus(true);
            sendKeyEvent(ev);
        });
    }

    public void injectKeyRelease(int vkCode, char keyChar, int mods) {
        injectKeyRelease(vkCode, keyChar, mods, 0);
    }

    public void injectKeyRelease(int vkCode, char keyChar, int mods, int scanCode) {
        CefKeyEvent ev = new CefKeyEvent(CefKeyEvent.KEY_RELEASE, vkCode, keyChar, mods);
        ev.scancode = scanCode;
        runOnCefThread(() -> sendKeyEvent(ev));
    }

    public void injectKeyTyped(char c, int mods) {
        injectKeyTyped(0, c, mods);
    }

    public void injectKeyTyped(int vkCode, char c, int mods) {
        CefKeyEvent ev = new CefKeyEvent(CefKeyEvent.KEY_TYPE, vkCode, c, mods);
        runOnCefThread(() -> sendKeyEvent(ev));
    }

    // --- MTML：以下 CEF 触碰调用需在 CEF UI 线程执行 ---

    @Override
    public void loadURL(String url) {
        runOnCefThread(() -> super.loadURL(url));
    }

    @Override
    public void reload() {
        runOnCefThread(super::reload);
    }

    @Override
    public void setFocus(boolean enable) {
        runOnCefThread(() -> super.setFocus(enable));
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        runOnCefThread(() -> super.executeJavaScript(code, url, line));
    }

    @Override
    public String getURL() {
        if (CefTaskRunner.isCefThread()) {
            return super.getURL();
        }
        // 游戏线程同步读取：postTask 到 CEF 线程并等待（CEF 线程自带循环，不会死锁）
        CountDownLatch latch = new CountDownLatch(1);
        String[] result = {null};
        runOnCefThread(() -> {
            result[0] = super.getURL();
            latch.countDown();
        });
        try {
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result[0] != null ? result[0] : "";
    }

    @Override
    public void close(boolean force) {
        // 必须等 CEF 线程完成 N_Close 再释放 AWT 资源（dummyFrame 的 HWND 是
        // 浏览器父窗口，提前销毁会导致 N_Close 失败/崩溃）。N_Close 在超时内
        // 未完成（如视频播放中 teardown 慢）时放弃释放 dummyFrame：泄漏一个
        // 隐藏窗口远好于 HWND use-after-free 的原生崩溃。
        if (CefTaskRunner.isCefThread()) {
            try { super.close(force); } catch (Exception ignored) {}
        } else {
            CountDownLatch closeLatch = new CountDownLatch(1);
            runOnCefThread(() -> {
                try { super.close(force); } catch (Exception ignored) {}
                closeLatch.countDown();
            });
            try {
                if (!closeLatch.await(5000, TimeUnit.MILLISECONDS)) {
                    return; // N_Close 未完成：不触碰 AWT 资源
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        // AWT 组件只能从 AWT/游戏主线程操作；在 CEF 线程上 dispose 会破坏
        // AWT toolkit（隐藏窗口由进程退出时 OS 回收）。
        if (dummyFrame != null && !CefTaskRunner.isCefThread()) {
            try { dummyFrame.dispose(); } catch (Exception ignored) {}
        }
    }
}
