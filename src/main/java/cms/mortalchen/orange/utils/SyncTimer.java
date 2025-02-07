package cms.mortalchen.orange.utils;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;

public class SyncTimer {

    final static double
            NANO_RESOLUTION = 1000000000.0D,
            GLFW_RESOLUTION = 1.0D;

    public final static int
            JAVA_NANO = 1,
            LWJGL_GLFW = 2;

    @Getter
    private int mode;
    private double timeThen;
    @Setter
    @Getter
    private boolean enabled = true;

    public SyncTimer() {
        setNewMode(LWJGL_GLFW);
    }

    public SyncTimer(int mode) {
        setNewMode(mode);
    }

    private double getResolution() {
        return switch (mode) {
            case JAVA_NANO -> NANO_RESOLUTION;
            case LWJGL_GLFW -> GLFW_RESOLUTION;
            default -> 0;
        };
    }

    private double getTime() {
        return switch (mode) {
            case JAVA_NANO -> System.nanoTime();
            case LWJGL_GLFW -> GLFW.glfwGetTime();
            default -> 0;
        };
    }


    public void setNewMode(int timerMode) {
        mode = timerMode;
        timeThen = getTime();
        System.out.println("Timer mode set to " + getModeString() + " timer");
    }

    public String getModeString() {
        return switch (mode) {
            case JAVA_NANO -> "NANO";
            case LWJGL_GLFW -> "LWJGL";
            default -> null;
        };
    }



    public int sync(double fps) throws InterruptedException {
        double resolution = getResolution();
        double timeNow = getTime();
        int updates = 0;

        if (enabled) {
            double gapTo = resolution / fps + timeThen;

            while (gapTo < timeNow) {
                gapTo = resolution / fps + gapTo;
                updates++;
            }
            while (gapTo > timeNow) {
                if (fps < 300) {
                    Thread.sleep(0, 1);
                }
                timeNow = getTime();
            }
            updates++;

            timeThen = gapTo;
        } else {
            while (timeThen < timeNow) {
                timeThen = resolution / fps + timeThen;
                updates++;
            }
        }

        return updates;
    }
}