package cms.mortalchen.orange;

import cms.mortalchen.orange.audio.AudioManager;
import cms.mortalchen.orange.resource.ResourceManager;
import cms.mortalchen.orange.states.GameState;
import cms.mortalchen.orange.states.DefaultState;
import cms.mortalchen.orange.states.PauseState;
import cms.mortalchen.orange.utils.SyncTimer;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.input.InputManager;
import cms.mortalchen.orange.render.FPS;
import cms.mortalchen.orange.utils.Utils;
import cms.mortalchen.orange.utils.render.RenderUtils;
import cms.mortalchen.orange.utils.render.postprocess.PostProcessPipeline;
import cms.mortalchen.orange.oralightcef.OraLightCEFRuntime;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;

import javax.swing.*;
import java.awt.*;

import static cms.mortalchen.orange.utils.Utils.loadImage;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class MainGame {
    public static String name = "OrangeEngine";
    public final Logger logger = LogManager.getLogger(name + "-Game");
    public static int WINDOW_WIDTH = 1280;
    public static int WINDOW_HEIGHT = 720;
    public static String WINDOW_TITLE = name;
    public GameState currentState;
    public OrangeEngine engine;
    public boolean isGamePaused = false;
    public long vg;
    public Font font;
    public long window;
    private boolean showFPS = false;
    public static float deltaTime;
    public static MainGame instance;
    public static boolean isPauseEnable = true;
    public static boolean introEnable = true;
    public static boolean fastIntro = false;
    public PauseState pauseState = new PauseState(this);
    public boolean exiting = false;
    public PostProcessPipeline postProcessPipeline;

    public boolean bloomEnabled = true;
    public boolean glassBlurEnabled = true;
    public float bloomIntensity = 0.9f;
    public int bloomPasses = 10;

    public static Runnable startMainGame = () -> {};

    public Thread gameUpdateThread = new Thread(() -> {
        logger.info("GameLogic Thread started.");
        SyncTimer logicTimer = new SyncTimer(SyncTimer.JAVA_NANO);

        while (!exiting) {
            try {
                if (!isGamePaused) {
                    currentState.updateTick(deltaTime);
                }
            }catch (Exception e){
                logger.fatal("A Exception at GameLogic Thread!");
                Utils.panic(logger,e);
            }
            logicTimer.sync(30);
        }

        logger.info("GameLogic Thread ended.");
    },"GameLogicThread");

    public void startGame() {
        instance = this;
        initGame();
        double lastTime = glfwGetTime();
        currentState = new DefaultState(this);

        gameUpdateThread.start();

        while (!glfwWindowShouldClose(window)) {
            try {
                double currentTime = glfwGetTime();

                deltaTime = (float) (currentTime - lastTime);

                try {
                    renderGame();
                }catch (Exception e) {
                    logger.error("Render ERROR!", e);
                }
                updateGame(deltaTime);
                glfwSwapBuffers(window);
                glfwPollEvents();

                lastTime = currentTime;
                engine.fpsCounter.update(deltaTime);

                OrangeEngine.deltaTimeMills = System.currentTimeMillis() - OrangeEngine.lastDeltaTimeMills;
                OrangeEngine.lastDeltaTimeMills = System.currentTimeMillis();

                engine.syncTimer.sync(180);
            } catch (Exception e) {
                Utils.panic(logger, e);
            }
        }
        endGame();
    }

    private void initGame() {
        try {
            // Initialize JCEF/AWT BEFORE GLFW so AWT gets a valid Win32 GraphicsDevice
            OraLightCEFRuntime.initialize();

            GLFWErrorCallback.createPrint(System.err).set();
            if (!glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }
            createWindow();
            setupCallbacks();

            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            initNanoVG();

            glfwShowWindow(window);

            initialize();

            setWindowIcon();
            logger.info("Running on OpenGL:{}", glGetString(GL_VERSION));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,"引擎启动失败！请向开发者提供您的游戏日志！","Error",JOptionPane.ERROR_MESSAGE);
            Utils.panic(logger,e);
        }
    }

    private void createWindow() {
        logger.info("Creating window...");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        try(GLFWWindowSizeCallback windowSize = glfwSetWindowSizeCallback(window, (windowHandle, width, height) -> glViewport(0, 0, width, height))) {
            logger.info("Window created.");
        }catch (Exception e){
            logger.error("Err:", e);
        }
    }

    private void initNanoVG() {
        logger.info("Initializing NanoVG...");
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new RuntimeException("Failed to create NanoVG context");
        }
        logger.info("NanoVG initialized");
    }

    private void setWindowIcon() {
        logger.info("Setting window icon...");
        GLFWImage.Buffer iconBuffer = loadImage("textures/image/icon.png");
        glfwSetWindowIcon(window, iconBuffer);
        logger.info("Window icon set");
    }

    private void renderGame() {
        prepareRendering();
        RenderUtils.setNanoVGFrameSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        RenderUtils.setGlassBlurEnabled(glassBlurEnabled);
        postProcessPipeline.beginScene(WINDOW_WIDTH, WINDOW_HEIGHT);
        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1.0f);
        RenderUtils.drawRect(vg,-10,-10,WINDOW_WIDTH + 10, WINDOW_HEIGHT + 10, Color.BLACK);
        nvgEndFrame(vg);

        OraLightCEFRuntime.doMessageLoopWork();

        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1.0f);
        currentState.render(vg);
        nvgEndFrame(vg);

        boolean scopedBloomRequested = RenderUtils.consumeBloomScopeRequestedThisFrame();
        boolean shouldRenderBloomSource = RenderUtils.shouldRenderBloomSource(
                bloomEnabled,
                currentState.useDedicatedBloomPass(),
                scopedBloomRequested
        );

        if (shouldRenderBloomSource) {
            postProcessPipeline.beginBloomSource();
            nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1.0f);
            if (currentState.useDedicatedBloomPass()) {
                currentState.renderBloom(vg);
            } else {
                RenderUtils.beginBloomCapturePass(vg);
                currentState.render(vg);
                RenderUtils.endBloomCapturePass(vg);
            }
            nvgEndFrame(vg);
        }

        postProcessPipeline.setBloomEnabled(bloomEnabled);
        postProcessPipeline.setBloomParams(bloomIntensity, bloomPasses);
        postProcessPipeline.compositeToScreen();

        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1.0f);

        if (isGamePaused) {
            pauseState.render(vg);
        }

        if (showFPS) {
            String fps = String.format("FPS: %.0f", engine.fpsCounter.getFPS());
            font.drawTextWithShadow(fps, NVG_ALIGN_BASELINE, 15, WINDOW_HEIGHT - 20, 22, new Color(255,255,255));
        }

        nvgEndFrame(vg);
        finishRendering();
    }

    private void prepareRendering() {
        glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void finishRendering() {
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void updateGame(float deltaTime) {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);
        WINDOW_WIDTH = width[0];
        WINDOW_HEIGHT = height[0];

        if (isGamePaused) {
            pauseState.updateDisplaySize(WINDOW_WIDTH, WINDOW_HEIGHT);
            pauseState.update(deltaTime);
            pauseState.updateTick(deltaTime);
        }else {
            currentState.update(deltaTime);
        }
        currentState.updateDisplaySize(WINDOW_WIDTH, WINDOW_HEIGHT);

        OrangeEngine.instance.inputManager.onUpdate();
    }

    private void setupCallbacks() {
        logger.info("Setting up callbacks...");

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (isGamePaused) {
                pauseState.onMouseClick(button, action, OrangeEngine.instance.inputManager.getMousePos());
            }else  {
                currentState.onMouseClick(button, action,OrangeEngine.instance.inputManager.getMousePos());
            }
        });

        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (isGamePaused) {
                pauseState.onMouseMove(xpos, ypos);
            } else {
                currentState.onMouseMove(xpos, ypos);
            }
        });

        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            if (isGamePaused) {
                pauseState.onMouseScroll(xoffset, yoffset);
            } else {
                currentState.onMouseScroll(xoffset, yoffset);
            }
        });
        
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (engine.inputManager.isKeyPressed(GLFW_KEY_ESCAPE)) {
                if (!(currentState instanceof PauseState) && currentState.allowPause && isPauseEnable) {
                    isGamePaused = !isGamePaused;
                }
            }

            if (isGamePaused) {
                pauseState.onKeyPressed(key, scancode, action, mods);
            }else {
                currentState.onKeyPressed(key, scancode, action, mods);
            }

            if (engine.inputManager.isKeyPressed(GLFW_KEY_L))
                showFPS = !showFPS;

        });

        glfwSetCharCallback(window, (window, codepoint) -> {
            if (isGamePaused) {
                pauseState.onChar(codepoint);
            } else {
                currentState.onChar(codepoint);
            }
        });
        logger.info("Callbacks set");
    }

    @SneakyThrows(InterruptedException.class)
    public void endGame() {
        logger.info("Ending game...");
        exiting = true;
        while (gameUpdateThread.isAlive()){
            Thread.sleep(100);
        }
        cleanup();
        logger.info("Game ended");
        System.exit(0);
    }

    private void cleanup() {
        logger.info("Cleaning up...");
        glfwFreeCallbacks(window);
        if (postProcessPipeline != null) {
            postProcessPipeline.cleanup(vg);
        }
        OraLightCEFRuntime.cleanup();
        nvgDelete(vg);
        glfwDestroyWindow(window);
        glfwTerminate();
        logger.info("Cleaned up");
    }

    public void initialize() {
        logger.info("Initializing game...");
        engine = new OrangeEngine();

        OrangeEngine.resourceManager = new ResourceManager(vg);
        OrangeEngine.fontManager = new FontManager(vg);
        OrangeEngine.audioManager = AudioManager.getInstance();
        font = FontManager.hmSans_CHS;
        engine.inputManager = new InputManager(window);
        engine.fpsCounter = new FPS(0.35f);
        postProcessPipeline = new PostProcessPipeline();
        postProcessPipeline.initialize();
        RenderUtils.setPostProcessPipeline(postProcessPipeline);
        logger.info("Game initialized");
    }

    public void changeState(GameState nextState) {
        if (isGamePaused) {
            isGamePaused = false;
        }
        currentState = nextState;
    }
}
