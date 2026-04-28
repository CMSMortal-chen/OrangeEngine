package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import org.joml.Vector2f;

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

    public void renderBloom(long vg) {

    }

    public boolean useDedicatedBloomPass() {
        return false;
    }

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


    public void onMouseClick(int mouseButton, int action, Vector2f mousePos){

    }

    /**
     * 按键输入
     * @param key 按键ID
     * @param action 按键行为
     */
    public void onKeyPressed(int key, int action) {

    }

    /**
     * 字符输入
     * @param codepoint 字符的Unicode码点
     */
    public void onChar(int codepoint) {

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

    /**
     * 鼠标移动事件
     * @param x 鼠标X坐标
     * @param y 鼠标Y坐标
     */
    public void onMouseMove(double x, double y) {
        // 默认实现，可以在子类中重写
    }
    
    /**
     * 鼠标滚轮事件
     * @param xoffset 水平滚轮偏移
     * @param yoffset 垂直滚轮偏移
     */
    public void onMouseScroll(double xoffset, double yoffset) {
        // 默认实现，可以在子类中重写
    }
}
