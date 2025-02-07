package cms.mortalchen.orange.utils.render.anim;

import cms.mortalchen.orange.utils.render.color.Palette;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

public class AnimatedColor {
    @Getter
    @Setter
    public Color color;
    public Color targetColor;

    public float progress = 100f;
    public float speed;

    public AnimatedColor(int red, int green, int blue, float speed) {
        color = new Color(red, green, blue);
        targetColor = color;
        this.speed = speed;
    }
    public AnimatedColor(int red, int green, int blue, int alpha ,float speed) {
        color = new Color(red, green, blue, alpha);
        targetColor = color;
        this.speed = speed;
    }

    public AnimatedColor(Color color, float speed) {
        this.color = color;
        targetColor = color;
        this.speed = speed;
    }

    public void setTranslate(Color color) {
        if (this.color != color && targetColor != color) {
            targetColor = color;
            progress = 100f;
        }
    }

    public void translate() {
        if (color != targetColor) {
            progress = AnimationUtil.moveUD(progress, 0f, speed, speed);
            color = Palette.blend(color, targetColor, progress / 100f);
        }
    }
}
