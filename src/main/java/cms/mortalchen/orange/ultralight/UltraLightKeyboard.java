package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.event.UlKeyCode;
import net.janrupf.ujr.api.event.UlKeyEventModifiers;

import java.util.EnumSet;

import static org.lwjgl.glfw.GLFW.*;

final class UltraLightKeyboard {
    private UltraLightKeyboard() {
    }

    static EnumSet<UlKeyEventModifiers> modifiers(int mods) {
        EnumSet<UlKeyEventModifiers> result = EnumSet.noneOf(UlKeyEventModifiers.class);
        if ((mods & GLFW_MOD_SHIFT) != 0) {
            result.add(UlKeyEventModifiers.SHIFT);
        }
        if ((mods & GLFW_MOD_CONTROL) != 0) {
            result.add(UlKeyEventModifiers.CTRL);
        }
        if ((mods & GLFW_MOD_ALT) != 0) {
            result.add(UlKeyEventModifiers.ALT);
        }
        if ((mods & GLFW_MOD_SUPER) != 0) {
            result.add(UlKeyEventModifiers.META);
        }
        return result;
    }

    static int keyCode(int glfwKeyCode) {
        return switch (glfwKeyCode) {
            case GLFW_KEY_SPACE -> UlKeyCode.SPACE;
            case GLFW_KEY_APOSTROPHE -> UlKeyCode.OEM_7;
            case GLFW_KEY_COMMA -> UlKeyCode.OEM_COMMA;
            case GLFW_KEY_MINUS -> UlKeyCode.OEM_MINUS;
            case GLFW_KEY_PERIOD -> UlKeyCode.OEM_PERIOD;
            case GLFW_KEY_SLASH -> UlKeyCode.OEM_2;
            case GLFW_KEY_0 -> UlKeyCode.NUMBER_0;
            case GLFW_KEY_1 -> UlKeyCode.NUMBER_1;
            case GLFW_KEY_2 -> UlKeyCode.NUMBER_2;
            case GLFW_KEY_3 -> UlKeyCode.NUMBER_3;
            case GLFW_KEY_4 -> UlKeyCode.NUMBER_4;
            case GLFW_KEY_5 -> UlKeyCode.NUMBER_5;
            case GLFW_KEY_6 -> UlKeyCode.NUMBER_6;
            case GLFW_KEY_7 -> UlKeyCode.NUMBER_7;
            case GLFW_KEY_8 -> UlKeyCode.NUMBER_8;
            case GLFW_KEY_9 -> UlKeyCode.NUMBER_9;
            case GLFW_KEY_SEMICOLON -> UlKeyCode.OEM_1;
            case GLFW_KEY_EQUAL, GLFW_KEY_KP_EQUAL -> UlKeyCode.OEM_PLUS;
            case GLFW_KEY_A -> UlKeyCode.A;
            case GLFW_KEY_B -> UlKeyCode.B;
            case GLFW_KEY_C -> UlKeyCode.C;
            case GLFW_KEY_D -> UlKeyCode.D;
            case GLFW_KEY_E -> UlKeyCode.E;
            case GLFW_KEY_F -> UlKeyCode.F;
            case GLFW_KEY_G -> UlKeyCode.G;
            case GLFW_KEY_H -> UlKeyCode.H;
            case GLFW_KEY_I -> UlKeyCode.I;
            case GLFW_KEY_J -> UlKeyCode.J;
            case GLFW_KEY_K -> UlKeyCode.K;
            case GLFW_KEY_L -> UlKeyCode.L;
            case GLFW_KEY_M -> UlKeyCode.M;
            case GLFW_KEY_N -> UlKeyCode.N;
            case GLFW_KEY_O -> UlKeyCode.O;
            case GLFW_KEY_P -> UlKeyCode.P;
            case GLFW_KEY_Q -> UlKeyCode.Q;
            case GLFW_KEY_R -> UlKeyCode.R;
            case GLFW_KEY_S -> UlKeyCode.S;
            case GLFW_KEY_T -> UlKeyCode.T;
            case GLFW_KEY_U -> UlKeyCode.U;
            case GLFW_KEY_V -> UlKeyCode.V;
            case GLFW_KEY_W -> UlKeyCode.W;
            case GLFW_KEY_X -> UlKeyCode.X;
            case GLFW_KEY_Y -> UlKeyCode.Y;
            case GLFW_KEY_Z -> UlKeyCode.Z;
            case GLFW_KEY_LEFT_BRACKET -> UlKeyCode.OEM_4;
            case GLFW_KEY_BACKSLASH -> UlKeyCode.OEM_5;
            case GLFW_KEY_RIGHT_BRACKET -> UlKeyCode.OEM_6;
            case GLFW_KEY_GRAVE_ACCENT -> UlKeyCode.OEM_3;
            case GLFW_KEY_ESCAPE -> UlKeyCode.ESCAPE;
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> UlKeyCode.RETURN;
            case GLFW_KEY_TAB -> UlKeyCode.TAB;
            case GLFW_KEY_BACKSPACE -> UlKeyCode.BACK;
            case GLFW_KEY_INSERT -> UlKeyCode.INSERT;
            case GLFW_KEY_DELETE -> UlKeyCode.DELETE;
            case GLFW_KEY_RIGHT -> UlKeyCode.RIGHT;
            case GLFW_KEY_LEFT -> UlKeyCode.LEFT;
            case GLFW_KEY_DOWN -> UlKeyCode.DOWN;
            case GLFW_KEY_UP -> UlKeyCode.UP;
            case GLFW_KEY_PAGE_UP -> UlKeyCode.PRIOR;
            case GLFW_KEY_PAGE_DOWN -> UlKeyCode.NEXT;
            case GLFW_KEY_HOME -> UlKeyCode.HOME;
            case GLFW_KEY_END -> UlKeyCode.END;
            case GLFW_KEY_F1 -> UlKeyCode.F1;
            case GLFW_KEY_F2 -> UlKeyCode.F2;
            case GLFW_KEY_F3 -> UlKeyCode.F3;
            case GLFW_KEY_F4 -> UlKeyCode.F4;
            case GLFW_KEY_F5 -> UlKeyCode.F5;
            case GLFW_KEY_F6 -> UlKeyCode.F6;
            case GLFW_KEY_F7 -> UlKeyCode.F7;
            case GLFW_KEY_F8 -> UlKeyCode.F8;
            case GLFW_KEY_F9 -> UlKeyCode.F9;
            case GLFW_KEY_F10 -> UlKeyCode.F10;
            case GLFW_KEY_F11 -> UlKeyCode.F11;
            case GLFW_KEY_F12 -> UlKeyCode.F12;
            default -> UlKeyCode.UNKNOWN;
        };
    }
}
