package cms.mortalchen.orange.ultralight;

import cms.mortalchen.orange.utils.render.anim.Motion;
import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.cursor.UlCursor;
import net.janrupf.ujr.api.listener.UlMessageLevel;
import net.janrupf.ujr.api.listener.UlMessageSource;
import net.janrupf.ujr.api.listener.UltralightLoadListener;
import net.janrupf.ujr.api.listener.UltralightViewListener;
import net.janrupf.ujr.api.math.IntRect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class UltraLightViewListener implements UltralightViewListener, UltralightLoadListener {
    private static final Logger LOGGER = LogManager.getLogger(UltraLightViewListener.class);

    private final UltraLightIpc ipc;
    private final UltraLight ultraLight;

    UltraLightViewListener(UltraLightIpc ipc, UltraLight ultraLight) {
        this.ipc = ipc;
        this.ultraLight = ultraLight;
    }

    @Override
    public void onWindowObjectReady(UltralightView view, long frameId, boolean isMainFrame, String url) {
        if (isMainFrame) {
            ipc.installJavaScriptBridge();
        }
    }

    @Override
    public void onDOMReady(UltralightView view, long frameId, boolean isMainFrame, String url) {
        if (isMainFrame) {
            ipc.installJavaScriptBridge();
        }
    }

    @Override
    public UltralightView onCreateChildView(UltralightView view, String openerUrl, String targetUrl, boolean popup, IntRect rect) {
        view.loadURL(targetUrl);
        return ultraLight.createChildView(rect.width(), rect.height());
    }

    @Override
    public UltralightView onCreateInspectorView(UltralightView view, boolean isLocal, String inspectedUrl) {
        return ultraLight.createChildView(800, 600);
    }

    @Override
    public void onChangeCursor(UltralightView view, UlCursor cursor) {
    }

    @Override
    public void onAddConsoleMessage(UltralightView view, UlMessageSource source, UlMessageLevel level, String message, long lineNumber, long columnNumber, String sourceId) {
        if (message != null && message.startsWith(UltraLightIpc.CONSOLE_PREFIX)) {
            ipc.receive(message.substring(UltraLightIpc.CONSOLE_PREFIX.length()));
            return;
        }
        LOGGER.debug("[{}] {}:{} {}", level, sourceId, lineNumber, message);
    }

    @Override
    public void onBeginLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
        if (!isMainFrame) return;
        ultraLight.isLoading = true;
        ultraLight.loadProgress.set(0.10f);
        ultraLight.loadProgress.to(0.70f, 3000, Motion.Easing.SINE_IN_OUT);
    }

    @Override
    public void onFinishLoading(UltralightView view, long frameId, boolean isMainFrame, String url) {
        if (!isMainFrame) return;
        ultraLight.isLoading = false;
        ultraLight.loadProgress.to(1.0f, 300, Motion.Easing.QUAD_OUT);
    }

    @Override
    public void onFailLoading(UltralightView view, long frameId, boolean isMainFrame, String url, String description, String errorDomain, int errorCode) {
        if (!isMainFrame) return;
        ultraLight.isLoading = false;
        ultraLight.loadProgress.to(1.0f, 300, Motion.Easing.QUAD_OUT);
    }

    @Override
    public void onChangeURL(UltralightView view, String url) {
        ultraLight.isLoading = true;
        ultraLight.loadProgress.set(0.0f);
        ultraLight.loadProgress.to(0.70f, 3000, Motion.Easing.SINE_IN_OUT);
    }
}
