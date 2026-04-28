package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.resource.ResourceLocation;
import cms.mortalchen.orange.utils.render.RenderUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP;

public class BlurBloomBackgroundTestState extends GameState {
    private final ResourceLocation background = new ResourceLocation("/textures/image/bg13.jpg");

    public BlurBloomBackgroundTestState(MainGame game) {
        super(game);
    }

    @Override
    public void render(long vg) {
        ensureBackgroundLoaded();
        RenderUtils.drawImageCover(vg, background, 0f, 0f, width, height, 255, Color.WHITE);
        RenderUtils.drawGradientUpDown(vg, 0f, 0f, width, height, new Color(0,0,0, 180), new Color(0,0,0,0));

        //RenderUtils.startBloom(vg);
        float t = (System.currentTimeMillis() % 100000L) * 0.001f;
        float pulse = (float) ((Math.sin(t * 1.8f) + 1.0f) * 0.5f);
        float radius = 30f + pulse * 16f;
        RenderUtils.drawCircle(vg, width / 2f, height * 0.82f, radius, new Color(90, 210, 255));
        RenderUtils.drawRoundedRect(vg, width * 0.70f, height * 0.75f, width * 0.86f, height * 0.84f, 20f, new Color(120, 235, 255));

        //RenderUtils.stopBloom(vg);

        float panelW = width * 0.32f;
        float panelH = height * 0.18f;
        float gap = width * 0.025f;
        float x = (width - (panelW * 2f + gap)) * 0.5f;
        float y = height * 0.16f;

        RenderUtils.drawGlassRect(vg, x, y, x + panelW, y + panelH, new Color(255, 255, 255, 15), 8f);
        RenderUtils.drawGlassRoundedRect(vg, x + panelW + gap, y, x + panelW * 2f + gap, y + panelH, 24f, new Color(245, 250, 255, 15), 14f);
        RenderUtils.drawGlassRoundedRect(vg, width * 0.28f, height * 0.56f, width * 0.72f, height * 0.80f, 24f, new Color(235, 245, 255, 15), 20f);

        //RenderUtils.startBloom(vg);
        game.font.drawText("Bloom/Glow 文字", NVG_ALIGN_CENTER | NVG_ALIGN_TOP, width * 0.5f, height * 0.6f, 30, Color.WHITE);
        //RenderUtils.stopBloom(vg);

        game.font.drawText("Blur/Bloom Scope TestState (test sources)", NVG_ALIGN_CENTER | NVG_ALIGN_TOP, width * 0.5f, height * 0.05f, 30, Color.WHITE);
        game.font.drawText("Background uses cover fit (no stretch)", NVG_ALIGN_CENTER | NVG_ALIGN_TOP, width * 0.5f, height * 0.10f, 18, new Color(230, 240, 255));
        game.font.drawText("F10 to return to DefaultState", NVG_ALIGN_CENTER | NVG_ALIGN_TOP, width * 0.5f, height * 0.14f, 18, new Color(230, 240, 255));
    }

    @Override
    public void onKeyPressed(int key, int action) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_F10)) {
            game.changeState(new DefaultState(game));
        }
        super.onKeyPressed(key, action);
    }

    private void ensureBackgroundLoaded() {
        if (background.getId() != 0) {
            return;
        }
        background.setId(OrangeEngine.resourceManager.loadImage(background.getPath(), 0));
    }
}
