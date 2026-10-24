package io.github.kalmemarq.kgl.api.gpu;

public record GpuVertexState(GpuVertexBufferLayout... buffers) {
    public static final GpuVertexState EMPTY = new GpuVertexState();
}
