package cms.mortalchen.orange.utils.render;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.nanovg.NVGLUFramebuffer;

/**
 * NanoVG 离屏 framebuffer 封装（nvgluCreateFramebuffer）。
 * <p>
 * 用法（必须发生在渲染线程、主帧 beginFrame/endFrame 之外）：
 * <pre>
 * NanoVGFramebuffer fbo = new NanoVGFramebuffer(vg, w, h);
 * fbo.beginRender();
 * NanoVG.nvgBeginFrame(vg, w, h, 1.0f);
 * // ... 绘制内容 ...
 * NanoVG.nvgEndFrame(vg);
 * fbo.endRender();
 * // 之后每帧用 fbo.image 作为普通 NanoVG 图像绘制（drawImage 等）
 * </pre>
 * 典型用途：瓦片地图静态层等低频变化内容预渲染，把每帧数千次 draw 降到 1 次。
 */
public class NanoVGFramebuffer implements AutoCloseable {
    public final int width;
    public final int height;
    /** NanoVG 图像句柄：可直接交给 drawImage/drawTileImage 等绘制 */
    public final int image;

    private final long vg;
    private final NVGLUFramebuffer handle;
    private boolean disposed;

    public NanoVGFramebuffer(long vg, int width, int height) {
        this.vg = vg;
        this.width = width;
        this.height = height;
        this.handle = NanoVGGL3.nvgluCreateFramebuffer(vg, width, height,
                NanoVG.NVG_IMAGE_NEAREST | NanoVG.NVG_IMAGE_FLIPY);
        if (handle == null || handle.address() == 0L) {
            throw new IllegalStateException("nvgluCreateFramebuffer failed: " + width + "x" + height);
        }
        this.image = handle.image();
    }

    /** 开始向离屏渲染（随后调用 nvgBeginFrame/nvgEndFrame，再调用 {@link #endRender()}） */
    public void beginRender() {
        NanoVGGL3.nvgluBindFramebuffer(vg, handle);
    }

    /** 结束离屏渲染，恢复默认 framebuffer */
    public void endRender() {
        NanoVGGL3.nvgluBindFramebuffer(vg, null);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        NanoVGGL3.nvgluDeleteFramebuffer(vg, handle);
        handle.free();
    }

    @Override
    public void close() {
        dispose();
    }
}
