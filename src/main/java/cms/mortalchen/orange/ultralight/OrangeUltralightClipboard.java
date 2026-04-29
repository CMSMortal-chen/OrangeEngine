package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.clipboard.UltralightClipboard;

import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;

final class OrangeUltralightClipboard implements UltralightClipboard {
    private final long window;

    OrangeUltralightClipboard(long window) {
        this.window = window;
    }

    @Override
    public void clear() {
        glfwSetClipboardString(window, "");
    }

    @Override
    public String readPlainText() {
        String value = glfwGetClipboardString(window);
        return value == null ? "" : value;
    }

    @Override
    public void writePlainText(String text) {
        glfwSetClipboardString(window, text == null ? "" : text);
    }
}
