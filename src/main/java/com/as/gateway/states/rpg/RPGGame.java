package com.as.gateway.states.rpg;

import cms.mortalchen.orange.MainGame;
import com.as.gateway.gameplay.entity.PlayerEntity;
import com.as.gateway.gameplay.map.Map;
import com.as.gateway.gameplay.map.impl.InfHallwayMap;
import com.as.gateway.gameplay.map.impl.MCHouseMap;
import com.as.gateway.gameplay.render.Camera;
import com.as.gateway.states.rpg.gui.GameGui;
import org.joml.Vector2f;

public class RPGGame {
    public static RPGGame instance;
    public static final float scale = 1f;
    public static final float tileSize = 32f;

    public GameGui currentGui = null;
    public GameGui guiGoal = null;

    public boolean isDebug = false;

    public Map currentMap = new MCHouseMap();
    public PlayerEntity player = new PlayerEntity(new Vector2f(64,64));
    public Camera camera = new Camera(new Vector2f(0,0));

    public RPGGame(){
        instance = this;
    }

    public void update(){
        camera.updatePos(player.pos);

        if (currentGui != null){
            currentGui.width = MainGame.WINDOW_WIDTH;
            currentGui.height = MainGame.WINDOW_HEIGHT;
            if (guiGoal != null) {
                currentGui.closing = true;
            }
            if (currentGui.closed) {
                currentGui = guiGoal;
                guiGoal = null;
            }
        }
    }

    public void setCurrentGui(GameGui gui){
        if (gui == null){
            currentGui.closing = true;
            guiGoal = null;
        }else {
            gui.init();
            if (currentGui != null) {
                guiGoal = gui;
            } else {
                currentGui = gui;
            }
        }
    }
}
