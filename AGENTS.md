# OrangeEngine — Agent Guide

OrangeEngine is a **2D game engine** built on **LWJGL 3.4 + OpenGL 4.6 + NanoVG**, with integrated **JCEF** (Java Chromium Embedded Framework) for in-game web browser support. Written in Java 21+, built with Gradle using the Shadow plugin for fat JARs.

## Build & Run Commands

```bash
# Build (produces fat JAR via shadowJar — shadowJar is wired as build.dependsOn)
./gradlew build

# Just compile (no shadow)
./gradlew classes

# Run tests (JUnit 5 platform)
./gradlew test

# Run the main application (launches MainGame directly, no CLI args)
# The shadow JAR manifest points to cms.mortalchen.orange.Main
./gradlew shadowJar
java -jar build/libs/OrangeEngine-*.jar

# Windows-only — LWJGL natives are natives-windows
```

**Key build details:**
- `lwjglNatives = "natives-windows"` — this is Windows-only
- Shadow JAR excludes signature/license META-INF files
- JCEF native sources are in `libs/java-cef-src` and compiled as an extra source set (excludes `tests/**`)
- There's a `buildJavaCefJar` Gradle task that repackages `org.cef/**` classes into `libs/java-cef.jar`
- Logging: Log4j2 via `src/main/resources/log4j2.xml` — console + rolling file at `logs/OE_latest.log`
- All paths in resources are classpath-based (loaded via `getResourceAsStream`)

## Architecture & Lifecycle

### Entry Point
`cms.mortalchen.orange.Main` → redirects System.out/err to Log4j2 → creates `MainGame` → calls `startGame()`.

### Application Flow (MainGame.startGame())
1. `initGame()` — initializes in this order: **JCEF runtime**, GLFW, GLFW window (OpenGL 4.6 core), NanoVG context, engine singletons, post-process pipeline, window icon
2. Enters the **main render loop** (target ~180 FPS via `SyncTimer`):
   - `OraLightCEFRuntime.doMessageLoopWork()` — pumps CEF browser message loop
   - Scene → NanoVG frame → post-process composite → swap buffers
3. A separate **game logic thread** (`gameUpdateThread`) runs `currentState.updateTick()` at 30 TPS, decoupled from rendering

### Game States
All game logic lives in `GameState` subclasses (`src/main/java/cms/mortalchen/orange/states/`):
- **`GameState`** — abstract base; defines `render(vg)`, `update(deltaTime)`, `updateTick(deltaTime)`, input callbacks, bloom pass (`renderBloom` / `useDedicatedBloomPass`)
- **`DefaultState`** — the intro/splash screen with a Hulu-style animated border+text (uses `AnimatedFloat` + `Motion.Easing`)
- **`PauseState`** — pause menu with keyboard-navigable buttons (`MenuButtonObject`)
- `MainGame.changeState(GameState)` switches states; paused state is a boolean flag (`isGamePaused`), not a state switch

### Singletons & Globals
- `OrangeEngine.instance` — holds `inputManager`, `fontManager`, `resourceManager`, `fpsCounter`, `syncTimer`, `deltaTimeMills`
- `MainGame.instance` — holds the GLFW window, NanoVG context (`vg`), `postProcessPipeline`, `currentState`, `font`, `deltaTime`
- `OrangeEngine.fontManager` — static; fonts are loaded once and stored in static fields (`FontManager.hmSans_CHS`, `geometos`, etc.)
- `OrangeEngine.resourceManager` — static; image loading with caching by path

### Rendering Pipeline

**NanoVG** is the primary rendering API. All 2D drawing goes through:
- `RenderUtils` (static methods) — rectangles, rounded rects, circles, triangles, gradients, images, glass/blur effects, bloom scoping
- `Font.drawText(...)` — text rendering with NanoVG font face/styling

**Post-processing** (`PostProcessPipeline`):
1. `beginScene()` — binds scene FBO, clears
2. All NanoVG content is drawn to the scene FBO
3. Optional bloom source pass — `beginBloomSource()` binds a separate "bright" FBO
4. `compositeToScreen()` — Gaussian blur ping-pong pass → composite shader → default framebuffer
5. Glass blur rectangles use `getBlurredSceneImage()` which snapshots the current scene FBO, applies blur, and returns a NanoVG image handle for use as a fill pattern

**Bloom workflow in states:**
- Call `RenderUtils.startBloom(vg)` / `stopBloom(vg)` around elements that should glow
- Override `useDedicatedBloomPass()` returning true for a separate bloom render pass (useful for keeping backgrounds out of bloom)
- Without dedicated pass: `RenderUtils.beginBloomCapturePass(vg)` / `endBloomCapturePass(vg)` re-renders the entire scene with alpha toggling

**Important rendering gotcha:** Between NanoVG `nvgBeginFrame`/`nvgEndFrame` calls, you CANNOT directly bind other FBOs or draw with raw OpenGL. The post-process pipeline handles this by ending the NanoVG frame, doing raw GL work, then starting a new frame — see `RenderUtils.drawGlassRectInternal()` which calls `nvgEndFrame`/`nvgBeginFrame` internally.

### Animation System

Two-tier animation system in `utils/render/anim/`:

1. **`Motion.Controller`** — low-level time-based interpolator with 30+ easing curves (`LINEAR`, `SINE_IN/OUT/IN_OUT`, `QUAD_*`, `CUBIC_*`, `QUART_*`, `QUINT_*`, `EXPO_*`, `CIRC_*`, `BACK_*`, `ELASTIC_*`, `BOUNCE_*`). Smoothly transitions from current value to target when a new `.to()` is called mid-animation.

2. **`AnimatedFloat`** (and `AnimatedInt`, `AnimatedDouble`, `AnimatedLong`, `AnimatedColor`) — thin wrapper around `Motion.Controller` for typed convenience. Call `.to(target, durationMs, easing)` to animate, `.get()` to read current value every frame.

### CEF / Browser Integration (`oralightcef/`)

- **`OraLightCEFRuntime`** — initializes CEF (one-time), extracts native libs from `jcef-native.zip` (classpath or `libs/`), creates `CefApp`, sets up AWT exception handler
- **`OraLightCEF`** — off-screen browser instance; each is a `JcefBrowser` (extends `CefBrowser_N`) with handlers for load/lifecycle/display events
- **`JcefBrowser`** — custom CEF browser in `org.cef.browser` package (must live there to access package-private `CefBrowser_N`). Uses a hidden AWT `Frame` + `Canvas` for a valid native HWND. Renders via `onPaint` callback → CPU-side `ByteBuffer` → OpenGL texture → NanoVG image
- **`OraLightCEFIpc`** — JS-to-Java IPC bridge using `CefMessageRouter`. JS calls `window.orange.post(channel, payload)` or `window.orange.request(channel, payload)` (Promise-based). Java registers handlers via `orLightCEF.on(channel, backend)`. Each page load re-installs the bridge via `installJavaScriptBridge()`. Also installs `POPUP_BLOCK_JS` to handle `target="_blank"` links in OSR mode.
- **Browser rendering cycle:** `OraLightCEF.render()` → `browser.consumeFrame()` → uploads BGRA pixel buffer to GL texture → creates NanoVG image from GL handle → draws via `RenderUtils.drawImage()`

### Input System
- `InputManager` wraps GLFW key/mouse state; polled each frame via `onUpdate()`
- `KeyBinding` enum maps logical actions to GLFW key codes
- `InputUtils.isHovering(Vector4f, mouseX, mouseY)` for axis-aligned bounds checks

### Resource Management
- `ResourceManager` — loads images via classpath, caches NanoVG image handles by path
- `ResourceLocation` — path + NanoVG image ID pair; use `.getId()` for rendering, `.setId()` after loading
- Image pattern: load once via `ResourceManager.loadImage(path, flag)`, store in a `ResourceLocation`, draw with `RenderUtils.drawImage(vg, location, ...)` or `drawImageCover(vg, location, ...)`

## Code Conventions & Patterns

### Package Structure
```
cms.mortalchen.orange          — Core: Main, MainGame, OrangeEngine
cms.mortalchen.orange.api      — Interfaces (ILoggerImp) and exceptions
cms.mortalchen.orange.input    — InputManager, KeyBinding
cms.mortalchen.orange.oralightcef — CEF browser integration
cms.mortalchen.orange.render   — Rendering: FPS, Image, Font, MenuFlow, OILMenuFlow, fonts
cms.mortalchen.orange.resource — ResourceManager, ResourceLocation
cms.mortalchen.orange.states   — Game state base + implementations
cms.mortalchen.orange.utils    — Utilities: math, render (anim, color, postprocess), time, SyncTimer
cms.mortalchen.orange.widget   — UI widget system
org.cef.browser                — JcefBrowser (package-private access hack)
```

### Naming & Style
- **Lombok is used everywhere**: `@Getter`, `@Setter`, `@UtilityClass`, `@SneakyThrows`, `@NonNull`
- Logging: `LogManager.getLogger("Category-Name")` via Log4j2; some classes implement `ILoggerImp` (interface with static `logger` field bound to `MainGame.instance.logger` — this is **fragile**, requires `MainGame.instance` to exist)
- Public fields over getters: many classes use direct field access (`MainGame.WINDOW_WIDTH`, `OrangeEngine.fontManager`, `game.font`)
- Chinese comments/JavaDoc mixed with English code — `GameState` abstract methods, `KeyBinding` enum values are in English; older utility JavaDoc has English
- Error handling: `Utils.panic(logger, e)` logs at FATAL and calls `System.exit(-1)` — used for unrecoverable errors
- Colors: `java.awt.Color` throughout; converted to `NVGColor` at the render boundary
- Vectors: `org.joml.Vector2f`, `Vector4f` for positions/rectangles

### State Lifecycle
- States are created with `new XxxState(MainGame.instance)` — no dependency injection
- `MainGame.startMainGame` is a static `Runnable` set by test/application entry points to define what happens after the intro
- States receive input via overridden callbacks; the main loop multiplexes between `currentState` and `pauseState` based on `isGamePaused`

### Test Code
Tests are **not conventional unit tests** — they are `main()` entry points in `src/test/java/` that serve as interactive test harnesses:
- `test.Main` — starts the engine and loads `OraLightCEFTestState` (browser test)
- `OraLightCEFTestState` — shows a full-screen CEF browser with a glass-effect HTML demo
- `BlurBloomBackgroundTestState` — demonstrates bloom/glow + glass blur effects with a background image
- `BlurBloomBackgroundFlowTest` — actual JUnit 5 tests for `computeCoverRect` and bloom scope logic
- To run test states, either run the test `Main` class or use the engine's `startMainGame` hook

### Gotchas

1. **Init order matters**: JCEF must initialize **before** GLFW (in `initGame()`), because JCEF needs AWT to set up a valid graphics device before GLFW takes over the OpenGL context.

2. **`ILoggerImp` pattern is broken**: It declares `Logger logger = MainGame.instance.logger` as an interface field. If `MainGame.instance` is null when the interface is first loaded, it NPEs. Some classes work around this by not implementing the interface.

3. **Font fallback recursion**: `Font.loadFont()` has a recursive fallback — if a font fails to load, it retries with "HarmonyOS_Sans_SC_Regular" as the hardcoded default. This can cause infinite recursion if that font also fails.

4. **NanoVG frame boundary violations**: Any raw OpenGL operations (FBO binds, texture uploads) between `nvgBeginFrame`/`nvgEndFrame` corrupt NanoVG state. The codebase handles this by explicitly calling `nvgEndFrame` before raw GL work and `nvgBeginFrame` after — see `drawGlassRectInternal()` and `compositeToScreen()`.

5. **`ResourceLocation` is mutable**: Its `id` field is set after loading. Make sure to call `resourceManager.loadImage()` before using it in rendering, or the ID will be 0 (invalid).

6. **Main entry point is `void main()`**, not `static void main(String[])`. The test code uses `static void main(String[])` as expected. The shadow JAR manifest points to `cms.mortalchen.orange.Main`, but this class has no `static main(String[])` — the actual runnable entry is the test `Main` class. This may be intentional (test harness as primary mode) or an oversight.

7. **Platform lock**: LWJGL natives are hardcoded to `natives-windows`. The `libs/jcef-native.zip` likely contains Windows DLLs. This is a Windows-only project.

8. **`MenuButtonObject.selectedButtonIndex` is shared across instances**: The pause state creates 3 `MenuButtonObject` instances but only the first one's `selectedButtonIndex` is used for navigation; the index marks the selected item across all buttons.
