package io.github.kalmemarq;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImBoolean;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPipelineDescriptor;
import io.github.kalmemarq.kgl.api.window.Event;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.util.*;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import org.joml.Matrix4f;
import org.joml.Random;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Lines implements App {
    private static final UboLayout UNIFORMS_LAYOUT = new UboLayout.Builder()
            .add("proj_view_model", UboLayout.Type.MAT4x4)
            .add("resolution", UboLayout.Type.FLOAT32X2)
            .add("line_width", UboLayout.Type.FLOAT32)
            .build();

    private Matrix4f proj;
    private Matrix4f view;
    private Matrix4f model;
    private Matrix4f projViewModel;

    private GpuRenderPipeline pipeline;
    private GpuRenderPipeline pipelineInstanced;
    private GpuRenderPipeline pipelineStorage;

    private GpuBuffer vbo;
    private GpuBuffer vboInstanced;
    private GpuBuffer ubo;
    private GpuBuffer ssbo;
    private ByteBuffer stagingBuffer;
    private QuadIndexBuffer ibo;

    private Camera camera = new Camera();
    private Int2BooleanMap keys = new Int2BooleanOpenHashMap();

    private ImBoolean useInstancing;
    private ImBoolean useSSBO;
    private int[] lineCount = {800};
    private float[] lineWidth = {1f};

    static void main() {
        App.run(new Lines());
    }

    @Override
    public void initialize(Window window, GpuDevice device) {
        this.proj = new Matrix4f();
        this.view = new Matrix4f();
        this.model = new Matrix4f();
        this.projViewModel = new Matrix4f();

        GpuRenderPipelineDescriptor.ShaderSource linesSrc = new GpuRenderPipelineDescriptor.ShaderSource(
                IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/lines.vsh"))),
                IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/lines.fsh")))
        );

        GpuRenderPipelineDescriptor.ShaderSource linesInstanceSrc = new GpuRenderPipelineDescriptor.ShaderSource(
                IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/lines_instance.vsh"))),
                IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/lines_instance.fsh")))
        );

        GpuRenderPipelineDescriptor.ShaderSource linesStorageSrc = new GpuRenderPipelineDescriptor.ShaderSource(
                IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/lines_storage.vsh"))),
                IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/lines_storage.fsh")))
        );

        this.pipeline = device.createRenderPipeline(new GpuRenderPipelineDescriptor(
                "Lines",
                linesSrc,
                new GpuBindGroupLayout(new GpuBindGroupLayoutEntry.UniformBuffer("uniforms", 0)),
                new GpuVertexState(new GpuVertexBufferLayout(GpuVertexFormat.FLOAT32X3, GpuVertexFormat.FLOAT32X3, GpuVertexFormat.UNORM8X4)),
                new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST, GpuCullMode.BACK),
                new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL)
        ));

        this.pipelineInstanced = device.createRenderPipeline(new GpuRenderPipelineDescriptor(
                "LinesInstanced",
                linesInstanceSrc,
                new GpuBindGroupLayout(new GpuBindGroupLayoutEntry.UniformBuffer("uniforms", 0)),
                new GpuVertexState(new GpuVertexBufferLayout(GpuVertexStepMode.INSTANCE, GpuVertexFormat.FLOAT32X3, GpuVertexFormat.FLOAT32X3, GpuVertexFormat.UNORM8X4)),
                new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_STRIP, GpuCullMode.BACK),
                new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL)
        ));

        this.pipelineStorage = device.createRenderPipeline(new GpuRenderPipelineDescriptor(
                "LinesStorage",
                linesStorageSrc,
                new GpuBindGroupLayout(new GpuBindGroupLayoutEntry.UniformBuffer("uniforms", 0), new GpuBindGroupLayoutEntry.StorageBuffer("lines", 1)),
                new GpuVertexState(),
                new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST, GpuCullMode.BACK),
                new GpuDepthStencilState(GpuTextureFormat.DEPTH32FLOAT, true, GpuCompareFunction.LESS_EQUAL)
        ));

        this.vbo = device.createBuffer(new GpuBufferDescriptor("VBO", this.pipeline.vertexState().buffers()[0].arrayStride() * 4L * 100000L, GpuBufferUsage.VERTEX | GpuBufferUsage.COPY_DST));
        this.vboInstanced = device.createBuffer(new GpuBufferDescriptor("VBO Instanced", this.pipeline.vertexState().buffers()[0].arrayStride() * 100000L, GpuBufferUsage.VERTEX | GpuBufferUsage.COPY_DST));
        this.ubo = device.createBuffer(new GpuBufferDescriptor("UBO", UNIFORMS_LAYOUT.byteSize(), GpuBufferUsage.UNIFORM | GpuBufferUsage.COPY_DST));

        this.stagingBuffer = MemoryUtil.memAlloc((int) this.vbo.size());
        this.ibo = new QuadIndexBuffer(device, new int[]{0, 1, 2, 2, 1, 3});

        this.ssbo = device.createBuffer(new GpuBufferDescriptor("SSBO", this.pipeline.vertexState().buffers()[0].arrayStride() * 100000L, GpuBufferUsage.STORAGE | GpuBufferUsage.COPY_DST));

        window.addEventHandler(event -> {
            if (event instanceof Event.Key keyEvent) {
                this.keys.put(keyEvent.key(), keyEvent.pressed());
            } else if (window.isMouseLocked() && event instanceof Event.MouseMove wheelEvent) {
                float dx = wheelEvent.xrel();
                float dy = wheelEvent.yrel();

                float sensitivity = 0.0025f;
                this.camera.rotate(-dx * sensitivity, -dy * sensitivity);
            } else if (event instanceof Event.MouseButton buttonEvent) {
                if (!buttonEvent.pressed() && buttonEvent.button() == 2) {
                    if (window.isMouseLocked()) window.unlockMouse();
                    else window.lockMouse();
                }
            }
        });

        this.useInstancing = new ImBoolean(false);
        this.useSSBO = new ImBoolean(false);

        {
            Random random = new Random(0);

            this.stagingBuffer.clear();
            int lines = 0;
            for (int i = 0; i < 100000; ++i, ++lines) {
                int r = random.nextInt(256);
                int g = random.nextInt(256);
                int b = random.nextInt(256);

                int y = i % 500;
                int x = i / 500;

                int color = 0xFF << 24 | r << 16 | g << 8 | b;
                addLine(this.stagingBuffer, x * 6f, y * 0.1f, 0f, x * 6f + 5f, y * 0.1f, 0f, color);
            }

            this.stagingBuffer.flip();
            device.writeToBuffer(this.vbo, this.stagingBuffer);
            this.ibo.ensureCount(lines * 6);
        }

        {
            Random random = new Random(0);

            this.stagingBuffer.clear();
            int lines = 0;
            for (int i = 0; i < 100000; ++i, ++lines) {
                int r = random.nextInt(256);
                int g = random.nextInt(256);
                int b = random.nextInt(256);

                int y = i % 500;
                int x = i / 500;

                int color = 0xFF << 24 | r << 16 | g << 8 | b;
                addLineInstanced(this.stagingBuffer, x * 6f, y * 0.1f, 0f, x * 6f + 5f, y * 0.1f, 0f, color);
            }

            this.stagingBuffer.flip();
            device.writeToBuffer(this.ssbo, this.stagingBuffer);
            device.writeToBuffer(this.vboInstanced, this.stagingBuffer);
        }
    }

    @Override
    public void shutdown(Window window, GpuDevice device) {
        this.pipeline.close();
        this.pipelineInstanced.close();
        this.ibo.close();
        this.vbo.close();
        MemoryUtil.memFree(this.stagingBuffer);
    }

    private static void addLine(ByteBuffer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len;
        dy /= len;
        dz /= len;

        buffer.putFloat(x0).putFloat(y0).putFloat(z0).putFloat(dx).putFloat(dy).putFloat(dz).putInt(color);
        buffer.putFloat(x0).putFloat(y0).putFloat(z0).putFloat(dx).putFloat(dy).putFloat(dz).putInt(color);
        buffer.putFloat(x1).putFloat(y1).putFloat(z1).putFloat(dx).putFloat(dy).putFloat(dz).putInt(color);
        buffer.putFloat(x1).putFloat(y1).putFloat(z1).putFloat(dx).putFloat(dy).putFloat(dz).putInt(color);
    }

    private static void addLineInstanced(ByteBuffer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;

        buffer.putFloat(x0).putFloat(y0).putFloat(z0).putFloat(x1).putFloat(y1).putFloat(z1).putInt(color);
    }

    @Override
    public void render(Window window, GpuDevice device, RenderTarget target, int lastFps, ImGuiFrame imGuiFrame) {
        this.proj.setPerspective((float) Math.toRadians(70f), window.width() / (float) window.height(), 0.05f, 1000f);

        float speed = 0.05f;
        float forward = 0f, right = 0f, up = 0f;

        if (this.keys.getOrDefault(SDLKeycode.SDLK_W, false)) forward += speed;
        if (this.keys.getOrDefault(SDLKeycode.SDLK_S, false)) forward -= speed;
        if (this.keys.getOrDefault(SDLKeycode.SDLK_D, false)) right += speed;
        if (this.keys.getOrDefault(SDLKeycode.SDLK_A, false)) right -= speed;
        if (this.keys.getOrDefault(SDLKeycode.SDLK_SPACE, false)) up += speed;
        if (this.keys.getOrDefault(SDLKeycode.SDLK_LSHIFT, false)) up -= speed;

        this.camera.move(forward, right, up);
        this.camera.getView(this.view);

        this.model.identity();
        this.projViewModel.set(this.proj).mul(this.view).mul(this.model);

        UNIFORMS_LAYOUT.put(device, this.ubo, "proj_view_model", this.projViewModel);
        UNIFORMS_LAYOUT.put(device, this.ubo, "line_width", this.lineWidth[0]);
        UNIFORMS_LAYOUT.put(device, this.ubo, "resolution", (float) window.width(), (float) window.height());

        int lines = this.lineCount[0];

        device.clearColorAndDepthTextures(target.color(), 0xFF_181818, target.depth(), 1);

        if (!this.useInstancing.get() && !this.useSSBO.get()) {
            try (GpuRenderPass renderPass = device.createRenderPass(target.color(), target.depth())) {
                renderPass.setPipeline(this.pipeline);
                renderPass.setResource("uniforms", this.ubo);
                renderPass.setVertexBuffer(0, this.vbo);
                renderPass.setIndexBuffer(this.ibo.buffer(), GpuIndexFormat.UINT32);
                renderPass.drawIndexed(lines * 6);
            }
        } else if (!this.useSSBO.get()) {
            try (GpuRenderPass renderPass = device.createRenderPass(target.color(), target.depth())) {
                renderPass.setPipeline(this.pipelineInstanced);
                renderPass.setResource("uniforms", this.ubo);
                renderPass.setVertexBuffer(0, this.vboInstanced);
                renderPass.draw(lines * 4, lines);
            }
        } else {
            try (GpuRenderPass renderPass = device.createRenderPass(target.color(), target.depth())) {
                renderPass.setPipeline(this.pipelineStorage);
                renderPass.setResource("uniforms", this.ubo);
                renderPass.setResource("lines", this.ssbo.slice(0, lines * 28L));
                renderPass.draw(lines * 6);
            }
        }

        device.clearDepthTexture(target.depth(), 1);

        try (var _ = imGuiFrame.begin()) {
            ImGui.setNextWindowPos(new ImVec2(window.width() - window.width() / 4.5f - 10.0f, 10.0f));
            ImGui.setNextWindowSize(new ImVec2(window.width() / 4.5f, window.height() / 3.5f));

            ImGui.begin("Settings");
            ImGui.text(lastFps + " FPS");
//            ImGui.text("XYZ: " + this.position.x + ", " + this.position.y + ", " + this.position.z);

            if (ImGui.checkbox("Instance-Step VBO w/ TriangleStrip", this.useInstancing)) {
                if (this.useInstancing.get()) {
                    this.useSSBO.set(false);
                }
            }

            if (ImGui.checkbox("SSBO", this.useSSBO)) {
                if (this.useSSBO.get()) {
                    this.useInstancing.set(false);
                }
            }

            ImGui.sliderInt("Lines", this.lineCount, 1, 100000);
            ImGui.sliderFloat("Line width", this.lineWidth, 1f, 15f);

            if (!this.useInstancing.get() && !this.useSSBO.get()) {
                ImGui.text("VBO: " + (lines * 28 * 4) + " bytes");
                ImGui.text("IBO: " + (lines * 6 * 4) + " bytes");
                ImGui.text(formatBytes(lines * 28 * 4L + lines * 6 * 4L) + " (" + (lines * 28 * 4L + lines * 6 * 4L) + " bytes)");
            } else if (!this.useSSBO.get()) {
                ImGui.text("VBO: " + (lines * 28) + " bytes");
                ImGui.text("IBO: none");
                ImGui.text(formatBytes(lines * 28L) + " (" + (lines * 28) + " bytes)");
            } else {
                ImGui.text("SSBO: " + (lines * 28) + " bytes");
                ImGui.text("IBO: none");
                ImGui.text(formatBytes(lines * 28L) + " (" + (lines * 28) + " bytes)");
            }

            ImGui.newLine();
            ImGui.text("Middle-Button to lock/unlock cursor");

            ImGui.end();
        }

        device.blit(target.color());
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";

        double value = bytes;
        String[] units = {"KB", "MB", "GB"};
        int unit = -1;

        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024.0 && unit < units.length - 1);

        return String.format("%.2f %s", value, units[unit]);
    }
}
