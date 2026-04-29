package cms.mortalchen.orange.ultralight;

@FunctionalInterface
public interface UltraLightBackend {
    String handle(String channel, String payload);
}
