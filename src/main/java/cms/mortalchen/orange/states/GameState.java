package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;

public abstract class GameState {
    public final MainGame game;
    public float width, height;
    public boolean allowPause = true;

    public GameState(MainGame game) {
        this.game = game;
        width = MainGame.WINDOW_WIDTH;
        height = MainGame.WINDOW_HEIGHT;
    }

    /**
     * 内容渲染
     * @param vg NanoVg实例
     */
    public abstract void render(long vg);

    /**
     * 游戏刻更新
     * 每秒钟更新30tick
     * @param deltaTime deltatime
     */
    public void updateTick(float deltaTime){

    }

    /**
     * 游戏更新
     * 每帧更新一次
     * @param deltaTime deltatime
     */
    public void update(float deltaTime){

    }


    public void onMouseClick(int mouseButton){

    }

    /**
     * 按键输入
     * @param key 按键ID
     * @param action 按键行为
     */
    public void onKeyPressed(int key, int action) {

    }

    /**
     * 更新窗口大小
     * @param width 宽度
     * @param height 高度
     */
    public void updateDisplaySize(float width, float height){
        this.width = width;
        this.height = height;
    }
}
