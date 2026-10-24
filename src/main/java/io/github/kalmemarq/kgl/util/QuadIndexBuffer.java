package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.GpuBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuBufferUsage;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import io.github.kalmemarq.kgl.api.gpu.GpuIndexFormat;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import org.lwjgl.system.MemoryUtil;

public class QuadIndexBuffer implements AutoCloseable {
    private final GpuDevice device;
    private GpuBuffer buffer;
    private GpuIndexFormat format;
    private int count;
    private final int[] baseIndices;

    public QuadIndexBuffer(GpuDevice device) {
        this(device, new int[]{0, 1, 2, 2, 3, 0});
    }

    public QuadIndexBuffer(GpuDevice device, int[] baseIndices) {
        this.device = device;
        this.baseIndices = baseIndices;
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

        GpuBuffer newBuffer = this.device.createBuffer(new GpuBufferDescriptor("QuadIndexBuffer", count * 4L, GpuBufferUsage.COPY_SRC | GpuBufferUsage.COPY_DST | GpuBufferUsage.INDEX));

        if (this.buffer != null) {
            this.device.copyBufferToBuffer(this.buffer, newBuffer);
            this.buffer.close();
        }

        this.buffer = newBuffer;

        int c = count - this.count;
        int size = (count - this.count) * 4;
        long pointer = MemoryUtil.nmemAlloc(size);
        try {
            for (int i = 0, j = this.count / 6 * 4; i < c; i += 6, j += 4) {
                MemoryUtil.memPutInt(pointer + i * 4L, j + this.baseIndices[0]);
                MemoryUtil.memPutInt(pointer + (i + 1) * 4L, j + this.baseIndices[1]);
                MemoryUtil.memPutInt(pointer + (i + 2) * 4L, j + this.baseIndices[2]);
                MemoryUtil.memPutInt(pointer + (i + 3) * 4L, j + this.baseIndices[3]);
                MemoryUtil.memPutInt(pointer + (i + 4) * 4L, j + this.baseIndices[4]);
                MemoryUtil.memPutInt(pointer + (i + 5) * 4L, j + this.baseIndices[5]);
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
