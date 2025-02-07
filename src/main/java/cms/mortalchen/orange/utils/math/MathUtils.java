package cms.mortalchen.orange.utils.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {
    public float diff(float value1, float value2) {
        return Math.abs(value1 - value2);
    }
    public double diff(double value1, double value2) {
        return Math.abs(value1 - value2);
    }
    public int diff(int value1, int value2) {
        return Math.abs(value1 - value2);
    }
}
