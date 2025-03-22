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

    public void render(float x, float y, float width, float height, Color color) {
        RenderUtils.drawImage(resourceManager.getNvg(), location, x, y, width, height, 255, color);
    }

    public void cleanup() {
        resourceManager.deleteImage(location.getId());
    }
}