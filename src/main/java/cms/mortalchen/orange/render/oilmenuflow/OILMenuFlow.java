package cms.mortalchen.orange.render.oilmenuflow;

import cms.mortalchen.orange.MainGame;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class OILMenuFlow {
    public ArrayList<OILMenuFlowObj> menuFlowList = new ArrayList<>();

    public void render(long vg, float x, float y, int index) {
        for (int i = 0; i < menuFlowList.size(); i++) {
            menuFlowList.get(i).render(vg, x, y + (i * (MainGame.WINDOW_HEIGHT / 6.5f)), i == index ? new Color(64, 64, 64) : new Color(168, 168, 168));
            menuFlowList.get(i).update(index);
        }
    }

    public void add(OILMenuFlowObj... menuFlowObjs) {
        Collections.addAll(menuFlowList, menuFlowObjs);
    }
}
