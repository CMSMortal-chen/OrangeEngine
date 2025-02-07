package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;

public abstract class GameState {
    public final MainGame game;
    public float width, height;

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
     * 游戏更新
     * @param deltaTime deltatime
     */
    public void update(float deltaTime) {

    }

    public void onMouseClick(int mouseButton){

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
