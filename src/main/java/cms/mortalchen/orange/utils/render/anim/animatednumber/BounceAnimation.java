package cms.mortalchen.orange.utils.render.anim.animatednumber;

import cms.mortalchen.orange.utils.math.MathUtils;
import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BounceAnimation {
    @NonNull
    public Float value;
    @NonNull
    public Float targetValue;
    public float bounceValue = 10f;
    @NonNull
    public Float speed;

    public boolean bounced = true;

    public void setTargetValue(float targetValue) {
        this.targetValue = targetValue;
    }

    public void bounce(float bounceValue) {
        this.bounceValue = bounceValue;
        bounced = false;
    }

    public void translate() {
        value = AnimationUtil.moveUD(value, targetValue + (bounced ? 0f : bounceValue), speed, Math.min(speed / 1.3f, 1f));
        if (Math.abs(value - targetValue - bounceValue) < Math.abs(bounceValue / 2.5f)) {
            bounced = true;
        }
        //System.out.println(Math.abs((value - targetValue) - bounceValue));
    }

    public float get() {
        return value;
    }
}
