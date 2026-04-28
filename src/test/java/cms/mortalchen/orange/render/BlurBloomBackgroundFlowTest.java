package cms.mortalchen.orange.render;

import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.postprocess.PostProcessPipeline;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlurBloomBackgroundFlowTest {
    private static final float EPS = 0.0001f;
    private static final String ATTACHED_IMAGE_RESOURCE = "/textures/image/bg13.jpg";

    @Test
    void cover_shouldFillTargetWithoutStretch_usingAttachedImage_16x9() throws IOException {
        BufferedImage image = readAttachedImage();

        RenderUtils.CoverRect rect = RenderUtils.computeCoverRect(0f, 0f, 1920f, 1080f, image.getWidth(), image.getHeight());
        assertCoverConstraints(rect, 1920f, 1080f, image.getWidth(), image.getHeight());
    }

    @Test
    void cover_shouldFillTargetWithoutStretch_usingAttachedImage_4x3() throws IOException {
        BufferedImage image = readAttachedImage();

        RenderUtils.CoverRect rect = RenderUtils.computeCoverRect(0f, 0f, 1440f, 1080f, image.getWidth(), image.getHeight());
        assertCoverConstraints(rect, 1440f, 1080f, image.getWidth(), image.getHeight());
    }

    @Test
    void bloomIsolationPolicy_shouldKeepBackgroundOutOfBloomPass() {
        assertTrue(PostProcessPipeline.shouldUseDedicatedBloomSource(true));
        assertFalse(PostProcessPipeline.shouldUseDedicatedBloomSource(false));
    }

    @Test
    void bloomScopeSwitch_shouldRequestBloomSourceOnlyWithinStartStopFlow() {
        assertFalse(RenderUtils.consumeBloomScopeRequestedThisFrame());

        RenderUtils.startBloom(0L);
        assertTrue(RenderUtils.consumeBloomScopeRequestedThisFrame());
        assertFalse(RenderUtils.consumeBloomScopeRequestedThisFrame());

        assertTrue(RenderUtils.shouldRenderBloomSource(true, false, true));
        assertFalse(RenderUtils.shouldRenderBloomSource(true, false, false));
        assertTrue(RenderUtils.shouldRenderBloomSource(true, true, false));
        assertFalse(RenderUtils.shouldRenderBloomSource(false, true, true));
    }

    private static BufferedImage readAttachedImage() throws IOException {
        try (InputStream in = BlurBloomBackgroundFlowTest.class.getResourceAsStream(ATTACHED_IMAGE_RESOURCE)) {
            Assumptions.assumeTrue(in != null, "Attached image resource not found: " + ATTACHED_IMAGE_RESOURCE);
            BufferedImage image = ImageIO.read(in);
            Assumptions.assumeTrue(image != null, "Failed to decode attached image");
            return image;
        }
    }

    private static void assertCoverConstraints(RenderUtils.CoverRect rect, float targetW, float targetH, int imageW, int imageH) {
        float sourceAspect = imageW / (float) imageH;
        float drawAspect = rect.width / rect.height;

        assertEquals(sourceAspect, drawAspect, EPS, "Cover draw aspect must match source aspect");
        assertTrue(rect.width >= targetW - EPS, "Cover width must fill target width");
        assertTrue(rect.height >= targetH - EPS, "Cover height must fill target height");
    }
}
