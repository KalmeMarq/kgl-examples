package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL45C;

public record GlTexture(int handle, GpuTextureDimension dimension, GpuTextureFormat format, @GpuTextureUsage int usage,
                        int width, int height, int depthOrLayers,
                        @Nullable GpuTextureViewDimension textureBindingViewDimension) implements GpuTexture {
    @Override
    public void close() {
        GL45C.glDeleteTextures(this.handle);
    }
}
