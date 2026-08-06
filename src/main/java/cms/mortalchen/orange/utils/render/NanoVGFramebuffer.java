package cms.mortalchen.orange.utils.render;

import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;

/**
 * NanoVG 离屏 framebuffer（手工 GL framebuffer，与 PostProcessPipeline 的 sceneBuffer 同款）。
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
 * <p>
 * 说明：不使用 NanoVG 的 NVGLU framebuffer（nvgluCreateFramebuffer）——实测其渲染目标
 * 内容不可靠（绘制错位/内容缺失）。本类直接创建 GL framebuffer + 纹理 + 深度模板
 * renderbuffer（与引擎 postprocess 的 sceneBuffer 相同，NanoVG 渲染到该目标是已验证路径），
 * 渲染后用 {@link NanoVGGL3#nvglCreateImageFromHandle} 包装纹理
 * （FLIPY 修正 GL 纹理方向，NODELETE 防止删除时销毁 GL 纹理，NEAREST 保持像素锐利）。
 */
public class NanoVGFramebuffer implements AutoCloseable {
    public final int width;
    public final int height;
    /** NanoVG 图像句柄：可直接交给 drawImage/drawTileImage 等绘制 */
    public final int image;

    private final long vg;
    private final int fboId;
    private final int textureId;
    private final int rboId;
    private int savedFbo;
    private boolean disposed;

    public NanoVGFramebuffer(long vg, int width, int height) {
        this.vg = vg;
        this.width = width;
        this.height = height;

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);

        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        rboId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rboId);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteRenderbuffers(rboId);
            glDeleteTextures(textureId);
            glDeleteFramebuffers(fboId);
            throw new IllegalStateException("Framebuffer is not complete, status=" + status);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        this.image = NanoVGGL3.nvglCreateImageFromHandle(vg, textureId, width, height,
                NanoVGGL3.NVG_IMAGE_NODELETE | NanoVG.NVG_IMAGE_FLIPY | NanoVG.NVG_IMAGE_NEAREST);
        if (image == 0) {
            glDeleteRenderbuffers(rboId);
            glDeleteTextures(textureId);
            glDeleteFramebuffers(fboId);
            throw new IllegalStateException("nvglCreateImageFromHandle failed: " + width + "x" + height);
        }
    }

    /** 开始向离屏渲染（随后调用 nvgBeginFrame/nvgEndFrame，再调用 {@link #endRender()}） */
    public void beginRender() {
        savedFbo = GLStateManager.captureFramebufferBinding();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, width, height);
    }

    /** 结束离屏渲染，恢复之前的 framebuffer 绑定 */
    public void endRender() {
        GLStateManager.bindFramebuffer(savedFbo);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        if (image != 0) {
            NanoVG.nvgDeleteImage(vg, image);
        }
        glDeleteRenderbuffers(rboId);
        glDeleteTextures(textureId);
        glDeleteFramebuffers(fboId);
    }

    @Override
    public void close() {
        dispose();
    }
}
