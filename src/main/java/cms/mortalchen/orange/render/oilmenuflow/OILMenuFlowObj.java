package cms.mortalchen.orange.render.oilmenuflow;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.utils.render.anim.AnimatedColor;
import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

@RequiredArgsConstructor
public class OILMenuFlowObj {
    @NonNull
    public Integer index;
    @NonNull
    public String text;

    @NonNull
    @Setter
    public Float fontSize;
    public float fontSizeAnim;
    public float fontSizeAnimTarget;
    @NonNull
    public Font font;

    float x, y;
    float xAnim, yAnim = Float.NEGATIVE_INFINITY;

    @NonNull
    public AnimatedColor animatedColor;

    @NonNull
    public Runnable runnable;

    public void render(long vg, float x, float y, Color color) {
        this.x = x;
        this.y = y;
        if (Float.isInfinite(xAnim)) {
            xAnim = x;
        }
        if (Float.isInfinite(yAnim)) {
            yAnim = y;
        }
        animatedColor.setTranslate(color);
        animatedColor.translate();
        font.drawText(text, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE, xAnim, yAnim, fontSize, animatedColor.getColor());
    }

    public void update(int index) {
//        fontSizeAnim = AnimationUtil.moveUD(fontSizeAnim,fontSizeAnimTarget,14f,8f);
        if (this.index == index) {
            //fontSizeAnimTarget = fontSize + 5;
            xAnim = AnimationUtil.moveUD(xAnim, x + (MainGame.WINDOW_WIDTH / 2.5f) - font.getStringWidth(text, fontSize), 14f, 8f);
        } else {
            xAnim = AnimationUtil.moveUD(xAnim, x, 14f, 8f);
            //fontSizeAnimTarget = fontSize;
        }
        yAnim = AnimationUtil.moveUD(yAnim, y, 14f, 8f);
    }

    public void trigger(int index) {
        if (this.index == index) {
            runnable.run();
        }
    }
}
