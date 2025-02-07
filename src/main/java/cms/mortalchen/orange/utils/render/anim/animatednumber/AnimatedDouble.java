package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.AnimationUtil;

public class AnimatedDouble {
    public AnimatedDouble(double initValue){
        value = initValue;
    }
    public AnimatedDouble(){

    }
    public double value = 0d;

    public void move(double target, double speed) {
        if (Double.isNaN(value) || Double.isInfinite(value)) value = 0d;
        value = AnimationUtil.animate(target, value, speed);
    }

    public void moveUD(double target, float smoothSpeed, float minSpeed) {
        if (Double.isNaN(value) || Double.isInfinite(value)) value = 0d;
        value = AnimationUtil.moveUD(value, target, smoothSpeed, minSpeed);
    }

    public double get() {
        return value;
    }

    public void set(double value) {
        this.value = value;
    }
}
