package io.github.kalmemarq.kgl.api.gpu;

public record GpuPrimitiveState(GpuPrimitiveTopology topology, GpuFrontFace frontFace, GpuCullMode cullMode) {
    public GpuPrimitiveState(GpuPrimitiveTopology topology, GpuFrontFace frontFace) {
        this(topology, frontFace, GpuCullMode.NONE);
    }

    public GpuPrimitiveState(GpuPrimitiveTopology topology, GpuCullMode cullMode) {
        this(topology, GpuFrontFace.CCW, cullMode);
    }

    public GpuPrimitiveState(GpuPrimitiveTopology topology) {
        this(topology, GpuFrontFace.CCW, GpuCullMode.NONE);
    }
}
