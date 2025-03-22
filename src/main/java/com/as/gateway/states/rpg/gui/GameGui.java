package com.as.gateway.states.rpg.gui;

/**
 * 游戏内用户界面
 */
public abstract class GameGui {
    public boolean closed = false;
    public boolean closing = false;

    public float width, height = 0f;


    /**
     * 界面初始化
     */
    public abstract void init();

    /**
     * 界面渲染
     * @param vg NanoVG实例
     */
    public abstract void render(long vg);

    /**
     * 这个界面是否会导致游戏暂停
     * @return 是否暂停
     */
    public boolean doseGuiPauseGame(){
        return true;
    }

    public void onKeyPressed(int key, int action) {

    }
}
