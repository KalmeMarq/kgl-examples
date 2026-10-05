package io.github.kalmemarq.kgl.util.image;

import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Image implements AutoCloseable {
    protected long pointer;
    protected int width;
    protected int height;

    protected Image(long pointer, int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        this.pointer = pointer;
        this.width = width;
        this.height = height;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public Image(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        this.width = width;
        this.height = height;
        this.pointer = MemoryUtil.nmemCalloc(1L, this.width * this.height * 4L);
    }

    public long pointer() {
        return this.pointer;
    }

    public ByteBuffer asByteBuffer() {
        return MemoryUtil.memByteBuffer(this.pointer, this.width * this.height * 4);
    }

    public void setColor(int x, int y, int color) {
        if (x < 0 || x >= this.width) {
            throw new IllegalArgumentException("x < 0 || x >= width");
        }

        if (y < 0 || y >= this.height) {
            throw new IllegalArgumentException("y < 0 || y >= height");
        }

        color = color & 0xFF_000000 | (color >> 16 & 0xFF) | (color >> 8 & 0xFF) << 8 | (color & 0xFF) << 16;
        MemoryUtil.memPutInt(this.pointer + ((long) (y) * this.width + x) * 4L, color);
    }

    public void fillRect(int x, int y, int width, int height, ColorSupplier colorSupplier) {
        if (x < 0 || x >= this.width) {
            throw new IllegalArgumentException("x < 0 || x >= width");
        }

        if (y < 0 || y >= this.height) {
            throw new IllegalArgumentException("y < 0 || y >= height");
        }

        if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("height <= 0");
        }

        for (int yy = 0; yy < height; yy++) {
            int yyy = yy + y;
            if (yyy >= this.height) continue;

            for (int xx = 0; xx < width; xx++) {
                int xxx = xx + x;
                if (xxx >= this.width) continue;

                int color = colorSupplier.get(xx, yy, width, height);
                color = color & 0xFF_000000 | (color >> 16 & 0xFF) | (color >> 8 & 0xFF) << 8 | (color & 0xFF) << 16;
                MemoryUtil.memPutInt(this.pointer + ((long) (yyy) * this.width + xxx) * 4L, color);
            }
        }
    }

    public void copyRect(Image source, int srcX, int srcY, int dstX, int dstY, int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("height <= 0");
        }

        for (int yy = 0; yy < height; yy++) {
            int yyy = yy + dstY;
            int syyy = yy + srcY;
            if (yyy >= this.height) continue;
            if (syyy >= source.height) continue;

            for (int xx = 0; xx < width; xx++) {
                int xxx = xx + dstX;
                int sxxx = xx + srcX;
                if (xxx >= this.width) continue;
                if (sxxx >= source.width) continue;

                int color = MemoryUtil.memGetInt(source.pointer + ((long) (syyy) * source.width + sxxx) * 4L);
                MemoryUtil.memPutInt(this.pointer + ((long) (yyy) * this.width + xxx) * 4L, color);
            }
        }
    }

    public int getColor(int x, int y) {
        int color = MemoryUtil.memGetInt(this.pointer + ((long) (y) * this.width + x) * 4L);
        int r = (color & 0xFF);
        int g = (color >> 8 & 0xFF);
        int b = (color >> 16 & 0xFF);
        int a = (color >> 24 & 0xFF);

        color = color & 0xFF_000000 | (color >> 16 & 0xFF) | (color >> 8 & 0xFF) << 8 | (color & 0xFF) << 16;
        return color;
    }

    public Image cropRect(int x, int y, int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        if (height <= 0) {
            throw new IllegalArgumentException("height <= 0");
        }

        if (x < 0 || x + width > this.width) {
            throw new IllegalArgumentException("x < 0 || x >= width");
        }

        if (y < 0 || y + height > this.height) {
            throw new IllegalArgumentException("y < 0 || y >= height");
        }

        Image image = new Image(width, height);
        image.copyRect(this, x, y, 0, 0, width, height);
        return image;
    }

    public void saveTo(Path path) throws IOException {
        try (WritableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            STBImageWrite.stbi_write_png_to_func((context, data, size) -> {
                ByteBuffer dataBuffer = MemoryUtil.memByteBuffer(data, size);
                try {
                    channel.write(dataBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0L, this.width, this.height, 4, MemoryUtil.memByteBuffer(this.pointer, this.width * this.height * 4), this.width * 4);
        }
    }

    @Override
    public void close() {
        MemoryUtil.nmemFree(this.pointer);
    }

    public interface ColorSupplier {
        int get(int x, int y, int w, int h);
    }
}
