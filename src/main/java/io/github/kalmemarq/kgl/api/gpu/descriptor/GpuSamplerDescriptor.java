package io.github.kalmemarq.kgl.api.gpu.descriptor;

import io.github.kalmemarq.kgl.api.gpu.GpuAddressMode;
import io.github.kalmemarq.kgl.api.gpu.GpuFilterMode;
import org.jspecify.annotations.Nullable;

public record GpuSamplerDescriptor(@Nullable String label, GpuAddressMode addressModeU, GpuAddressMode addressModeV, GpuAddressMode addressModeW, GpuFilterMode minFilter, GpuFilterMode magFilter) {
    public GpuSamplerDescriptor(@Nullable String label, GpuAddressMode addressModeUVW, GpuFilterMode minMagFilter) {
        this(label, addressModeUVW, addressModeUVW, addressModeUVW, minMagFilter, minMagFilter);
    }
}
