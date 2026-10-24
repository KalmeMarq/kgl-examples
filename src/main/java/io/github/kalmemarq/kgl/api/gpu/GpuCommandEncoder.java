package io.github.kalmemarq.kgl.api.gpu;

import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPassDescriptor;
import org.joml.Vector3i;

public interface GpuCommandEncoder {
    GpuRenderPass beginRenderPass(GpuRenderPassDescriptor descriptor);
    void copyBufferToBuffer(GpuBufferSlice source, GpuBufferSlice destination);
    void copyTextureToTexture(GpuTexture src, Vector3i srcOrigin, GpuTexture dst, Vector3i dstOrigin, Vector3i copySize);

    void clearBuffer(GpuBufferSlice buffer);
    void clearColorTexture(GpuTexture texture, int color);
    void clearDepthTexture(GpuTexture texture, float clearDepth);
    void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, float clearDepth);

    GpuCommandBuffer finish();
}
