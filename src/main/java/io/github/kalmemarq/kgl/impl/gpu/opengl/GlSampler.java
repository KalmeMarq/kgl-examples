package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuAddressMode;
import io.github.kalmemarq.kgl.api.gpu.GpuFilterMode;
import io.github.kalmemarq.kgl.api.gpu.GpuSampler;
import org.lwjgl.opengl.GL45C;

public record GlSampler(int handle, GpuAddressMode addressModeU, GpuAddressMode addressModeV, GpuFilterMode minFilter,
                        GpuFilterMode magFilter) implements GpuSampler {
    @Override
    public void close() {
        GL45C.glDeleteSamplers(this.handle);
    }
}
