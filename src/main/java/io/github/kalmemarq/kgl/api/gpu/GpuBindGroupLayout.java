package io.github.kalmemarq.kgl.api.gpu;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public record GpuBindGroupLayout(Map<String, GpuBindGroupLayoutEntry> entries) {
    public GpuBindGroupLayout(GpuBindGroupLayoutEntry... entries) {
        this(Arrays.stream(entries).collect(Collectors.toMap(GpuBindGroupLayoutEntry::name, entry -> entry)));
    }
}
