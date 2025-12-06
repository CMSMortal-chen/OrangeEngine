package com.as.gateway.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.states.GameState;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.Motion;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TestAnimState extends GameState {

    boolean flag = true;
    private final List<Motion.Controller> controllers = new ArrayList<>();
    private final List<Motion.Easing> easings = List.of(Motion.Easing.values());

    // 基础配置
    private static final double START_X = 0; // 屏幕外左侧
    private static final double TARGET_X = 1000;  // 目标 X 坐标
    private static final long DURATION_MS = 1500; // 动画时长 1.5 秒

    public TestAnimState(MainGame game) {
        super(game);
        // 1. 初始化所有控制器
        for (int i = 0; i < easings.size(); i++) {
            // 所有矩形从屏幕外的 START_X 启动
            Motion.Controller controller = new Motion.Controller(START_X);
            controllers.add(controller);
        }
    }

    /**
     * 内容渲染
     *
     * @param vg NanoVg实例
     */
    @Override
    public void render(long vg) {
        final double rectWidth = 150;
        final double rectHeight = 15;
        final double rectSpacing = 5;
        double currentY = 50; // 初始 Y 坐标

        for (int i = 0; i < controllers.size(); i++) {
            Motion.Controller controller = controllers.get(i);
            Motion.Easing easing = easings.get(i);

            // 1. 获取当前动画 X 坐标
            double x = controller.get();

            // 2. 计算矩形边界
            double x1 = x;
            double y1 = currentY;
            double x2 = x + rectWidth;
            double y2 = currentY + rectHeight;

            // 3. 绘制矩形
            // 假设 Color.WHITE 只是一个示例颜色
            RenderUtils.drawRect(vg, x1, y1, x2, y2, Color.WHITE);

            // 4. 绘制曲线名称 (可选，用于调试)
            // 假设你有一个绘制文本的工具类 FontManager
            FontManager.hmSans_CHS.drawText(easing.name(), (float) (x + rectWidth + 10), (float) (currentY + 3),14f,Color.WHITE);
            // FontManager.drawString(easing.name(), x + rectWidth + 10, currentY + 3, Color.WHITE.getRGB());

            // 5. 更新下一个矩形的 Y 坐标
            currentY += rectHeight + rectSpacing;
        }    }

    @Override
    public void onKeyPressed(int key, int action) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_G)){
            flag = !flag;
            for (int i = 0; i < controllers.size(); i++) {
                controllers.get(i).to(flag? START_X : TARGET_X,500, easings.get(i));
            }
        }
        super.onKeyPressed(key, action);
    }
}
