package cms.mortalchen.orange.utils.render;

import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.Utils;
import cms.mortalchen.orange.utils.render.postprocess.PostProcessPipeline;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class RenderUtils {
    private static PostProcessPipeline postProcessPipeline;
    private static int frameWidth = 0;
    private static int frameHeight = 0;
    private static boolean bloomCapturePass = false;
    private static int bloomScopeDepth = 0;
    private static boolean bloomScopeRequestedThisFrame = false;
    private static boolean glassBlurEnabled = true;

    public static final class CoverRect {
        public final float x;
        public final float y;
        public final float width;
        public final float height;

        public CoverRect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static void beginBloomCapturePass(long vg) {
        bloomCapturePass = true;
        bloomScopeDepth = 0;
        NanoVG.nvgGlobalAlpha(vg, 0f);
    }

    public static void setPostProcessPipeline(PostProcessPipeline pipeline) {
        postProcessPipeline = pipeline;
    }

    public static void setNanoVGFrameSize(int w, int h) {
        frameWidth = w;
        frameHeight = h;
    }

    public static boolean isGlassBlurEnabled() {
        return glassBlurEnabled;
    }

    public static void setGlassBlurEnabled(boolean enabled) {
        glassBlurEnabled = enabled;
    }

    public static void endBloomCapturePass(long vg) {
        bloomScopeDepth = 0;
        bloomCapturePass = false;
        NanoVG.nvgGlobalAlpha(vg, 1f);
    }

    public static void startBloom(long vg) {
        if (!bloomCapturePass) {
            bloomScopeRequestedThisFrame = true;
            return;
        }

        if (bloomScopeDepth == 0) {
            NanoVG.nvgGlobalAlpha(vg, 1f);
        }
        bloomScopeDepth++;
    }

    public static void stopBloom(long vg) {
        if (!bloomCapturePass) {
            return;
        }

        if (bloomScopeDepth > 0) {
            bloomScopeDepth--;
        }
        if (bloomScopeDepth == 0) {
            NanoVG.nvgGlobalAlpha(vg, 0f);
        }
    }

    public static boolean consumeBloomScopeRequestedThisFrame() {
        boolean requested = bloomScopeRequestedThisFrame;
        bloomScopeRequestedThisFrame = false;
        return requested;
    }

    public static boolean shouldRenderBloomSource(boolean bloomEnabled, boolean dedicatedBloomPass, boolean scopedBloomRequested) {
        return bloomEnabled && (dedicatedBloomPass || scopedBloomRequested);
    }

    public static void drawLine(long vg, float x1, float y1, float x2, float y2, float width, Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgMoveTo(vg, x1, y1);
        NanoVG.nvgLineTo(vg, x2, y2);
        NanoVG.nvgStrokeColor(vg, nvgColor);
        NanoVG.nvgStrokeWidth(vg, width);
        NanoVG.nvgStroke(vg);
    }

    public static void drawRoundedRect(long vg, float x, float y, float x2, float y2, float radius, Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y,(Math.max(x, x2) - Math.min(x, x2)), (Math.max(y, y2) - Math.min(y, y2)), radius);
        NanoVG.nvgFillColor(vg, nvgColor);
        NanoVG.nvgFill(vg);
    }

    public static void drawGlassRoundedRect(long vg, float x, float y, float x2, float y2, float radius, boolean border) {
        drawGlassRoundedRect(vg, x, y, x2, y2, radius, new Color(255, 255, 255, 36), 12f, border);
    }

    public static void drawGlassRoundedRect(long vg, float x, float y, float x2, float y2, float radius, Color tint, float blurStrength, boolean border) {
        drawGlassRectInternal(vg, x, y, x2, y2, radius, true, tint, blurStrength, border);
    }

    public static void drawGlassRect(long vg, float x, float y, float x2, float y2,boolean border) {
        drawGlassRect(vg, x, y, x2, y2, new Color(255, 255, 255, 36), 12f, border);
    }

    public static void drawGlassRect(long vg, float x, float y, float x2, float y2, Color tint, float blurStrength, boolean border) {
        drawGlassRectInternal(vg, x, y, x2, y2, 0f, false, tint, blurStrength,border);
    }

    public static void drawGlassRoundedRect(long vg, float x, float y, float x2, float y2, float radius) {
        drawGlassRoundedRect(vg, x, y, x2, y2, radius, new Color(255, 255, 255, 36), 12f);
    }

    public static void drawGlassRoundedRect(long vg, float x, float y, float x2, float y2, float radius, Color tint, float blurStrength) {
        drawGlassRectInternal(vg, x, y, x2, y2, radius, true, tint, blurStrength,false);
    }

    public static void drawGlassRect(long vg, float x, float y, float x2, float y2) {
        drawGlassRect(vg, x, y, x2, y2, new Color(255, 255, 255, 36), 12f);
    }

    public static void drawGlassRect(long vg, float x, float y, float x2, float y2, Color tint, float blurStrength) {
        drawGlassRectInternal(vg, x, y, x2, y2, 0f, false, tint, blurStrength, false);
    }

    private static void drawGlassRectInternal(long vg, float x, float y, float x2, float y2, float radius, boolean rounded, Color tint, float blurStrength, boolean border) {
        float left = Math.min(x, x2);
        float top = Math.min(y, y2);
        float width = Math.abs(x2 - x);
        float height = Math.abs(y2 - y);
        float safeRadius = rounded ? Math.max(0f, radius) : 0f;

        if (width <= 0f || height <= 0f) {
            return;
        }

        if (bloomCapturePass) {
            return;
        }

        if (glassBlurEnabled && postProcessPipeline != null && postProcessPipeline.isInitialized() && frameWidth > 0 && frameHeight > 0) {
            nvgEndFrame(vg);

            int prevFbo = GLStateManager.captureFramebufferBinding();
            int blurImage = postProcessPipeline.getBlurredSceneImage(vg, blurStrength);
            GLStateManager.bindFramebuffer(prevFbo);

            nvgBeginFrame(vg, frameWidth, frameHeight, 1.0f);

            if (blurImage > 0) {
                NVGPaint blurPaint = NVGPaint.create();
                nvgImagePattern(vg, 0f, 0f, frameWidth, frameHeight, 0f, blurImage, 1f, blurPaint);
                nvgBeginPath(vg);
                if (rounded) {
                    nvgRoundedRect(vg, left, top, width, height, safeRadius);
                } else {
                    nvgRect(vg, left, top, width, height);
                }
                nvgFillPaint(vg, blurPaint);
                nvgFill(vg);
            }
        }

        drawGlassTintAndBorder(vg, left, top, width, height, safeRadius, rounded, tint, border);
    }

    private static void drawGlassTintAndBorder(long vg, float left, float top, float width, float height, float radius, boolean rounded, Color tint, boolean withBorder) {
        NVGColor tintColor = NVGColor.create()
                .r(tint.getRed() / 255f)
                .g(tint.getGreen() / 255f)
                .b(tint.getBlue() / 255f)
                .a(tint.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);
        if (rounded) {
            NanoVG.nvgRoundedRect(vg, left, top, width, height, radius);
        } else {
            NanoVG.nvgRect(vg, left, top, width, height);
        }
        NanoVG.nvgFillColor(vg, tintColor);
        NanoVG.nvgFill(vg);

        if (!withBorder) {
            return;
        }

        float borderAlpha = Math.clamp(tint.getAlpha() / 255f + 0.08f, 0.08f, 0.15f);
        NVGColor borderColor = NVGColor.create().r(1f).g(1f).b(1f).a(borderAlpha);
        NanoVG.nvgBeginPath(vg);
        if (rounded) {
            NanoVG.nvgRoundedRect(vg, left + 0.5f, top + 0.5f, Math.max(0f, width - 1f), Math.max(0f, height - 1f), Math.max(0f, radius - 0.5f));
        } else {
            NanoVG.nvgRect(vg, left + 0.5f, top + 0.5f, Math.max(0f, width - 1f), Math.max(0f, height - 1f));
        }
        NanoVG.nvgStrokeWidth(vg, 1f);
        NanoVG.nvgStrokeColor(vg, borderColor);
        NanoVG.nvgStroke(vg);
    }

    public static void drawCircleStroke(long vg,float cx, float cy, float radius, float width,Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);

        NanoVG.nvgCircle(vg, cx, cy, radius);
        NanoVG.nvgStrokeColor(vg, nvgColor);
        NanoVG.nvgStrokeWidth(vg, width);
        NanoVG.nvgStroke(vg);

        NanoVG.nvgFillColor(vg, Utils.color(0,0,0,0));
        NanoVG.nvgFill(vg);
    }

    public static void drawCircle(long vg,float cx, float cy, float radius, Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);

        NanoVG.nvgCircle(vg, cx, cy, radius);

        NanoVG.nvgFillColor(vg, nvgColor);
        NanoVG.nvgFill(vg);
    }

    public static void drawRect(long vg, float x, float y, float x2, float y2, Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        float left = Math.min(x, x2);
        float top = Math.min(y, y2);
        float w = Math.max(x, x2) - left;
        float h = Math.max(y, y2) - top;

        // 细线（宽或高 < 2px）：对齐到整数像素并临时关闭抗锯齿，
        // 避免半像素坐标下 nvgFill 的边缘羽化（1px 线出现浮雕感/端点亮边）
        boolean thin = w < 2f || h < 2f;
        if (thin) {
            NanoVG.nvgSave(vg);
            NanoVG.nvgShapeAntiAlias(vg, false);
            left = Math.round(left);
            top = Math.round(top);
            if (w < 2f) w = Math.max(1f, Math.round(w));
            if (h < 2f) h = Math.max(1f, Math.round(h));
        }
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, left, top, w, h);
        NanoVG.nvgFillColor(vg, nvgColor);
        NanoVG.nvgFill(vg);
        if (thin) {
            NanoVG.nvgRestore(vg);
        }
    }

    public static void drawRect(long vg, double x, double y, double x2, double y2, Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        float left = (float) Math.min(x, x2);
        float top = (float) Math.min(y, y2);
        float w = (float) (Math.max(x, x2) - Math.min(x, x2));
        float h = (float) (Math.max(y, y2) - Math.min(y, y2));

        // 细线（宽或高 < 2px）：对齐到整数像素并临时关闭抗锯齿
        boolean thin = w < 2f || h < 2f;
        if (thin) {
            NanoVG.nvgSave(vg);
            NanoVG.nvgShapeAntiAlias(vg, false);
            left = Math.round(left);
            top = Math.round(top);
            if (w < 2f) w = Math.max(1f, Math.round(w));
            if (h < 2f) h = Math.max(1f, Math.round(h));
        }
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, left, top, w, h);
        NanoVG.nvgFillColor(vg, nvgColor);
        NanoVG.nvgFill(vg);
        if (thin) {
            NanoVG.nvgRestore(vg);
        }
    }

    public static void drawGradientSideWay(long vg, float x, float y, float x2, float y2, Color color,Color colorTo) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);
        NVGColor nvgColorTo = NVGColor.create()
                .r(colorTo.getRed() / 255f)
                .g(colorTo.getGreen() / 255f)
                .b(colorTo.getBlue() / 255f)
                .a(colorTo.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x, y, (Math.max(x, x2) - Math.min(x, x2)), (Math.max(y, y2) - Math.min(y, y2)));
        NVGPaint paint = NVGPaint.create();
        NanoVG.nvgLinearGradient(vg, x, y, x2, y, nvgColor, nvgColorTo, paint);
        NanoVG.nvgFillPaint(vg, paint);
        NanoVG.nvgFill(vg);

    }

    public static void drawGradientUpDown(long vg, float x, float y, float x2, float y2, Color color,Color colorTo) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);
        NVGColor nvgColorTo = NVGColor.create()
                .r(colorTo.getRed() / 255f)
                .g(colorTo.getGreen() / 255f)
                .b(colorTo.getBlue() / 255f)
                .a(colorTo.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x, y, (Math.max(x, x2) - Math.min(x, x2)), (Math.max(y, y2) - Math.min(y, y2)));
        NVGPaint paint = NVGPaint.create();
        NanoVG.nvgLinearGradient(vg, x, y, x, y2, nvgColor, nvgColorTo, paint);
        NanoVG.nvgFillPaint(vg, paint);
        NanoVG.nvgFill(vg);

    }

    public static NVGColor nvgColor(Color color) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        return NVGColor.create();
    }

    public static void fillTriangle(
            long nvgContext,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            Color color
    ) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(nvgContext);
        NanoVG.nvgMoveTo(nvgContext, x1, y1);
        NanoVG.nvgLineTo(nvgContext, x2, y2);
        NanoVG.nvgLineTo(nvgContext, x3, y3);
        NanoVG.nvgClosePath(nvgContext);

        NanoVG.nvgFillColor(nvgContext, nvgColor);
        NanoVG.nvgFill(nvgContext);

    }

    public static void drawImage(long nvg, ResourceLocation location, float x, float y, float width, float height, int alpha, Color color) {
        nvgBeginPath(nvg);
        NVGPaint paint = nvgImagePattern(nvg, x, y, width, height, 0.0F, location.getId(), alpha / 255.0F, NVGPaint.create());

        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        paint.innerColor(nvgColor);
        paint.outerColor(nvgColor);
        nvgRect(nvg, x, y, width, height);
        nvgFillPaint(nvg, paint);
        nvgFill(nvg);
    }

    public static void drawImageCover(long nvg, ResourceLocation location, float x, float y, float width, float height, int alpha, Color color) {
        if (location == null || location.getId() <= 0 || width <= 0f || height <= 0f) {
            return;
        }

        int[] imageWidth = new int[1];
        int[] imageHeight = new int[1];
        nvgImageSize(nvg, location.getId(), imageWidth, imageHeight);
        if (imageWidth[0] <= 0 || imageHeight[0] <= 0) {
            drawImage(nvg, location, x, y, width, height, alpha, color);
            return;
        }

        CoverRect coverRect = computeCoverRect(x, y, width, height, imageWidth[0], imageHeight[0]);

        nvgBeginPath(nvg);
        NVGPaint paint = nvgImagePattern(nvg, coverRect.x, coverRect.y, coverRect.width, coverRect.height, 0.0F, location.getId(), alpha / 255.0F, NVGPaint.create());

        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        paint.innerColor(nvgColor);
        paint.outerColor(nvgColor);
        nvgRect(nvg, x, y, width, height);
        nvgFillPaint(nvg, paint);
        nvgFill(nvg);
    }

    public static CoverRect computeCoverRect(float x, float y, float width, float height, int imageWidth, int imageHeight) {
        if (width <= 0f || height <= 0f || imageWidth <= 0 || imageHeight <= 0) {
            return new CoverRect(x, y, width, height);
        }

        float sourceAspect = imageWidth / (float) imageHeight;
        float targetAspect = width / height;
        float drawWidth;
        float drawHeight;
        float drawX = x;
        float drawY = y;

        if (targetAspect > sourceAspect) {
            drawWidth = width;
            drawHeight = width / sourceAspect;
            drawY = y - (drawHeight - height) * 0.5f;
        } else {
            drawHeight = height;
            drawWidth = height * sourceAspect;
            drawX = x - (drawWidth - width) * 0.5f;
        }
        return new CoverRect(drawX, drawY, drawWidth, drawHeight);
    }

    public static void drawTileImage(long nvg, ResourceLocation location, float x, float y ,float width, float height, int u, int v, float scales ,float uWidth, float uHeight, int alpha, Color color) {
        nvgBeginPath(nvg);
        NVGPaint paint = nvgImagePattern(nvg, x - (u * scales), y - (v * scales), width * scales, height * scales, 0.0F, location.getId(), alpha / 255.0F, NVGPaint.create());

        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        paint.innerColor(nvgColor);
        paint.outerColor(nvgColor);
        nvgRect(nvg, x, y, uWidth * scales, uHeight * scales);
        nvgFillPaint(nvg, paint);
        nvgFill(nvg);

    }


    public static void strokeTriangle(
            long nvgContext,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float strokeWidth,
            Color color
    ) {
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(nvgContext);
        NanoVG.nvgMoveTo(nvgContext, x1, y1);
        NanoVG.nvgLineTo(nvgContext, x2, y2);
        NanoVG.nvgLineTo(nvgContext, x3, y3);
        NanoVG.nvgClosePath(nvgContext);

        NanoVG.nvgStrokeWidth(nvgContext, strokeWidth);
        NanoVG.nvgStrokeColor(nvgContext, nvgColor);
        NanoVG.nvgStroke(nvgContext);
    }

}

