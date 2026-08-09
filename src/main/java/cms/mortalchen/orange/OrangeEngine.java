package cms.mortalchen.orange;

import cms.mortalchen.orange.audio.AudioManager;
import cms.mortalchen.orange.input.InputManager;
import cms.mortalchen.orange.render.FPS;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.resource.ResourceManager;
import cms.mortalchen.orange.utils.SyncTimer;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OrangeEngine {
    public static final String name = "Orange";
    public static final String engineName = "Orange Engine";
    public static final String version;
    /**
     * Metro 主题 Accent（Windows Cobalt #0050EF，docs/orange-engine-ui-spec.md §3）。
     * 组件取色应经过此字段，禁止硬编码 hex。
     */
    public static final Color accentColor = new Color(0x00, 0x50, 0xEF);
    public InputManager inputManager;
    public static FontManager fontManager;
    public static ResourceManager resourceManager;
    public static AudioManager audioManager;
    public FPS fpsCounter;
    public SyncTimer syncTimer = new SyncTimer(SyncTimer.JAVA_NANO);
    public static long deltaTimeMills = 0;
    public static long lastDeltaTimeMills = 0;

    public static OrangeEngine instance;
    public OrangeEngine(){
        instance = this;
    }
    static {
        String versionRead = "";
        Properties props = new Properties();
        try (InputStream in = OrangeEngine.class.getResourceAsStream("version.properties")) {
            props.load(in);
        } catch (IOException e) {
            versionRead = "DEV";
        }
        if (!versionRead.equals("DEV")){
            versionRead = props.getProperty("version");
        }

        version = versionRead;
    }
}
