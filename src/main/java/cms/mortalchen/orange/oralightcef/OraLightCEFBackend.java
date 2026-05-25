package cms.mortalchen.orange.oralightcef;

@FunctionalInterface
public interface OraLightCEFBackend {
    String handle(String channel, String payload);
}
