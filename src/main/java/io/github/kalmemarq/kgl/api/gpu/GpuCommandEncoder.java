package io.github.kalmemarq.kgl.api.gpu;

import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPassDescriptor;

import java.nio.ByteBuffer;

public interface GpuCommandEncoder {
    GpuRenderPass beginRenderPass(GpuRenderPassDescriptor descriptor);
    void copyBufferToBuffer(GpuBufferSlice source, GpuBufferSlice destination);
    void copyBufferToTexture();
    void copyTextureToBuffer();
    void copyTextureToTexture();

    void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data);
    void writeToTexture(GpuTexture texture, ByteBuffer data);
    void writeToTexture(GpuTexture texture, ByteBuffer data, int dstX, int dstY, int dstWidth, int dstHeight);

    void clearBuffer(GpuBufferSlice buffer);
    void clearColorTexture(GpuTexture texture, int color);
    void clearDepthTexture(GpuTexture texture, double clearDepth);
    void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, double clearDepth);
}
