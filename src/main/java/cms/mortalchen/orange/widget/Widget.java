package cms.mortalchen.orange.widget;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.utils.InputUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.joml.Vector2f;
import org.joml.Vector4f;

@RequiredArgsConstructor
public abstract class Widget {
    @NonNull
    public Vector4f renderPos;

    public abstract void onRender(long nvg);

    public abstract void onTrigger();

    public abstract void onUpdate();

    public boolean isMouseHover() {
        Vector2f mousePos = OrangeEngine.instance.inputManager.mousePos;
        return InputUtils.isHovering(renderPos, mousePos.x, mousePos.y);
    }
}
