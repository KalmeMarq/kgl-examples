package io.github.kalmemarq.kgl.api.gpu;

import org.jspecify.annotations.Nullable;

public interface GpuTexture extends AutoCloseable {
    GpuTextureDimension dimension();
    GpuTextureFormat format();
    @GpuTextureUsage int usage();

    int width();
    int height();
    int depthOrLayers();

    @Nullable GpuTextureViewDimension textureBindingViewDimension();

    @Override
    void close();
}
