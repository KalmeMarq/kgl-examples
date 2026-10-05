package io.github.kalmemarq.kgl.util;

import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class IoUtils {
    public static final LazyConstant<ExecutorService> IO_EXECUTOR = LazyConstant.of(() ->
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "Io Thread");
                thread.setDaemon(true);
                return thread;
            }));

    public static final Path jarPath;

    public static ByteBuffer mapInputStreamToBuffer(InputStream inputStream, long streamSize) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        long size = channel instanceof SeekableByteChannel seekableChannel ? seekableChannel.size() : streamSize < 0 ? 8192 : streamSize;

        ByteBuffer buffer = MemoryUtil.memAlloc((int) size);

        try {
            while (channel.read(buffer) != -1) {
                if (!buffer.hasRemaining()) {
                    buffer = MemoryUtil.memRealloc(buffer, (int) (buffer.capacity() * 1.5));
                }
            }
        } catch (IOException e) {
            MemoryUtil.memFree(buffer);
            throw e;
        }

        buffer.flip();
        return buffer;
    }

    public static boolean createDirectories(Path path) {
        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException _) {
            return false;
        }
    }

    public static <T, E extends Exception> T unchecked(CheckedSupplier<T, E> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public interface CheckedSupplier<T, E extends Exception> {
        T get() throws E;
    }

    static {
        Path path = null;

        URL url = IoUtils.class.getResource("/.root");
        if (url != null) {
            try {
                URI uri = url.toURI();

                try {
                    path = Path.of(uri).getParent().toAbsolutePath();
                } catch (FileSystemNotFoundException _) {
                    FileSystems.newFileSystem(uri, Collections.emptyMap());
                    path = Path.of(uri).getParent().toAbsolutePath();
                }

            } catch (URISyntaxException | IOException _) {
            }
        }

        jarPath = path;
    }

    public static class LazyConstant<T> {
        private final Supplier<T> supplier;
        private T value;

        public LazyConstant(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public static <T> LazyConstant<T> of(Supplier<T> supplier) {
            return new LazyConstant<>(supplier);
        }

        public T get() {
            if (this.value == null) {
                this.value = this.supplier.get();
            }
            return this.value;
        }
    }
}
