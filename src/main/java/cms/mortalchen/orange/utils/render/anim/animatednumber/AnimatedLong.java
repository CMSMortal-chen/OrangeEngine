package cms.mortalchen.orange.utils.render.anim.animatednumber;


import cms.mortalchen.orange.utils.render.anim.AnimationUtil;

public class AnimatedLong {
    public long value = 0;
    public void move(long target, long speed){
        value = (long) AnimationUtil.animate(target,value,speed);
    }
    public void moveUD(int target, float smoothSpeed, float minSpeed) {
        value = (long) AnimationUtil.moveUD(value, target, smoothSpeed, minSpeed);
    }
    public long get(){
        return value;
    }
}
