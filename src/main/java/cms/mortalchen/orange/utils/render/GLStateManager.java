package cms.mortalchen.orange.utils.render;

import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB;
import static org.lwjgl.opengl.GL20.glBlendEquationSeparate;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public final class GLStateManager {
    private GLStateManager() {
    }

    public static final class GLStateSnapshot {
        public final int framebufferBinding;
        public final int[] viewport;
        public final int currentProgram;
        public final int vertexArrayBinding;
        public final int arrayBufferBinding;
        public final int elementArrayBufferBinding;
        public final int activeTextureUnit;
        public final int texture2DBinding;
        public final boolean blendEnabled;
        public final int blendSrcRgb;
        public final int blendDstRgb;
        public final int blendSrcAlpha;
        public final int blendDstAlpha;
        public final int blendEqRgb;
        public final int blendEqAlpha;
        public final boolean depthTestEnabled;
        public final boolean depthMaskEnabled;
        public final boolean cullFaceEnabled;
        public final boolean scissorEnabled;

        private GLStateSnapshot(
                int framebufferBinding,
                int[] viewport,
                int currentProgram,
                int vertexArrayBinding,
                int arrayBufferBinding,
                int elementArrayBufferBinding,
                int activeTextureUnit,
                int texture2DBinding,
                boolean blendEnabled,
                int blendSrcRgb,
                int blendDstRgb,
                int blendSrcAlpha,
                int blendDstAlpha,
                int blendEqRgb,
                int blendEqAlpha,
                boolean depthTestEnabled,
                boolean depthMaskEnabled,
                boolean cullFaceEnabled,
                boolean scissorEnabled
        ) {
            this.framebufferBinding = framebufferBinding;
            this.viewport = viewport;
            this.currentProgram = currentProgram;
            this.vertexArrayBinding = vertexArrayBinding;
            this.arrayBufferBinding = arrayBufferBinding;
            this.elementArrayBufferBinding = elementArrayBufferBinding;
            this.activeTextureUnit = activeTextureUnit;
            this.texture2DBinding = texture2DBinding;
            this.blendEnabled = blendEnabled;
            this.blendSrcRgb = blendSrcRgb;
            this.blendDstRgb = blendDstRgb;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.blendEqRgb = blendEqRgb;
            this.blendEqAlpha = blendEqAlpha;
            this.depthTestEnabled = depthTestEnabled;
            this.depthMaskEnabled = depthMaskEnabled;
            this.cullFaceEnabled = cullFaceEnabled;
            this.scissorEnabled = scissorEnabled;
        }
    }

    public static GLStateSnapshot captureState() {
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);

        int activeTextureUnit = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int texture2DBinding = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(activeTextureUnit);

        return new GLStateSnapshot(
                glGetInteger(GL_FRAMEBUFFER_BINDING),
                viewport,
                glGetInteger(GL_CURRENT_PROGRAM),
                glGetInteger(GL_VERTEX_ARRAY_BINDING),
                glGetInteger(GL_ARRAY_BUFFER_BINDING),
                glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING),
                activeTextureUnit,
                texture2DBinding,
                glIsEnabled(GL_BLEND),
                glGetInteger(GL_BLEND_SRC_RGB),
                glGetInteger(GL_BLEND_DST_RGB),
                glGetInteger(GL_BLEND_SRC_ALPHA),
                glGetInteger(GL_BLEND_DST_ALPHA),
                glGetInteger(GL_BLEND_EQUATION_RGB),
                glGetInteger(GL_BLEND_EQUATION_ALPHA),
                glIsEnabled(GL_DEPTH_TEST),
                glGetBoolean(GL_DEPTH_WRITEMASK),
                glIsEnabled(GL_CULL_FACE),
                glIsEnabled(GL_SCISSOR_TEST)
        );
    }

    public static void restoreState(GLStateSnapshot snapshot) {
        bindFramebuffer(snapshot.framebufferBinding);
        glViewport(snapshot.viewport[0], snapshot.viewport[1], snapshot.viewport[2], snapshot.viewport[3]);
        glUseProgram(snapshot.currentProgram);

        glBindVertexArray(snapshot.vertexArrayBinding);
        glBindBuffer(GL_ARRAY_BUFFER, snapshot.arrayBufferBinding);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, snapshot.elementArrayBufferBinding);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, snapshot.texture2DBinding);
        glActiveTexture(snapshot.activeTextureUnit);

        setCapability(GL_BLEND, snapshot.blendEnabled);
        glBlendEquationSeparate(snapshot.blendEqRgb, snapshot.blendEqAlpha);
        glBlendFuncSeparate(snapshot.blendSrcRgb, snapshot.blendDstRgb, snapshot.blendSrcAlpha, snapshot.blendDstAlpha);

        setCapability(GL_DEPTH_TEST, snapshot.depthTestEnabled);
        glDepthMask(snapshot.depthMaskEnabled);
        setCapability(GL_CULL_FACE, snapshot.cullFaceEnabled);
        setCapability(GL_SCISSOR_TEST, snapshot.scissorEnabled);
    }

    public static int captureFramebufferBinding() {
        return glGetInteger(GL_FRAMEBUFFER_BINDING);
    }

    public static void bindFramebuffer(int framebuffer) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    }

    public static int[] captureViewport() {
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        return viewport;
    }

    public static void restoreViewport(int[] viewport) {
        if (viewport == null || viewport.length < 4) {
            return;
        }
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    public static void runPreservingState(Runnable runnable) {
        GLStateSnapshot snapshot = captureState();
        try {
            runnable.run();
        } finally {
            restoreState(snapshot);
        }
    }

    public static <T> T callPreservingState(Supplier<T> supplier) {
        GLStateSnapshot snapshot = captureState();
        try {
            return supplier.get();
        } finally {
            restoreState(snapshot);
        }
    }

    private static void setCapability(int cap, boolean enabled) {
        if (enabled) {
            glEnable(cap);
        } else {
            glDisable(cap);
        }
    }
}
