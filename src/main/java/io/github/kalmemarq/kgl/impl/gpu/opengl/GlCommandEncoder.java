package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPassDescriptor;
import org.joml.Vector3i;

class GlCommandEncoder implements GpuCommandEncoder {
    private final GlDevice device;
    private final GlCommandBuffer commandBuffer;
    private boolean finished;

    GlCommandEncoder(GlDevice device, GlCommandBuffer commandBuffer) {
        this.device = device;
        this.commandBuffer = commandBuffer;
    }

    private void check() {
        if (this.finished) throw new GpuValidationException("Encoder has already finished");
    }

    @Override
    public GpuRenderPass beginRenderPass(GpuRenderPassDescriptor descriptor) {
        return null;
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
        return this.commandBuffer;
    }
}
