package com.as.gateway.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.input.KeyBinding;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.render.menuflow.MenuFlow;
import cms.mortalchen.orange.render.menuflow.MenuFlowObj;
import cms.mortalchen.orange.states.GameState;
import cms.mortalchen.orange.utils.Utils;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.AnimatedColor;
import com.as.gateway.states.rpg.RPGGameState;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static cms.mortalchen.orange.MainGame.WINDOW_HEIGHT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP;

public class MainMenuState extends GameState {
    MenuFlow menuFlow = new MenuFlow();
    int index = 0;


    public MainMenuState(MainGame game) {
        super(game);
        menuFlow.add(new MenuFlowObj(0,"Start",25f, FontManager.exo,new AnimatedColor(new Color(255,255,255),16f), this::startGame));
        menuFlow.add(new MenuFlowObj(1,"Exit",25f, FontManager.exo,new AnimatedColor(new Color(255,255,255),16f), game::endGame));

        menuFlow.flowItems.forEach(menuFlowObj -> {
            menuFlowObj.selectAnimatedColorTarget = new Color(237, 255, 181,255);
            menuFlowObj.unSelectAnimatedColorTarget = new Color(200,200,200,0);
        });
    }

    @Override
    public void render(long vg) {
        RenderUtils.drawGradientUpDown(vg,60f + FontManager.montserrat.getStringWidth("Project: Gateway",50f),(float) WINDOW_HEIGHT / 2 - 190,60f + FontManager.montserrat.getStringWidth("Project: Gateway",50f) + 10f,(float) WINDOW_HEIGHT / 2 - 190 + FontManager.montserrat.getStringHeight(50f), new Color(255,255,255, (int) (255 * Math.abs(Math.sin(GLFW.glfwGetTime())))),new Color(120,120,120, (int) (255 * Math.abs(Math.sin(GLFW.glfwGetTime())))));

        FontManager.montserrat.drawText("Project: Gateway", NVG_ALIGN_LEFT | NVG_ALIGN_TOP, 40f, (float) WINDOW_HEIGHT / 2 - 190, 50, Utils.color(1f, 1f, 1f, 1f));

//        FontManager.geometos.drawText(OrangeEngine.engineName.split(" ")[0], NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM, width - 19f, height - 30f, 35, new Color(255,255,255));
//        FontManager.geometos.drawText(OrangeEngine.engineName.split(" ")[1], NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM, width - 20f, height - 10f, 25, new Color(255,255,255));

        menuFlow.render(vg, 40f,height / 1.9f,40f,index);
    }

    @Override
    public void updateTick(float deltaTime) {
    }

    boolean isPressed = false;

    public void onKeyPressed(int key, int action) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.UP.get())) {
            if (index > 0 && !isPressed) {
                index--;
                isPressed = true;
            }
        }
        if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.DOWN.get())){
            if (index < menuFlow.flowItems.size() - 1 && !isPressed) {
                index++;
                isPressed = true;
            }
        }
        if (!OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.UP.get()) && !OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.DOWN.get())){
            isPressed = false;
        }

        if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.INTERACT.get())){
            menuFlow.onTrigger(index);
        }
    }

    private void startGame() {
        game.changeState(new RPGGameState(game));
    }
}
