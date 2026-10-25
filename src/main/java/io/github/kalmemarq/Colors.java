package io.github.kalmemarq;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.type.ImInt;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.*;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.util.IoUtils;
import io.github.kalmemarq.kgl.util.RenderTarget;
import io.github.kalmemarq.kgl.util.image.StbImage;
import org.joml.Vector3i;

import java.io.IOException;
import java.nio.file.Files;

public class Colors implements App {
    private GpuRenderPipeline pipeline;

    private RenderTarget allRT;
    private RenderTarget redRT;
    private RenderTarget greenRT;
    private RenderTarget blueRT;

    private GpuTexture texture;
    private GpuSampler sampler;

    private static final String[] CHANNELS = {"All", "Red", "Green", "Blue"};
    private ImInt channel = new ImInt(0);

    static void main() {
        App.run(new Colors());
    }

    @Override
    public void initialize(Window window, GpuDevice device) {
        this.allRT = new RenderTarget(GpuTextureFormat.RGBA8UNORM, null);
        this.redRT = new RenderTarget(GpuTextureFormat.RGBA8UNORM, null);
        this.greenRT = new RenderTarget(GpuTextureFormat.RGBA8UNORM, null);
        this.blueRT = new RenderTarget(GpuTextureFormat.RGBA8UNORM, null);

        GpuRenderPipelineDescriptor.ShaderSource source = new GpuRenderPipelineDescriptor.ShaderSource(IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/colors.vsh"))), IoUtils.unchecked(() -> Files.readString(IoUtils.jarPath.resolve("shaders/colors.fsh"))));
        this.pipeline = device.createRenderPipeline(new GpuRenderPipelineDescriptor(
                null,
                source,
                new GpuBindGroupLayout(
                        new GpuBindGroupLayoutEntry.Texture("txr", 0),
                        new GpuBindGroupLayoutEntry.Sampler("smp", 1)
                ),
                GpuVertexState.EMPTY,
                new GpuPrimitiveState(GpuPrimitiveTopology.TRIANGLE_LIST),
                null,
                new GpuFragmentState(
                        GpuColorTargetState.DEFAULT,
                        GpuColorTargetState.DEFAULT,
                        GpuColorTargetState.DEFAULT,
                        GpuColorTargetState.DEFAULT
                )
        ));

        this.texture = device.createTexture(new GpuTextureDescriptor(null, GpuTextureDimension.D2, GpuTextureFormat.RGBA8UNORM, 512, 512, GpuTextureUsage.COPY_DST | GpuTextureUsage.TEXTURE_BINDING));

        try {
            StbImage data = StbImage.load(IoUtils.jarPath.resolve("textures/wall.jpg"));

            device.writeToTexture(this.texture, data.asByteBuffer());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.sampler = device.createSampler(new GpuSamplerDescriptor(null, GpuAddressMode.REPEAT, GpuFilterMode.LINEAR));
    }

    @Override
    public void shutdown(Window window, GpuDevice device) {
        this.allRT.close();
        this.redRT.close();
        this.greenRT.close();
        this.blueRT.close();
    }

    @Override
    public void render(Window window, GpuDevice device, RenderTarget target, int lastFps, ImGuiFrame imGuiFrame) {
        this.allRT.resize(device, target.width(), target.height());
        this.redRT.resize(device, target.width(), target.height());
        this.greenRT.resize(device, target.width(), target.height());
        this.blueRT.resize(device, target.width(), target.height());

        GpuCommandEncoder encoder = device.createCommandEncoder();

        try (GpuRenderPass renderPass = encoder.beginRenderPass(new GpuRenderPassDescriptor(
                new GpuRenderPassDescriptor.ColorAttachment(this.allRT.color(), GpuLoadOp.CLEAR, GpuStoreOp.STORE, 0x181818),
                new GpuRenderPassDescriptor.ColorAttachment(this.redRT.color(), GpuLoadOp.CLEAR, GpuStoreOp.STORE, 0x181818),
                new GpuRenderPassDescriptor.ColorAttachment(this.greenRT.color(), GpuLoadOp.CLEAR, GpuStoreOp.STORE, 0x181818),
                new GpuRenderPassDescriptor.ColorAttachment(this.blueRT.color(), GpuLoadOp.CLEAR, GpuStoreOp.STORE, 0x181818)
        ))) {
            renderPass.setPipeline(this.pipeline);
            renderPass.setResource("txr", this.texture, this.sampler);
            renderPass.draw(3);
        }

        encoder.copyTextureToTexture(switch (this.channel.get()) {
            case 1 -> this.redRT.color();
            case 2 -> this.greenRT.color();
            case 3 -> this.blueRT.color();
            default -> this.allRT.color();
        }, new Vector3i(), target.color(), new Vector3i(), new Vector3i(target.width(), target.height(), 1));

        device.submit(encoder.finish());

        try (var _ = imGuiFrame.begin()) {
            ImGui.setNextWindowPos(new ImVec2(window.width() - window.width() / 5.0f - 10.0f, 10.0f));
            ImGui.setNextWindowSize(new ImVec2(window.width() / 5.0f, window.height() / 3.5f));

            ImGui.begin("Settings");
            ImGui.text(lastFps + " FPS");

            ImGui.combo("Channel", this.channel, CHANNELS);

            ImGui.end();
        }

        device.blit(target.color());
    }
}
