package com.as.gateway.states.rpg.gui.menu;

import org.joml.Vector2f;

public abstract class MenuWindow {

    public boolean exited = false;
    public boolean exiting = false;

    public float introX = 0f;

    /**
     * 主体渲染
     */
    public abstract void render(long vg);

    public abstract void onKeyPressed(int key, int action);
}
