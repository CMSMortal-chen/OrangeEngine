package cms.mortalchen.orange.test;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.states.BlurBloomBackgroundTestState;

public class Main {
    public static void main(String[] args) {
        //MainGame.fastIntro = true;
        MainGame.startMainGame = () -> {
            if (MainGame.instance != null) {
                MainGame.instance.changeState(new BlurBloomBackgroundTestState(MainGame.instance));
            }
        };

        MainGame mainGame = new MainGame();
        mainGame.startGame();
    }
}
