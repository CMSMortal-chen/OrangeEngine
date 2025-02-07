package cms.mortalchen.orange.widget.impl;

import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.AnimatedColor;
import cms.mortalchen.orange.utils.render.color.Palette;
import cms.mortalchen.orange.widget.Widget;
import lombok.NonNull;
import org.joml.Vector4f;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

public class ButtonWidget extends Widget {
    public Runnable triggerAction;
    public float radius = 0f;
    public AnimatedColor color, backgroundColor;
    public String text;
    private Font font;
    private float fontSize;

    public ButtonWidget(@NonNull Vector4f renderPos, String text, Font font, float fontSize, AnimatedColor color, AnimatedColor backgroundColor, Runnable triggerAction) {
        super(renderPos);
        this.triggerAction = triggerAction;
        this.text = text;
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.font = font;
        this.fontSize = fontSize;
    }

    public ButtonWidget(@NonNull Vector4f renderPos, String text, Font font, float fontSize, float radius, AnimatedColor color, AnimatedColor backgroundColor, Runnable triggerAction) {
        super(renderPos);
        this.triggerAction = triggerAction;
        this.radius = radius;
        this.text = text;
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.font = font;
        this.fontSize = fontSize;
    }


    @Override
    public void onRender(long nvg) {
        super.onRender(nvg);
        RenderUtils.drawRoundedRect(nvg, renderPos.x, renderPos.y, renderPos.z, renderPos.w, radius, backgroundColor.getColor());
        font.drawText(text, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE, width / 2f, height / 2f, color.getColor());
        if (isMouseHover()) {
            RenderUtils.drawRoundedRect(nvg, renderPos.x, renderPos.y, renderPos.z, renderPos.w, radius, Palette.reAlpha(Color.WHITE, 0.35f));
        }
    }

    @Override
    public void onTrigger() {
        if (isMouseHover()) {
            triggerAction.run();
        }
    }

    @Override
    public void onUpdate() {

    }
}
