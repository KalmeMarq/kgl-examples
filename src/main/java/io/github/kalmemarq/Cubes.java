package io.github.kalmemarq;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImBoolean;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.util.IoUtils;
import io.github.kalmemarq.kgl.util.RenderPipeline;
import io.github.kalmemarq.kgl.util.RenderTarget;
import io.github.kalmemarq.kgl.util.UboLayout;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.sdl.SDLTimer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Cubes implements App {
    private static final UboLayout UBO_LAYOUT = new UboLayout("proj", UboLayout.MemberType.MAT4x4, "view", UboLayout.MemberType.MAT4x4, "model", UboLayout.MemberType.MAT4x4);
    public static final PosColorVertex[] CUBE_VERTICES = {
            new PosColorVertex(-1.0f,  1.0f,  1.0f, 0xff000000),
            new PosColorVertex( 1.0f,  1.0f,  1.0f, 0xff0000ff),
            new PosColorVertex(-1.0f, -1.0f,  1.0f, 0xff00ff00),
            new PosColorVertex( 1.0f, -1.0f,  1.0f, 0xff00ffff),
            new PosColorVertex(-1.0f,  1.0f, -1.0f, 0xffff0000),
            new PosColorVertex( 1.0f,  1.0f, -1.0f, 0xffff00ff),
            new PosColorVertex(-1.0f, -1.0f, -1.0f, 0xffffff00),
            new PosColorVertex( 1.0f, -1.0f, -1.0f, 0xffffffff),
    };

    private Vector3f at;
    private Vector3f eye;
    private Vector3f up;

    private GpuRenderPipeline[] pipelines;

    private Matrix4f proj;
    private Matrix4f view;
    private Matrix4f model;

    private GpuBuffer vbh;
    private GpuBuffer ibh;
    private GpuBuffer ub;

    // ImGui
    private ImBoolean r;
    private ImBoolean g;
    private ImBoolean b;
    private ImBoolean a;

    static void main() {
        App.run(new Cubes());
    }

    @Override
    public void initialize(Window window, GpuDevice device) {
        this.at = new Vector3f(0, 0, 0);
        this.eye = new Vector3f(0, 0, -35f);
        this.up = new Vector3f(0, 1, 0);

        this.proj = new Matrix4f();
        this.view = new Matrix4f();
        this.model = new Matrix4f();

        String vs_cubes = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/cubes.vsh")));
        String fs_cubes = IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/cubes.fsh")));

        RenderPipeline.Builder pipelineBuilder = new RenderPipeline.Builder()
                .withPrimitiveState(new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST, GpuCullMode.BACK))
                .withDepthStencilState(new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL))
                .withVertexBuffer(new GpuVertexBufferLayout(GpuVertexFormat.FLOAT32X3, GpuVertexFormat.UNORM8X4))
                .withBindGroupLayout(new GpuBindGroupLayout(
                        new GpuBindGroupLayoutEntry.Buffer("uniforms", 0)
                ));

        this.pipelines = new GpuRenderPipeline[16];

        for (int r = 0, i = 0; r <= 1; ++r) {
            for (int g = 0; g <= 1; ++g) {
                for (int b = 0; b <= 1; ++b) {
                    for (int a = 0; a <= 1; ++a) {
                        GpuColorTargetState colorTargetState = new GpuColorTargetState(null,
                                (r == 1 ? GpuColorWrite.RED : GpuColorWrite.NONE) |
                                (g == 1 ? GpuColorWrite.GREEN : GpuColorWrite.NONE) |
                                (b == 1 ? GpuColorWrite.BLUE : GpuColorWrite.NONE) |
                                (a == 1 ? GpuColorWrite.ALPHA : GpuColorWrite.NONE)
                        );

                        RenderPipeline pipeline = pipelineBuilder.withColorTargets(colorTargetState).build();

                        this.pipelines[i] = device.createRenderPipeline(vs_cubes, fs_cubes, pipeline.primitiveState(), pipeline.depthStencilState(), pipeline.vertexState(), pipeline.bindGroupLayout(), pipeline.colorTargets());

                        ++i;
                    }
                }
            }
        }
        this.vbh = device.createBuffer(new GpuBufferDescriptor(null, 16L * CUBE_VERTICES.length, GpuBufferUsage.COPY_DST | GpuBufferUsage.VERTEX));
        this.ibh = device.createBuffer(new GpuBufferDescriptor(null, 36 * 4, GpuBufferUsage.COPY_DST | GpuBufferUsage.INDEX));
        this.ub = device.createBuffer(new GpuBufferDescriptor(null, UBO_LAYOUT.byteSize(), GpuBufferUsage.COPY_DST | GpuBufferUsage.UNIFORM));

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer tempBuffer = stack.malloc((int) this.vbh.size());

            for (var vertex : Cubes.CUBE_VERTICES) {
                tempBuffer.putFloat(vertex.x);
                tempBuffer.putFloat(vertex.y);
                tempBuffer.putFloat(vertex.z);
                tempBuffer.put((byte) (vertex.argb >> 16 & 0xFF));
                tempBuffer.put((byte) (vertex.argb >> 8 & 0xFF));
                tempBuffer.put((byte) (vertex.argb >> 0 & 0xFF));
                tempBuffer.put((byte) (vertex.argb >> 24 & 0xFF));
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

        this.r = new ImBoolean(true);
        this.g = new ImBoolean(true);
        this.b = new ImBoolean(true);
        this.a = new ImBoolean(true);
    }

    @Override
    public void shutdown(Window window, GpuDevice device) {
        this.vbh.close();
        this.ibh.close();
        this.ub.close();
        for (GpuRenderPipeline pipeline : this.pipelines) {
            pipeline.close();
        }
    }

    @Override
    public void render(Window window, GpuDevice device, RenderTarget mainTarget, int lastFps, ImGuiFrame imGuiFrame) {
        device.clearColorTexture(mainTarget.color(), 0xff303030);

        this.proj.identity().perspective((float) Math.toRadians(60), window.width() / (float) window.height(), 0.1f, 1000f);
        this.view.identity().lookAt(this.eye, this.at, this.up);

        UBO_LAYOUT.put(device, this.ub, "proj", this.proj);
        UBO_LAYOUT.put(device, this.ub, "view", this.view);

        try (GpuRenderPass renderPass = device.createRenderPass(mainTarget.color(), null)) {
            renderPass.setPipeline(this.pipelines[((r.get() ? 1 : 0) * 8) + ((g.get() ? 1 : 0) * 4) + ((b.get() ? 1 : 0) * 2) + (a.get() ? 1 : 0)]);
            renderPass.setVertexBuffer(0, this.vbh);
            renderPass.setIndexBuffer(this.ibh, GpuIndexFormat.UINT32);
            renderPass.setResource("uniforms", this.ub);

            for (int y = 0; y < 11; ++y) {
                for (int x = 0; x < 11; ++x) {
                    this.model.identity()
                            .translate(-15f + x * 3f, -15f + y * 3f, 0f)
                            .rotateXYZ(x * 0.21f + SDLTimer.SDL_GetTicks() / 1000f, y * 0.37f + SDLTimer.SDL_GetTicks() / 1000f, 0);

                    UBO_LAYOUT.put(device, this.ub, "model", this.model);

                    renderPass.drawIndexed(36);
                }
            }
        }

        try (var _ = imGuiFrame.begin()) {
            ImGui.setNextWindowPos(new ImVec2(window.width() - window.width() / 5.0f - 10.0f, 10.0f));
            ImGui.setNextWindowSize(new ImVec2(window.width() / 5.0f, window.height() / 3.5f));

            ImGui.begin("Settings");
            ImGui.text(lastFps + " FPS");

            ImGui.checkbox("Write R", this.r);
            ImGui.checkbox("Write G", this.g);
            ImGui.checkbox("Write B", this.b);
            ImGui.checkbox("Write A", this.a);

            ImGui.end();
        }

        device.blit(mainTarget.color());
    }

    public record PosColorVertex(float x, float y, float z, int argb) {
    }
}
