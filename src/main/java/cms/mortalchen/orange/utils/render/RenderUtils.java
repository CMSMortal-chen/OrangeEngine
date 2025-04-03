package cms.mortalchen.orange.utils.render;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.Utils;
import lombok.experimental.UtilityClass;
import org.joml.Vector2f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.nanovg.NanoVG.*;

@UtilityClass
public class RenderUtils {
    public void drawLine(long vg, float x1, float y1, float x2, float y2, float width, Color color) {
        // 创建颜色（RGBA）
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

    public void drawRoundedRect(long vg, float x, float y, float x2, float y2, float radius, Color color) {
        // 创建颜色对象并设置
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

    public void drawCircleStroke(long vg,float cx, float cy, float radius, float width,Color color) {
        // 创建颜色（RGBA）
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        // 开始路径
        NanoVG.nvgBeginPath(vg);

        // 定义圆形路径
        NanoVG.nvgCircle(vg, cx, cy, radius);
        // 可选：设置描边颜色和宽度
        NanoVG.nvgStrokeColor(vg, nvgColor);
        NanoVG.nvgStrokeWidth(vg, width);
        NanoVG.nvgStroke(vg);

        // 设置填充颜色并填充
        NanoVG.nvgFillColor(vg, Utils.color(0,0,0,0));
        NanoVG.nvgFill(vg);
    }

    public void drawCircle(long vg,float cx, float cy, float radius, Color color) {
        // 创建颜色（RGBA）
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        // 开始路径
        NanoVG.nvgBeginPath(vg);

        // 定义圆形路径
        NanoVG.nvgCircle(vg, cx, cy, radius);

        // 设置填充颜色并填充
        NanoVG.nvgFillColor(vg, nvgColor);
        NanoVG.nvgFill(vg);
    }

    public void drawRect(long vg, float x, float y, float x2, float y2, Color color) {
        // 创建颜色对象并设置
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x, y, (Math.max(x, x2) - Math.min(x, x2)), (Math.max(y, y2) - Math.min(y, y2)));
        NanoVG.nvgFillColor(vg, nvgColor);
        NanoVG.nvgFill(vg);
    }

    public void drawGradientSideWay(long vg, float x, float y, float x2, float y2, Color color,Color colorTo) {
        // 创建颜色对象并设置
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

    public void drawGradientUpDown(long vg, float x, float y, float x2, float y2, Color color,Color colorTo) {
        // 创建颜色对象并设置
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

    // 绘制填充三角形
    public static void fillTriangle(
            long nvgContext,
            float x1, float y1, // 顶点1
            float x2, float y2, // 顶点2
            float x3, float y3, // 顶点3
            Color color // 颜色
    ) {
        // 创建颜色对象并设置
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        // 绘制路径
        NanoVG.nvgBeginPath(nvgContext);
        NanoVG.nvgMoveTo(nvgContext, x1, y1);
        NanoVG.nvgLineTo(nvgContext, x2, y2);
        NanoVG.nvgLineTo(nvgContext, x3, y3);
        NanoVG.nvgClosePath(nvgContext);

        // 填充颜色
        NanoVG.nvgFillColor(nvgContext, nvgColor);
        NanoVG.nvgFill(nvgContext);

 // 释放颜色对象
    }

    public static void drawImage(long nvg, ResourceLocation location, float x, float y, float width, float height, int alpha, Color color) {
        nvgBeginPath(nvg);
        NVGPaint paint = nvgImagePattern(nvg, x, y, width, height, 0.0F, location.getId(), alpha / 255.0F, NVGPaint.create());

        // 创建颜色对象并设置
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

    public static void drawTileImage(long nvg, ResourceLocation location, float x, float y ,float width, float height, int u, int v, float scales ,float uWidth, float uHeight, int alpha, Color color) {
        nvgBeginPath(nvg);
        NVGPaint paint = nvgImagePattern(nvg, x - (u * scales), y - (v * scales), width * scales, height * scales, 0.0F, location.getId(), alpha / 255.0F, NVGPaint.create());

        // 创建颜色对象并设置
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


    // 绘制描边三角形
    public static void strokeTriangle(
            long nvgContext,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float strokeWidth, // 描边宽度
            Color color // 颜色
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

        // 设置描边样式
        NanoVG.nvgStrokeWidth(nvgContext, strokeWidth);
        NanoVG.nvgStrokeColor(nvgContext, nvgColor);
        NanoVG.nvgStroke(nvgContext);
    }
}