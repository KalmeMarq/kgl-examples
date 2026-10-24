package io.github.kalmemarq.kgl.api.gpu.descriptor;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.jspecify.annotations.Nullable;

public record GpuRenderPipelineDescriptor(@Nullable String label, ShaderSource source, GpuBindGroupLayout[] bindGroupLayouts, GpuVertexState vertex, GpuPrimitiveState primitive, GpuDepthStencilState depthStencil, GpuFragmentState fragment) {
    public GpuRenderPipelineDescriptor(@Nullable String label, ShaderSource source, GpuBindGroupLayout bindGroupLayouts, GpuVertexState vertex, GpuPrimitiveState primitive, GpuDepthStencilState depthStencil, GpuFragmentState fragment) {
        this(label, source, new GpuBindGroupLayout[]{bindGroupLayouts}, vertex, primitive, depthStencil, fragment);
    }

    public GpuRenderPipelineDescriptor(@Nullable String label, ShaderSource source, GpuBindGroupLayout bindGroupLayouts, GpuVertexState vertex, GpuPrimitiveState primitive, GpuDepthStencilState depthStencil) {
        this(label, source, new GpuBindGroupLayout[]{bindGroupLayouts}, vertex, primitive, depthStencil, new GpuFragmentState(new GpuColorTargetState[0]));
    }

    public record ShaderSource(String vertex, String fragment) {
    }
}
