package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.UltralightConfigBuilder;
import net.janrupf.ujr.api.UltralightPlatform;
import net.janrupf.ujr.api.UltralightRenderer;
import net.janrupf.ujr.core.UltralightJavaReborn;
import net.janrupf.ujr.core.platform.PlatformEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public final class UltraLightRuntime {
    private static final Logger LOGGER = LogManager.getLogger(UltraLightRuntime.class);
    private static UltraLightRuntime active;

    private final UltralightJavaReborn ujr;
    private final UltralightRenderer renderer;

    private UltraLightRuntime(long window) {
        PlatformEnvironment environment = PlatformEnvironment.load();
        ujr = new UltralightJavaReborn(environment);
        ujr.activate();

        UltralightPlatform platform = UltralightPlatform.instance();
        platform.usePlatformFontLoader();
        platform.setLogger(new OrangeUltralightLogger());
        platform.setClipboard(new OrangeUltralightClipboard(window));
        platform.setFilesystem(new OrangeUltralightFilesystem());
        platform.setSurfaceFactory(new OrangeUltralightSurfaceFactory());
        platform.setConfig(new UltralightConfigBuilder()
                .cachePath(System.getProperty("java.io.tmpdir") + File.separator + "orange-ultralight")
                .resourcePathPrefix(OrangeUltralightFilesystem.RESOURCE_PREFIX)
                .build());

        renderer = UltralightRenderer.getOrCreate();
    }

    public static synchronized UltraLightRuntime getOrCreate(long window) {
        if (active == null) {
            active = new UltraLightRuntime(window);
        }
        return active;
    }

    public static synchronized void updateAndRenderActive() {
        if (active == null) {
            return;
        }
        active.renderer.update();
        active.renderer.render();
    }

    public static synchronized void cleanupActive() {
        if (active == null) {
            return;
        }

        try {
            active.ujr.cleanup();
        } catch (Exception e) {
            LOGGER.warn("Failed to cleanup Ultralight", e);
        } finally {
            active = null;
        }
    }

    UltralightRenderer renderer() {
        return renderer;
    }
}
