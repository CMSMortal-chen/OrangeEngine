package cms.mortalchen.orange.oralightcef;

import static org.lwjgl.glfw.GLFW.*;

final class OraLightCEFKeyboard {
    private OraLightCEFKeyboard() {}

    static int glfwModsToAwt(int glfwMods) {
        // Native JCEF N_SendMouseEvent/N_SendKeyEvent reads modifiers via
        // getModifiers() and interprets them as GLFW modifier flags
        // (GLFW_MOD_SHIFT=1, GLFW_MOD_CONTROL=2, GLFW_MOD_ALT=4, GLFW_MOD_SUPER=8).
        // Return the GLFW mods directly — no conversion needed.
        return glfwMods;
    }

    static int keyCode(int glfwKeyCode) {
        // On Windows, N_SendKeyEvent ignores getKeyCode() and uses scancode.
        // On Linux/macOS, N_SendKeyEvent calls getKeyCode() and compares
        // against GLFW_KEY_* constants, so pass the GLFW code through directly.
        return glfwKeyCode;
    }
}
