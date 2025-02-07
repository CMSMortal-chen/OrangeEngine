package cms.mortalchen.orange;

import cms.mortalchen.orange.states.GameState;
import com.as.gateway.states.*;
import cms.mortalchen.orange.render.font.FontManager;
import cms.mortalchen.orange.render.font.Font;
import cms.mortalchen.orange.input.InputManager;
import cms.mortalchen.orange.render.FPS;
import cms.mortalchen.orange.utils.render.RenderUtils;
import com.as.gateway.states.olos.OILOpenOSState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;

import java.awt.*;

import static cms.mortalchen.orange.utils.Utils.loadImage;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class MainGame {
    public static final String name = "Project Gateway";
    public final Logger logger = LogManager.getLogger(name + "-Game");
    public static int WINDOW_WIDTH = 1280;
    public static int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = name;
    public GameState currentState;
    public OrangeEngine engine;
    //public PongServer server;
    public boolean isGamePaused = false;
    public long vg;
    public Font font;
    private long window;
    private boolean showFPS = true;
    public static float deltaTime;
    public static MainGame instance;
    public PauseState pauseState = new PauseState(this);

    public void startGame() {
        instance = this;
        initGame();
        double lastTime = glfwGetTime();
        currentState = new OILOpenOSState(this);

        while (!glfwWindowShouldClose(window)) {
            try {
                double currentTime = glfwGetTime();

                deltaTime = (float) (currentTime - lastTime);

                renderGame();
                updateGame(deltaTime);
                glfwSwapBuffers(window);
                glfwPollEvents();

                lastTime = currentTime;
                engine.fpsCounter.update(deltaTime);

                OrangeEngine.deltaTimeMills = System.currentTimeMillis() - OrangeEngine.lastDeltaTimeMills;
                OrangeEngine.lastDeltaTimeMills = System.currentTimeMillis();

                engine.syncTimer.sync(165);
//            glfwSwapInterval(1);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        endGame();
    }

    private void initGame() {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createWindow() {
        logger.info("Creating window...");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        //(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        glfwSetWindowSizeCallback(window, (windowHandle, width, height) -> {
            glViewport(0, 0, width, height);
        });
        logger.info("Window created");
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

        nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, 1.0f);

        RenderUtils.drawRect(vg,-10,-10,WINDOW_WIDTH + 10, WINDOW_HEIGHT + 10, Color.BLACK);

        currentState.render(vg);

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
        }else {
            currentState.update(deltaTime);
        }
        currentState.updateDisplaySize(WINDOW_WIDTH, WINDOW_HEIGHT);

        OrangeEngine.instance.inputManager.onUpdate();
    }

    private void setupCallbacks() {
        logger.info("Setting up callbacks...");
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
                currentState.onMouseClick(button);
        });

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (currentState instanceof MainMenuState)
                ((MainMenuState) currentState).onKeyPressed(key, action);

            if (engine.inputManager.isKeyPressed(GLFW_KEY_ESCAPE)) {
                if (!(currentState instanceof PauseState) && !(currentState instanceof MainMenuState)) {
                    isGamePaused = !isGamePaused;
                    //changeState(new PauseState(this));
                }
            }

            if (isGamePaused)
                pauseState.onKeyPressed(key, action);

            if (engine.inputManager.isKeyPressed(GLFW_KEY_L))
                showFPS = !showFPS;
        });
        logger.info("Callbacks set");
    }

    public void endGame() {
        logger.info("Ending game...");
        cleanup();
        logger.info("Game ended");
        System.exit(0);
    }

    private void cleanup() {
        logger.info("Cleaning up...");
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        nvgDelete(vg);
        glfwTerminate();
        logger.info("Cleaned up");
    }

    public void initialize() {
        logger.info("Initializing game...");
        engine = new OrangeEngine();
        //new Font(vg, "pixel")
        OrangeEngine.fontManager = new FontManager(vg);
        font = FontManager.hmSans_CHS;
        engine.inputManager = new InputManager(window);
        engine.fpsCounter = new FPS(0.35f);
        logger.info("Game initialized");
    }

    public void changeState(GameState nextState) {
        if (isGamePaused) {
            isGamePaused = false;
        }
        currentState = nextState;
    }
}
