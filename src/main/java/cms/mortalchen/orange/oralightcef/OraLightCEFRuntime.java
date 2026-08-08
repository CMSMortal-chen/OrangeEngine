package cms.mortalchen.orange.oralightcef;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
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
            // CEF 消息循环跑在独立线程：游戏主线程不再被 CEF 周期任务卡顿，
            // 但所有 CEF 触碰调用必须经 CefTaskRunner 封送到 CEF UI 线程
            settings.multi_threaded_message_loop = true;
            settings.locale = Locale.getDefault().toLanguageTag();
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

        // Try classpath first (bundled in JAR — single zip or chunks)
        ClassLoader cl = OraLightCEFRuntime.class.getClassLoader();
        InputStream zipStream = cl.getResourceAsStream("jcef-native.zip");
        if (zipStream == null) {
            // Try reassemble from classpath chunks
            zipStream = reassembleClasspathChunks(cl, "jcef-native.zip");
        }
        if (zipStream != null) {
            LOGGER.info("Extracting native runtime from JAR to {} ...", jcefDir);
            extractZip(zipStream, jcefDir);
            System.setProperty("jcef.path", jcefDir.getAbsolutePath());
            return;
        }

        // Try local disk (single zip)
        File diskZip = new File("libs/jcef-native.zip");
        if (diskZip.exists()) {
            LOGGER.info("Extracting native runtime from {} to {} ...", diskZip, jcefDir);
            try (InputStream fis = new BufferedInputStream(new FileInputStream(diskZip))) {
                extractZip(fis, jcefDir);
            }
            System.setProperty("jcef.path", jcefDir.getAbsolutePath());
            return;
        }

        // Try reassemble from disk chunks (jcef-native.zip.part001, .part002, ...)
        File chunksDir = new File("libs");
        File[] chunkFiles = chunksDir.listFiles((dir, name) -> name.startsWith("jcef-native.zip.part"));
        if (chunkFiles != null && chunkFiles.length > 0) {
            java.util.Arrays.sort(chunkFiles);
            LOGGER.info("Reassembling {} chunks into {} ...", chunkFiles.length, diskZip);
            try (FileOutputStream fos = new FileOutputStream(diskZip)) {
                for (File chunk : chunkFiles) {
                    try (FileInputStream fis = new FileInputStream(chunk)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
            }
            LOGGER.info("Reassembly complete ({} MB)", diskZip.length() / (1024 * 1024));
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
                + "Place jcef-native.zip (or .part chunks) in:\n"
                + "  " + jcefDir.getAbsoluteFile().getParent() + "\n"
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

    private static InputStream reassembleClasspathChunks(ClassLoader cl, String baseName) throws IOException {
        java.util.List<byte[]> chunks = new java.util.ArrayList<>();
        int partNum = 0;
        while (true) {
            partNum++;
            String partName = baseName + ".part" + String.format("%03d", partNum);
            InputStream partStream = cl.getResourceAsStream(partName);
            if (partStream == null) break;
            chunks.add(partStream.readAllBytes());
            partStream.close();
        }
        if (chunks.isEmpty()) return null;

        LOGGER.info("Reassembled {} classpath chunks for {}", chunks.size(), baseName);
        int totalSize = 0;
        for (byte[] c : chunks) totalSize += c.length;
        byte[] combined = new byte[totalSize];
        int offset = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, combined, offset, c.length);
            offset += c.length;
        }
        return new java.io.ByteArrayInputStream(combined);
    }

    static synchronized CefApp app() {
        if (cefApp == null) {
            initialize();
        }
        return cefApp;
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
