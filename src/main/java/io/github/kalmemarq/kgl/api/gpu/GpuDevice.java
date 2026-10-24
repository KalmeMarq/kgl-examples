package io.github.kalmemarq.kgl.api.gpu;

import io.github.kalmemarq.kgl.api.gpu.descriptor.*;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

public interface GpuDevice extends AutoCloseable {
    GpuLimits limits();

    GpuTexture createTexture(GpuTextureDescriptor descriptor);
    GpuSampler createSampler(GpuSamplerDescriptor descriptor);
    GpuBuffer createBuffer(GpuBufferDescriptor descriptor);
    GpuRenderPipeline createRenderPipeline(GpuRenderPipelineDescriptor descriptor);
    GpuRenderPass createRenderPass(GpuTexture colorAttachment, @Nullable GpuTexture depthStencilAttachment);
    GpuRenderPass createRenderPass(GpuRenderPassDescriptor descriptor);

    //
    void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data);
    void copyBufferToBuffer(GpuBufferSlice src, GpuBufferSlice dst);

    void writeToTexture(GpuTexture texture, ByteBuffer data);
    void writeToTexture(GpuTexture texture, ByteBuffer data, int dstX, int dstY, int dstWidth, int dstHeight);

    void copyBufferToTexture(GpuBufferSlice src, int srcX, int srcY, int srcWidth, int srcHeight, GpuTexture dst, int dstX, int dstY, int dstWidth, int dstHeight, int layer);

    void clearColorTexture(GpuTexture texture, int color);
    void clearDepthTexture(GpuTexture texture, double clearDepth);
    void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, double clearDepth);
    //

    void blit(GpuTexture texture);

    @Override
    void close();
}
