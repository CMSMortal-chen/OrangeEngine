package com.as.gateway;

import cms.mortalchen.orange.MainGame;
import com.as.gateway.states.MainMenuState;
import com.as.gateway.states.TestAnimState;
import com.as.gateway.states.olos.ABDUIState;
import com.as.gateway.states.olos.OILOpenOSState;

public class Main {
    public static void main(String[] args) {
        MainGame mainGame = new MainGame();
        MainGame.startMainGame = () -> {
            //mainGame.changeState(new MainMenuState(mainGame));
            //mainGame.changeState(new ABDUIState(mainGame));
            //mainGame.changeState(new OILOpenOSState(mainGame));
            mainGame.changeState(new TestAnimState(mainGame));
        };
        mainGame.startGame();
    }
}