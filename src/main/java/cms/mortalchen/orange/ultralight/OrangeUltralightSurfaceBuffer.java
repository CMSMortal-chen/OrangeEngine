package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.math.IntRect;
import net.janrupf.ujr.api.util.NioUltralightBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL12.GL_BGRA;

final class OrangeUltralightSurfaceBuffer extends NioUltralightBuffer {
    private final ByteBuffer pixelBuffer;
    private final OrangeUltralightSurface surface;

    OrangeUltralightSurfaceBuffer(ByteBuffer pixelBuffer, OrangeUltralightSurface surface) {
        super(pixelBuffer);
        this.pixelBuffer = pixelBuffer;
        this.surface = surface;
    }

    @Override
    public void close() {
        IntRect bounds = surface.dirtyBounds();
        if (bounds.width() > 0 && bounds.height() > 0) {
            long bufferPtr = MemoryUtil.memAddress(pixelBuffer);
            glBindTexture(GL_TEXTURE_2D, surface.texture());

            if (bounds.width() == surface.width()) {
                glTexSubImage2D(
                        GL_TEXTURE_2D,
                        0,
                        bounds.getLeft(),
                        bounds.getTop(),
                        bounds.width(),
                        bounds.height(),
                        GL_BGRA,
                        GL_UNSIGNED_BYTE,
                        bufferPtr + surface.rowBytes() * bounds.getTop() + bounds.getLeft() * 4L
                );
            } else {
                for (int y = bounds.getTop(); y < bounds.getBottom(); y++) {
                    glTexSubImage2D(
                            GL_TEXTURE_2D,
                            0,
                            bounds.getLeft(),
                            y,
                            bounds.width(),
                            1,
                            GL_BGRA,
                            GL_UNSIGNED_BYTE,
                            bufferPtr + surface.rowBytes() * y + bounds.getLeft() * 4L
                    );
                }
            }
        }

        surface.clearDirtyBounds();
        super.close();
    }
}
