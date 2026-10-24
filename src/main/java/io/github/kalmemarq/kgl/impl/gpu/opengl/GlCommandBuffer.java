package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBufferSlice;
import io.github.kalmemarq.kgl.api.gpu.GpuCommandBuffer;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class GlCommandBuffer implements GpuCommandBuffer {
    static final int COPY_BUFFER_TO_BUFFER = 0x1;
    static final int COPY_TEXTURE_TO_TEXTURE = 0x2;
    static final int CLEAR_BUFFER = 0x3;
    static final int CLEAR_COLOR = 0x4;
    static final int CLEAR_DEPTH = 0x5;

    ByteBuffer data = MemoryUtil.memAlloc(4096);
    List<GlTexture> textures = new ArrayList<>();
    List<GlBuffer> buffers = new ArrayList<>();

    public GlCommandBuffer() {
    }

    private static <T> int add(List<T> list, T o) {
        if (o == null) return -1;
        list.add(o);
        return list.size() - 1;
    }

    ByteBuffer begin(int type, int bytes) {
        if (this.data.remaining() < 4 + bytes) {
            int pos = this.data.position();
            this.data = MemoryUtil.memRealloc(this.data, Math.max(this.data.capacity() * 2, pos + 4 + bytes));
            this.data.limit(this.data.capacity());
            this.data.position(pos);
        }
        return this.data.putInt(type);
    }

    // CopyBufferToBuffer = src: ref<i32>, dst: ref<i32>, src_offset: u64, dst_offset: u64, size: u64
    void copyBufferToBuffer(GpuBufferSlice src, GpuBufferSlice dst) {
        ByteBuffer b = this.begin(COPY_BUFFER_TO_BUFFER, 4 + 4 + 8 + 8 + 8);
        b.putInt(add(this.buffers, (GlBuffer) src.buffer())).putInt(add(this.buffers, (GlBuffer) dst.buffer()));
        b.putLong(src.offset()).putLong(dst.offset()).putLong(src.size());
    }

    // CopyTextureToTexture = src: ref<i32>, dst: ref<i32>, src_origin: i32[3], dst_origin: i32[3], copy_size: i32[3]
    void copyTextureToTexture(GlTexture src, Vector3i srcOrigin, GlTexture dst, Vector3i dstOrigin, Vector3i size) {
        ByteBuffer b = this.begin(COPY_TEXTURE_TO_TEXTURE, 4 + 4 + 4 * 3 + 4 * 3 + 4 * 3);
        b.putInt(add(this.textures, src)).putInt(add(this.textures, dst));
        b.putInt(srcOrigin.x).putInt(srcOrigin.y).putInt(srcOrigin.z);
        b.putInt(dstOrigin.x).putInt(dstOrigin.y).putInt(dstOrigin.z);
        b.putInt(size.x).putInt(size.y).putInt(size.z);
    }

    // ClearBuffer = buf: ref<i32>, offset: u64, size: u64
    void clearBuffer(GpuBufferSlice bufferSlice) {
        this.begin(CLEAR_BUFFER, 4 + 8 + 8).putInt(add(this.buffers, (GlBuffer) bufferSlice.buffer())).putLong(bufferSlice.offset()).putLong(bufferSlice.size());
    }

    // ClearColorTexture = tex: ref<i32>, color: u32
    void clearColorTexture(GlTexture tex, int color) {
        this.begin(CLEAR_COLOR, 4 + 4).putInt(add(this.textures, tex)).putInt(color);
    }

    // ClearDepthTexture = tex: ref<i32>, depth: f32
    void clearDepthTexture(GlTexture tex, float depth) {
        this.begin(CLEAR_DEPTH, 4 + 4).putInt(add(this.textures, tex)).putFloat(depth);
    }

    void reset() {
        this.data.clear();
        this.textures.clear();
        this.buffers.clear();
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.data);
    }
}
