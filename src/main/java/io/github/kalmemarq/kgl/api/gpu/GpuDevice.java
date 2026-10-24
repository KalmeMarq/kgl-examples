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
    GpuCommandEncoder createCommandEncoder();

    //
    GpuRenderPass createRenderPass(GpuTexture colorAttachment, @Nullable GpuTexture depthStencilAttachment);
    GpuRenderPass createRenderPass(GpuRenderPassDescriptor descriptor);
    //

    void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data);
    void writeToTexture(GpuTexture texture, ByteBuffer data);

    void submit(GpuCommandBuffer commandBuffer);

    void blit(GpuTexture texture);

    @Override
    void close();
}
