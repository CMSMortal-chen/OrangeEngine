package cms.mortalchen.orange.render.font;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGTextRow;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Font {
    private final long nvg;
    private String fontName;
    private String fontFormat;

    public Font(long nvg, String fontName, String fontFormat) {
        this.nvg = nvg;
        this.fontName = fontName;
        this.fontFormat = fontFormat;
        loadFont();
    }

    public Font(long nvg, String fontName) {
        this.nvg = nvg;
        this.fontName = fontName;
        this.fontFormat = "ttf";
        loadFont();
    }

    public void loadFont() {
        String fontFilePath = "textures/fonts/" + fontName + "." + fontFormat;
        try (InputStream inputStream = Font.class.getClassLoader().getResourceAsStream(fontFilePath)) {
            assert inputStream != null : "Error: Could not open file for font: '" + fontName + "'";
            byte[] fontData = inputStream.readAllBytes();
            ByteBuffer fontDataBuffer = MemoryUtil.memAlloc(fontData.length + 1);
            fontDataBuffer.put(fontData).put((byte) 0).flip();

            ByteBuffer fontNameData = MemoryUtil.memUTF8(fontName);
            if (nvgCreateFontMem(nvg, fontNameData, fontDataBuffer, true) == -1) {
                throw new IOException("Failed to load font: " + fontName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.fontName = "HarmonyOS_Sans_SC_Regular";
            this.fontFormat = "ttf";
            loadFont();
        }
    }

    public void drawText(String text, int alignment, float x, float y, float fontSize, Color color) {
        // 创建颜色对象并设置
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        nvgFontSize(nvg, fontSize);
        nvgFontFace(nvg, fontName);
        nvgTextAlign(nvg, alignment);
        nvgFillColor(nvg, nvgColor);
        nvgText(nvg, x, y, text);


    }

    public void drawText(String text, int alignment, float x, float y, float fontSize, NVGColor nvgColor) {
        nvgFontSize(nvg, fontSize);
        nvgFontFace(nvg, fontName);
        nvgTextAlign(nvg, alignment);
        nvgFillColor(nvg, nvgColor);
        nvgText(nvg, x, y, text);
    }

    public void drawText(String text, float x, float y, float fontSize, Color color) {
        // 创建颜色对象并设置
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        nvgFontSize(nvg, fontSize);
        nvgFontFace(nvg, fontName);
        nvgTextAlign(nvg, NVG_ALIGN_TOP | NVG_ALIGN_LEFT);
        nvgFillColor(nvg, nvgColor);
        nvgText(nvg, x, y, text);

    }

    public void drawTextWithShadow(String text, float x, float y, float fontSize, Color color) {
        // 创建颜色对象并设置
        drawText(text, x + 1f, y + 1f, fontSize, new Color(0,0,0, (int) (150 * (color.getAlpha() / 255f))));
        drawText(text, x, y, fontSize, color);
    }

    public void drawTextWithShadow(String text, int alignment, float x, float y, float fontSize, Color color) {
        // 创建颜色对象并设置
        drawText(text, alignment, x + 1f, y + 1f, fontSize, new Color(0,0,0, (int) (150 * (color.getAlpha() / 255f))));
        drawText(text, alignment, x, y, fontSize, color);
    }

    public void drawCenteredText(String text, float x, float y, float fontSize, Color color) {
        // 创建颜色对象并设置
        NVGColor nvgColor = NVGColor.create()
                .r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        nvgFontSize(nvg, fontSize);
        nvgFontFace(nvg, fontName);
        nvgTextAlign(nvg, NVG_ALIGN_TOP | NVG_ALIGN_CENTER);
        nvgFillColor(nvg, nvgColor);
        nvgText(nvg, x, y, text);


    }

    public float getStringWidth(String text, float fontSize) {
        try (MemoryStack stack = stackPush()) {
            nvgFontFace(nvg, fontName);
            nvgFontSize(nvg, fontSize);

            NVGTextRow.Buffer textRows = NVGTextRow.calloc(1, stack);
            nvgTextBreakLines(nvg, text, Float.MAX_VALUE, textRows);

            return textRows.get(0).width();
        }
    }

    public float getStringHeight(float fontSize) {
        nvgFontFace(nvg, fontName);
        nvgFontSize(nvg, fontSize);

        float[] ascender = new float[1];
        float[] descender = new float[1];
        float[] lineHeight = new float[1];
        nvgTextMetrics(nvg, ascender, descender, lineHeight);

        return ascender[0] - descender[0];
    }
}
