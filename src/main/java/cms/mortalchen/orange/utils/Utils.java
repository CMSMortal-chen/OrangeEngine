package cms.mortalchen.orange.utils;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.PanicScreen;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Utils {

    /** 从 classpath 读取图像并解码为 GLFW 图标（窗口图标用） */
    public static GLFWImage.Buffer loadImage(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer encoded = readClasspath(path);
            if (encoded == null) {
                return null;
            }
            try {
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
                if (pixels == null) {
                    System.err.println("Failed to decode image: " + path);
                    return null;
                }
                GLFWImage.Buffer icon = GLFWImage.malloc(1);
                icon.width(width.get(0));
                icon.height(height.get(0));
                icon.pixels(pixels);
                return icon;
            } finally {
                MemoryUtil.memFree(encoded);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /** 读取 classpath 资源为可释放的 DirectByteBuffer；缺失/失败返回 null */
    private static ByteBuffer readClasspath(String path) {
        try (InputStream in = Utils.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            byte[] data = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static NVGColor color(float r, float g, float b, float a) {
        return NVGColor.calloc().a(a).r(r).g(g).b(b);
    }

    public static void panic(Logger logger, Exception e) {
        logger.fatal("!!!{} Engine Panic!!!", OrangeEngine.engineName);
        logger.fatal("Exception:", e);
        logger.fatal("!!!Encounter PANIC! {} will now EXIT!!!", OrangeEngine.engineName);

        MainGame main = MainGame.instance;
        if (main == null || !MainGame.loopRunning) {
            // 渲染循环未运行（启动失败/初始化前）：无法显示 Panic 屏，保持原行为直接退出
            System.exit(-1);
            return;
        }
        PanicScreen.trigger(
                OrangeEngine.engineName + " Panic",
                Thread.currentThread().getName(),
                "OrangeEngine v" + OrangeEngine.version,
                "0x00000000",
                e);
        main.exiting = true; // 停止逻辑线程，避免其反复触发 panic
    }
}
