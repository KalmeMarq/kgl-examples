package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuTextureDescriptor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL45C;

record GlTexture(int handle, GpuTextureDescriptor descriptor) implements GpuTexture {
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
        GL45C.glDeleteTextures(this.handle);
    }
}
