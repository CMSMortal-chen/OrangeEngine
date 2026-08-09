package cms.mortalchen.orange.render;

/**
 * 帧率统计：以固定采样窗口聚合帧数，窗口期满计算平均 FPS。
 * <p>
 * 防御：非正帧间隔直接忽略，避免除零/NaN。
 */
public class FPS {
    private final float sampleWindowSeconds;
    private int frameCount;
    private float timeAccumulator;
    private float framesPerSecond;

    public FPS(float sampleWindowSeconds) {
        this.sampleWindowSeconds = sampleWindowSeconds;
    }

    public void update(float deltaTime) {
        if (deltaTime <= 0f) {
            return;
        }
        frameCount++;
        timeAccumulator += deltaTime;
        if (timeAccumulator >= sampleWindowSeconds) {
            framesPerSecond = frameCount / timeAccumulator;
            frameCount = 0;
            timeAccumulator = 0f;
        }
    }

    public float getFPS() {
        return framesPerSecond;
    }
}
