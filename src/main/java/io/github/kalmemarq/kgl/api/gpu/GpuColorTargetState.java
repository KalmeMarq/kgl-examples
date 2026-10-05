package io.github.kalmemarq.kgl.api.gpu;

public record GpuColorTargetState(GpuBlendState blendState, @GpuColorWrite int writeMask) {
}
