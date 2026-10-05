package io.github.kalmemarq;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.util.IoUtils;
import io.github.kalmemarq.kgl.util.RenderTarget;
import io.github.kalmemarq.kgl.util.UboLayout;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.sdl.SDLTimer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Instancing implements App {
    private static final UboLayout UNIFORMS_LAYOUT = new UboLayout("proj", UboLayout.MemberType.MAT4x4, "view", UboLayout.MemberType.MAT4x4, "model", UboLayout.MemberType.MAT4x4);
    private static final UboLayout GRID_UNIFORMS_LAYOUT = new UboLayout("size", UboLayout.MemberType.INT32, "time", UboLayout.MemberType.FLOAT32, "scale", UboLayout.MemberType.FLOAT32);

    private Vector3f at;
    private Vector3f eye;
    private Vector3f up;

    private RenderTarget target;
    private GpuRenderPipeline pipeline;
    private GpuRenderPipeline instancePipeline;
    private GpuRenderPipeline instanceV2Pipeline;

    private Matrix4f proj;
    private Matrix4f view;
    private Matrix4f model;

    private GpuBuffer vbh;
    private GpuBuffer ibh;
    private GpuBuffer ub;
    private GpuBuffer v2ub;

    private ByteBuffer instanceData;
    private GpuBuffer instanceDataBuffer;

    private ImBoolean m_useInstancing;
    private ImBoolean m_useInstancingV2;
    private int[] m_sideSize;

    static void main() {
        App.run(new Instancing());
    }

    @Override
    public void initialize(Window window, GpuDevice device) {
        this.at = new Vector3f(0, 0, 0);
        this.eye = new Vector3f(0, 0, -35f);
        this.up = new Vector3f(0, -1, 0);

        this.target = new RenderTarget(GpuTextureFormat.RGBA8UINT, null);

        this.proj = new Matrix4f();
        this.view = new Matrix4f();
        this.model = new Matrix4f();

        String vsCubes = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/cubes.vsh")));
        String fsCubes = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/cubes.fsh")));
        String vsCubesInstance = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/cubes_instance.vsh")));
        String vsCubesInstanceV2 = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/cubes_instance_v2.vsh")));

        this.pipeline = device.createRenderPipeline(vsCubes, fsCubes, new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST, GpuCullMode.BACK), new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL), new GpuVertexState(new GpuVertexBufferLayout[]{
                new GpuVertexBufferLayout(GpuVertexFormat.FLOAT32X3, GpuVertexFormat.UNORM8X4),
        }), new GpuBindGroupLayout(
                new GpuBindGroupLayoutEntry.Buffer("uniforms", 0)
        ), new GpuColorTargetState[0]);

        this.instancePipeline = device.createRenderPipeline(vsCubesInstance, fsCubes, new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST, GpuCullMode.BACK), new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL), new GpuVertexState(new GpuVertexBufferLayout[]{
                new GpuVertexBufferLayout(GpuVertexFormat.FLOAT32X3, GpuVertexFormat.UNORM8X4),
                new GpuVertexBufferLayout(16, GpuVertexStepMode.INSTANCE, new GpuVertexAttribute(GpuVertexFormat.FLOAT32X4, 0, 2))
        }), new GpuBindGroupLayout(
                new GpuBindGroupLayoutEntry.Buffer("uniforms", 0)
        ), new GpuColorTargetState[0]);

        this.instanceV2Pipeline = device.createRenderPipeline(vsCubesInstanceV2, fsCubes, new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST, GpuCullMode.BACK), new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL), new GpuVertexState(new GpuVertexBufferLayout[]{
                new GpuVertexBufferLayout(GpuVertexFormat.FLOAT32X3, GpuVertexFormat.UNORM8X4)
        }), new GpuBindGroupLayout(
                new GpuBindGroupLayoutEntry.Buffer("uniforms", 0),
                new GpuBindGroupLayoutEntry.Buffer("grid_uniforms", 1)
        ), new GpuColorTargetState[0]);

        this.vbh = device.createBuffer(null, 16L * Cubes.CUBE_VERTICES.length, GpuBufferUsage.COPY_DST | GpuBufferUsage.VERTEX);
        this.ibh = device.createBuffer(null, 36 * 4, GpuBufferUsage.COPY_DST | GpuBufferUsage.INDEX);
        this.ub = device.createBuffer(null, UNIFORMS_LAYOUT.byteSize(), GpuBufferUsage.COPY_DST | GpuBufferUsage.UNIFORM);
        this.v2ub = device.createBuffer(null, GRID_UNIFORMS_LAYOUT.byteSize(), GpuBufferUsage.COPY_DST | GpuBufferUsage.UNIFORM);

        this.instanceData = MemoryUtil.memAlloc(512 * 512 * 16);
        this.instanceDataBuffer = device.createBuffer(null, this.instanceData.capacity(), GpuBufferUsage.COPY_DST | GpuBufferUsage.VERTEX);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer tempBuffer = stack.malloc((int) this.vbh.size());

            for (var vertex : Cubes.CUBE_VERTICES) {
                tempBuffer.putFloat(vertex.x());
                tempBuffer.putFloat(vertex.y());
                tempBuffer.putFloat(vertex.z());
                tempBuffer.put((byte) (vertex.argb() >> 16 & 0xFF));
                tempBuffer.put((byte) (vertex.argb() >> 8 & 0xFF));
                tempBuffer.put((byte) (vertex.argb() >> 0 & 0xFF));
                tempBuffer.put((byte) (vertex.argb() >> 24 & 0xFF));
            }

            tempBuffer.flip();

            device.writeToBuffer(this.vbh, tempBuffer);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer tempBuffer = stack.malloc((int) this.ibh.size());

            tempBuffer.asIntBuffer().put(new int[]{
                    0, 1, 2, // 0
                    1, 3, 2,
                    4, 6, 5, // 2
                    5, 6, 7,
                    0, 2, 4, // 4
                    4, 2, 6,
                    1, 5, 3, // 6
                    5, 7, 3,
                    0, 4, 1, // 8
                    4, 5, 1,
                    2, 3, 6, // 10
                    6, 3, 7
            });

            device.writeToBuffer(this.ibh, tempBuffer);
        }

        this.m_useInstancing = new ImBoolean(false);
        this.m_useInstancingV2 = new ImBoolean(false);
        this.m_sideSize = new int[]{11};
    }

    @Override
    public void shutdown(Window window, GpuDevice device) {
        this.target.close();
        this.vbh.close();
        this.ibh.close();
        this.ub.close();
        this.pipeline.close();
        this.instanceDataBuffer.close();
        this.instancePipeline.close();
        this.instanceV2Pipeline.close();
        MemoryUtil.memFree(this.instanceData);
    }

    @Override
    public void render(Window window, GpuDevice device, int lastFps, ImGuiFrame imGuiFrame) {
        this.target.resize(device, window.width(), window.height());
        device.clearColorTexture(this.target.color(), 0xFF303030);

        this.proj.identity().perspective((float) Math.toRadians(60), window.width() / (float) window.height(), 0.1f, 1000f);
        this.view.identity().lookAt(this.eye, this.at, this.up);

        UNIFORMS_LAYOUT.put(device, this.ub, "proj", this.proj);
        UNIFORMS_LAYOUT.put(device, this.ub, "view", this.view);

        float time = SDLTimer.SDL_GetTicks() / 1000f;
        float scale = 11f / Math.max(this.m_sideSize[0], 11f);

        if (this.m_useInstancing.get() && this.m_useInstancingV2.get()) {
            try (GpuRenderPass renderPass = device.createRenderPass(this.target.color(), null)) {
                this.model.identity().scale(scale);
                UNIFORMS_LAYOUT.put(device, this.ub, "model", this.model);

                GRID_UNIFORMS_LAYOUT.put(device, this.v2ub, "size", this.m_sideSize[0]);
                GRID_UNIFORMS_LAYOUT.put(device, this.v2ub, "time", time);
                GRID_UNIFORMS_LAYOUT.put(device, this.v2ub, "scale", scale);

                renderPass.setPipeline(this.instanceV2Pipeline);
                renderPass.setResource("uniforms", this.ub);
                renderPass.setResource("grid_uniforms", this.v2ub);
                renderPass.setVertexBuffer(0, this.vbh);
                renderPass.setIndexBuffer(this.ibh);
                renderPass.drawIndexed(36, this.m_sideSize[0] * this.m_sideSize[0], 0, 0, 0);
            }
        } else if (this.m_useInstancing.get()) {
            this.instanceData.clear();

            for (int y = 0; y < this.m_sideSize[0]; ++y) {
                for (int x = 0; x < this.m_sideSize[0]; ++x) {
                    this.instanceData.putFloat(-15f * (1f / scale) + x * 3f);
                    this.instanceData.putFloat(-15f * (1f / scale) + y * 3f);
                    this.instanceData.putFloat(x * 0.21f + time);
                    this.instanceData.putFloat(y * 0.37f + time);
                }
            }

            this.instanceData.flip();

            device.writeToBuffer(this.instanceDataBuffer, this.instanceData);

            try (GpuRenderPass renderPass = device.createRenderPass(this.target.color(), null)) {
                this.model.identity().scale(scale);
                UNIFORMS_LAYOUT.put(device, this.ub, "model", this.model);

                renderPass.setPipeline(this.instancePipeline);
                renderPass.setResource("uniforms", this.ub);
                renderPass.setVertexBuffer(0, this.vbh);
                renderPass.setVertexBuffer(1, this.instanceDataBuffer);
                renderPass.setIndexBuffer(this.ibh);
                renderPass.drawIndexed(36, this.m_sideSize[0] * this.m_sideSize[0], 0, 0, 0);
            }
        } else {
            try (GpuRenderPass renderPass = device.createRenderPass(this.target.color(), null)) {
                renderPass.setPipeline(this.pipeline);
                renderPass.setVertexBuffer(0, this.vbh);
                renderPass.setIndexBuffer(this.ibh);
                renderPass.setResource("uniforms", this.ub);

                for (int y = 0; y < this.m_sideSize[0]; ++y) {
                    for (int x = 0; x < this.m_sideSize[0]; ++x) {
                        this.model.identity()
                                .scale(scale)
                                .translate(-15f * (1f / scale) + x * 3f, -15f * (1f / scale) + y * 3f, 0f)
                                .rotateXYZ(x * 0.21f + time, y * 0.37f + time, 0);

                        UNIFORMS_LAYOUT.put(device, this.ub, "model", this.model);

                        renderPass.drawIndexed(36);
                    }
                }
            }
        }

        device.blit(this.target.color());

        imGuiFrame.begin();

        ImGui.setNextWindowPos(new ImVec2(window.width() - window.width() / 5.0f - 10.0f, 10.0f), ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(new ImVec2(window.width() / 5.0f, window.height() / 2.0f), ImGuiCond.FirstUseEver);
        ImGui.begin("Settings");
        ImGui.text(lastFps + " FPS");

        ImGui.text((this.m_useInstancing.get() ? 1 : this.m_sideSize[0] * this.m_sideSize[0]) + " draw calls");

        ImGui.checkbox("Use Instancing", this.m_useInstancing);
        if (this.m_useInstancing.get()) {
            ImGui.checkbox("Use Instancing V2", this.m_useInstancingV2);
        }

        ImGui.text("Grid Side Size:");
        ImGui.sliderInt("##size", this.m_sideSize, 1, 512);
        ImGui.text("Cubes: " + (this.m_sideSize[0] * this.m_sideSize[0]));

        ImGui.end();

        imGuiFrame.end();
    }
}
