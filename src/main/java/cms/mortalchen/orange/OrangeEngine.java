package cms.mortalchen.orange;

import cms.mortalchen.orange.input.InputManager;
import cms.mortalchen.orange.render.FPS;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.resource.ResourceManager;
import cms.mortalchen.orange.utils.SyncTimer;

public class OrangeEngine {
    public static final String name = "Orange";
    public static final String engineName = "Orange Engine";
    public static final String version = "ver0.10";
    public InputManager inputManager;
    public static FontManager fontManager;
    public static ResourceManager resourceManager;
    public FPS fpsCounter;
    public SyncTimer syncTimer = new SyncTimer(SyncTimer.JAVA_NANO);
    public static long deltaTimeMills = 0;
    public static long lastDeltaTimeMills = 0;

    public static OrangeEngine instance;
    public OrangeEngine(){
        instance = this;
    }

}
