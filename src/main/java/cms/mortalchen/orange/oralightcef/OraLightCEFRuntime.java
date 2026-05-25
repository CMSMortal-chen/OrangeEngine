package cms.mortalchen.orange.oralightcef;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipInputStream;

public final class OraLightCEFRuntime {
    private static final Logger LOGGER = LogManager.getLogger(OraLightCEFRuntime.class);
    private static CefApp cefApp;
    private static boolean initialized;

    private OraLightCEFRuntime() {}

    public static class AwtExceptionHandler {
        public void handle(Throwable thrown) {
            System.err.println("[AWT-EventQueue] " + thrown);
            thrown.printStackTrace(System.err);
        }
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        System.setProperty("sun.awt.exception.handler",
                AwtExceptionHandler.class.getName());
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("[Uncaught] " + t.getName() + ": " + e);
            e.printStackTrace(System.err);
        });

        try {
            if (System.getProperty("jcef.path") == null) {
                resolveNativeLibs();
            }
            CefApp.startup(new String[0]);

            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = true;
            settings.cache_path =
                    new File(System.getProperty("java.io.tmpdir"), "orange-jcef-cache")
                            .getAbsolutePath();
            settings.log_severity =
                    CefSettings.LogSeverity.LOGSEVERITY_WARNING;

            cefApp = CefApp.getInstance(settings);

            LOGGER.info("JCEF initialized, native path: {}",
                    System.getProperty("jcef.path"));
        } catch (Exception e) {
            LOGGER.error("Failed to initialize JCEF", e);
            throw new RuntimeException("JCEF initialization failed", e);
        }
    }

    private static void resolveNativeLibs() throws IOException {
        File jcefDir = new File("libs/jcef");

        // Already extracted?
        if (jcefDir.exists() && new File(jcefDir, "jcef_helper.exe").exists()) {
            System.setProperty("jcef.path", jcefDir.getAbsolutePath());
            return;
        }

        // Try classpath first (bundled in JAR)
        InputStream zipStream = OraLightCEFRuntime.class.getClassLoader()
                .getResourceAsStream("jcef-native.zip");
        if (zipStream != null) {
            LOGGER.info("Extracting native runtime from JAR to {} ...", jcefDir);
            extractZip(zipStream, jcefDir);
            System.setProperty("jcef.path", jcefDir.getAbsolutePath());
            return;
        }

        // Try local disk
        File diskZip = new File("libs/jcef-native.zip");
        if (diskZip.exists()) {
            LOGGER.info("Extracting native runtime from {} to {} ...", diskZip, jcefDir);
            try (InputStream fis = new BufferedInputStream(new FileInputStream(diskZip))) {
                extractZip(fis, jcefDir);
            }
            System.setProperty("jcef.path", jcefDir.getAbsolutePath());
            return;
        }

        // Nothing found — tell user
        jcefDir.mkdirs();
        String msg = "\n========================================\n"
                + "JCEF native runtime not found!\n"
                + "Please download jcef-native.zip and place it in:\n"
                + "  " + jcefDir.getAbsoluteFile().getParent() + "\n"
                + "File should be named: jcef-native.zip\n"
                + "========================================\n";
        LOGGER.error(msg);
        System.err.println(msg);
        throw new FileNotFoundException("jcef-native.zip not found in JAR or libs/");
    }

    private static void extractZip(InputStream in, File destDir) throws IOException {
        destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
            byte[] buf = new byte[8192];
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
        LOGGER.info("Native extraction complete");
    }

    static synchronized CefApp app() {
        if (cefApp == null) {
            initialize();
        }
        return cefApp;
    }

    public static synchronized void doMessageLoopWork() {
        if (cefApp != null) {
            cefApp.N_DoMessageLoopWork();
        }
    }

    public static synchronized void cleanup() {
        if (cefApp != null) {
            try {
                Thread t = new Thread(() -> {
                    try { cefApp.dispose(); } catch (Exception ignored) {}
                }, "JCEF-Dispose");
                t.setDaemon(true);
                t.start();
                t.join(2000);
            } catch (Exception e) {
                LOGGER.warn("JCEF dispose interrupted", e);
            }
            cefApp = null;
            initialized = false;
            LOGGER.info("JCEF cleaned up");
        }
    }
}
