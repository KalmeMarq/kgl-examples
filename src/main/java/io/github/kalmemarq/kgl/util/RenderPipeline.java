package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuFragmentState;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPipelineDescriptor;

import java.util.ArrayList;
import java.util.List;

public record RenderPipeline(GpuVertexState vertexState,
                             GpuPrimitiveState primitiveState, GpuDepthStencilState depthStencilState,
                             GpuBindGroupLayout bindGroupLayout, GpuColorTargetState[] colorTargets) {
    public GpuRenderPipelineDescriptor toDescriptor(String vertexSource, String fragmentSource) {
        return new GpuRenderPipelineDescriptor(null, new GpuRenderPipelineDescriptor.ShaderSource(vertexSource, fragmentSource), this.bindGroupLayout, this.vertexState, this.primitiveState, this.depthStencilState, new GpuFragmentState(this.colorTargets));
    }

    public static class Builder {
        private static final GpuBindGroupLayout EMPTY_BIND_GROUP_LAYOUT = new GpuBindGroupLayout();
        private static final GpuPrimitiveState DEFAULT_PRIMITIVE_STATE = new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST);
        private final List<GpuVertexBufferLayout> vertexStateBuffers;
        private GpuPrimitiveState primitiveState;
        private GpuDepthStencilState depthStencilState;
        private GpuBindGroupLayout bindGroupLayout;
        private GpuColorTargetState[] colorTargets;

        public Builder() {
            this.colorTargets = new GpuColorTargetState[0];
            this.vertexStateBuffers = new ArrayList<>();
            this.bindGroupLayout = EMPTY_BIND_GROUP_LAYOUT;
            this.primitiveState = DEFAULT_PRIMITIVE_STATE;
        }

        public Builder withVertexBuffer(GpuVertexBufferLayout vertexBufferLayout) {
            this.vertexStateBuffers.add(vertexBufferLayout);
            return this;
        }

        public Builder withPrimitiveState(GpuPrimitiveState primitiveState) {
            this.primitiveState = primitiveState;
            return this;
        }

        public Builder withDepthStencilState(GpuDepthStencilState depthStencilState) {
            this.depthStencilState = depthStencilState;
            return this;
        }

        public Builder withBindGroupLayout(GpuBindGroupLayout bindGroupLayout) {
            this.bindGroupLayout = bindGroupLayout;
            return this;
        }

        public Builder withColorTargets(GpuColorTargetState... colorTargetState) {
            this.colorTargets = colorTargetState;
            return this;
        }

        public RenderPipeline build() {
            return new RenderPipeline(new GpuVertexState(this.vertexStateBuffers.toArray(new GpuVertexBufferLayout[0])), this.primitiveState, this.depthStencilState, this.bindGroupLayout, this.colorTargets);
        }
    }
}
