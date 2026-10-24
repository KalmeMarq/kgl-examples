package io.github.kalmemarq.kgl.api.gpu;

import org.jspecify.annotations.Nullable;

public record GpuColorTargetState(@Nullable GpuBlendState blendState, @GpuColorWrite int writeMask) {
    public static final GpuColorTargetState DEFAULT = new GpuColorTargetState();

    public GpuColorTargetState(GpuBlendState blendState) {
        this(blendState, GpuColorWrite.ALL);
    }

    public GpuColorTargetState(@GpuColorWrite int writeMask) {
        this(null, writeMask);
    }

    public GpuColorTargetState() {
        this(null, GpuColorWrite.ALL);
    }
}
