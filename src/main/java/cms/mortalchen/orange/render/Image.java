package cms.mortalchen.orange.render;

import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.resource.ResourceManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import lombok.Getter;
import org.lwjgl.nanovg.NanoVG;

import java.awt.Color;

/**
 * 纹理包装：构造时经 {@link ResourceManager} 缓存加载纹理，负责渲染与释放。
 */
public class Image {
    private final ResourceManager textures;
    @Getter
    private final ResourceLocation location;

    /**
     * 由 PNG 编码字节直接创建（像素画 CPU 预缩放后 1:1 上传，避免二次缩放过滤）。
     *
     * @param cacheKey 缓存键，同一 key 复用同一纹理（如 "path@scale"）
     */
    public static Image fromPNG(ResourceManager textures, String cacheKey, byte[] pngBytes, int flag) {
        ResourceLocation loc = new ResourceLocation(cacheKey);
        loc.setId(textures.loadImagePNG(cacheKey, pngBytes, flag));
        return new Image(textures, loc, true);
    }

    public Image(ResourceManager textures, ResourceLocation location) {
        this(textures, location, NanoVG.NVG_IMAGE_GENERATE_MIPMAPS);
    }

    public Image(ResourceManager textures, ResourceLocation location, int flag) {
        this.textures = textures;
        this.location = location;
        location.setId(textures.loadImage(location.getPath(), flag));
    }

    /** 已预加载路径专用（fromPNG 使用）：不再触发 loadImage */
    private Image(ResourceManager textures, ResourceLocation location, boolean preloaded) {
        this.textures = textures;
        this.location = location;
    }

    public void render(float x, float y, float width, float height, Color color) {
        RenderUtils.drawImage(textures.getNvg(), location, x, y, width, height, 255, color);
    }

    public void cleanup() {
        textures.deleteImage(location.getId());
    }
}
