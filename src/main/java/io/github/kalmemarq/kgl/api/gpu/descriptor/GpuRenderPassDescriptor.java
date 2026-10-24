package io.github.kalmemarq.kgl.api.gpu.descriptor;

import io.github.kalmemarq.kgl.api.gpu.GpuLoadOp;
import io.github.kalmemarq.kgl.api.gpu.GpuStoreOp;
import io.github.kalmemarq.kgl.api.gpu.GpuTexture;
import org.jspecify.annotations.Nullable;

public record GpuRenderPassDescriptor(ColorAttachment[] colorAttachments, @Nullable DepthStencilAttachment depthStencilAttachment) {
    public GpuRenderPassDescriptor(ColorAttachment colorAttachment, @Nullable DepthStencilAttachment depthStencilAttachment) {
        this(new ColorAttachment[]{colorAttachment}, depthStencilAttachment);
    }

    public record ColorAttachment(GpuTexture texture, GpuLoadOp loadOp, GpuStoreOp storeOp, int color) {
        public ColorAttachment(GpuTexture texture) {
            this(texture, GpuLoadOp.LOAD, GpuStoreOp.STORE, 0);
        }
    }

    public record DepthStencilAttachment(GpuTexture texture, GpuLoadOp loadOp, GpuStoreOp storeOp) {
        public DepthStencilAttachment(GpuTexture texture) {
            this(texture, GpuLoadOp.LOAD, GpuStoreOp.STORE);
        }
    }
}
