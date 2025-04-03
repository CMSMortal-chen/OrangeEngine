package com.as.gateway.states.olos;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.states.GameState;
import cms.mortalchen.orange.render.oilmenuflow.OILMenuFlowObj;
import cms.mortalchen.orange.render.oilmenuflow.OILMenuFlow;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.AnimatedColor;
import cms.mortalchen.orange.utils.render.anim.animatednumber.BounceAnimation;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

public class OILOpenOSState extends GameState {

    public String version = "bulid200925_3R";

    /**
     * 型号:
     * 最新:UX 960
     * UX705
     * 前面字母代表主板类型
     * U - Ultimate 终极版 所有功能
     * M - Medium 基础版 有些身体部件没有高级版好
     * L - Light  轻量版 没有TRUEAI 身体部件更便宜
     * 后面字母代表CPU运行速度
     * X - eXtreme 高配
     * N - Normal 标配
     * L - Lite 低配
     * 数字百位代表第几代，后面两位代表这一代第几个版本
     * <p>
     * UX - 支持TRUEAI，高配版
     * UN - 支持TRUEAI，中配版
     * UL - 支持TRUEAI，低配版
     * <p>
     * MX - 支持TRUEAI，高配版
     * MN - 支持TRUEAI，中配版
     * ML - 支持TRUEAI，低配版
     * <p>
     * LX - 不支持TRUEAI，高配版
     * LN - 不支持TRUEAI，中配版
     * LL - 官网没有这个挡位，仅DIY用
     * 后面跟的英文Standard / Based
     * Standard 代表预先组装完成的整机，一般都是在官网售卖的整机
     * Based 代表基于此标准组装的机器
     */
    public String modelString = "Device:UX960 Standard";
    public OILMenuFlow menuFlow = new OILMenuFlow();
    public int index = 1;
    public BounceAnimation bounceAnimation = new BounceAnimation(0f, 0f, 21f);

    public OILOpenOSState(MainGame game) {
        super(game);
        menuFlow.add(
                new OILMenuFlowObj(0, "Memory Manager", 30f, FontManager.exo, new AnimatedColor(new Color(168, 168, 168), 14f), () -> {

                }),
                new OILMenuFlowObj(1, "Setting", 30f, FontManager.exo, new AnimatedColor(new Color(168, 168, 168), 14f), () -> {

                }),
                new OILMenuFlowObj(2, "Personality Editor", 30f, FontManager.exo, new AnimatedColor(new Color(168, 168, 168), 14f), () -> {

                })
        );
    }

    @Override
    public void render(long vg) {
        version = "bulid200925_3R";
        modelString = "Device:UX960 Standard";

        RenderUtils.drawRect(vg, -10, -10, width + 10, height + 10, Color.WHITE);
        RenderUtils.drawCircleStroke(vg, (float) (width + (Math.sin(GLFW.glfwGetTime() + 8f) * 10f)), (float) ((height / 4.2f) + (Math.sin(GLFW.glfwGetTime() + 3f) * 10f)), width / 5.5f, width / 50f, new Color(216, 216, 216));
        RenderUtils.drawCircleStroke(vg, (float) (width - (width / 4.2f) + (Math.sin(GLFW.glfwGetTime()) * 10f)), (float) ((height / 15f) + (Math.sin(GLFW.glfwGetTime() + 10f) * 10f)), width / 5f, width / 50f, new Color(69, 255, 69));

        Font font = FontManager.exo;

        float xPos = width - 10f;
        font.drawText(version, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, xPos, height - 4f - font.getStringHeight(16f) - 5f, 16f, new Color(128, 128, 128));
        float xPos2 = xPos - font.getStringWidth(version, 16f) - 5f;
        font.drawText("OPEN", NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, xPos2 - 5f - font.getStringWidth("LIFE", 36f) - font.getStringWidth("OS", 36), height - font.getStringHeight(36f) - 5f, 36f, new Color(69, 255, 69));
        font.drawText("LIFE", NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, xPos2 - 5f - font.getStringWidth("OS", 36f), height - font.getStringHeight(36f) - 5f, 36f, new Color(216, 216, 216));
        font.drawText(" OS", NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP, xPos2, height - font.getStringHeight(36f) - 5f, 36f, new Color(128, 128, 128));


        font.drawText(modelString, 10f, height - 4f - font.getStringHeight(22f) - 5f, 22f, new Color(128, 128, 128));
        //选择框

        bounceAnimation.translate();
        RenderUtils.drawRoundedRect(vg, -100, (height / 2f) - 50f + bounceAnimation.get(), (width / 2f), (height / 2f) + 50f + bounceAnimation.get(), 100f / 2f, new Color(171, 255, 171));
        FontManager.sourceHan_CHS.drawText(">", NanoVG.NVG_ALIGN_MIDDLE | NanoVG.NVG_ALIGN_CENTER, (width / 2f) - 35f, (height / 2f) + 4f + bounceAnimation.get(), 38f, new Color(38, 38, 38));

        menuFlow.render(vg, 20f, (height / 2f) - (index * (height / 6.5f)), index);
    }

    boolean isPressed = false;

    @Override
    public void updateTick(float deltaTime) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            if (index > 0 && !isPressed) {
                index--;
                bounceAnimation.bounce(30f);
                isPressed = true;
            }
        }
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            if (index < menuFlow.menuFlowList.size() - 1 && !isPressed) {
                index++;
                bounceAnimation.bounce(-30f);
                isPressed = true;
            }
        }
        if (!OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_S) && !OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            isPressed = false;
        }
    }
}
