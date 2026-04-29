package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.math.IntRect;
import net.janrupf.ujr.api.surface.UltralightSurface;
import net.janrupf.ujr.api.util.UltralightBuffer;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

final class OrangeUltralightSurface implements UltralightSurface {
    private int texture;
    private ByteBuffer pixelBuffer;
    private long width;
    private long height;
    private IntRect dirtyBounds;

    OrangeUltralightSurface(long width, long height) {
        resize(width, height);
    }

    @Override
    public long width() {
        return width;
    }

    @Override
    public long height() {
        return height;
    }

    @Override
    public long rowBytes() {
        return width * 4L;
    }

    @Override
    public long size() {
        return width * height * 4L;
    }

    @Override
    public UltralightBuffer lockPixels() {
        return new OrangeUltralightSurfaceBuffer(pixelBuffer, this);
    }

    @Override
    public void resize(long width, long height) {
        this.width = Math.max(1L, width);
        this.height = Math.max(1L, height);

        if (texture != 0) {
            glDeleteTextures(texture);
        }

        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, (int) this.width, (int) this.height, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0L);

        pixelBuffer = ByteBuffer.allocateDirect((int) size());
        dirtyBounds = new IntRect(0, 0, (int) this.width, (int) this.height);
    }

    @Override
    public void setDirtyBounds(IntRect bounds) {
        dirtyBounds = bounds;
    }

    @Override
    public IntRect dirtyBounds() {
        return dirtyBounds == null ? new IntRect(0, 0, 0, 0) : dirtyBounds;
    }

    @Override
    public void clearDirtyBounds() {
        dirtyBounds = null;
    }

    int texture() {
        return texture;
    }

    void destroy() {
        if (texture != 0) {
            glDeleteTextures(texture);
            texture = 0;
        }
    }
}
