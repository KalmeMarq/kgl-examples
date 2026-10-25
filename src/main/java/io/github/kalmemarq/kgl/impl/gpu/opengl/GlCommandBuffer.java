package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBufferSlice;
import io.github.kalmemarq.kgl.api.gpu.GpuCommandBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuIndexFormat;
import io.github.kalmemarq.kgl.api.gpu.GpuPrimitiveTopology;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPassDescriptor;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class GlCommandBuffer implements GpuCommandBuffer {
    static final int COPY_BUFFER_TO_BUFFER = 0x1;
    static final int COPY_TEXTURE_TO_TEXTURE = 0x2;
    static final int CLEAR_BUFFER = 0x3;
    static final int CLEAR_COLOR = 0x4;
    static final int CLEAR_DEPTH = 0x5;

    static final int SET_PIPELINE = 0x6;
    static final int SET_RESOURCES = 0x7;
    static final int SET_SCISSOR = 0x8;
    static final int SET_VIEWPORT = 0x9;
    static final int SET_VERTEX_BUFFERS = 0x10;
    static final int SET_INDEX_BUFFER = 0x11;
    static final int DRAW = 0x12;
    static final int DRAW_INDEXED = 0x13;
    static final int END_RENDER_PASS = 0x14;
    static final int BEGIN_RENDER_PASS = 0x15;

    ByteBuffer data = MemoryUtil.memAlloc(4096 * 2);
    List<GlTexture> textures = new ArrayList<>();
    List<GlBuffer> buffers = new ArrayList<>();
    List<GlRenderPipeline> renderPipelines = new ArrayList<>();
    List<Map<String, Object>> resources = new ArrayList<>();
    List<GpuRenderPassDescriptor> renderPassDescriptors = new ArrayList<>();

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

    // BeginRenderPass = pipeline: ref<i32>
    void beginRenderPass(GpuRenderPassDescriptor descriptor) {
        this.begin(BEGIN_RENDER_PASS, 4).putInt(add(this.renderPassDescriptors, descriptor));
    }

    // SetRenderPipeline = pipeline: ref<i32>
    void setRenderPipeline(GlRenderPipeline pipeline) {
        this.begin(SET_PIPELINE, 4).putInt(add(this.renderPipelines, pipeline));
    }

    // SetResources = pipeline: ref<i32>, resources: ref<i32>
    void setResources(GlRenderPipeline pipeline, Map<String, Object> resources) {
        this.begin(SET_RESOURCES, 4 + 4).putInt(add(this.renderPipelines, pipeline)).putInt(add(this.resources, resources));
    }

    // SetScissor = x0: i32, y0: i32, x1: i32, y1: i32
    void setScissor(int x, int y, int width, int height) {
        this.begin(SET_SCISSOR, 4 * 4).putInt(x).putInt(y).putInt(width).putInt(height);
    }

    // SetViewport = x0: i32, y0: i32, x1: i32, y1: i32
    void setViewport(int x, int y, int width, int height) {
        this.begin(SET_VIEWPORT, 4 * 4).putInt(x).putInt(y).putInt(width).putInt(height);
    }

    // SetVertexBuffer = pipeline: ref<i32>, slot_count: u32, slots: ...(u32, u64, u64)
    void setVertexBuffers(GlRenderPipeline pipeline, GpuBufferSlice[] slots) {
        ByteBuffer b = this.begin(SET_VERTEX_BUFFERS, 4 + 4 + (slots == null ? 0 : slots.length) * (4 + 8 + 8));
        b.putInt(add(this.renderPipelines, pipeline));

        if (slots == null) {
            b.putInt(0);
            return;
        }
        b.putInt(slots.length);
        for (GpuBufferSlice slot : slots) {
            b.putInt(slot == null ? -1 : add(this.buffers, (GlBuffer) slot.buffer()));
            if (slot == null) {
                b.putLong(0);
                b.putLong(0);
                continue;
            }
            b.putLong(slot.offset()).putLong(slot.size());
        }
    }

    // SetIndexBuffer = pipeline: ref<i32>, buf: ref<u16>, format: u16
    void setIndexBuffer(GlRenderPipeline pipeline, GlBuffer buffer, GpuIndexFormat format) {
        this.begin(SET_INDEX_BUFFER, 4 + 2 + 2).putInt(add(this.renderPipelines, pipeline)).putShort((short) add(this.buffers, buffer)).putShort((short) format.ordinal());
    }

    // Draw = topology: i32, count: i32, instanceCount: i32, baseVertex: i32, baseInstance: i32
    void draw(GpuPrimitiveTopology topology, int count, int instanceCount, int baseVertex, int baseInstance) {
        this.begin(DRAW, 4 * 4).putInt(GlUtils.toGlEnum(topology)).putInt(count).putInt(instanceCount).putInt(baseVertex).putInt(baseInstance);
    }

    // Draw = topology: u32, format: u32, indexCount: i32, instanceCount: i32, baseIndex: i32, baseVertex: i32, baseInstance: i32
    void drawIndexed(GpuPrimitiveTopology topology, GpuIndexFormat format, int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance) {
        this.begin(DRAW_INDEXED, 4 + 4 + 4 * 5).putInt(GlUtils.toGlEnum(topology)).putInt(GlUtils.toGlEnum(format)).putInt(indexCount).putInt(instanceCount).putInt(baseIndex).putInt(baseVertex).putInt(baseInstance);
    }

    // EndRenderPass
    void endRenderPass() {
        this.begin(END_RENDER_PASS, 0);
    }

    void reset() {
        this.data.clear();
        this.textures.clear();
        this.renderPipelines.clear();
        this.resources.clear();
        this.buffers.clear();
        this.renderPassDescriptors.clear();
    }

    @Override
    public void close() {
        MemoryUtil.memFree(this.data);
    }
}
