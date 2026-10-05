package io.github.kalmemarq.kgl.api.gpu;

public sealed interface GpuBindGroupLayoutEntry {
    String name();
    int binding();

    record Buffer(String name, int binding) implements GpuBindGroupLayoutEntry {}
    record Texture(String name, int binding) implements GpuBindGroupLayoutEntry {}
    record Sampler(String name, int binding) implements GpuBindGroupLayoutEntry {}
}
