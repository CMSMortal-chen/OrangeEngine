package cms.mortalchen.orange.utils.render.anim;


public class SmoothAnimationTimer {
    public float target;

    public float speed = 0.3f;

    public SmoothAnimationTimer(float target) {
        this.target = target;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public SmoothAnimationTimer(float target, float speed) {
        this.target = target;
        this.speed = speed;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    private float value = 0;

    public boolean update(boolean increment) {
        this.value = AnimationUtil.moveUD(value, increment ? target : 0, speed, speed - 5f);
        return value == target;
    }
}
