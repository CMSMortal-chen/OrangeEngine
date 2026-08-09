package cms.mortalchen.orange.resource;

import cms.mortalchen.orange.api.ILoggerImp;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.nvgCreateImageMem;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;

/**
 * 纹理缓存：按 classpath 路径缓存 NanoVG 图像句柄。
 * 图像必须由渲染线程创建（NanoVG/GL 线程约束），本类不做跨线程同步。
 */
public class ResourceManager implements ILoggerImp {
    @Getter
    private final long nvg;
    private final Map<String, Integer> textureCache = new HashMap<>();

    public ResourceManager(long nvg) {
        this.nvg = nvg;
    }

    public int loadImage(String imagePath, int flag) {
        Integer cached = textureCache.get(imagePath);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = readClasspath(imagePath);
        if (bytes == null) {
            return 0;
        }
        int imageId = createFromBytes(bytes, flag);
        if (imageId != 0) {
            textureCache.put(imagePath, imageId);
        }
        return imageId;
    }

    /** 由 PNG 编码字节创建图像（像素画 CPU 预缩放后上传）；key 为缓存键 */
    public int loadImagePNG(String key, byte[] pngBytes, int flag) {
        Integer cached = textureCache.get(key);
        if (cached != null) {
            return cached;
        }
        int imageId = createFromBytes(pngBytes, flag);
        if (imageId != 0) {
            textureCache.put(key, imageId);
        }
        return imageId;
    }

    /** nvgCreateImageMem 立即解码，输入缓冲可随即释放（stb 解码结果由 NanoVG 持有） */
    private int createFromBytes(byte[] data, int flag) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data).flip();
        return nvgCreateImageMem(nvg, flag, buffer);
    }

    private byte[] readClasspath(String path) {
        // 必须用 Class.getResourceAsStream：ClassLoader 版不剥离前导 "/"，
        // 对 "/textures/..." 这类路径恒返回 null
        String resourcePath = path.startsWith("/") ? path : "/" + path;
        try (InputStream in = ResourceManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.error("Resource not found: {}", path);
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            logger.error("Failed to read image resource: {}", path, e);
            return null;
        }
    }

    public void deleteImage(int imageId) {
        nvgDeleteImage(nvg, imageId);
    }
}
