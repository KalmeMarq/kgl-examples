package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBufferSlice;

record GlBufferSlice(GlBuffer buffer, long offset, long size) implements GpuBufferSlice {
    @Override
    public GpuBufferSlice slice(long offset, long size) {
        return new GlBufferSlice(this.buffer, this.offset + offset, size);
    }
}
