package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.jspecify.annotations.Nullable;

public class RenderTarget implements AutoCloseable {
    private GpuTexture color;
    private @Nullable GpuTexture depth;
    private int width;
    private int height;
    private final GpuTextureFormat colorFormat;
    private final @Nullable GpuTextureFormat depthFormat;

    public RenderTarget(GpuTextureFormat colorFormat, @Nullable GpuTextureFormat depthFormat) {
        this.colorFormat = colorFormat;
        this.depthFormat = depthFormat;
    }

    public void resize(GpuDevice device, int width, int height) {
        if (width == 0 || height == 0 || (this.width == width && this.height == height)) {
            return;
        }

        this.width = width;
        this.height = height;

        if (this.color != null) {
            this.color.close();
        }

        if (this.depth != null) {
            this.depth.close();
        }

        this.color = device.createTexture("RenderTargetColor", GpuTextureDimension.D2, this.colorFormat, this.width, this.height, GpuTextureUsage.RENDER_ATTACHMENT);
        if (this.depthFormat != null) {
            this.depth = device.createTexture("RenderTargetDepthStencil", GpuTextureDimension.D2, this.depthFormat, this.width, this.height, GpuTextureUsage.RENDER_ATTACHMENT);
        }
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public GpuTexture color() {
        return this.color;
    }

    public @Nullable GpuTexture depth() {
        return this.depth;
    }

    @Override
    public void close() {
        this.color.close();
        if (this.depth != null) {
            this.depth.close();
        }
    }
}
