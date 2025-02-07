package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.AnimationUtil;

public class AnimatedInteger {
    public AnimatedInteger(long initValue) {
        value = initValue;
    }

    public AnimatedInteger() {

    }

    public long value = 0L;

    public void move(int target, int speed) {
        value = (int) AnimationUtil.animate(target, value, speed);
    }

    public void moveUD(int target, float smoothSpeed, float minSpeed) {
        value = (int) AnimationUtil.moveUD(value, target, smoothSpeed, minSpeed);
    }

    public double get() {
        return value;
    }
}
