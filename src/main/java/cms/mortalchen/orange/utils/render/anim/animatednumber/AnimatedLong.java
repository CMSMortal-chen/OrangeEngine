package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.Motion;

public class AnimatedLong {
    Motion.Controller controller;

    public AnimatedLong(long initValue) {
        controller = new Motion.Controller(initValue);
    }

    public AnimatedLong() {
        controller = new Motion.Controller(0);
    }

    public void to(long target, long duration, Motion.Easing easing) {
        controller.to(target,duration,easing);
    }

    public long get() {
        return (long) controller.get();
    }

    public void set(long value) {
        controller.set(value);
    }
}
