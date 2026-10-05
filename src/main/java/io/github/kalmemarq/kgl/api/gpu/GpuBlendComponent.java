package io.github.kalmemarq.kgl.api.gpu;

public record GpuBlendComponent(GpuBlendOperation operation, GpuBlendFactor srcFactor, GpuBlendFactor dstFactor) {
}
