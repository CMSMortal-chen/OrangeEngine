package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.ultralight.UltraLight;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class UltralightTestState extends GameState {
    private UltraLight ultraLight;
    private boolean initialized;

    public UltralightTestState(MainGame game) {
        super(game);
    }

    @Override
    public void render(long vg) {
        initIfNeeded();
        if (ultraLight != null) {
            ultraLight.render(vg, 0, 0, width, height);
        }
    }

    @Override
    public void update(float deltaTime) {
        initIfNeeded();
        if (ultraLight != null) {
            ultraLight.update();
        }
    }

    @Override
    public void updateDisplaySize(float width, float height) {
        super.updateDisplaySize(width, height);
        if (ultraLight != null) {
            ultraLight.resize((int) width, (int) height);
        }
    }

    @Override
    public void onMouseMove(double x, double y) {
        if (ultraLight != null) {
            ultraLight.mouseMove((float) x, (float) y);
        }
    }

    @Override
    public void onMouseClick(int mouseButton, int action, Vector2f mousePos) {
        if (ultraLight != null) {
            ultraLight.mouseButton(mouseButton, action, mousePos);
        }
    }

    @Override
    public void onMouseScroll(double xoffset, double yoffset) {
        if (ultraLight != null) {
            ultraLight.scroll(xoffset, yoffset);
        }
    }

    @Override
    public void onKeyPressed(int key, int scancode, int action, int mods) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_F10)) {
            game.changeState(new DefaultState(game));
            return;
        }
        if (ultraLight != null) {
            ultraLight.key(key, scancode, action, mods);
        }
    }

    @Override
    public void onChar(int codepoint) {
        if (ultraLight != null) {
            ultraLight.character(codepoint);
        }
    }

    private void initIfNeeded() {
        if (initialized) {
            return;
        }
        initialized = true;
        ultraLight = new UltraLight((int) width, (int) height, false);
        ultraLight.loadURL("https://html5test.co/");
    }
}
