package io.github.kalmemarq.kgl.api.gpu;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

public interface GpuDevice extends AutoCloseable {
    GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, @GpuTextureUsage int usage);
    GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, @GpuTextureUsage int usage);
    GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, int depthOrLayers, @GpuTextureUsage int usage);
    GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, int depthOrLayers, @GpuTextureUsage int usage, @Nullable GpuTextureViewDimension textureBindingViewDimension);

    GpuSampler createSampler(@Nullable String label, GpuAddressMode addressModeUV, GpuFilterMode minMagFilter);
    GpuSampler createSampler(@Nullable String label, GpuAddressMode addressModeU, GpuAddressMode addressModeV, GpuFilterMode minFilter, GpuFilterMode magFilter);

    GpuBuffer createBuffer(@Nullable String label, long size, @GpuBufferUsage int usage);

    GpuRenderPipeline createRenderPipeline(String vertexSrc, String fragmentSrc, GpuPrimitiveState primitiveState, GpuDepthStencilState depthStencilState, GpuVertexState vertexState, GpuBindGroupLayout bindGroupLayout, GpuColorTargetState[] colorTargets);
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
