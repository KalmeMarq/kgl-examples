package io.github.kalmemarq.kgl.api.gpu.descriptor;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.jspecify.annotations.Nullable;

public record GpuTextureDescriptor(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, int depthOrLayers, @GpuTextureUsage int usage, @Nullable GpuTextureViewDimension textureBindingViewDimension) {
    public GpuTextureDescriptor(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, int depthOrLayers, @GpuTextureUsage int usage) {
        this(label, dimension, format, width, height, depthOrLayers, usage, null);
    }

    public GpuTextureDescriptor(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, @GpuTextureUsage int usage) {
        this(label, dimension, format, width, height, 1, usage, null);
    }

    public GpuTextureDescriptor(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, @GpuTextureUsage int usage) {
        this(label, dimension, format, width, 1, 1, usage, null);
    }
}
