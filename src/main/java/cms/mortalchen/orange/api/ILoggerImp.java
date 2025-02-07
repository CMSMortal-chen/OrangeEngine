package cms.mortalchen.orange.api;

import cms.mortalchen.orange.MainGame;
import org.apache.logging.log4j.Logger;

public interface ILoggerImp {
    Logger logger = MainGame.instance.logger;
}
