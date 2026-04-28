package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.Motion;
import cms.mortalchen.orange.utils.render.anim.animatednumber.AnimatedFloat;
import cms.mortalchen.orange.utils.render.color.Palette;
import cms.mortalchen.orange.utils.time.TimerUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class DefaultState extends GameState {
    private final TimerUtil timer = new TimerUtil();
    private boolean isStartedUp = false;

    // 阶段锁
    private boolean phase1Triggered = false; // 边框收缩
    private boolean phase2Triggered = false; // 文字浮现
    private boolean phase3Triggered = false; // 放大退场

    // --- Hulu 风格核心动画变量 ---
    // 1. 边框动画 (从极大的尺寸缩小到 Logo 尺寸)
    public AnimatedFloat boxWidth = new AnimatedFloat(3000f);  // 初始极宽
    public AnimatedFloat boxHeight = new AnimatedFloat(2000f); // 初始极高
    public AnimatedFloat boxStroke = new AnimatedFloat(40f);   // 边框粗细
    public AnimatedFloat boxRadius = new AnimatedFloat(100f);  // 圆角半径

    // 2. 文字动画
    public AnimatedFloat textAlpha = new AnimatedFloat(0f);
    public AnimatedFloat textY = new AnimatedFloat(30f);
    public AnimatedFloat textSpacing = new AnimatedFloat(20f); // 字母间距缩小效果

    // 3. 全局转场动画
    public AnimatedFloat globalScale = new AnimatedFloat(100f); // 100 = 1.0x
    public AnimatedFloat globalAlpha = new AnimatedFloat(100f);

    // 品牌色 (橙色，你也可以根据需要调整)
    private final Color BRAND_COLOR = new Color(255, 120, 0);

    public DefaultState(MainGame game) {
        super(game);
        allowPause = false;
        timer.reset();
    }

    @Override
    public void render(long vg) {
        float scale = globalScale.get() * 0.01f;
        float alpha = Math.max(0, Math.min(1, globalAlpha.get() * 0.01f));
        float tAlpha = Math.max(0, Math.min(1, textAlpha.get() * 0.01f));

        String[] words = OrangeEngine.engineName.split(" ");
        String word1 = words.length > 0 ? words[0] : "ORANGE";
        String word2 = words.length > 1 ? words[1] : "ENGINE";

        nvgBeginFrame(vg, width, height, 1.0f);

        // --- 开启全局矩阵 (缩放与透明度) ---
        nvgSave(vg);
        nvgGlobalAlpha(vg, alpha);
        nvgTranslate(vg, width * 0.5f, height * 0.5f);
        nvgScale(vg, scale, scale);
        nvgTranslate(vg, -width * 0.5f, -height * 0.5f);

        nvgShapeAntiAlias(vg, true);

        // 获取当前动画数值
        float currentBoxW = boxWidth.get();
        float currentBoxH = boxHeight.get();
        float currentStroke = boxStroke.get();
        float currentRadius = boxRadius.get();

        float boxX = (width - currentBoxW) / 2f;
        float boxY = (height - currentBoxH) / 2f;

        // 使用 MemoryStack 在原生栈上分配 NVGColor，保证高性能且无内存泄漏
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            org.lwjgl.nanovg.NVGColor nvgColor = org.lwjgl.nanovg.NVGColor.malloc(stack);

            float r = BRAND_COLOR.getRed() / 255f;
            float g = BRAND_COLOR.getGreen() / 255f;
            float b = BRAND_COLOR.getBlue() / 255f;

            // 1. 绘制边框外发光
            RenderUtils.startBloom(vg);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, boxX, boxY, currentBoxW, currentBoxH, currentRadius);

//            // 设置发光层的低透明度
//            nvgRGBAf(r, g, b, 0.15f, nvgColor);
//            nvgStrokeColor(vg, nvgColor);
//            nvgStrokeWidth(vg, currentStroke + (8f));
//            nvgStroke(vg);

            // 2. 绘制实体边框 (这一步现在绝对会显示了)
            nvgBeginPath(vg);
            nvgRoundedRect(vg, boxX, boxY, currentBoxW, currentBoxH, currentRadius);
            // 设置实体层为 1.0 的完全不透明度
            nvgRGBAf(r, g, b, 1.0f, nvgColor);
            nvgStrokeColor(vg, nvgColor);
            nvgStrokeWidth(vg, currentStroke);
            nvgStroke(vg);
            RenderUtils.stopBloom(vg);
        }

        // --- 绘制内部文字 ---
        if (tAlpha > 0.01f) {
            FontManager.geometos.drawText(
                    word1,
                    NVG_ALIGN_CENTER | NVG_ALIGN_BOTTOM,
                    width / 2f,
                    (height / 2f) + 20 + textY.get(),
                    55,
                    Palette.reAlpha(Color.WHITE, tAlpha)
            );

            FontManager.geometos.drawText(
                    word2,
                    NVG_ALIGN_CENTER | NVG_ALIGN_TOP,
                    width / 2f,
                    (height / 2f) + 20 + textY.get(),
                    22,
                    Palette.reAlpha(BRAND_COLOR, tAlpha * 0.9f)
            );
        }

        nvgRestore(vg); // 恢复矩阵
        nvgEndFrame(vg);
    }
    @Override
    public void updateTick(float deltaTime) {
        if (MainGame.fastIntro) {
            boxWidth.set(380f);
            boxHeight.set(140f);
            boxStroke.set(6f);
            boxRadius.set(20f);
            textAlpha.set(100f);
            textY.set(0f);

            if (timer.hasReached(500) && !isStartedUp) {
                MainGame.startMainGame.run();
                isStartedUp = true;
            }
            return;
        }

        // --- 阶段 1：巨型边框迅猛砸向屏幕中心 (Hulu Frame Collapse 效果) ---
        // 使用 BACK_OUT (带轻微回弹) 或 QUART_OUT 模拟极强的惯性冲击
        if (timer.hasReached(100) && !phase1Triggered) {
            // 目标尺寸：宽 380，高 140，是一个长条圆角矩形
            boxWidth.to(380f, 900, Motion.Easing.EXPO_IN_OUT);
            boxHeight.to(140f, 900, Motion.Easing.EXPO_IN_OUT);
            boxStroke.to(6f, 800, Motion.Easing.QUART_OUT);  // 线条变细
            boxRadius.to(20f, 800, Motion.Easing.QUART_OUT); // 圆角收缩
            phase1Triggered = true;
        }

        // --- 阶段 2：文字在边框中心浮现 ---
        // 错开时间，等边框缩得差不多了 (大概 600ms 后)，文字悄然浮现并上浮
        if (timer.hasReached(700) && !phase2Triggered) {
            textAlpha.to(100f, 500, Motion.Easing.QUART_OUT);
            textY.to(0f, 800, Motion.Easing.EXPO_OUT);
            phase2Triggered = true;
        }

        // --- 阶段 3：画面定格后，整体冲向镜头并淡出转场 ---
        if (timer.hasReached(2600) && !phase3Triggered) {
            // 全局放大到 5 倍 (500)
            globalScale.to(800f, 400, Motion.Easing.EXPO_IN);
            // 稍加延迟后淡出，制造 "冲破 Logo 进入画面" 的沉浸感
            globalAlpha.to(0f, 500, Motion.Easing.CUBIC_IN);
            phase3Triggered = true;
        }

        // 结束动画
        if (timer.hasReached(3600) && !isStartedUp) {
            MainGame.startMainGame.run();
            isStartedUp = true;
        }
    }

    @Override
    public void onKeyPressed(int key, int action) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_G)){
            game.changeState(new DefaultState(game));
        }
        super.onKeyPressed(key, action);
    }
}