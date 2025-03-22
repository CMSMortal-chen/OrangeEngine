package cms.mortalchen.orange.render.menuflow;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.AnimatedColor;
import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import cms.mortalchen.orange.utils.render.color.Palette;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

@RequiredArgsConstructor
public class MenuFlowObj {
    @NonNull
    public Integer index;
    @NonNull
    public String text;

    @NonNull
    @Setter
    public Float fontSize;
    public float fontSizeAnim = Float.NEGATIVE_INFINITY;
    public float fontSizeAnimTarget = Float.NEGATIVE_INFINITY;
    @NonNull
    public Font font;

    float x, y;
    float selectAnim = 0f;

    @NonNull
    public AnimatedColor animatedColor;
    public AnimatedColor selectAnimatedColor = new AnimatedColor(new Color(0,0,0,0),14f);
    public Color selectAnimatedColorTarget = new Color(255, 255, 255,255);
    public Color unSelectAnimatedColorTarget = new Color(204, 250, 180,0);

    @NonNull
    public Runnable runnable;

    public void render(long vg, float x, float y, Color color) {
        if (Float.isInfinite(fontSizeAnim) || Float.isNaN(fontSizeAnim)) {
            fontSizeAnim = fontSize;
        }
        if (Float.isInfinite(fontSizeAnimTarget) || Float.isNaN(fontSizeAnimTarget)) {
            fontSizeAnimTarget = fontSize;
        }

        this.x = x;
        this.y = y;
        animatedColor.setTranslate(color);
        animatedColor.translate();
        selectAnimatedColor.translate();

        RenderUtils.drawRect(vg, (50f - selectAnim) + x + font.getStringWidth(text, fontSizeAnim) + 20, y, (50f - selectAnim) + x + font.getStringWidth(text, fontSizeAnim) + 30, y + font.getStringHeight(fontSizeAnim), selectAnimatedColor.getColor());

        //RenderUtils.drawRect(vg, x - 3, y, x + font.getStringWidth(text, fontSizeAnim) + 30, y + font.getStringHeight(fontSizeAnim), new Color(200, 205, 183));

        font.drawText(text, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE, x, y + 3f + font.getStringHeight(fontSizeAnim) / 2f, fontSizeAnim, animatedColor.getColor());
    }

    public void update(int index) {
        if (this.index == index) {
            fontSizeAnimTarget = fontSize + (fontSize / 2f);
            selectAnimatedColor.setTranslate(selectAnimatedColorTarget);
        }else {
            fontSizeAnimTarget = fontSize;
            selectAnimatedColor.setTranslate(unSelectAnimatedColorTarget);
        }
        selectAnim = AnimationUtil.moveUD(selectAnim, this.index == index ? 50f : 0f, 15f, 10f);
        fontSizeAnim = AnimationUtil.moveUD(fontSizeAnim, fontSizeAnimTarget, 14f, 8f);
    }

    public void trigger(int index) {
        if (this.index == index) {
            runnable.run();
        }
    }
}
