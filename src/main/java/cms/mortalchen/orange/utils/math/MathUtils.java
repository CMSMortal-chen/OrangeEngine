package cms.mortalchen.orange.utils.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {
    public static final float EPSILON = 1e-6f;

    public float diff(float value1, float value2) {
        return Math.abs(value1 - value2);
    }
    public double diff(double value1, double value2) {
        return Math.abs(value1 - value2);
    }
    public int diff(int value1, int value2) {
        return Math.abs(value1 - value2);
    }

    public static double getFractionalPart(double number) {
        double integerPart = (number >= 0) ? Math.floor(number) : Math.ceil(number);
        return number - integerPart;
    }
    public static float getFractionalPart(float number) {
        float integerPart = (float) ((number >= 0) ? Math.floor(number) : Math.ceil(number));
        return number - integerPart;
    }

    public boolean isEqual(float value, float value2) {
        return diff(value, value2) < EPSILON;
    }
}
