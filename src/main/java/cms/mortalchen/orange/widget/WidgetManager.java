package cms.mortalchen.orange.widget;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class WidgetManager {
    public CopyOnWriteArrayList<Widget> widgets = new CopyOnWriteArrayList<>();

    public void addWidgets(Widget... widgets) {
        Collections.addAll(this.widgets, widgets);
    }
}
