package io.github.kalmemarq.kgl.util.image;

import io.github.kalmemarq.kgl.util.IoUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class StbImage extends Image {
    protected StbImage(long pointer, int width, int height) {
        super(pointer, width, height);
    }

    public static StbImage load(Path path) throws IOException {
        return load(path, false);
    }

    public static StbImage load(Path path, boolean flipY) throws IOException {
        return load(Files.newInputStream(path), flipY);
    }

    public static StbImage load(InputStream inputStream) throws IOException {
        return load(inputStream, false);
    }

    public static StbImage load(InputStream inputStream, boolean flipY) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = IoUtils.mapInputStreamToBuffer(inputStream, -1);

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(flipY);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);
            STBImage.stbi_set_flip_vertically_on_load(false);
            if (pixels == null) {
                throw new IOException("Failed to load image from memory");
            }

            MemoryUtil.memFree(buffer);
            inputStream.close();

            return new StbImage(MemoryUtil.memAddress(pixels), width.get(0), height.get(0));
        }
    }

    @Override
    public void close() {
        STBImage.nstbi_image_free(this.pointer);
    }
}
