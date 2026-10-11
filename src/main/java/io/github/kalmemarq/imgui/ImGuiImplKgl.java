package io.github.kalmemarq.imgui;

import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImVec4;
import imgui.type.ImInt;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuSamplerDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuTextureDescriptor;
import io.github.kalmemarq.kgl.util.IoUtils;
import io.github.kalmemarq.kgl.util.RenderPipeline;
import io.github.kalmemarq.kgl.util.UboLayout;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ImGuiImplKgl {
    private static final UboLayout UNIFORMS_LAYOUT = new UboLayout("proj", UboLayout.MemberType.MAT4x4);
    private static final GpuVertexBufferLayout VERTEX_LAYOUT = new GpuVertexBufferLayout(
            GpuVertexFormat.FLOAT32X2,
            GpuVertexFormat.FLOAT32X2,
            GpuVertexFormat.UNORM8X4
    );
    private static final RenderPipeline PIPELINE_DESC = new RenderPipeline.Builder()
            .withVertexBuffer(VERTEX_LAYOUT)
            .withColorTargets(
                    new GpuColorTargetState(
                            new GpuBlendState(
                                    new GpuBlendComponent(GpuBlendOperation.ADD, GpuBlendFactor.SRC_ALPHA, GpuBlendFactor.ONE_MINUS_SRC_ALPHA),
                                    new GpuBlendComponent(GpuBlendOperation.ADD, GpuBlendFactor.ONE, GpuBlendFactor.ONE_MINUS_SRC_ALPHA)
                            ),
                            GpuColorWrite.ALL
                    )
            )
            .withBindGroupLayout(new GpuBindGroupLayout(
                    new GpuBindGroupLayoutEntry.Buffer("uniforms", 0),
                    new GpuBindGroupLayoutEntry.Texture("txr", 1),
                    new GpuBindGroupLayoutEntry.Sampler("smp", 2)
            ))
            .build();

    private final GpuDevice device;
    private GpuRenderPipeline pipeline;
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;
    private GpuBuffer uniformBuffer;
    private GpuTexture fontTexture;
    private GpuSampler fontSampler;
    private long vertexBufferSize;
    private long indexBufferSize;
    private final ImVec4 reusableClipRect = new ImVec4();

    public ImGuiImplKgl(GpuDevice device) {
        this.device = device;
    }

    public void newFrame() {
        if (this.pipeline == null) {
            this.createPipeline();
            this.createFontsTexture();
            this.uniformBuffer = this.device.createBuffer(new GpuBufferDescriptor(null, 64, GpuBufferUsage.UNIFORM | GpuBufferUsage.COPY_DST));
        }
    }

    private void createPipeline() {
        String vs_cubes = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/imgui.vsh")));
        String fs_cubes = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/imgui.fsh")));
        this.pipeline = this.device.createRenderPipeline(vs_cubes, fs_cubes, PIPELINE_DESC);
    }

    private void createFontsTexture() {
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
        ByteBuffer pixels = fontAtlas.getTexDataAsRGBA32(width, height);

        this.fontTexture = this.device.createTexture(
                new GpuTextureDescriptor(null,
                        GpuTextureDimension.D2,
                        GpuTextureFormat.RGBA8UINT,
                        width.get(), height.get(),
                        GpuTextureUsage.TEXTURE_BINDING | GpuTextureUsage.COPY_DST)
        );

        this.device.writeToTexture(this.fontTexture, pixels);

        this.fontSampler = this.device.createSampler(new GpuSamplerDescriptor(
                        null,
                        GpuAddressMode.CLAMP_TO_EDGE,
                        GpuFilterMode.LINEAR
                )
        );

        fontAtlas.setTexID(1);
    }

    public void uploadData(ImDrawData drawData) {
        final int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        final int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());

        if (fbWidth <= 0 || fbHeight <= 0 || drawData.getCmdListsCount() == 0) {
            return;
        }

        long totalVertexSize = 0;
        long totalIndexSize = 0;
        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            totalVertexSize += (long) drawData.getCmdListVtxBufferSize(n) * ImDrawData.sizeOfImDrawVert();
            totalIndexSize += (long) drawData.getCmdListIdxBufferSize(n) * ImDrawData.sizeOfImDrawIdx();
        }

        if (totalVertexSize >= 1024 * 1024 * 100) {
            throw new RuntimeException("Fuck: " + totalIndexSize);
        }

        if (this.vertexBuffer == null || this.vertexBufferSize < totalVertexSize) {
            if (this.vertexBuffer != null) {
                this.vertexBuffer.close();
            }
            this.vertexBufferSize = totalVertexSize + 4096;
            this.vertexBuffer = this.device.createBuffer(new GpuBufferDescriptor(null, this.vertexBufferSize, GpuBufferUsage.VERTEX | GpuBufferUsage.COPY_DST));
        }

        if (this.indexBuffer == null || this.indexBufferSize < totalIndexSize) {
            if (this.indexBuffer != null) {
                this.indexBuffer.close();
            }
            this.indexBufferSize = totalIndexSize + 1024;
            this.indexBuffer = this.device.createBuffer(new GpuBufferDescriptor(null, this.indexBufferSize, GpuBufferUsage.INDEX | GpuBufferUsage.COPY_DST));
        }

        long vertexOffset = 0;
        long indexOffset = 0;
        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            final int vtxCount = drawData.getCmdListVtxBufferSize(n);
            final int idxCount = drawData.getCmdListIdxBufferSize(n);

            if (vtxCount > 0) {
                final ByteBuffer vtxData = drawData.getCmdListVtxBufferData(n);
                final int vtxSize = vtxCount * ImDrawData.sizeOfImDrawVert();
                this.device.writeToBuffer(this.vertexBuffer.slice(vertexOffset, vtxSize), vtxData);
                vertexOffset += vtxSize;
            }

            if (idxCount > 0) {
                final ByteBuffer idxData = drawData.getCmdListIdxBufferData(n);
                final int idxSize = idxCount * ImDrawData.sizeOfImDrawIdx();
                this.device.writeToBuffer(this.indexBuffer.slice(indexOffset, idxSize), idxData);
                indexOffset += idxSize;
            }
        }

        final float L = drawData.getDisplayPosX();
        final float R = drawData.getDisplayPosX() + drawData.getDisplaySizeX();
        final float T = drawData.getDisplayPosY();
        final float B = drawData.getDisplayPosY() + drawData.getDisplaySizeY();
        UNIFORMS_LAYOUT.put(this.device, this.uniformBuffer, "proj", new Matrix4f().ortho(L, R - L, B - T, T, -1, 1));
    }

    public void renderData(GpuRenderPass renderPass, ImDrawData drawData) {
        final int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        final int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());

        if (fbWidth <= 0 || fbHeight <= 0 || drawData.getCmdListsCount() == 0) {
            return;
        }

        renderPass.setPipeline(this.pipeline);
        renderPass.setResource("uniforms", this.uniformBuffer);

        final float clipOffX = drawData.getDisplayPosX();
        final float clipOffY = drawData.getDisplayPosY();
        final float clipScaleX = drawData.getFramebufferScaleX();
        final float clipScaleY = drawData.getFramebufferScaleY();

        GpuIndexFormat indexType = ImDrawData.sizeOfImDrawIdx() == 2 ? GpuIndexFormat.UINT16 : GpuIndexFormat.UINT32;

        long vertexOffset = 0;
        long indexOffset = 0;

        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            final int vtxCount = drawData.getCmdListVtxBufferSize(n);
            final int idxCount = drawData.getCmdListIdxBufferSize(n);
            final int vtxSize = vtxCount * ImDrawData.sizeOfImDrawVert();
            final int idxSize = idxCount * ImDrawData.sizeOfImDrawIdx();

            if (vtxCount == 0 || idxCount == 0) {
                continue;
            }

            renderPass.setVertexBuffer(0, this.vertexBuffer.slice(vertexOffset, vtxSize));
            renderPass.setIndexBuffer(this.indexBuffer, indexType);

            for (int cmdI = 0; cmdI < drawData.getCmdListCmdBufferSize(n); cmdI++) {
                drawData.getCmdListCmdBufferClipRect(this.reusableClipRect, n, cmdI);

                final float clipMinX = (this.reusableClipRect.x - clipOffX) * clipScaleX;
                final float clipMinY = (this.reusableClipRect.y - clipOffY) * clipScaleY;
                final float clipMaxX = (this.reusableClipRect.z - clipOffX) * clipScaleX;
                final float clipMaxY = (this.reusableClipRect.w - clipOffY) * clipScaleY;

                if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                    continue;
                }

                final int scissorX = (int) clipMinX;
                final int scissorY = (int) (fbHeight - clipMaxY);
                final int scissorW = (int) (clipMaxX - clipMinX);
                final int scissorH = (int) (clipMaxY - clipMinY);

                renderPass.setScissorRect(scissorX, scissorY, scissorW, scissorH);
                renderPass.setResource("txr", this.fontTexture, this.fontSampler);

                final int elemCount = drawData.getCmdListCmdBufferElemCount(n, cmdI);
                final int idxBufferOffset = drawData.getCmdListCmdBufferIdxOffset(n, cmdI);
                final int vtxBufferOffset = drawData.getCmdListCmdBufferVtxOffset(n, cmdI);

                final int firstIndex = (int) (indexOffset / ImDrawData.sizeOfImDrawIdx()) + idxBufferOffset;

                renderPass.drawIndexed(elemCount, 1, firstIndex, vtxBufferOffset, 0);
            }

            vertexOffset += vtxSize;
            indexOffset += idxSize;
        }
    }

    public void shutdown() {
        if (this.vertexBuffer != null) this.vertexBuffer.close();
        if (this.indexBuffer != null) this.indexBuffer.close();
        if (this.uniformBuffer != null) this.uniformBuffer.close();
        if (this.fontTexture != null) this.fontTexture.close();
    }
}