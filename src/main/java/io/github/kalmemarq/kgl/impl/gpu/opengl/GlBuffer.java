package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuBufferSlice;
import io.github.kalmemarq.kgl.api.gpu.GpuBufferUsage;

record GlBuffer(GlDevice device, int handle, long size, @GpuBufferUsage int usage) implements GpuBuffer {
    @Override
    public GpuBufferSlice slice(long offset, long size) {
        return new GlBufferSlice(this, offset, size);
    }

    @Override
    public void close() {
        this.device.deleteBuffer(this);
    }
}
