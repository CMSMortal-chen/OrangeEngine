package cms.mortalchen.orange.utils.render.color;

import lombok.experimental.UtilityClass;

import java.awt.*;

@UtilityClass
public class Palette {
    /**
     * Creates a new <code>Color</code> that is a brighter version of this
     * <code>Color</code>.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this <code>Color</code> to create a brighter version
     * of this <code>Color</code>.
     * The {@code alpha} value is preserved.
     * Although <code>brighter</code> and
     * <code>darker</code> are inverse operations, the results of a
     * series of invocations of these two methods might be inconsistent
     * because of rounding errors.
     * @return     a new <code>Color</code> object that is
     *                 a brighter version of this <code>Color</code>
     *                 with the same {@code alpha} value.
     * @see        Color#darker
     * @since      JDK1.0
     */
    public Color brighter(Color color ,double FACTOR) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / FACTOR), 255),
                Math.min((int) (g / FACTOR), 255),
                Math.min((int) (b / FACTOR), 255),
                alpha);
    }

    /**
     * Creates a new <code>Color</code> that is a darker version of this
     * <code>Color</code>.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this <code>Color</code> to create a darker version of
     * this <code>Color</code>.
     * The {@code alpha} value is preserved.
     * Although <code>brighter</code> and
     * <code>darker</code> are inverse operations, the results of a series
     * of invocations of these two methods might be inconsistent because
     * of rounding errors.
     * @return  a new <code>Color</code> object that is
     *                    a darker version of this <code>Color</code>
     *                    with the same {@code alpha} value.
     * @see        Color#brighter
     * @since      JDK1.0
     */
    public Color darker(Color color,double FACTOR) {
        return new Color((int) Math.max(color.getRed() * FACTOR, 0),
                (int) Math.max(color.getGreen() * FACTOR, 0),
                (int) Math.max(color.getBlue() * FACTOR, 0),
                color.getAlpha());
    }
    public static Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0f - r;
        //float[] rgb1 = color1.getColorComponents(new float[4]);
        //float[] rgb2 = color2.getColorComponents(new float[4]);
        float red = (color1.getRed() / 255f) * r + (color2.getRed() / 255f) * ir;
        float green = (color1.getGreen() / 255f) * r + (color2.getGreen() / 255f) * ir;
        float blue = (color1.getBlue() / 255f) * r + (color2.getBlue() / 255f) * ir;
        float alpha = (color1.getAlpha() / 255f) * r + (color2.getAlpha() / 255f) * ir;
        if (red < 0.0f) {
            red = 0.0f;
        } else if (red > 255.0f) {
            red = 255.0f;
        }
        if (green < 0.0f) {
            green = 0.0f;
        } else if (green > 255.0f) {
            green = 255.0f;
        }
        if (blue < 0.0f) {
            blue = 0.0f;
        } else if (blue > 255.0f) {
            blue = 255.0f;
        }
        if (alpha < 0.0f) {
            alpha = 0.0f;
        } else if (alpha > 255.0f) {
            alpha = 255.0f;
        }
        Color color3 = null;
        try {
            color3 = new Color(red, green, blue, alpha);
        } catch (IllegalArgumentException illegalArgumentException) {
            System.err.println("Color blend error!");
            illegalArgumentException.printStackTrace();
            color3 = Color.WHITE;
        }
        return color3;
    }
    public static int reAlpha(int color, float alpha) {
        Color c = new Color(color);
        return (new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha)).getRGB();
    }
    public static Color reAlpha(Color color, float alpha) {
        alpha = Math.min(Math.abs(alpha), 1f);
        return (new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha));
    }
    public static Color inverse(Color color) {
        return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue(),color.getAlpha());
    }
}
