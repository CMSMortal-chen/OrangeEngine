package cms.mortalchen.orange.utils.render.postprocess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.nanovg.NanoVG.NVG_IMAGE_FLIPY;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_IMAGE_NODELETE;
import static org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class PostProcessPipeline {
    private static final String FULLSCREEN_VERT = "shaders/postprocess/fullscreen.vert";
    private static final String GAUSSIAN_BLUR_FRAG = "shaders/postprocess/gaussian_blur.frag";
    private static final String COMPOSITE_FRAG = "shaders/postprocess/composite.frag";

    private int width = -1;
    private int height = -1;

    private FrameBuffer sceneBuffer;
    private FrameBuffer brightBuffer;
    private FrameBuffer pingBuffer;
    private FrameBuffer pongBuffer;

    private int blurProgram;
    private int compositeProgram;

    private int quadVao;
    private int quadVbo;

    private long frameToken;
    private long blurCacheToken = -1L;
    private int blurCachePasses = -1;
    private int blurCacheTexture = 0;
    private int blurCacheImage = -1;
    private int blurImageTexture = 0;
    private boolean bloomSourceDrawnThisFrame = false;

    private boolean initialized;
    private boolean bloomEnabled = true;
    private float bloomIntensity = 0.9f;
    private int bloomPasses = 5;

    public void initialize() {
        if (initialized) {
            return;
        }
        createQuad();
        blurProgram = createProgram(FULLSCREEN_VERT, GAUSSIAN_BLUR_FRAG);
        compositeProgram = createProgram(FULLSCREEN_VERT, COMPOSITE_FRAG);
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void beginScene(int newWidth, int newHeight) {
        initialize();
        resizeIfNeeded(newWidth, newHeight);
        frameToken++;
        blurCacheToken = -1L;
        bloomSourceDrawnThisFrame = false;
        glBindFramebuffer(GL_FRAMEBUFFER, sceneBuffer.fboId);
        glViewport(0, 0, width, height);
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public void beginBloomSource() {
        if (!initialized || brightBuffer == null) {
            return;
        }
        bloomSourceDrawnThisFrame = true;
        glBindFramebuffer(GL_FRAMEBUFFER, brightBuffer.fboId);
        glViewport(0, 0, width, height);
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    public void setBloomEnabled(boolean enabled) {
        this.bloomEnabled = enabled;
    }

    public void setBloomParams(float intensity, int passes) {
        bloomIntensity = Math.max(0f, intensity);
        bloomPasses = Math.max(2, passes);
    }

    public void compositeToScreen() {
        if (!initialized || sceneBuffer == null) {
            return;
        }

        int bloomTexture = 0;
        if (bloomEnabled && bloomIntensity > 0f && bloomSourceDrawnThisFrame) {
            bloomTexture = blurTexture(brightBuffer.colorTextureId, bloomPasses);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);

        glUseProgram(compositeProgram);
        bindTexture(0, sceneBuffer.colorTextureId);
        bindTexture(1, bloomTexture == 0 ? sceneBuffer.colorTextureId : bloomTexture);

        int sceneLoc = glGetUniformLocation(compositeProgram, "uScene");
        if (sceneLoc >= 0) {
            glUniform1i(sceneLoc, 0);
        }
        int bloomLoc = glGetUniformLocation(compositeProgram, "uBloom");
        if (bloomLoc >= 0) {
            glUniform1i(bloomLoc, 1);
        }
        boolean hasBloom = bloomEnabled && bloomIntensity > 0f && bloomSourceDrawnThisFrame;
        int intensityLoc = glGetUniformLocation(compositeProgram, "uBloomIntensity");
        if (intensityLoc >= 0) {
            glUniform1f(intensityLoc, hasBloom ? bloomIntensity : 0f);
        }

        renderQuad();
        glUseProgram(0);
    }

    public int getBlurredSceneImage(long vg, float blurStrength) {
        if (!initialized || sceneBuffer == null) {
            return -1;
        }
        int passCount = Math.clamp(Math.round(blurStrength), 2, 24);
        int blurredTexture = ensureSceneBlurTexture(passCount);
        return ensureBlurImage(vg, blurredTexture);
    }

    public void cleanup(long vg) {
        if (blurCacheImage != -1 && vg != 0L) {
            nvgDeleteImage(vg, blurCacheImage);
        }
        blurCacheImage = -1;
        blurImageTexture = 0;
        blurCacheTexture = 0;

        destroyFrameBuffer(sceneBuffer);
        destroyFrameBuffer(brightBuffer);
        destroyFrameBuffer(pingBuffer);
        destroyFrameBuffer(pongBuffer);
        sceneBuffer = null;
        brightBuffer = null;
        pingBuffer = null;
        pongBuffer = null;

        if (blurProgram != 0) {
            glDeleteProgram(blurProgram);
            blurProgram = 0;
        }
        if (compositeProgram != 0) {
            glDeleteProgram(compositeProgram);
            compositeProgram = 0;
        }
        if (quadVbo != 0) {
            glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        if (quadVao != 0) {
            glDeleteVertexArrays(quadVao);
            quadVao = 0;
        }
        initialized = false;
    }

    private int ensureSceneBlurTexture(int passCount) {
        if (blurCacheToken == frameToken && blurCachePasses == passCount && blurCacheTexture != 0) {
            return blurCacheTexture;
        }
        blurCacheTexture = blurTexture(sceneBuffer.colorTextureId, passCount);
        blurCachePasses = passCount;
        blurCacheToken = frameToken;
        if (blurImageTexture != blurCacheTexture) {
            blurCacheImage = -1;
            blurImageTexture = 0;
        }
        return blurCacheTexture;
    }

    private int ensureBlurImage(long vg, int textureId) {
        if (textureId == 0) {
            return -1;
        }
        if (blurCacheImage != -1 && blurImageTexture == textureId) {
            return blurCacheImage;
        }
        if (blurCacheImage != -1) {
            nvgDeleteImage(vg, blurCacheImage);
            blurCacheImage = -1;
        }
        int imageId = nvglCreateImageFromHandle(vg, textureId, width, height, NVG_IMAGE_NODELETE | NVG_IMAGE_FLIPY);
        if (imageId <= 0) {
            return -1;
        }
        blurCacheImage = imageId;
        blurImageTexture = textureId;
        return imageId;
    }

    public static boolean shouldUseDedicatedBloomSource(boolean bloomSourceDrawnThisFrame) {
        return bloomSourceDrawnThisFrame;
    }

    private int blurTexture(int sourceTexture, int passCount) {
        int currentSource = sourceTexture;

        for (int i = 0; i < passCount; i++) {
            boolean horizontal = (i & 1) == 0;
            FrameBuffer target = horizontal ? pingBuffer : pongBuffer;

            glBindFramebuffer(GL_FRAMEBUFFER, target.fboId);
            glViewport(0, 0, width, height);
            glClearColor(0f, 0f, 0f, 0f);
            glClear(GL_COLOR_BUFFER_BIT);

            glUseProgram(blurProgram);
            bindTexture(0, currentSource);

            int texLoc = glGetUniformLocation(blurProgram, "uScene");
            if (texLoc >= 0) {
                glUniform1i(texLoc, 0);
            }
            int texelLoc = glGetUniformLocation(blurProgram, "uTexelSize");
            if (texelLoc >= 0) {
                glUniform2f(texelLoc, 1f / width, 1f / height);
            }
            int dirLoc = glGetUniformLocation(blurProgram, "uDirection");
            if (dirLoc >= 0) {
                glUniform2f(dirLoc, horizontal ? 1f : 0f, horizontal ? 0f : 1f);
            }
            int regionPosLoc = glGetUniformLocation(blurProgram, "uRegionPosPx");
            if (regionPosLoc >= 0) {
                glUniform2f(regionPosLoc, 0f, 0f);
            }
            int regionSizeLoc = glGetUniformLocation(blurProgram, "uRegionSizePx");
            if (regionSizeLoc >= 0) {
                glUniform2f(regionSizeLoc, width, height);
            }
            int radiusLoc = glGetUniformLocation(blurProgram, "uRadius");
            if (radiusLoc >= 0) {
                glUniform1f(radiusLoc, Math.max(2f, Math.min(20f, passCount)));
            }

            renderQuad();
            glUseProgram(0);
            currentSource = target.colorTextureId;
        }

        return currentSource;
    }

    private void resizeIfNeeded(int newWidth, int newHeight) {
        if (width == newWidth && height == newHeight && sceneBuffer != null) {
            return;
        }

        width = Math.max(1, newWidth);
        height = Math.max(1, newHeight);
        blurCacheToken = -1L;
        blurCachePasses = -1;
        blurCacheTexture = 0;
        blurImageTexture = 0;
        blurCacheImage = -1;

        destroyFrameBuffer(sceneBuffer);
        destroyFrameBuffer(brightBuffer);
        destroyFrameBuffer(pingBuffer);
        destroyFrameBuffer(pongBuffer);

        sceneBuffer = createFrameBuffer(width, height, true);
        brightBuffer = createFrameBuffer(width, height, false);
        pingBuffer = createFrameBuffer(width, height, false);
        pongBuffer = createFrameBuffer(width, height, false);
    }

    private FrameBuffer createFrameBuffer(int w, int h, boolean withDepthStencil) {
        FrameBuffer buffer = new FrameBuffer();
        buffer.fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, buffer.fboId);

        buffer.colorTextureId = createColorTexture(w, h);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, buffer.colorTextureId, 0);

        if (withDepthStencil) {
            buffer.depthStencilRboId = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, buffer.depthStencilRboId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, w, h);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, buffer.depthStencilRboId);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer is not complete, status=" + status);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return buffer;
    }

    private void destroyFrameBuffer(FrameBuffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.depthStencilRboId != 0) {
            glDeleteRenderbuffers(buffer.depthStencilRboId);
        }
        if (buffer.colorTextureId != 0) {
            glDeleteTextures(buffer.colorTextureId);
        }
        if (buffer.fboId != 0) {
            glDeleteFramebuffers(buffer.fboId);
        }
    }

    private int createColorTexture(int w, int h) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        return textureId;
    }

    private void createQuad() {
        float[] vertices = {
                -1f, -1f, 0f, 0f,
                 1f, -1f, 1f, 0f,
                -1f,  1f, 0f, 1f,
                 1f,  1f, 1f, 1f
        };

        quadVao = glGenVertexArrays();
        quadVbo = glGenBuffers();

        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void renderQuad() {
        glBindVertexArray(quadVao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);
    }

    private void bindTexture(int unit, int textureId) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    private int createProgram(String vertPath, String fragPath) {
        int vertShader = createShader(GL_VERTEX_SHADER, readResource(vertPath));
        int fragShader = createShader(GL_FRAGMENT_SHADER, readResource(fragPath));
        int program = glCreateProgram();

        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String info = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            glDeleteShader(vertShader);
            glDeleteShader(fragShader);
            throw new IllegalStateException("Program link failed: " + info);
        }

        glDetachShader(program, vertShader);
        glDetachShader(program, fragShader);
        glDeleteShader(vertShader);
        glDeleteShader(fragShader);
        return program;
    }

    private int createShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String info = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + info);
        }
        return shader;
    }

    private String readResource(String path) {
        try (InputStream stream = PostProcessPipeline.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shader resource: " + path);
            }
            byte[] bytes = stream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read shader resource: " + path, e);
        }
    }

    private static final class FrameBuffer {
        private int fboId;
        private int colorTextureId;
        private int depthStencilRboId;
    }
}
