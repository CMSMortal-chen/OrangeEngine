package cms.mortalchen.orange.resource;

import lombok.Getter;
import lombok.Setter;

/**
 * 资源标识：classpath 路径 + 已缓存的 NanoVG 纹理句柄。
 */
@Getter
@Setter
public final class ResourceLocation {
    private final String path;
    /** NanoVG 纹理句柄（未加载为 0） */
    private int id;

    public ResourceLocation(String path) {
        this.path = path;
    }
}
