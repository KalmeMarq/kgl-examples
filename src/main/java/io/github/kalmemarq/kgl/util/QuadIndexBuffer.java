package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.GpuBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuBufferUsage;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import org.lwjgl.system.MemoryUtil;

public class QuadIndexBuffer implements AutoCloseable {
    private final GpuDevice device;
    private GpuBuffer buffer;
    private int count;

    public QuadIndexBuffer(GpuDevice device) {
        this.device = device;
    }

    public static int indexCountForVertexCount(int vertexCount) {
        return vertexCount / 4 * 6;
    }

    public GpuBuffer buffer() {
        return this.buffer;
    }

    public int ensureForVertexCount(int vertexCount) {
        return this.ensureCount(indexCountForVertexCount(vertexCount));
    }

    public int ensureCount(int count) {
        if (count <= this.count)
            return count;

        count *= 2;

        GpuBuffer newBuffer = this.device.createBuffer("QuadIndexBuffer", count * 4L, GpuBufferUsage.COPY_SRC | GpuBufferUsage.COPY_DST | GpuBufferUsage.INDEX);

        if (this.buffer != null) {
            this.device.copyToBuffer(this.buffer, newBuffer);
            this.buffer.close();
        }

        this.buffer = newBuffer;

        int c = count - this.count;
        int size = (count - this.count) * 4;
        long pointer = MemoryUtil.nmemAlloc(size);
        try {
            for (int i = 0, j = this.count / 6 * 4; i < c; i += 6, j += 4) {
                MemoryUtil.memPutInt(pointer + i * 4L, j);
                MemoryUtil.memPutInt(pointer + (i + 1) * 4L, j + 1);
                MemoryUtil.memPutInt(pointer + (i + 2) * 4L, j + 2);
                MemoryUtil.memPutInt(pointer + (i + 3) * 4L, j + 2);
                MemoryUtil.memPutInt(pointer + (i + 4) * 4L, j + 3);
                MemoryUtil.memPutInt(pointer + (i + 5) * 4L, j);
            }

            this.device.writeToBuffer(this.buffer.slice(this.count * 4L, size), MemoryUtil.memByteBuffer(pointer, size));
        } finally {
            MemoryUtil.nmemFree(pointer);
        }

        this.count = count;
        return this.count;
    }

    @Override
    public void close() {
        if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
        this.count = 0;
    }
}
