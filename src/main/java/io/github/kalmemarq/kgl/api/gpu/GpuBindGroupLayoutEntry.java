package io.github.kalmemarq.kgl.api.gpu;

public sealed interface GpuBindGroupLayoutEntry {
    String name();
    int binding();

    record UniformBuffer(String name, int binding) implements GpuBindGroupLayoutEntry {}
    record StorageBuffer(String name, int binding) implements GpuBindGroupLayoutEntry {}
    record Texture(String name, int binding) implements GpuBindGroupLayoutEntry {}
    record Sampler(String name, int binding) implements GpuBindGroupLayoutEntry {}
}
