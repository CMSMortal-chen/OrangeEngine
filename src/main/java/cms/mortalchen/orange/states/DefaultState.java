package cms.mortalchen.orange.states;

import cms.mortalchen.orange.MainGame;
import cms.mortalchen.orange.OrangeEngine;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.anim.Motion;
import cms.mortalchen.orange.utils.render.anim.animatednumber.AnimatedFloat;
import cms.mortalchen.orange.utils.render.color.Palette;
import cms.mortalchen.orange.utils.time.TimerUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class DefaultState extends GameState {
    TimerUtil timer = new TimerUtil();
    boolean isStartedUp = false;
    public AnimatedFloat introAnim = new AnimatedFloat(0f);
    public AnimatedFloat introAnim2 = new AnimatedFloat(0f);
    public AnimatedFloat introAnim3 = new AnimatedFloat(0f);
    public AnimatedFloat introAnim4 = new AnimatedFloat(0f);
    public AnimatedFloat introAnim5 = new AnimatedFloat(100f);

    public DefaultState(MainGame game) {
        super(game);
        allowPause = false;
    }

    @Override
    public void render(long vg) {
        nvgBeginFrame(vg,width,height,1.0f);
        nvgTranslate(vg,width * 0.5f, height * 0.5f);
        nvgScale(vg, introAnim.get() * 0.01f, introAnim.get() * 0.01f);
        nvgTranslate(vg,-width * 0.5f, -height * 0.5f);
        nvgShapeAntiAlias(vg, true);
        if (timer.hasReached(800)) {
            introAnim.to(100f, 350, Motion.Easing.ELASTIC_OUT);
        }

        if (timer.hasReached(830)){
            introAnim2.to(100f, 350, Motion.Easing.ELASTIC_OUT);
        }

        FontManager.geometos.drawText(OrangeEngine.engineName.split(" ")[0], NVG_ALIGN_CENTER | NVG_ALIGN_BOTTOM, (width / 2f) - introAnim3.get(), height / 1.95f, 50, Palette.reAlpha(Color.WHITE, (introAnim.get() * 0.01f) * (introAnim5.get() * 0.01f)));
        nvgEndFrame(vg);
        nvgBeginFrame(vg,width,height,1.0f);
        nvgTranslate(vg,width * 0.5f, height * 0.5f);
        nvgScale(vg, introAnim2.get() * 0.01f, introAnim2.get() * 0.01f);
        nvgTranslate(vg,-width * 0.5f, -height * 0.5f);
        nvgShapeAntiAlias(vg, true);
        FontManager.geometos.drawText(OrangeEngine.engineName.split(" ")[1], NVG_ALIGN_CENTER | NVG_ALIGN_BOTTOM, (width / 2f)  - introAnim3.get(), height / 1.85f, 25, Palette.reAlpha(Color.WHITE, (introAnim2.get() * 0.01f) * (introAnim5.get() * 0.01f)));
        nvgEndFrame(vg);
    }

    @Override
    public void updateTick(float deltaTime) {
        if (timer.hasReached(3200) && !isStartedUp) {
            MainGame.startMainGame.run();
            isStartedUp = true;
        }
        if (timer.hasReached(2500)) {
            introAnim3.to(width / 3f, 400, Motion.Easing.EXPO_OUT);
            introAnim4.to(100, 400, Motion.Easing.EXPO_OUT);
            introAnim5.to(0f,350, Motion.Easing.EXPO_OUT);
        }
    }

    @Override
    public void onKeyPressed(int key, int action) {
        if (OrangeEngine.instance.inputManager.isKeyPressed(GLFW.GLFW_KEY_G)){
            game.changeState(new DefaultState(game));
        }
        super.onKeyPressed(key, action);
    }
}
