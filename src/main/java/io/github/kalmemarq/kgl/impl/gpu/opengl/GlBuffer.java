package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuBufferSlice;
import io.github.kalmemarq.kgl.api.gpu.GpuBufferUsage;
import org.lwjgl.opengl.GL45C;

record GlBuffer(int handle, long size, @GpuBufferUsage int usage) implements GpuBuffer {
    @Override
    public GpuBufferSlice slice(long offset, long size) {
        return new GlBufferSlice(this, offset, size);
    }

    @Override
    public void close() {
        GL45C.glDeleteBuffers(this.handle);
    }
}
