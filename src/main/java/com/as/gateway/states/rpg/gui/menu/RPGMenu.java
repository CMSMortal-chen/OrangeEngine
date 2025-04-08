package com.as.gateway.states.rpg.gui.menu;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.input.KeyBinding;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.render.menuflow.MenuFlow;
import cms.mortalchen.orange.render.menuflow.MenuFlowObj;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.AnimatedColor;
import cms.mortalchen.orange.utils.render.anim.AnimationUtil;
import com.as.gateway.states.MainMenuState;
import com.as.gateway.states.rpg.RPGGame;
import com.as.gateway.states.rpg.gui.GameGui;

import java.awt.*;

/**
 * Cancel键菜单
 */
public class RPGMenu extends GameGui {
    public MenuWindow currentWindow = null;
    public MenuWindow targetWindow = null;

    float animX = width / 3f;


    public MenuFlow mainMenuFlow = new MenuFlow();
    int index = 0;

    /**
     * 界面初始化
     */
    @Override
    public void init() {
        animX = MainGame.WINDOW_WIDTH / 3f;
        mainMenuFlow.add(new MenuFlowObj(0,"状态",35f, FontManager.sourceSerif_CHS,new AnimatedColor(new Color(5,5,5),16f), () -> {
            targetWindow = new StatusWindow();
        }));
        mainMenuFlow.add(new MenuFlowObj(1,"物品",35f, FontManager.sourceSerif_CHS,new AnimatedColor(new Color(5,5,5),16f), () -> {

        }));
        mainMenuFlow.add(new MenuFlowObj(2,"暂离",35f, FontManager.sourceSerif_CHS,new AnimatedColor(new Color(5,5,5),16f), () -> {
            MainGame.instance.changeState(new MainMenuState(MainGame.instance));
        }));
    }

    /**
     * 界面渲染
     *
     * @param vg NanoVG实例
     */
    @Override
    public void render(long vg) {
        if (!closing){
            animX = AnimationUtil.moveUD(animX,0f,20f,14f);
        }else {
            animX = AnimationUtil.moveUD(animX,width / 2.5f,20f,14f);
            if (animX >= width / 2.5f){
                closed = true;
            }
        }

        RenderUtils.drawGradientSideWay(vg,0,0,(width / 2.5f) - animX,height,new Color(255, 255, 255, 219), new Color(255,255,255,0));

        mainMenuFlow.render(vg, 40 - animX, 50, 80, index);
        if (currentWindow != null) {
            currentWindow.render(vg);
        }
        if (targetWindow != null){
            if (currentWindow != null) {
                currentWindow.exiting = true;
                if (currentWindow.exited) {
                    currentWindow = targetWindow;
                    targetWindow = null;
                }
            }else {
                currentWindow = targetWindow;
                targetWindow = null;
            }
        }else {
            if (currentWindow != null) {
                if (currentWindow.exited) {
                    currentWindow = null;
                }
            }
        }
        //FontManager.hmSans_CHS.drawText("[!] 在菜单中并不会导致时间暂停！请小心！", NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_BOTTOM, width + animX - 10, height - 10,15f, new Color(255,255,255,50));
    }

    boolean isPressed = false;
    @Override
    public void onKeyPressed(int key, int action) {
        if (currentWindow != null){
            if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.CANCEL.get())){
                currentWindow.exiting = true;
            }else {
                currentWindow.onKeyPressed(key, action);
            }
        }else {
            if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.CANCEL.get())){
                RPGGame.instance.setCurrentGui(null);
            }
            if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.UP.get())) {
                if (index > 0 && !isPressed) {
                    index--;
                    isPressed = true;
                }
            }
            if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.DOWN.get())) {
                if (index < mainMenuFlow.flowItems.size() - 1 && !isPressed) {
                    index++;
                    isPressed = true;
                }
            }
            if (!OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.UP.get()) && !OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.DOWN.get())) {
                isPressed = false;
            }
            if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.INTERACT.get())) {
                mainMenuFlow.onTrigger(index);
            }
        }
    }
}
