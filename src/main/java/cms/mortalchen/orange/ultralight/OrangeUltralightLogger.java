package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.logger.UltralightLogLevel;
import net.janrupf.ujr.api.logger.UltralightLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class OrangeUltralightLogger implements UltralightLogger {
    private static final Logger LOGGER = LogManager.getLogger("Orange-Ultralight");

    @Override
    public void logMessage(UltralightLogLevel logLevel, String message) {
        LOGGER.log(switch (logLevel) {
            case ERROR -> Level.ERROR;
            case WARNING -> Level.WARN;
            case INFO -> Level.INFO;
        }, message);
    }
}
