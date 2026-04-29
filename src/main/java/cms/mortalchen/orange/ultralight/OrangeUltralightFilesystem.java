package cms.mortalchen.orange.ultralight;

import net.janrupf.ujr.api.UltralightResources;
import net.janrupf.ujr.api.filesystem.UltralightFilesystem;
import net.janrupf.ujr.api.util.NioUltralightBuffer;
import net.janrupf.ujr.api.util.UltralightBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

final class OrangeUltralightFilesystem implements UltralightFilesystem {
    static final String RESOURCE_PREFIX = "orange://resources/";

    @Override
    public boolean fileExists(String path) {
        if (path == null || !path.startsWith(RESOURCE_PREFIX)) {
            return false;
        }

        String resource = path.substring(RESOURCE_PREFIX.length());
        if (classpathResourceExists(resource)) {
            return true;
        }
        return UltralightResources.getResource(resource) != null;
    }

    @Override
    public String getFileMimeType(String path) {
        String mime = URLConnection.guessContentTypeFromName(path);
        return mime == null ? "text/plain" : mime;
    }

    @Override
    public String getFileCharset(String path) {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public UltralightBuffer openFile(String path) throws IOException {
        if (path == null || !path.startsWith(RESOURCE_PREFIX)) {
            return null;
        }

        String resource = path.substring(RESOURCE_PREFIX.length());
        InputStream stream = classpathResourceStream(resource);
        if (stream != null) {
            return readFromStream(stream);
        }

        URI uri = UltralightResources.getResource(resource);
        if (uri != null) {
            URLConnection connection = uri.toURL().openConnection();
            return readFromConnection(connection);
        }

        throw new IOException("Resource " + resource + " does not exist");
    }

    private boolean classpathResourceExists(String resource) {
        InputStream stream = classpathResourceStream(resource);
        if (stream == null) {
            return false;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
        }
        return true;
    }

    private InputStream classpathResourceStream(String resource) {
        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        return OrangeUltralightFilesystem.class.getResourceAsStream(resource);
    }

    private UltralightBuffer readFromStream(InputStream in) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = input.read(chunk)) != -1) {
                out.write(chunk, 0, read);
            }

            byte[] bytes = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return new NioUltralightBuffer(buffer);
        }
    }

    private UltralightBuffer readFromConnection(URLConnection connection) throws IOException {
        int length = connection.getContentLength();
        if (length != -1) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(length);
            try (ReadableByteChannel channel = Channels.newChannel(connection.getInputStream())) {
                while (channel.read(buffer) > 0) { /* keep going */ }
            }
            return new NioUltralightBuffer((ByteBuffer) buffer.flip());
        }

        try (InputStream in = connection.getInputStream()) {
            return readFromStream(in);
        }
    }
}
