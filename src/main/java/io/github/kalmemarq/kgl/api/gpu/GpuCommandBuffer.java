package io.github.kalmemarq.kgl.api.gpu;

public interface GpuCommandBuffer extends AutoCloseable {
    @Override
    void close();
}
