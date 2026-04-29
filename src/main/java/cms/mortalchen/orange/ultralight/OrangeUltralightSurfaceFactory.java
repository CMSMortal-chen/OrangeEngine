package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.surface.UltralightSurface;
import net.janrupf.ujr.api.surface.UltralightSurfaceFactory;

final class OrangeUltralightSurfaceFactory implements UltralightSurfaceFactory {
    @Override
    public UltralightSurface createSurface(long width, long height) {
        return new OrangeUltralightSurface(width, height);
    }

    @Override
    public void destroySurface(UltralightSurface surface) {
        if (surface instanceof OrangeUltralightSurface orangeSurface) {
            orangeSurface.destroy();
        }
    }
}
