package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import cms.mortalchen.orange.utils.render.anim.Motion;

public class AnimatedFloat {
    Motion.Controller controller;

    public AnimatedFloat(float initValue) {
        controller = new Motion.Controller(initValue);
    }

    public AnimatedFloat() {
        controller = new Motion.Controller(0);
    }

    public void to(float target, long duration, Motion.Easing easing) {
        controller.to(target,duration,easing);
    }

    public float get() {
        return controller.getF();
    }

    public void set(float value) {
        controller.set(value);
    }
}
