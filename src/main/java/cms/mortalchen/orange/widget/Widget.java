package cms.mortalchen.orange.widget;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.utils.InputUtils;
import org.joml.Vector2f;
import org.joml.Vector4f;

public abstract class Widget {
    public Vector4f renderPos;
    public float width, height;

    public Widget(Vector4f renderPos) {
        this.renderPos = renderPos;
        width = Math.max(renderPos.x, renderPos.z) - Math.min(renderPos.x, renderPos.z);
        height = Math.max(renderPos.y, renderPos.w) - Math.min(renderPos.y, renderPos.w);
    }

    public void onRender(long nvg) {
        width = Math.max(renderPos.x, renderPos.z) - Math.min(renderPos.x, renderPos.z);
        height = Math.max(renderPos.y, renderPos.w) - Math.min(renderPos.y, renderPos.w);
    }

    public abstract void onTrigger();

    public abstract void onUpdate();

    public boolean isMouseHover() {
        Vector2f mousePos = OrangeEngine.instance.inputManager.mousePos;
        return InputUtils.isHovering(renderPos, mousePos.x, mousePos.y);
    }
}
