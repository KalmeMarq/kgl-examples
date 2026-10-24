package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.joml.Vector4i;
import org.joml.Vector4ic;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class GlRenderPass implements GpuRenderPass {
    private final GlDevice device;
    final GlTexture color;
    final @Nullable GlTexture depth;
    GlRenderPipeline pipeline;
    Map<String, Object> resources;
    GpuBufferSlice[] vertexBuffers;
    GlBuffer indexBuffer;
    GpuIndexFormat indexFormat;
    Vector4ic scissorRect;
    Vector4ic viewport;

    public GlRenderPass(GlDevice device, GlTexture color, @Nullable GlTexture depth) {
        this.device = device;
        this.color = color;
        this.depth = depth;
        this.resources = null;
    }

    @Override
    public void setPipeline(GpuRenderPipeline pipeline) {
        this.pipeline = (GlRenderPipeline) pipeline;
    }

    @Override
    public void setResource(String name, GpuBufferSlice buffer) {
        if (this.resources == null) {
            this.resources = new HashMap<>();
        }
        this.resources.put(name, buffer);
    }

    @Override
    public void setResource(String name, GpuTexture texture, GpuSampler sampler) {
        if (this.resources == null) {
            this.resources = new HashMap<>();
        }
        this.resources.put(name, new TextureAndSampler((GlTexture) texture, (GlSampler) sampler));
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        this.viewport = new Vector4i(x, y, width, height);
    }

    @Override
    public void setScissorRect(int x, int y, int width, int height) {
        this.scissorRect = new Vector4i(x, y, width, height);
    }

    @Override
    public void setVertexBuffer(int slot, @Nullable GpuBufferSlice buffer) {
        if (buffer != null && (buffer.buffer().usage() & GpuBufferUsage.VERTEX) == 0) {
            throw new GpuValidationException("Buffer usage does not include VERTEX");
        }

        if (slot < 0) {
            throw new GpuValidationException("Slot can't be negative");
        }

        if (slot > 6) {
            throw new GpuValidationException("Slot can't be greater than 6");
        }

        if (buffer == null && this.vertexBuffers == null) {
            return;
        }

        if (this.vertexBuffers == null) {
            this.vertexBuffers = new GpuBufferSlice[slot > 1 ? 6 : 2];
        } else if (slot >= this.vertexBuffers.length) {
            this.vertexBuffers = Arrays.copyOf(this.vertexBuffers, this.vertexBuffers.length * 2);
        }

        this.vertexBuffers[slot] = buffer;
    }

    @Override
    public void setIndexBuffer(GpuBuffer buffer, GpuIndexFormat format) {
        if ((buffer.buffer().usage() & GpuBufferUsage.INDEX) == 0) {
            throw new GpuValidationException("Buffer usage does not include INDEX");
        }
        this.indexBuffer = (GlBuffer) buffer;
        this.indexFormat = format;
    }

    @Override
    public void drawIndexed(int indexCount) {
        this.device.drawIndexed(this, indexCount, 1, 0, 0, 0);
    }

    @Override
    public void drawIndexed(int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance) {
        this.device.drawIndexed(this, indexCount, instanceCount, baseIndex, baseVertex, baseInstance);
    }

    @Override
    public void draw(int count) {
        this.device.draw(this, count, 1, 0, 0);
    }

    @Override
    public void draw(int count, int instanceCount) {
        this.device.draw(this, count, instanceCount, 0, 0);
    }

    @Override
    public void draw(int count, int instanceCount, int baseVertex, int baseInstance) {
        this.device.draw(this, count, instanceCount, baseVertex, baseInstance);
    }

    @Override
    public void close() {
    }

    record TextureAndSampler(GlTexture texture, GlSampler sampler) {}
}
