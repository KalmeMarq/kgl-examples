package io.github.kalmemarq.kgl.api.gpu;

public interface GpuBufferSlice {
    GpuBuffer buffer();

    GpuBufferSlice slice(long offset, long size);

    long offset();
    long size();
}
