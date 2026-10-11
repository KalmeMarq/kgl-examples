package io.github.kalmemarq.kgl.api.gpu.descriptor;

import io.github.kalmemarq.kgl.api.gpu.GpuBufferUsage;
import org.jspecify.annotations.Nullable;

public record GpuBufferDescriptor(@Nullable String label, long size, @GpuBufferUsage int usage) {
}
