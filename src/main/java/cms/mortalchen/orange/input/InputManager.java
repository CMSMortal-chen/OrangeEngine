package cms.mortalchen.orange.input;

import lombok.Getter;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class InputManager {
    private final long window;
    double[] xPos = new double[1];
    double[] yPos = new double[1];
    @Getter
    public Vector2f mousePos = new Vector2f();

    public InputManager(long window) {
        this.window = window;
    }

    public boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public void onUpdate() {
        GLFW.glfwGetCursorPos(window, xPos, yPos);
        mousePos.set(xPos[0], yPos[0]);
    }
}
