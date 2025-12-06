package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.Motion;

public class AnimatedDouble {
    Motion.Controller controller;

    public AnimatedDouble(double initValue) {
        controller = new Motion.Controller(initValue);
    }

    public AnimatedDouble() {
        controller = new Motion.Controller(0);
    }

    public void to(double target, long duration, Motion.Easing easing) {
        controller.to(target,duration,easing);
    }

    public double get() {
        return controller.get();
    }

    public void set(double value) {
        controller.set(value);
    }
}
