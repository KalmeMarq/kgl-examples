package io.github.kalmemarq.kgl.api.gpu.descriptor;

import io.github.kalmemarq.kgl.api.gpu.GpuColorTargetState;

public record GpuFragmentState(GpuColorTargetState[] targets) {
}
