package cms.mortalchen.orange.states;


import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.Motion;
import cms.mortalchen.orange.utils.render.anim.animatednumber.AnimatedFloat;
import org.joml.Vector2f;

import java.awt.Color;

import static cms.mortalchen.orange.MainGame.WINDOW_HEIGHT;
import static cms.mortalchen.orange.MainGame.WINDOW_WIDTH;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Metro 风格暂停页（docs/orange-engine-ui-spec.md §7.8）。
 *
 * 背景 = 黑色半透打底（47% 黑压暗游戏画面，不做不透明纯色背景）；
 * 菜单选中 = 白底 + Accent 反色文字；Logo（geometos）右下角落款，不作标题。
 * 入场 = Motion 引擎错落滑入（Fast In, Smooth Out，见 §9 Power Easing）。
 * 交互：↑↓ 选择、Enter/J 确认、Esc 返回游戏（Esc 由 MainGame 统一切换）；
 * 左键点击菜单项直接执行。
 */
public class PauseState extends GameState {
    private static final Color DIM = new Color(0, 0, 0, 120); // ≈47% 黑半透
    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_FAINT = new Color(255, 255, 255, 72);

    private static final float MARGIN = 120f;
    private static final float ITEM_H = 56f;
    private static final float ITEM_W = 420f;
    private static final float SLIDE = 24f;          // 入场右滑位移 px
    private static final long ENTRANCE_MS = 250;     // Metro 标准时长
    private static final Motion.Easing ENTRANCE_EASING = Motion.Easing.QUAD_OUT; // Fast In, Smooth Out

    private final String[] items = {"Resume", "Main Menu", "Exit"};
    private final Runnable[] actions;
    private final float[] itemY = new float[3];
    private int selectedIndex;

    /** 错落入场动画：dim/kicker/hero/菜单×3/hint/logo */
    private final AnimatedFloat[] entrance;
    /** 各元素相对暂停打开的启动延迟（ms） */
    private static final long[] START_MS = {0, 50, 160, 260, 320, 380, 500, 560};
    private long openElapsedMs;
    private int stage;

    public PauseState(MainGame game) {
        super(game);
        allowPause = false;
        actions = new Runnable[]{
                this::resumeGame,
                () -> MainGame.startMainGame.run(),
                game::endGame
        };
        entrance = new AnimatedFloat[]{
                new AnimatedFloat(0f), // 0 dim
                new AnimatedFloat(0f), // 1 kicker
                new AnimatedFloat(0f), // 2 hero
                new AnimatedFloat(0f), // 3 item0
                new AnimatedFloat(0f), // 4 item1
                new AnimatedFloat(0f), // 5 item2
                new AnimatedFloat(0f), // 6 hint
                new AnimatedFloat(0f)  // 7 logo
        };
    }

    /** MainGame 在 ESC 切入暂停时调用，重置错落入场动画 */
    public void onPauseOpened() {
        for (AnimatedFloat a : entrance) {
            a.set(0f);
        }
        openElapsedMs = 0;
        stage = 0;
    }

    @Override
    public void update(float deltaTime) {
        openElapsedMs += (long) (deltaTime * 1000);
        // 到点启动各元素的入场动画（Motion 引擎驱动）
        while (stage < entrance.length && openElapsedMs >= START_MS[stage]) {
            entrance[stage].to(1f, ENTRANCE_MS, ENTRANCE_EASING);
            stage++;
        }
    }

    /** 入场进度（0~1），用于位移与透明度 */
    private float progress(int index) {
        return entrance[index].get();
    }

    private float slideX(float base, float p) {
        return base + (1 - p) * SLIDE;
    }

    private static Color alpha(Color c, float p) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * p));
    }

    @Override
    public void render(long vg) {
        int w = WINDOW_WIDTH, h = WINDOW_HEIGHT;

        // 黑色半透打底：压暗其后的游戏画面（无遮罩时不动画，首帧即 47%）
        float dimP = progress(0);
        RenderUtils.drawRect(vg, 0, 0, w, h, new Color(0, 0, 0, (int) (DIM.getAlpha() * Math.max(dimP, 0.001f))));

        float top = 64f;

        // 小标题（与 Panic 页统一格式：ORANGE ENGINE v<版本> · <状态>）
        float kickerP = progress(1);
        String kicker = "ORANGE ENGINE v" + OrangeEngine.version + " · GAME PAUSED";
        FontManager.jetBrainsMono_CHS.drawText(kicker, NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE,
                slideX(MARGIN, kickerP), top + 13, 13, alpha(TEXT_FAINT, kickerP));

        // 主标题
        float heroP = progress(2);
        float heroY = top + 16 + 56;
        FontManager.montserrat.drawText("Pause Menu", NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE,
                slideX(MARGIN, heroP), heroY, 56, alpha(TEXT, heroP));

        // 菜单（选中 = 白底 + Accent 反色文字；错落入场）
        float menuTop = heroY + 32;
        for (int i = 0; i < items.length; i++) {
            float p = progress(3 + i);
            float y = menuTop + i * ITEM_H;
            itemY[i] = y;
            float x = slideX(MARGIN, p);
            boolean selected = i == selectedIndex;
            if (selected) {
                RenderUtils.drawRect(vg, x, y, x + ITEM_W, y + ITEM_H, alpha(TEXT, p));
                FontManager.montserrat.drawText(items[i], NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE,
                        x + 20, y + ITEM_H / 2, 15, alpha(OrangeEngine.accentColor, p));
            } else {
                FontManager.montserrat.drawText(items[i], NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE,
                        x + 20, y + ITEM_H / 2, 15, alpha(TEXT, p));
            }
        }

        // 操作提示
        float hintP = progress(6);
        FontManager.hmSans_CHS.drawText("↑ ↓ 选择 · Enter 确认 · Esc 返回游戏",
                NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE,
                slideX(MARGIN, hintP), menuTop + items.length * ITEM_H + 32, 12, alpha(TEXT_FAINT, hintP));

        // Logo（geometos，右下角，非标题）
        float logoP = progress(7);
        String[] words = OrangeEngine.engineName.split(" ");
        if (words.length >= 2) {
            FontManager.geometos.drawText(words[0], NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM,
                    w - 19 + (1 - logoP) * SLIDE, h - 30, 35, alpha(TEXT, logoP));
            FontManager.geometos.drawText(words[1], NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM,
                    w - 20 + (1 - logoP) * SLIDE, h - 10, 25, alpha(TEXT, logoP));
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
        }
        // 注意：Esc 返回游戏由 MainGame 的 ESC 切换统一处理（同一次 ESC 按下
        // 事件会先置 isGamePaused=true 再分发到这里，此处不得再 resumeGame）
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
