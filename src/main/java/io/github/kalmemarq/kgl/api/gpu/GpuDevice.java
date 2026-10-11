package io.github.kalmemarq.kgl.api.gpu;

import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuSamplerDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuTextureDescriptor;
import io.github.kalmemarq.kgl.util.RenderPipeline;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

public interface GpuDevice extends AutoCloseable {
    GpuLimits limits();

    GpuTexture createTexture(GpuTextureDescriptor descriptor);
    GpuSampler createSampler(GpuSamplerDescriptor descriptor);
    GpuBuffer createBuffer(GpuBufferDescriptor descriptor);

    GpuRenderPipeline createRenderPipeline(String vertexSrc, String fragmentSrc, GpuPrimitiveState primitiveState, GpuDepthStencilState depthStencilState, GpuVertexState vertexState, GpuBindGroupLayout bindGroupLayout, GpuColorTargetState[] colorTargets);
    default GpuRenderPipeline createRenderPipeline(String vertexSrc, String fragmentSrc, RenderPipeline pipeline) {
        return this.createRenderPipeline(vertexSrc, fragmentSrc, pipeline.primitiveState(), pipeline.depthStencilState(), pipeline.vertexState(), pipeline.bindGroupLayout(), pipeline.colorTargets());
    }
    GpuRenderPass createRenderPass(GpuTexture colorTexture, @Nullable GpuTexture depthStencilTexture);

    void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data);
    void copyToBuffer(GpuBufferSlice src, GpuBufferSlice dst);

    void writeToTexture(GpuTexture texture, ByteBuffer data);
    void writeToTexture(GpuTexture texture, ByteBuffer data, int dstX, int dstY, int dstWidth, int dstHeight);

    void copyBufferToTexture(GpuBufferSlice src, int srcX, int srcY, int srcWidth, int srcHeight, GpuTexture dst, int dstX, int dstY, int dstWidth, int dstHeight, int layer);

    void clearColorTexture(GpuTexture texture, int color);
    void clearDepthTexture(GpuTexture texture, double clearDepth);
    void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, double clearDepth);

    void blit(GpuTexture texture);

    @Override
    void close();
}
