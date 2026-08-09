package cms.mortalchen.orange.input;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

/**
 * 轮询式输入：按键状态直接查 GLFW，光标位置每帧（onUpdate）刷新。
 */
public class InputManager {
    private final long windowHandle;
    private final double[] cursorX = new double[1];
    private final double[] cursorY = new double[1];
    /** 公开字段：部分调用方（Widget 等）直接读取 */
    public final Vector2f mousePos = new Vector2f();

    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    /** 按键是否处于按下（held）状态 */
    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
    }

    /** 每帧刷新光标位置（供各状态在渲染/逻辑中读取） */
    public void onUpdate() {
        GLFW.glfwGetCursorPos(windowHandle, cursorX, cursorY);
        mousePos.set((float) cursorX[0], (float) cursorY[0]);
    }

    public Vector2f getMousePos() {
        return mousePos;
    }
}
