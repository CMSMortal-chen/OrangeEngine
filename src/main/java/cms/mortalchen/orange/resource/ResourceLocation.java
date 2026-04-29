package cms.mortalchen.orange.resource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class ResourceLocation {
    private final String path;
    private int id;

    public ResourceLocation(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
