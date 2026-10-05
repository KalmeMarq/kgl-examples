package io.github.kalmemarq.kgl.api.gpu;

public record GpuBlendState(GpuBlendComponent color, GpuBlendComponent alpha) {
    public GpuBlendState(GpuBlendComponent colorAlpha) {
        this(colorAlpha, colorAlpha);
    }
}
