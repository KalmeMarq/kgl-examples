package io.github.kalmemarq.kgl.api.gpu;

public interface GpuSampler extends AutoCloseable {
    GpuAddressMode addressModeU();
    GpuAddressMode addressModeV();
    GpuAddressMode addressModeW();

    GpuFilterMode minFilter();
    GpuFilterMode magFilter();

    @Override
    void close();
}
