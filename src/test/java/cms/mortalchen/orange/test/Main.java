package cms.mortalchen.orange.test;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.states.BlurBloomBackgroundTestState;
import cms.mortalchen.orange.states.OraLightCEFTestState;

public class Main {
    public static void main(String[] args) {
        //MainGame.introEnable = false;
        MainGame.startMainGame = () -> {
            if (MainGame.instance != null) {
                MainGame.instance.changeState(new OraLightCEFTestState(MainGame.instance));
            }
        };

        MainGame mainGame = new MainGame();
        mainGame.startGame();
    }
}
