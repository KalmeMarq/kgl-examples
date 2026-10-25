package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuTextureDescriptor;
import org.jspecify.annotations.Nullable;

record GlTexture(GlDevice device, int handle, GpuTextureDescriptor descriptor) implements GpuTexture {
    @Override
    public GpuTextureDimension dimension() {
        return this.descriptor.dimension();
    }

    @Override
    public GpuTextureFormat format() {
        return this.descriptor.format();
    }

    @Override
    public @GpuTextureUsage int usage() {
        return this.descriptor.usage();
    }

    @Override
    public int width() {
        return this.descriptor.width();
    }

    @Override
    public int height() {
        return this.descriptor.height();
    }

    @Override
    public int depthOrLayers() {
        return this.descriptor.depthOrLayers();
    }

    @Override
    public @Nullable GpuTextureViewDimension textureBindingViewDimension() {
        return this.descriptor.textureBindingViewDimension();
    }

    @Override
    public void close() {
        this.device.deleteTexture(this);
    }
}
