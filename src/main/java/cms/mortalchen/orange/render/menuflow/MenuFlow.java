package cms.mortalchen.orange.render.menuflow;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

public class MenuFlow {
    public ArrayList<MenuFlowObj> flowItems = new ArrayList<>();

    public void add(MenuFlowObj... flowObjs) {
        Collections.addAll(flowItems, flowObjs);
    }

    public void render(long vg, float x, float y ,float gap ,int index) {
        for (int i = 0; i < flowItems.size(); i++) {
            flowItems.get(i).update(index);
            flowItems.get(i).render(vg, x, y + (i * gap), index == flowItems.get(i).index ? new Color(255, 255, 255) : new Color(46, 46, 46));
        }
    }
    public void onTrigger(int index){
        flowItems.stream().filter(menuFlowObj -> menuFlowObj.index == index).findFirst().ifPresent(menuFlowObj -> menuFlowObj.trigger(index));
    }
}
