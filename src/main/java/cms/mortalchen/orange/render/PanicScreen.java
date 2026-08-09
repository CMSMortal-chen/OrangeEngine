package cms.mortalchen.orange.render;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.Motion;
import cms.mortalchen.orange.utils.render.anim.animatednumber.AnimatedFloat;
import org.lwjgl.nanovg.NanoVG;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Metro 风格 Panic 屏（docs/orange-engine-ui-spec.md §7.9）。
 *
 * 触发后**常驻显示**，等待用户操作退出：点击「关闭引擎」或任意按键 →
 * System.exit(-1)；「复制诊断信息」把摘要 + 完整栈写入系统剪贴板
 * （触发瞬间也会自动写入一次，尽力而为）。
 * 入场：Motion 引擎错落滑入（Fast In, Smooth Out，见规范 §9）。
 * 渲染循环未运行时（启动失败/初始化前）由调用方直接退出，不经过本类。
 */
public final class PanicScreen {
    private static final long ENTRANCE_MS = 250;
    private static final Motion.Easing ENTRANCE_EASING = Motion.Easing.QUAD_OUT;
    private static final float SLIDE = 24f;

    private static final Color BG = new Color(0x1A, 0x1A, 0x1A);
    private static final Color PANEL = new Color(0x2D, 0x2D, 0x30);
    private static final Color TERM_BG = new Color(0x0C, 0x0C, 0x0C);
    private static final Color CRIMSON = new Color(0xA2, 0x00, 0x25);
    private static final Color LINE = new Color(255, 255, 255, 30);
    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_DIM = new Color(255, 255, 255, 140);
    private static final Color TEXT_FAINT = new Color(255, 255, 255, 72);

    private static volatile boolean active;
    private static volatile String title = "Engine Panic";
    private static volatile String threadName = "-";
    private static volatile String module = "-";
    private static volatile String code = "-";
    private static volatile String[] stackLines = new String[0];
    private static volatile String diagnosticsText = "";

    /** 错落入场：dim/kicker/hero/subtitle/lede/panel/term/buttons/logo */
    private static final AnimatedFloat[] entrance = {
            new AnimatedFloat(0f), new AnimatedFloat(0f), new AnimatedFloat(0f),
            new AnimatedFloat(0f), new AnimatedFloat(0f), new AnimatedFloat(0f),
            new AnimatedFloat(0f), new AnimatedFloat(0f), new AnimatedFloat(0f)
    };
    private static final long[] START_MS = {0, 50, 160, 240, 300, 380, 480, 560, 620};
    private static long openElapsedMs;
    private static int stage;

    // 按钮几何（render 时按最终位置计算，onClick 复用）
    private static float continueBtnX, continueBtnY, continueBtnW, continueBtnH;
    private static float closeBtnX, closeBtnY, closeBtnW, closeBtnH;
    private static float copyBtnX, copyBtnY, copyBtnW, copyBtnH;

    private PanicScreen() {}

    public static boolean isActive() {
        return active;
    }

    /** 幂等触发：已激活时忽略后续 panic（防止重复弹屏/反复退出） */
    public static void trigger(String panicTitle, String threadName, String module, String code, Throwable t) {
        if (active) {
            return;
        }
        active = true;
        PanicScreen.title = panicTitle;
        PanicScreen.threadName = threadName;
        PanicScreen.module = module;
        PanicScreen.code = code;
        StringWriter sw = new StringWriter();
        if (t != null) {
            t.printStackTrace(new PrintWriter(sw));
        } else {
            sw.write("(no stack trace available)");
        }
        stackLines = sw.toString().split("\n");
        diagnosticsText = "ORANGE ENGINE PANIC\n"
                + "module: " + module + "\n"
                + "thread: " + threadName + "\n"
                + "code:   " + code + "\n"
                + "error:  " + panicTitle + "\n\n"
                + sw;
        // 重置错落入场动画
        for (AnimatedFloat a : entrance) {
            a.set(0f);
        }
        openElapsedMs = 0;
        stage = 0;
        copyToClipboard(diagnosticsText);
    }

    /** 每帧推进入场动画（MainGame.renderGame 调用） */
    public static void update(float deltaTime) {
        if (!active) {
            return;
        }
        openElapsedMs += (long) (deltaTime * 1000);
        while (stage < entrance.length && openElapsedMs >= START_MS[stage]) {
            entrance[stage].to(1f, ENTRANCE_MS, ENTRANCE_EASING);
            stage++;
        }
    }

    /** 任意按键直接退出 */
    public static void onKey() {
        doExit();
    }

    public static void onClick(int button, int action, float x, float y) {
        if (action != 1) { // GLFW_PRESS
            return;
        }
        if (hit(continueBtnX, continueBtnY, continueBtnW, continueBtnH, x, y)) {
            resumeGame();
        } else if (hit(closeBtnX, closeBtnY, closeBtnW, closeBtnH, x, y)) {
            doExit();
        } else if (hit(copyBtnX, copyBtnY, copyBtnW, copyBtnH, x, y)) {
            copyToClipboard(diagnosticsText);
        }
    }

    /**
     * 继续游戏：关闭 Panic 屏并尽力恢复运行。
     * <p>可能产生 bug（尽力而为，不保证状态一致）：
     * <ul>
     *   <li>panic 发生在 updateTick/render 中途，对象可能处于半更新状态，继续运行可能立即再次抛异常（重新弹屏）</li>
     *   <li>逻辑线程被重启，但其持有上下文（局部状态/游标等）已丢失</li>
     *   <li>若异常源于渲染/离屏/CEF 路径，GL 纹理或浏览器可能处于坏状态（花屏/黑屏/音频异常）</li>
     * </ul>
     */
    public static void resumeGame() {
        if (!active) {
            return;
        }
        active = false;
        for (AnimatedFloat a : entrance) {
            a.set(0f);
        }
        MainGame main = MainGame.instance;
        if (main != null) {
            main.resumeAfterPanic();
        }
    }

    private static boolean hit(float bx, float by, float bw, float bh, float x, float y) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh;
    }

    private static void doExit() {
        System.exit(-1);
    }

    private static void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (Throwable ignored) {
            // 剪贴板不可用时静默跳过（日志文件仍可读取）
        }
    }

    // ---- 入场辅助 ----

    private static float progress(int index) {
        return entrance[index].get();
    }

    private static float slideX(float base, float p) {
        return base + (1 - p) * SLIDE;
    }

    private static Color alpha(Color c, float p) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * p));
    }

    public static void render(long vg, int width, int height) {
        float margin = 120f;
        float top = 64f;

        // 背景淡入（覆盖在游戏画面上）
        float bgP = progress(0);
        RenderUtils.drawRect(vg, 0, 0, width, height,
                new Color(BG.getRed(), BG.getGreen(), BG.getBlue(), (int) (255 * bgP)));

        // 小标题（与暂停页统一格式：ORANGE ENGINE v<版本> · <状态>）
        float kickerP = progress(1);
        String kicker = "ORANGE ENGINE v" + OrangeEngine.version + " · ENGINE PANIC";
        FontManager.jetBrainsMono_CHS.drawText(kicker, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                slideX(margin, kickerP), top + 13, 13, alpha(TEXT_FAINT, kickerP));

        // 主标题 + 副标题
        float heroP = progress(2);
        float heroY = top + 16 + 56;
        FontManager.hmSans_CHS.drawText("引擎遇到问题。", NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                slideX(margin, heroP), heroY, 56, alpha(TEXT, heroP));
        float subP = progress(3);
        float subY = heroY + 16 + 20;
        FontManager.jetBrainsMono_CHS.drawText("ENGINE PANIC", NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                slideX(margin, subP), subY, 20, alpha(CRIMSON, subP));

        // 说明
        float ledeP = progress(4);
        FontManager.hmSans_CHS.drawText("发生了无法恢复的错误，引擎等待处理。可复制诊断信息以提交问题。",
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                slideX(margin, ledeP), subY + 20, 15, alpha(TEXT_DIM, ledeP));

        // 崩溃摘要面板
        float panelP = progress(5);
        float panelW = Math.min(560, width - margin * 2);
        float panelY = subY + 16 + 28;
        float rowH = 49f;
        RenderUtils.drawRect(vg, margin, panelY, margin + panelW, panelY + 4 + 4 * rowH, alpha(PANEL, panelP));
        RenderUtils.drawRect(vg, margin, panelY, margin + panelW, panelY + 1, new Color(255, 255, 255, (int) (30 * panelP)));
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 0, "异常", title, panelP);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 1, "线程", threadName, panelP);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 2, "模块", module, panelP);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 3, "代码", code, panelP);

        // 诊断终端（仅显示前若干行）
        float termP = progress(6);
        float termY = panelY + 4 + 4 * rowH + 20;
        float termH = Math.min(36, stackLines.length) * 22 + 32;
        RenderUtils.drawRect(vg, margin, termY, margin + panelW, termY + termH, alpha(TERM_BG, termP));
        RenderUtils.drawRect(vg, margin, termY, margin + panelW, termY + 1, new Color(255, 255, 255, (int) (30 * termP)));
        float ty = termY + 24;
        int shown = Math.min(36, stackLines.length);
        for (int i = 0; i < shown; i++) {
            String line = stackLines[i];
            Color c = TEXT_DIM;
            if (line.startsWith("java.") || line.startsWith("Exception")
                    || line.startsWith("Caused by") || line.startsWith("FATAL")) {
                c = CRIMSON;
            }
            FontManager.jetBrainsMono_CHS.drawText(line, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                    margin + 20, ty, 13, alpha(c, termP));
            ty += 22;
        }

        // 底部操作按钮（右下角）：[继续游戏][复制诊断信息][关闭引擎]
        float btnP = progress(7);
        float btnY = height - 96;
        float continueW = 132, closeW = 132, copyW = 176, gap = 12, btnH = 40;
        closeBtnX = width - 120 - closeW;
        closeBtnY = btnY;
        closeBtnW = closeW;
        closeBtnH = btnH;
        copyBtnX = closeBtnX - gap - copyW;
        copyBtnY = btnY;
        copyBtnW = copyW;
        copyBtnH = btnH;
        continueBtnX = copyBtnX - gap - continueW;
        continueBtnY = btnY;
        continueBtnW = continueW;
        continueBtnH = btnH;

        // 继续游戏（Accent 实底，逃生通道）
        RenderUtils.drawRect(vg, continueBtnX, btnY, continueBtnX + continueW, btnY + btnH,
                alpha(OrangeEngine.accentColor, btnP));
        FontManager.hmSans_CHS.drawText("继续游戏", NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE,
                continueBtnX + continueW / 2, btnY + btnH / 2, 15, alpha(TEXT, btnP));

        // 关闭引擎（Crimson 实底，右缘）
        RenderUtils.drawRect(vg, closeBtnX, btnY, closeBtnX + closeW, btnY + btnH, alpha(CRIMSON, btnP));
        FontManager.hmSans_CHS.drawText("关闭引擎", NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE,
                closeBtnX + closeW / 2, btnY + btnH / 2, 15, alpha(TEXT, btnP));

        // 复制诊断信息（透明底）
        RenderUtils.drawRect(vg, copyBtnX, btnY, copyBtnX + copyW, btnY + btnH,
                new Color(255, 255, 255, (int) (26 * btnP)));
        FontManager.hmSans_CHS.drawText("复制诊断信息", NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE,
                copyBtnX + copyW / 2, btnY + btnH / 2, 15, alpha(TEXT, btnP));

        // Logo（geometos，左下角，与暂停页同款两行落款）
        float logoP = progress(8);
        String[] words = OrangeEngine.engineName.split(" ");
        if (words.length >= 2) {
            FontManager.geometos.drawText(words[0], NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BOTTOM,
                    slideX(margin, logoP), height - 30, 35, alpha(TEXT, logoP));
            FontManager.geometos.drawText(words[1], NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BOTTOM,
                    slideX(margin, logoP), height - 10, 25, alpha(TEXT, logoP));
        }
    }

    private static void drawSummaryRow(long vg, float x, float y, float w, float rowH,
                                       int index, String label, String value, float p) {
        float rowY = y + 4 + index * rowH;
        FontManager.jetBrainsMono_CHS.drawText(label, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE,
                x + 20, rowY + rowH / 2, 12, alpha(TEXT_FAINT, p));
        FontManager.jetBrainsMono_CHS.drawText(value, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE,
                x + 120, rowY + rowH / 2, 15, alpha(index == 0 ? CRIMSON : TEXT, p));
        if (index < 3) {
            RenderUtils.drawRect(vg, x, rowY + rowH, x + w, rowY + rowH + 1, new Color(255, 255, 255, (int) (30 * p)));
        }
    }
}
