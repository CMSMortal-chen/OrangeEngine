package cms.mortalchen.orange.widget.impl;

import cms.mortalchen.orange.widget.Widget;
import lombok.NonNull;
import org.joml.Vector4f;

public class ButtonWidget extends Widget {
    public Runnable triggerAction;

    public ButtonWidget(@NonNull Vector4f renderPos, Runnable triggerAction) {
        super(renderPos);
        this.triggerAction = triggerAction;
    }

    @Override
    public void onRender(long nvg) {

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
