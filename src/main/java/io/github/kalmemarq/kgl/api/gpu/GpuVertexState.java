package io.github.kalmemarq.kgl.api.gpu;

public record GpuVertexState(GpuVertexBufferLayout[] buffers) {
    public GpuVertexState(GpuVertexBufferLayout buffer) {
        this(new  GpuVertexBufferLayout[]{buffer});
    }
}
