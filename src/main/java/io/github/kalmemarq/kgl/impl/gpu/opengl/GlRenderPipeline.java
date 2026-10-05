package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.lwjgl.opengl.GL45C;

record GlRenderPipeline(int handle, int vertex, int fragment, GpuPrimitiveState primitiveState,
                        GpuDepthStencilState depthStencilState, GpuVertexState vertexState,
                        GpuBindGroupLayout bindGroupLayout,
                        GpuColorTargetState[] colorTargets) implements GpuRenderPipeline {
    @Override
    public void close() {
        GL45C.glDeleteProgramPipelines(this.handle);
        GL45C.glDeleteProgram(this.vertex);
        GL45C.glDeleteProgram(this.fragment);
    }
}
