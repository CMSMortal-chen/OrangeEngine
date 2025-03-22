package cms.mortalchen.orange.input;

import org.lwjgl.glfw.GLFW;

public enum KeyBinding {
    UP(GLFW.GLFW_KEY_W),
    DOWN(GLFW.GLFW_KEY_S),
    LEFT(GLFW.GLFW_KEY_A),
    RIGHT(GLFW.GLFW_KEY_D),
    INTERACT(GLFW.GLFW_KEY_J),
    CANCEL(GLFW.GLFW_KEY_K),
    ESCAPE(GLFW.GLFW_KEY_ESCAPE);

    int keyBind;

    KeyBinding(int keyBind) {
        this.keyBind = keyBind;
    }

    public void setKeyBind(int newKey) {
        this.keyBind = newKey;
    }

    public int get() {
        return keyBind;
    }
}
