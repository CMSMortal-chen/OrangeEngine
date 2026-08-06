package cms.mortalchen.orange.resource;

import cms.mortalchen.orange.api.ILoggerImp;
import cms.mortalchen.orange.utils.Utils;
import lombok.Getter;
import org.lwjgl.nanovg.NanoVG;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.lwjgl.nanovg.NanoVG.nvgCreateImageMem;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;

public class ResourceManager implements ILoggerImp {
    @Getter
    private final long nvg;
    private final Map<String, Integer> loadedImages;

    public ResourceManager(long nvg) {
        this.nvg = nvg;
        this.loadedImages = new HashMap<>();
    }

    public int loadImage(String imagePath, int flag) {
        if (loadedImages.containsKey(imagePath)) {
            return loadedImages.get(imagePath);
        }

        try (InputStream in = Utils.class.getResourceAsStream(imagePath);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            int bytesRead;
            byte[] data = new byte[1024];
            while ((bytesRead = Objects.requireNonNull(in).read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            buffer.flush();
            byte[] imageBytes = buffer.toByteArray();

            ByteBuffer imageBuffer = ByteBuffer.allocateDirect(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            int imageId = nvgCreateImageMem(nvg, flag, imageBuffer);
            loadedImages.put(imagePath, imageId);
            return imageId;
        } catch (IOException | NullPointerException e) {
            logger.error("Failed to load image: {}", imagePath , e);
        }
        return 0;
    }

    /** 由 PNG 编码字节创建图像（预缩放像素画用）；key 为缓存键，命中直接复用纹理 */
    public int loadImagePNG(String key, byte[] pngBytes, int flag) {
        Integer cached = loadedImages.get(key);
        if (cached != null) {
            return cached;
        }

        ByteBuffer imageBuffer = ByteBuffer.allocateDirect(pngBytes.length);
        imageBuffer.put(pngBytes);
        imageBuffer.flip();

        int imageId = nvgCreateImageMem(nvg, flag, imageBuffer);
        if (imageId != 0) {
            loadedImages.put(key, imageId);
        }
        return imageId;
    }

    public void deleteImage(int imageId) {
        nvgDeleteImage(nvg, imageId);
    }
}
