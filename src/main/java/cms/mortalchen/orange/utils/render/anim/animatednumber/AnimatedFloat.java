package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.AnimationUtil;

public class AnimatedFloat {
    public AnimatedFloat(float initValue) {
        value = initValue;
    }

    public AnimatedFloat() {

    }

    public float value = 0f;

    public void move(float target, float speed) {
        if (Float.isNaN(value) || Float.isInfinite(value)) value = 0f;
        value = AnimationUtil.animate(target, value, speed);
    }

    public void moveUD(float target, float smoothSpeed, float minSpeed) {
        if (Float.isNaN(value) || Float.isInfinite(value)) value = 0f;
        value = AnimationUtil.moveUD(value, target, smoothSpeed, minSpeed);
    }

    public float get() {
        return value;
    }

    public void set(float value) {
        this.value = value;
    }
}
