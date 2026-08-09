package cms.mortalchen.orange.render;

import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Metro 风格 Panic 屏（docs/orange-engine-ui-spec.md §7.9）。
 *
 * 由 {@code Utils.panic} 触发：先弹屏渲染（含崩溃摘要 + 诊断终端），
 * 4 秒后或按键/点击「关闭引擎」时 {@code System.exit(-1)}。
 * 触发瞬间把诊断文本写入系统剪贴板（尽力而为）。
 * 渲染循环未运行时（启动失败/初始化前）由调用方直接退出，不经过本类。
 */
public final class PanicScreen {
    private static final long SHOW_MS = 4000;

    private static final Color BG = new Color(0x1A, 0x1A, 0x1A);
    private static final Color PANEL = new Color(0x2D, 0x2D, 0x30);
    private static final Color TERM_BG = new Color(0x0C, 0x0C, 0x0C);
    private static final Color CRIMSON = new Color(0xA2, 0x00, 0x25);
    private static final Color LINE = new Color(255, 255, 255, 30);
    private static final Color TEXT = Color.WHITE;
    private static final Color TEXT_DIM = new Color(255, 255, 255, 140);
    private static final Color TEXT_FAINT = new Color(255, 255, 255, 72);

    private static volatile boolean active;
    private static volatile long exitDeadline;
    private static volatile String title = "Engine Panic";
    private static volatile String threadName = "-";
    private static volatile String module = "-";
    private static volatile String code = "-";
    private static volatile String[] stackLines = new String[0];
    private static volatile String diagnosticsText = "";

    // 按钮几何（render 时计算，onClick 复用）
    private static float closeBtnX, closeBtnY, closeBtnW, closeBtnH;
    private static float copyBtnX, copyBtnY, copyBtnW, copyBtnH;

    private PanicScreen() {}

    public static boolean isActive() {
        return active;
    }

    public static long exitDeadline() {
        return exitDeadline;
    }

    /** 幂等触发：已激活时忽略后续 panic（防止重复弹屏/反复 System.exit）。 */
    public static void trigger(String panicTitle, String threadName, String module, String code, Throwable t) {
        if (active) {
            return;
        }
        active = true;
        exitDeadline = System.currentTimeMillis() + SHOW_MS;
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
        copyToClipboard(diagnosticsText);
    }

    public static void onKey() {
        doExit();
    }

    public static void onClick(int button, int action, float x, float y) {
        if (action != 1) { // GLFW_PRESS
            return;
        }
        if (x >= closeBtnX && x <= closeBtnX + closeBtnW && y >= closeBtnY && y <= closeBtnY + closeBtnH) {
            doExit();
        } else if (x >= copyBtnX && x <= copyBtnX + copyBtnW && y >= copyBtnY && y <= copyBtnY + copyBtnH) {
            copyToClipboard(diagnosticsText);
        }
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

    public static void render(long vg, int width, int height) {
        float margin = 120f;
        float top = 64f;
        RenderUtils.drawRect(vg, 0, 0, width, height, BG);

        // 小标题（与暂停页统一格式：ORANGE ENGINE v<版本> · <状态>）
        String kicker = "ORANGE ENGINE v" + OrangeEngine.version + " · ENGINE PANIC";
        FontManager.jetBrainsMono_CHS.drawText(kicker, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                margin, top + 13, 13, TEXT_FAINT);

        // 主标题 + 副标题
        float heroY = top + 16 + 56;
        FontManager.hmSans_CHS.drawText("引擎遇到问题。", NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                margin, heroY, 56, TEXT);
        float subY = heroY + 16 + 20;
        FontManager.jetBrainsMono_CHS.drawText("ENGINE PANIC", NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BASELINE,
                margin, subY, 20, CRIMSON);

        // 崩溃摘要面板
        float panelW = Math.min(560, width - margin * 2);
        float panelY = subY + 28;
        float rowH = 49f;
        RenderUtils.drawRect(vg, margin, panelY, margin + panelW, panelY + 4 + 4 * rowH, PANEL);
        RenderUtils.drawRect(vg, margin, panelY, margin + panelW, panelY + 1, LINE);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 0, "异常", title);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 1, "线程", threadName);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 2, "模块", module);
        drawSummaryRow(vg, margin, panelY, panelW, rowH, 3, "代码", code);

        // 诊断终端（仅显示前若干行）
        float termY = panelY + 4 + 4 * rowH + 20;
        float termH = Math.min(36, stackLines.length) * 22 + 32;
        RenderUtils.drawRect(vg, margin, termY, margin + panelW, termY + termH, TERM_BG);
        RenderUtils.drawRect(vg, margin, termY, margin + panelW, termY + 1, LINE);
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
                    margin + 20, ty, 13, c);
            ty += 22;
        }

        // 底部操作按钮（右下角）
        float btnY = height - 96;
        float closeW = 132, copyW = 176, gap = 12, btnH = 40;
        closeBtnX = width - 120 - closeW;
        closeBtnY = btnY;
        closeBtnW = closeW;
        closeBtnH = btnH;
        copyBtnX = closeBtnX - gap - copyW;
        copyBtnY = btnY;
        copyBtnW = copyW;
        copyBtnH = btnH;

        RenderUtils.drawRect(vg, closeBtnX, btnY, closeBtnX + closeW, btnY + btnH, CRIMSON);
        FontManager.hmSans_CHS.drawText("关闭引擎", NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE,
                closeBtnX + closeW / 2, btnY + btnH / 2, 15, TEXT);

        RenderUtils.drawRect(vg, copyBtnX, btnY, copyBtnX + copyW, btnY + btnH, new Color(255, 255, 255, 26));
        FontManager.hmSans_CHS.drawText("复制诊断信息", NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE,
                copyBtnX + copyW / 2, btnY + btnH / 2, 15, TEXT);
    }

    private static void drawSummaryRow(long vg, float x, float y, float w, float rowH,
                                       int index, String label, String value) {
        float rowY = y + 4 + index * rowH;
        FontManager.jetBrainsMono_CHS.drawText(label, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE,
                x + 20, rowY + rowH / 2, 12, TEXT_FAINT);
        FontManager.jetBrainsMono_CHS.drawText(value, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE,
                x + 120, rowY + rowH / 2, 15, index == 0 ? CRIMSON : TEXT);
        if (index < 3) {
            RenderUtils.drawRect(vg, x, rowY + rowH, x + w, rowY + rowH + 1, LINE);
        }
    }
}
