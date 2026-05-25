package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.oralightcef.OraLightCEF;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

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
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_F10)) {
            game.changeState(new DefaultState(game));
            return;
        }
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
        oraLightCEF = new OraLightCEF((int) width, (int) height, false);
        oraLightCEF.loadHTML("<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>高斯模糊矩形示例</title>\n" +
                "    <style>\n" +
                "        /* 背景装饰，为了更好地观察模糊效果 */\n" +
                "        body {\n" +
                "            margin: 0;\n" +
                "            height: 100vh;\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            gap: 40px;\n" +
                "            background: linear-gradient(45deg, #ff9a9e 0%, #fad0c4 99%, #fad0c4 100%);\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "\n" +
                "        /* 装饰背景圆圈 */\n" +
                "        .circle {\n" +
                "            position: absolute;\n" +
                "            z-index: -1;\n" +
                "            border-radius: 50%;\n" +
                "            background: rgba(255, 255, 255, 0.4);\n" +
                "        }\n" +
                "        .c1 { width: 300px; height: 300px; top: 10%; left: 15%; background: #a18cd1; }\n" +
                "        .c2 { width: 200px; height: 200px; bottom: 15%; right: 20%; background: #fbc2eb; }\n" +
                "\n" +
                "        /* 通用玻璃板样式 */\n" +
                "        .glass-card {\n" +
                "            width: 300px;\n" +
                "            height: 200px;\n" +
                "            /* 核心属性：背景模糊 */\n" +
                "            backdrop-filter: blur(15px);\n" +
                "            -webkit-backdrop-filter: blur(15px);\n" +
                "            \n" +
                "            /* 半透明背景和边框 */\n" +
                "            background: rgba(255, 255, 255, 0.2);\n" +
                "            border: 1px solid rgba(255, 255, 255, 0.3);\n" +
                "            box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.15);\n" +
                "            \n" +
                "            display: flex;\n" +
                "            flex-direction: column;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            color: #333;\n" +
                "            transition: transform 0.3s ease;\n" +
                "        }\n" +
                "\n" +
                "        .glass-card:hover {\n" +
                "            transform: translateY(-5px);\n" +
                "        }\n" +
                "\n" +
                "        /* 圆角矩形 */\n" +
                "        .rounded {\n" +
                "            border-radius: 20px;\n" +
                "        }\n" +
                "\n" +
                "        /* 非圆角（直角）矩形 */\n" +
                "        .sharp {\n" +
                "            border-radius: 0px;\n" +
                "        }\n" +
                "\n" +
                "        h2 { margin-bottom: 10px; font-size: 1.2rem; }\n" +
                "        p { font-size: 0.9rem; opacity: 0.8; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "    <div class=\"circle c1\"></div>\n" +
                "    <div class=\"circle c2\"></div>\n" +
                "\n" +
                "    <div class=\"glass-card rounded\">\n" +
                "        <h2>圆角矩形</h2>\n" +
                "        <p>border-radius: 20px;</p>\n" +
                "        <p>backdrop-filter: blur(15px);</p>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"glass-card sharp\">\n" +
                "        <h2>直角矩形</h2>\n" +
                "        <p>border-radius: 0px;</p>\n" +
                "        <p>backdrop-filter: blur(15px);</p>\n" +
                "    </div>\n" +
                "\n" +
                "</body>\n" +
                "</html>");
    }
}
