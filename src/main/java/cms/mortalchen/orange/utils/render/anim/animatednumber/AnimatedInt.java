package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.Motion;

public class AnimatedInt {
    Motion.Controller controller;

    public AnimatedInt(int initValue) {
        controller = new Motion.Controller(initValue);
    }

    public AnimatedInt() {
        controller = new Motion.Controller(0);
    }

    public void to(int target, int duration, Motion.Easing easing) {
        controller.to(target,duration,easing);
    }

    public int get() {
        return (int) controller.get();
    }

    public void set(int value) {
        controller.set(value);
    }
}
