package com.as.gateway.states;

import cms.mortalchen.orange.states.GameState;
import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.resource.ResourceManager;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.utils.render.RenderUtils;
import com.as.gateway.gameplay.MenuButtonObject;
import cms.mortalchen.orange.render.Image;
import cms.mortalchen.orange.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cms.mortalchen.orange.MainGame.WINDOW_HEIGHT;
import static cms.mortalchen.orange.MainGame.WINDOW_WIDTH;
import static org.lwjgl.nanovg.NanoVG.*;

public class MainMenuState extends GameState {
    private final List<MenuButtonObject> menuButtonObjects;
    private final Image dbRed;
    private final Image dbGreen;
    private MenuButtonObject startMenuButtonObject;
    private MenuButtonObject exitMenuButtonObject;

    public MainMenuState(MainGame game) {
        super(game);
        menuButtonObjects = new ArrayList<>();

        dbRed = new Image(new ResourceManager(game.vg), new ResourceLocation("/textures/image/db-red.png"));
        dbGreen = new Image(new ResourceManager(game.vg), new ResourceLocation("/textures/image/db-green.png"));


        startMenuButtonObject = new MenuButtonObject(game, 0, 40, (float) WINDOW_HEIGHT / 2 - 50, 200, 80, "Start", 35);
        //keybindingList = new Button(game, 2, 40, (float) WINDOW_HEIGHT / 2 + 60, 200, 80, "Keybinding List", 20);
        exitMenuButtonObject = new MenuButtonObject(game, 1, 40, (float) WINDOW_HEIGHT / 2 + 110, 200, 80, "Exit", 18);


        startMenuButtonObject.setOnActivated(this::startGame);
        exitMenuButtonObject.setOnActivated(game::endGame);

        menuButtonObjects.add(startMenuButtonObject);
        //buttons.add(topListButton);
        //buttons.add(keybindingList);
        menuButtonObjects.add(exitMenuButtonObject);

    }

    @Override
    public void render(long vg) {
        //RenderUtils.drawGradientSideWay(vg,0,0,width,height, Color.BLUE,Color.RED);

        game.font.drawText("Project: Gateway", NVG_ALIGN_LEFT | NVG_ALIGN_BOTTOM, 40f, (float) WINDOW_HEIGHT / 2 - 190, 50, Utils.color(1f, 1f, 1f, 1f));

        game.font.drawText(OrangeEngine.engineName, NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM, (float) WINDOW_WIDTH + 470, (float) WINDOW_HEIGHT / 2 + 335, 20, Utils.color(0.4f, 0.4f, 0.4f, 0.6f));

        menuButtonObjects.forEach(e -> e.renderButton(game.vg, game.font,NVG_ALIGN_LEFT | NVG_ALIGN_BOTTOM));

        for (int i = 0; i < menuButtonObjects.size(); i++) {
            menuButtonObjects.get(i).updatePos(40f ,(height / 2f - 10f) + i * 50f, 200, 80);
        }
    }

    @Override
    public void update(float deltaTime) {
    }

    public void onKeyPressed(int key, int action) {
        menuButtonObjects.forEach(menuButtonObject -> menuButtonObject.onKeyPressed(key, action, menuButtonObjects));
    }

    private void startGame() {
        //game.changeState();
        dbRed.cleanup();
        dbGreen.cleanup();
    }
}
