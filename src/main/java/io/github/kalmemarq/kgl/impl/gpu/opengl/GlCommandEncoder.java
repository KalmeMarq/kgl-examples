package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPassDescriptor;
import org.joml.Vector3i;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class GlCommandEncoder implements GpuCommandEncoder {
    final GlDevice device;
    final GlCommandBuffer commandBuffer;
    boolean inRenderPass;
    boolean finished;

    GlCommandEncoder(GlDevice device, GlCommandBuffer commandBuffer) {
        this.device = device;
        this.commandBuffer = commandBuffer;
    }

    private void check() {
        if (this.inRenderPass) throw new GpuValidationException("There is a render pass open");
        if (this.finished) throw new GpuValidationException("Encoder has already finished");
    }

    @Override
    public GpuRenderPass beginRenderPass(GpuRenderPassDescriptor descriptor) {
        this.inRenderPass = true;
        return new GlRenderPassEx(this, this.commandBuffer, descriptor);
    }

    @Override
    public void copyBufferToBuffer(GpuBufferSlice src, GpuBufferSlice dst) {
        if (this.device.validation) {
            GlValidator.validateCopyBufferToBuffer(src, dst);
        }

        this.check();
        this.commandBuffer.copyBufferToBuffer(src, dst);
    }

    @Override
    public void copyTextureToTexture(GpuTexture src, Vector3i srcOrigin, GpuTexture dst, Vector3i dstOrigin, Vector3i copySize) {
        this.check();
        this.commandBuffer.copyTextureToTexture((GlTexture) src, srcOrigin, (GlTexture) dst, dstOrigin, copySize);
    }

    @Override
    public void clearBuffer(GpuBufferSlice buffer) {
        this.check();
        this.commandBuffer.clearBuffer(buffer);
    }

    @Override
    public void clearColorTexture(GpuTexture texture, int color) {
        this.check();
        this.commandBuffer.clearColorTexture((GlTexture) texture, color);
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, float clearDepth) {
        this.check();
        this.commandBuffer.clearDepthTexture((GlTexture) texture, clearDepth);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, float clearDepth) {
        this.clearColorTexture(colorTexture, color);
        this.clearDepthTexture(depthTexture, clearDepth);
    }

    @Override
    public GpuCommandBuffer finish() {
        this.finished = true;
        this.inRenderPass = false;
        return this.commandBuffer;
    }

    static class GlRenderPassEx implements GpuRenderPass {
        private final GlCommandEncoder encoder;
        private final GlCommandBuffer commandBuffer;
        private boolean finished;

        private GlRenderPipeline renderPipeline;
        private GpuIndexFormat indexFormat;
        private final Map<String, Object> resources;
        GpuBufferSlice[] vertexBuffers;

        public GlRenderPassEx(GlCommandEncoder encoder, GlCommandBuffer commandBuffer, GpuRenderPassDescriptor descriptor) {
            this.encoder = encoder;
            this.commandBuffer = commandBuffer;
            this.resources = new HashMap<>();
            this.indexFormat = GpuIndexFormat.UINT16;
            this.commandBuffer.beginRenderPass(descriptor);
        }

        private void check() {
            if (this.finished) throw new GpuValidationException("Encoder has already finished");
        }

        @Override
        public void setPipeline(GpuRenderPipeline pipeline) {
            this.check();
            this.renderPipeline = (GlRenderPipeline) pipeline;
            this.commandBuffer.setRenderPipeline(this.renderPipeline);
        }

        @Override
        public void setResource(String name, GpuBufferSlice buffer) {
            this.check();
            this.resources.put(name, buffer);
        }

        @Override
        public void setViewport(int x, int y, int width, int height) {
            this.check();
            this.commandBuffer.setViewport(x, y, width, height);
        }

        @Override
        public void setResource(String name, GpuTexture texture, GpuSampler sampler) {
            this.check();
            this.resources.put(name, new TextureSampler((GlTexture) texture, (GlSampler) sampler));
        }

        @Override
        public void setScissorRect(int x, int y, int width, int height) {
            this.check();
            this.commandBuffer.setScissor(x, y, width, height);
        }

        @Override
        public void setVertexBuffer(int slot, GpuBufferSlice buffer) {
            this.check();
            if (this.encoder.device.validation) {
                if (buffer != null && (buffer.buffer().usage() & GpuBufferUsage.VERTEX) == 0) {
                    throw new GpuValidationException("Buffer usage does not include VERTEX");
                }

                if (slot < 0) {
                    throw new GpuValidationException("Slot can't be negative");
                }

                if (slot > 6) {
                    throw new GpuValidationException("Slot can't be greater than 6");
                }
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
            this.check();
            this.indexFormat = format;
            this.commandBuffer.setIndexBuffer(this.renderPipeline, (GlBuffer) buffer, format);
        }

        @Override
        public void drawIndexed(int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance) {
            this.check();
            this.commandBuffer.setResources(this.renderPipeline, this.resources);
            this.commandBuffer.setVertexBuffers(this.renderPipeline, this.vertexBuffers);
            this.commandBuffer.drawIndexed(this.renderPipeline.primitiveState().topology(), this.indexFormat, indexCount, instanceCount, baseIndex, baseVertex, baseInstance);
        }

        @Override
        public void draw(int count, int instanceCount, int baseVertex, int baseInstance) {
            this.check();
            this.commandBuffer.setResources(this.renderPipeline, this.resources);
            this.commandBuffer.setVertexBuffers(this.renderPipeline, this.vertexBuffers);
            this.commandBuffer.draw(this.renderPipeline.primitiveState().topology(), count, instanceCount, baseVertex, baseInstance);
        }

        @Override
        public void close() {
            this.check();
            this.commandBuffer.endRenderPass();
            this.finished = true;
            this.encoder.inRenderPass = false;
        }
    }

    record TextureSampler(GlTexture texture, GlSampler sampler) {}
}
