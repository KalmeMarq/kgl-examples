package io.github.kalmemarq.kgl.api.gpu;

public interface GpuRenderPass extends AutoCloseable {
    void setPipeline(GpuRenderPipeline pipeline);
    void setResource(String name, GpuBufferSlice buffer);
    void setViewport(int x, int y, int width, int height);
    void setResource(String name, GpuTexture texture, GpuSampler sampler);
    void setScissorRect(int x, int y, int width, int height);
    void setVertexBuffer(int slot, GpuBufferSlice buffer);
    void setIndexBuffer(GpuBuffer buffer, GpuIndexFormat format);

    default void drawIndexed(int indexCount) {
        this.drawIndexed(indexCount, 1, 0, 0, 0);
    }

    void drawIndexed(int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance);

    default void draw(int count) {
        this.draw(count, 1, 0, 0);
    }

    default void draw(int count, int instanceCount) {
        this.draw(count, instanceCount, 0, 0);
    }

    void draw(int count, int instanceCount, int baseVertex, int baseInstance);

    @Override
    void close();
}
