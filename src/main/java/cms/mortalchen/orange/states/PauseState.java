package cms.mortalchen.orange.states;


import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.render.MenuButtonObject;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static cms.mortalchen.orange.MainGame.WINDOW_HEIGHT;
import static cms.mortalchen.orange.MainGame.WINDOW_WIDTH;
import static org.lwjgl.nanovg.NanoVG.*;

public class PauseState extends GameState {
    private final List<MenuButtonObject> menuButtonObjects;
    private final MenuButtonObject resumeMenuButtonObject;
    private final MenuButtonObject exitMenuButtonObject;
    private final MenuButtonObject mainMenuMenuButtonObject;

    public PauseState(MainGame game) {
        super(game);
        allowPause = false;
        menuButtonObjects = new ArrayList<>();


        resumeMenuButtonObject = new MenuButtonObject(game, 0, 40f, height / 2f - 50f, 200, 80, "Resume", 35);
        mainMenuMenuButtonObject = new MenuButtonObject(game, 1, 40f, height / 2f + 10f, 200, 80, "Main Menu", 22);
        exitMenuButtonObject = new MenuButtonObject(game, 2, 40f, height / 2f + 60f, 200, 80, "Exit", 18);


        resumeMenuButtonObject.setOnActivated(this::resumeGame);
        mainMenuMenuButtonObject.setOnActivated(() -> MainGame.startMainGame.run());
        exitMenuButtonObject.setOnActivated(game::endGame);

        menuButtonObjects.add(resumeMenuButtonObject);
        menuButtonObjects.add(mainMenuMenuButtonObject);
        menuButtonObjects.add(exitMenuButtonObject);
    }

    @Override
    public void render(long vg) {
        //RenderUtils.drawGlassRect(game.vg, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, new Color(255, 255, 255, 24), 14f);

        RenderUtils.drawRect(game.vg,0,0, WINDOW_WIDTH, WINDOW_HEIGHT, new Color(0,0,0,120));
        game.font.drawText("Pause Menu", NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE, 40f, height / 2f - 200f, 50, Color.WHITE);
        FontManager.geometos.drawText(OrangeEngine.engineName.split(" ")[0], NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM, width - 19f, height - 30f, 35, new Color(255,255,255));
        FontManager.geometos.drawText(OrangeEngine.engineName.split(" ")[1], NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM, width - 20f, height - 10f, 25, new Color(255,255,255));

        menuButtonObjects.forEach(e -> e.renderButton(game.vg, game.font, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE));
        menuButtonObjects.get(0).updatePos(40f ,height / 2f - 50f, 200, 80);
        menuButtonObjects.get(1).updatePos(40f ,height / 2f + 10f, 200, 80);
        menuButtonObjects.get(2).updatePos(40f ,height / 2f + 60f, 200, 80);
    }

    @Override
    public void updateTick(float deltaTime) {

    }

    public void onKeyPressed(int key, int action) {
        if (game.isGamePaused) {
            resumeMenuButtonObject.onKeyPressed(key, action, menuButtonObjects);
            exitMenuButtonObject.onKeyPressed(key, action, menuButtonObjects);
            mainMenuMenuButtonObject.onKeyPressed(key, action, menuButtonObjects);
        }
    }

    private void resumeGame() {
        game.isGamePaused = false;
    }
}
