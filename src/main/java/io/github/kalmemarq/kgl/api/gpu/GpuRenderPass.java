package io.github.kalmemarq.kgl.api.gpu;

public interface GpuRenderPass extends AutoCloseable {
    void setPipeline(GpuRenderPipeline pipeline);
    void setResource(String name, GpuBufferSlice buffer);
    void setViewport(int x, int y, int width, int height);
    void setResource(String name, GpuTexture texture, GpuSampler sampler);
    void setScissorRect(int x, int y, int width, int height);
    void setVertexBuffer(int slot, GpuBufferSlice buffer);
    void setIndexBuffer(GpuBuffer buffer, GpuIndexFormat format);
    void drawIndexed(int indexCount);
    void drawIndexed(int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance);
    void draw(int count);

    @Override
    void close();
}
