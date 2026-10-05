package io.github.kalmemarq;

import imgui.ImDrawData;
import imgui.ImVec4;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.util.RenderPipeline;

// TODO
public class ImGuiImplKgl {
    private static final GpuVertexBufferLayout VERTEX_LAYOUT = new GpuVertexBufferLayout(GpuVertexFormat.FLOAT32X3, GpuVertexFormat.FLOAT32X2, GpuVertexFormat.UNORM8X4);

    private static final RenderPipeline PIPELINE = new RenderPipeline.Builder()
            .withColorTargets(
                    new GpuColorTargetState(
                            new GpuBlendState(
                                    new GpuBlendComponent(GpuBlendOperation.ADD, GpuBlendFactor.SRC_ALPHA, GpuBlendFactor.ONE_MINUS_SRC_ALPHA),
                                    new GpuBlendComponent(GpuBlendOperation.ADD, GpuBlendFactor.ONE, GpuBlendFactor.ONE_MINUS_SRC_ALPHA)
                            ), GpuColorWrite.ALL))
            .build();

    private GpuRenderPipeline pipeline;
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;

    public void renderDrawData(GpuDevice device, GpuRenderPass renderPass, final ImDrawData drawData) {
        final int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        final int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
        if (fbWidth <= 0 || fbHeight <= 0) {
            return;
        }

        if (drawData.getCmdListsCount() <= 0) {
            return;
        }

        // Will project scissor/clipping rectangles into framebuffer space
        final float clipOffX = drawData.getDisplayPosX(); // (0,0) unless using multi-viewports
        final float clipOffY = drawData.getDisplayPosY(); // (0,0) unless using multi-viewports
        final float clipScaleX = drawData.getFramebufferScaleX(); // (1,1) unless using retina display which are often (2,2)
        final float clipScaleY = drawData.getFramebufferScaleY(); // (1,1) unless using retina display which are often (2,2)

        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            renderPass.setPipeline(this.pipeline);
            renderPass.setVertexBuffer(0, this.vertexBuffer);
            renderPass.setIndexBuffer(this.indexBuffer);

            for (int cmdIdx = 0; cmdIdx < drawData.getCmdListCmdBufferSize(n); cmdIdx++) {
                ImVec4 clipRect = new ImVec4();
                drawData.getCmdListCmdBufferClipRect(clipRect, n, cmdIdx);

                final float clipMinX = (clipRect.x - clipOffX) * clipScaleX;
                final float clipMinY = (clipRect.y - clipOffY) * clipScaleY;
                final float clipMaxX = (clipRect.z - clipOffX) * clipScaleX;
                final float clipMaxY = (clipRect.w - clipOffY) * clipScaleY;

                if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                    continue;
                }

                renderPass.setScissorRect((int) clipMinX, (int) (fbHeight - clipMaxY), (int) (clipMaxX - clipMinX), (int) (clipMaxY - clipMinY));
            }
        }
    }
}
