package io.github.kalmemarq.kgl.api.gpu;

public interface GpuBuffer extends GpuBufferSlice, AutoCloseable {
    @GpuBufferUsage int usage();

    default GpuBuffer buffer() {
        return this;
    }

    @Override
    default long offset() {
        return 0;
    }

    @Override
    void close();
}
