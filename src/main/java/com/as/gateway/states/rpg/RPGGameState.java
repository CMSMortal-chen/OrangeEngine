package com.as.gateway.states.rpg;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.input.KeyBinding;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.states.GameState;
import com.as.gateway.states.rpg.gui.menu.RPGMenu;
import org.joml.Vector2f;

import java.awt.*;

public class RPGGameState extends GameState {

    RPGGame rpgGame = new RPGGame();

    public Vector2f originPoint;

    public RPGGameState(MainGame game) {
        super(game);
        rpgGame.player.pos.set(new Vector2f(38, 7));
    }

    /**
     * 内容渲染
     *
     * @param vg NanoVg实例
     */
    @Override
    public void render(long vg) {
        originPoint = new Vector2f((int) (width / 2f) - 32, (int) (height / 2f) - 32);

        rpgGame.currentMap.render(vg, rpgGame.player.pos, originPoint);
        rpgGame.player.render(vg, originPoint.x + rpgGame.camera.pos.x, originPoint.y + rpgGame.camera.pos.y);

        if (rpgGame.currentGui != null){
            rpgGame.currentGui.render(vg);
        }

        if (rpgGame.isDebug) {
            FontManager.exo.drawTextWithShadow(rpgGame.player.pos.x + " | " + rpgGame.player.pos.y, 10, 10, 16f, Color.WHITE);
        }
    }

    @Override
    public void update(float deltaTime){
        rpgGame.player.controller.handleMovement();
        rpgGame.update();
    }

    /**
     * 游戏更新
     *
     * @param deltaTime deltatime
     */
    @Override
    public void updateTick(float deltaTime) {
        if (rpgGame.currentGui == null) {
            rpgGame.player.controller.handleControl();
        }
        rpgGame.player.onUpdate();
        rpgGame.currentMap.update();
    }

    @Override
    public void onKeyPressed(int key, int action) {
        if (rpgGame.currentGui != null) {
            rpgGame.currentGui.onKeyPressed(key, action);
            rpgGame.player.controller.isWalking = false;
        }else {
            if (OrangeEngine.instance.inputManager.isKeyPressed(KeyBinding.CANCEL.get())){
                rpgGame.setCurrentGui(new RPGMenu());
            }
        }
    }
}
