package cms.mortalchen.orange.render;

import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.resource.ResourceManager;
import cms.mortalchen.orange.utils.Utils;
import cms.mortalchen.orange.utils.render.RenderUtils;
import lombok.Getter;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

public class Image {
    private final ResourceManager resourceManager;
    @Getter
    private final ResourceLocation location;

    public Image(ResourceManager resourceManager, ResourceLocation location) {
        this.resourceManager = resourceManager;
        this.location = location;
        location.setId(resourceManager.loadImage(location.getPath(), NanoVG.NVG_IMAGE_GENERATE_MIPMAPS));
    }

    public Image(ResourceManager resourceManager, ResourceLocation location, int flag) {
        this.resourceManager = resourceManager;
        this.location = location;
        location.setId(resourceManager.loadImage(location.getPath(), flag));
    }

    private Image(ResourceManager resourceManager, ResourceLocation location, boolean preloaded) {
        this.resourceManager = resourceManager;
        this.location = location;
    }

    /**
     * 由 PNG 编码字节直接创建 Image（不经过 loadImage）：
     * 用于像素画 CPU 预缩放（最近邻放大到整数像素）后上传，1:1 绘制避免纹理二次缩放过滤。
     *
     * @param cacheKey 缓存键（同一 key 复用同一纹理，如 "path@scale"）
     */
    public static Image fromPNG(ResourceManager resourceManager, String cacheKey, byte[] pngBytes, int flag) {
        ResourceLocation location = new ResourceLocation(cacheKey);
        location.setId(resourceManager.loadImagePNG(cacheKey, pngBytes, flag));
        return new Image(resourceManager, location, true);
    }

    public void render(float x, float y, float width, float height, Color color) {
        RenderUtils.drawImage(resourceManager.getNvg(), location, x, y, width, height, 255, color);
    }

    public void cleanup() {
        resourceManager.deleteImage(location.getId());
    }
}