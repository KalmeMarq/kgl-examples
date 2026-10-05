package io.github.kalmemarq.kgl.util.image;

import io.github.kalmemarq.kgl.util.IoUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spng.SPNG;
import org.lwjgl.util.spng.spng_ihdr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpngImage extends Image {
    protected SpngImage(long pointer, int width, int height) {
        super(pointer, width, height);
    }

    public static SpngImage load(Path path) throws IOException {
        return load(Files.newInputStream(path));
    }

    public static SpngImage load(InputStream inputStream) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = IoUtils.mapInputStreamToBuffer(inputStream, -1);

            long context = SPNG.spng_ctx_new(0);
            SPNG.spng_set_image_limits(context, 4096, 4096);
            SPNG.spng_set_chunk_limits(context, 1024L * 1024L, 1024L * 1024L);

            SPNG.spng_set_png_buffer(context, buffer);

            spng_ihdr header = spng_ihdr.calloc(stack);
            int error = SPNG.spng_get_ihdr(context, header);
            if (error == SPNG.SPNG_EUSER_WIDTH || error == SPNG.SPNG_EUSER_HEIGHT) {
                throw new IOException("Failed to load image from memory");
            }

            int width = header.width();
            int height = header.height();
            PointerBuffer bufferSizePointer = stack.callocPointer(1);
            SPNG.spng_decoded_image_size(context, SPNG.SPNG_FMT_RGBA8, bufferSizePointer);

            int size = Math.toIntExact(bufferSizePointer.get(0));
            ByteBuffer pixels = MemoryUtil.memAlloc(size);
            SPNG.spng_decode_image(context, pixels, SPNG.SPNG_FMT_RGBA8, 0);

            SPNG.spng_ctx_free(context);

            return new SpngImage(MemoryUtil.memAddress(pixels), width, height);
        }
    }
}
