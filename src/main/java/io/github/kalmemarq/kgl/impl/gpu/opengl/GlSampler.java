package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuAddressMode;
import io.github.kalmemarq.kgl.api.gpu.GpuFilterMode;
import io.github.kalmemarq.kgl.api.gpu.GpuSampler;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuSamplerDescriptor;
import org.lwjgl.opengl.GL45C;

record GlSampler(GlDevice device, int handle, GpuSamplerDescriptor descriptor) implements GpuSampler {
    @Override
    public GpuAddressMode addressModeU() {
        return this.descriptor.addressModeU();
    }

    @Override
    public GpuAddressMode addressModeV() {
        return this.descriptor.addressModeV();
    }

    @Override
    public GpuAddressMode addressModeW() {
        return this.descriptor.addressModeW();
    }

    @Override
    public GpuFilterMode minFilter() {
        return this.descriptor.minFilter();
    }

    @Override
    public GpuFilterMode magFilter() {
        return this.descriptor.magFilter();
    }

    @Override
    public void close() {
        this.device.deleteSampler(this);
    }
}
