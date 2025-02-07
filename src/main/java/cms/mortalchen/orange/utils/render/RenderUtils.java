package cms.mortalchen.orange.utils.render;

import cms.mortalchen.orange.utils.Utils;
import lombok.experimental.UtilityClass;
import org.joml.Vector2f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

@UtilityClass
public class RenderUtils {
    public void renderPauseMenu(long vg, float screenWidth, float screenHeight) {
        NVGColor color = NVGColor.create();
        color.r(0.18f);
        color.g(0.18f);
        color.b(0.18f);
        color.a(0.66f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, 0, 0, screenWidth, screenHeight);
        NanoVG.nvgFillColor(vg, color);
        NanoVG.nvgFill(vg);
    }

    public void renderLine(long vg, float x1, float y1, float x2, float y2) {
        drawLine(vg, x1, y1, x2, y2, 2f, Color.BLACK);
    }

    public void drawLine(long vg, float x1, float y1, float x2, float y2, float width, Color color) {
        // 创建颜色对象并设置
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
        try (NVGPaint paint = NVGPaint.create()) {
            NanoVG.nvgLinearGradient(vg, x, y, x2, y, nvgColor, nvgColorTo, paint);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
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
        try (NVGPaint paint = NVGPaint.create()) {
            NanoVG.nvgLinearGradient(vg, x, y, x, y2, nvgColor, nvgColorTo, paint);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    private void drawEntity(long vg, Vector2f position, float width, float height) {
        NVGColor color = NVGColor.create();
        color.r(1f);
        color.g(1f);
        color.b(1f);
        color.a(1f);

        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, position.x, position.y, width, height);
        NanoVG.nvgFillColor(vg, color);
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
        if (nvgContext == 0) throw new IllegalStateException("未初始化 NanoVG 上下文");

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

    // 绘制描边三角形
    public static void strokeTriangle(
            long nvgContext,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float strokeWidth, // 描边宽度
            Color color // 颜色
    ) {
        if (nvgContext == 0) throw new IllegalStateException("未初始化 NanoVG 上下文");

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