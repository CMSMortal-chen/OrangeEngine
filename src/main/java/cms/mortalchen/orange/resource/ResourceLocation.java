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
}
