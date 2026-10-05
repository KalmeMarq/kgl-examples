package io.github.kalmemarq.kgl.api.gpu;

import org.jspecify.annotations.NonNull;

public interface GpuRenderPipeline extends AutoCloseable {
    @NonNull GpuPrimitiveState primitiveState();
    GpuDepthStencilState depthStencilState();
    GpuVertexState vertexState();
    GpuBindGroupLayout bindGroupLayout();
    GpuColorTargetState[] colorTargets();

    @Override
    void close();


}
