package cms.mortalchen.orange.states;


import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import org.joml.Vector2f;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Metro 风格暂停页（docs/orange-engine-ui-spec.md §7.8）。
 *
 * 结构：全屏 Accent 实底 → 47% 黑半透打底 → 排版内容。
 * 菜单选中 = 白底 + Accent 反色文字；Logo（geometos）右下角落款，不作标题。
 * 交互：↑↓ 选择、Enter/J 确认、Esc/K 返回游戏；左键点击菜单项直接执行。
 */
public class PauseState extends GameState {
    private static final Color ACCENT = OrangeEngine.accentColor;
    private static final Color DIM = new Color(0, 0, 0, 120);
    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_FAINT = new Color(255, 255, 255, 72);

    private static final float MARGIN = 120f;
    private static final float ITEM_H = 56f;
    private static final float ITEM_W = 420f;

    private final String[] items = {"Resume", "Main Menu", "Exit"};
    private final Runnable[] actions;
    private final float[] itemY = new float[3];
    private int selectedIndex;

    public PauseState(MainGame game) {
        super(game);
        allowPause = false;
        actions = new Runnable[]{
                this::resumeGame,
                () -> MainGame.startMainGame.run(),
                game::endGame
        };
    }

    @Override
    public void render(long vg) {
        int w = (int) width, h = (int) height;
        // Metro：Accent 全屏 + 47% 黑半透打底（压暗其后的游戏画面）
        RenderUtils.drawRect(vg, 0, 0, w, h, ACCENT);
        RenderUtils.drawRect(vg, 0, 0, w, h, DIM);

        float top = 64f;
        // 小标题（与 Panic 页统一格式：ORANGE ENGINE v<版本> · <状态>）
        String kicker = "ORANGE ENGINE v" + OrangeEngine.version + " · GAME PAUSED";
        FontManager.jetBrainsMono_CHS.drawText(kicker, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                MARGIN, top + 13, 13, TEXT_FAINT);

        // 主标题
        float heroY = top + 16 + 56;
        FontManager.montserrat.drawText("Pause Menu", NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                MARGIN, heroY, 56, TEXT);

        // 菜单（选中 = 白底 + Accent 反色文字）
        float menuTop = heroY + 32;
        for (int i = 0; i < items.length; i++) {
            float y = menuTop + i * ITEM_H;
            itemY[i] = y;
            boolean selected = i == selectedIndex;
            if (selected) {
                RenderUtils.drawRect(vg, MARGIN, y, MARGIN + ITEM_W, y + ITEM_H, TEXT);
                FontManager.montserrat.drawText(items[i], NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE,
                        MARGIN + 20, y + ITEM_H / 2, 15, ACCENT);
            } else {
                FontManager.montserrat.drawText(items[i], NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE,
                        MARGIN + 20, y + ITEM_H / 2, 15, TEXT);
            }
        }

        // 操作提示
        FontManager.hmSans_CHS.drawText("↑ ↓ 选择 · Enter 确认 · Esc 返回游戏",
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                MARGIN, menuTop + items.length * ITEM_H + 32, 12, TEXT_FAINT);

        // Logo（geometos，右下角，非标题）
        String[] words = OrangeEngine.engineName.split(" ");
        if (words.length >= 2) {
            FontManager.geometos.drawText(words[0], NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_BOTTOM,
                    w - 19, h - 30, 35, TEXT);
            FontManager.geometos.drawText(words[1], NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_BOTTOM,
                    w - 20, h - 10, 25, TEXT);
        }
    }

    @Override
    public void onKeyPressed(int key, int scancode, int action, int mods) {
        boolean press = action == GLFW_PRESS;
        boolean repeat = action == GLFW_REPEAT;
        if (!press && !repeat) {
            return;
        }
        if (key == GLFW_KEY_UP || key == GLFW_KEY_W) {
            selectedIndex = (selectedIndex - 1 + items.length) % items.length;
        } else if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) {
            selectedIndex = (selectedIndex + 1) % items.length;
        } else if (press && (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER || key == GLFW_KEY_J)) {
            actions[selectedIndex].run();
        } else if (press && (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_K)) {
            resumeGame();
        }
    }

    @Override
    public void onMouseClick(int mouseButton, int action, Vector2f mousePos) {
        if (action != GLFW_PRESS || mouseButton != GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        float x = mousePos.x, y = mousePos.y;
        for (int i = 0; i < items.length; i++) {
            if (x >= MARGIN && x <= MARGIN + ITEM_W && y >= itemY[i] && y <= itemY[i] + ITEM_H) {
                selectedIndex = i;
                actions[i].run();
                return;
            }
        }
    }

    private void resumeGame() {
        game.isGamePaused = false;
    }
}
