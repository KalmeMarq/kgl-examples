package io.github.kalmemarq.kgl.api.gpu;

public record GpuDepthStencilState(GpuTextureFormat format, boolean depthWriteEnabled, GpuCompareFunction depthCompare) {
}
