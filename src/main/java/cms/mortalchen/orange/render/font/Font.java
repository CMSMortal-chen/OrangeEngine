package cms.mortalchen.orange.render.font;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGTextRow;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * NanoVG 字体封装：负责字体加载与文本绘制。
 * <p>
 * 性能：Color 重载复用线程本地 NVGColor（nvgFillColor 立即拷贝颜色值），
 * 避免每帧每次 drawText(Color) 都 NVGColor.create() 泄漏 DirectByteBuffer
 * （大量文本绘制时会造成周期性 GC 停顿）。
 */
public class Font {
    private static final String FALLBACK_FONT = "HarmonyOS_Sans_SC_Regular";

    private final long nvg;
    private String fontName;
    private String fontFormat;
    private final ThreadLocal<NVGColor> colorCache = ThreadLocal.withInitial(NVGColor::calloc);

    public Font(long nvg, String fontName, String fontFormat) {
        this.nvg = nvg;
        this.fontName = fontName;
        this.fontFormat = fontFormat;
        loadFont();
    }

    public Font(long nvg, String fontName) {
        this(nvg, fontName, "ttf");
    }

    public void loadFont() {
        String path = "fonts/" + fontName + "." + fontFormat;
        try (InputStream in = Font.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Font resource missing: " + path);
            }
            byte[] data = in.readAllBytes();
            ByteBuffer fontData = MemoryUtil.memAlloc(data.length + 1);
            fontData.put(data).put((byte) 0).flip();

            ByteBuffer fontNameData = MemoryUtil.memUTF8(fontName);
            try {
                // freeData=true：NanoVG 接管字体数据内存
                if (nvgCreateFontMem(nvg, fontNameData, fontData, true) == -1) {
                    throw new IOException("nvgCreateFontMem failed: " + fontName);
                }
            } finally {
                MemoryUtil.memFree(fontNameData);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 仅回退一次到内置中文字体，避免回退也失败时无限递归
            if (!FALLBACK_FONT.equals(fontName)) {
                fontName = FALLBACK_FONT;
                fontFormat = "ttf";
                loadFont();
            }
        }
    }

    public void drawTextWithShadow(String text, float x, float y, float fontSize, Color color) {
        drawText(text, x + 1f, y + 1f, fontSize, shadowColor(color));
        drawText(text, x, y, fontSize, color);
    }

    public void drawTextWithShadow(String text, int alignment, float x, float y, float fontSize, Color color) {
        drawText(text, alignment, x + 1f, y + 1f, fontSize, 0, shadowColor(color));
        drawText(text, alignment, x, y, fontSize, 0, color);
    }

    public void drawCenteredText(String text, float x, float y, float fontSize, Color color) {
        drawText(text, NVG_ALIGN_TOP | NVG_ALIGN_CENTER, x, y, fontSize, 0, color);
    }

    public void drawCenteredTextWithSpacing(String text, float x, float y, float fontSize, float spacing, Color color) {
        drawText(text, NVG_ALIGN_TOP | NVG_ALIGN_CENTER, x, y, fontSize, spacing, color);
    }

    public void drawTextWithSpacing(String text, float x, float y, float fontSize, float spacing, Color color) {
        drawText(text, NVG_ALIGN_TOP | NVG_ALIGN_LEFT, x, y, fontSize, spacing, color);
    }

    public void drawTextWithSpacing(String text, float x, float y, float fontSize, float spacing, NVGColor nvgColor) {
        drawText(text, NVG_ALIGN_TOP | NVG_ALIGN_LEFT, x, y, fontSize, spacing, nvgColor);
    }

    public void drawText(String text, float x, float y, float fontSize, Color color) {
        drawText(text, NVG_ALIGN_TOP | NVG_ALIGN_LEFT, x, y, fontSize, 0, color);
    }

    public void drawText(String text, float x, float y, float fontSize, NVGColor nvgColor) {
        drawText(text, NVG_ALIGN_TOP | NVG_ALIGN_LEFT, x, y, fontSize, 0, nvgColor);
    }

    public void drawText(String text, int alignment, float x, float y, float fontSize, Color color) {
        drawText(text, alignment, x, y, fontSize, 0, color);
    }

    public void drawText(String text, int alignment, float x, float y, float fontSize, NVGColor color) {
        drawText(text, alignment, x, y, fontSize, 0, color);
    }

    public void drawText(String text, int alignment, float x, float y, float fontSize, float spacing, Color color) {
        drawText(text, alignment, x, y, fontSize, spacing, toNvgColor(color));
    }

    public void drawText(String text, int alignment, float x, float y, float fontSize, float spacing, NVGColor nvgColor) {
        nvgFontSize(nvg, fontSize);
        nvgFontFace(nvg, fontName);
        nvgTextAlign(nvg, alignment);
        nvgTextLetterSpacing(nvg, spacing);
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

    /** Color → 复用缓冲 NVGColor（调用方不应持有返回对象跨 drawText 使用） */
    private NVGColor toNvgColor(Color color) {
        NVGColor c = colorCache.get();
        c.r(color.getRed() / 255f)
         .g(color.getGreen() / 255f)
         .b(color.getBlue() / 255f)
         .a(color.getAlpha() / 255f);
        return c;
    }

    private static Color shadowColor(Color color) {
        return new Color(0, 0, 0, (int) (150 * (color.getAlpha() / 255f)));
    }
}
