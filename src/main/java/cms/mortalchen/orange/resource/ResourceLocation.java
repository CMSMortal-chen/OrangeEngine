package cms.mortalchen.orange.resource;

/**
 * 资源标识：classpath 路径 + 已缓存的 NanoVG 纹理句柄。
 */
public final class ResourceLocation {
    private final String path;
    private int textureId;

    public ResourceLocation(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public int getId() {
        return textureId;
    }

    public void setId(int id) {
        this.textureId = id;
    }
}
