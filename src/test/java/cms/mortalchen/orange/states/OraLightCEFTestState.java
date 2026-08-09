package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.oralightcef.OraLightCEF;
import org.joml.Vector2f;

public class OraLightCEFTestState extends GameState {
    private OraLightCEF oraLightCEF;
    private boolean initialized;

    public OraLightCEFTestState(MainGame game) {
        super(game);
    }

    @Override
    public void render(long vg) {
        initIfNeeded();
        if (oraLightCEF != null) {
            oraLightCEF.render(vg, 0, 0, width, height);
        }
    }

    @Override
    public void update(float deltaTime) {
        initIfNeeded();
        if (oraLightCEF != null) {
            oraLightCEF.update();
        }
    }

    @Override
    public void updateDisplaySize(float width, float height) {
        super.updateDisplaySize(width, height);
        if (oraLightCEF != null) {
            oraLightCEF.resize((int) width, (int) height);
        }
    }

    @Override
    public void onMouseMove(double x, double y) {
        if (oraLightCEF != null) {
            oraLightCEF.mouseMove((float) x, (float) y);
        }
    }

    @Override
    public void onMouseClick(int mouseButton, int action, Vector2f mousePos) {
        if (oraLightCEF != null) {
            oraLightCEF.mouseButton(mouseButton, action, mousePos);
        }
    }

    @Override
    public void onMouseScroll(double xoffset, double yoffset) {
        if (oraLightCEF != null) {
            oraLightCEF.scroll(xoffset, yoffset);
        }
    }

    @Override
    public void onKeyPressed(int key, int scancode, int action, int mods) {
        if (oraLightCEF != null) {
            oraLightCEF.key(key, scancode, action, mods);
        }
    }

    @Override
    public void onChar(int codepoint) {
        if (oraLightCEF != null) {
            oraLightCEF.character(codepoint);
        }
    }

    private void initIfNeeded() {
        if (initialized) {
            return;
        }
        initialized = true;
        // URL 直接走构造函数（浏览器原生创建即携带该 URL），避免 createImmediately 异步
        // 与随后 loadURL 的竞态导致偶发停在 about:blank
        oraLightCEF = new OraLightCEF((int) width, (int) height, false,
                "file:///G:/codes/ProjectGatewayGame/tmp/test_video.html");
    }

    @Override
    public void updateTick(float deltaTime) {
        super.updateTick(deltaTime);
    }
}
