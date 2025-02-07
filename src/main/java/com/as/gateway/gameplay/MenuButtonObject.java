package com.as.gateway.gameplay;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.input.KeyBindingManager;
import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.utils.Utils;
import lombok.Setter;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class MenuButtonObject {
    private final MainGame game;
    public float x;
    public float y;
    public float width;
    public float height;
    public int selectedButtonIndex;
    public int id;
    public int fontSize;
    public String text;
    @Setter
    private Runnable onActivated;

    public MenuButtonObject(MainGame game, int id, float x, float y, float width, float height, String text, int fontSize) {
        this.game = game;
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.fontSize = fontSize;
        this.selectedButtonIndex = 0;
    }

    public void renderButton(long vg, Font fontManager, int alignment) {
        float textWidth = fontManager.getStringWidth(text, fontSize);
        float textX = x;
        float textY = y;

        //RenderUtils.drawRoundedRect(vg, x0, y0, x0 + width, y0 + height, 4f);

        if (selectedButtonIndex == id) {
            fontManager.drawText("> " + text, alignment, textX, textY, fontSize, Utils.color(1.0f, 1.0f, 1.0f, 1.0f));
        } else {
            fontManager.drawText(text, alignment, textX, textY, fontSize, Utils.color(0.6f, 0.6f, 0.6f, 1.0f));
        }
    }

    public void onKeyPressed(int key, int action, List<MenuButtonObject> menuButtonObjects) {
        boolean keyPressed = action == GLFW_PRESS;

        if (keyPressed && key == KeyBindingManager.Keys.UP.get() && selectedButtonIndex > 0) {
            selectedButtonIndex--;
        } else if (keyPressed && key == KeyBindingManager.Keys.DOWN.get() && selectedButtonIndex < menuButtonObjects.size() - 1) {
            selectedButtonIndex++;
        } else if (keyPressed && (key == KeyBindingManager.Keys.INTERACT.get()) && onActivated != null) {
            menuButtonObjects.get(selectedButtonIndex).onActivated.run();
        }
    }

    public void updatePos(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
