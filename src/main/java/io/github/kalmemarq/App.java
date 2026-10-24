package io.github.kalmemarq;

import imgui.ImDrawData;
import imgui.ImGui;
import io.github.kalmemarq.imgui.ImGuiKgl;
import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.window.Event;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.api.window.WindowConfig;
import io.github.kalmemarq.kgl.util.RenderTarget;
import org.lwjgl.sdl.SDLKeycode;

public interface App {
    void initialize(Window window, GpuDevice device);
    void shutdown(Window window, GpuDevice device);
    default void update(Window window, GpuDevice device) {}
    void render(Window window, GpuDevice device, RenderTarget target, int lastFps, ImGuiFrame imGuiFrame);

    static void run(App app) {
        GpuBackend backend = GpuBackend.getFirstSupportedBackend().orElseThrow();
        Window window = Window.create(new WindowConfig().withTitle("Hey").withSize(1280, 720), backend).orElseThrow();

        boolean[] running = {true};
        window.addEventHandler(event -> {
            if (event instanceof Event.Quit) {
                running[0] = false;
            } else if (event instanceof Event.Key ev) {
                if (ev.key() == SDLKeycode.SDLK_F11 && !ev.pressed()) {
                    window.toggleFullscreen();
                }
            }
        });

        GpuDevice device = backend.createDevice(window.handle(), true);
        GpuSurface surface = backend.createSurface(window.handle());
        surface.configure(GpuSurface.PresentMode.FIFO);

        ImGuiKgl imGuiKgl = new ImGuiKgl();
        imGuiKgl.init(window, device);
        RenderTarget target = new RenderTarget(GpuTextureFormat.RGBA8UNORM, GpuTextureFormat.DEPTH32FLOAT);

        app.initialize(window, device);

        window.show();

        ImGuiFrame imGuiFrame = new ImGuiFrame() {
            boolean started = false;

            @Override
            public ImGuiFrame begin() {
                imGuiKgl.newFrame(window);
                this.started = true;
                return this;
            }

            @Override
            public void close() {
                if (!this.started) {
                    return;
                }

                ImGui.render();

                try (GpuRenderPass renderPass = device.createRenderPass(target.color(), null)) {
                    ImDrawData drawData = ImGui.getDrawData();
                    imGuiKgl.uploadData(drawData);
                    imGuiKgl.renderData(renderPass, drawData);
                }
                this.started = false;
            }
        };

        long lastTime = System.currentTimeMillis();
        int frames = 0;
        int lastFps = 0;

        while (running[0]) {
            window.pollEvents();

            app.update(window, device);

            target.resize(device, window.width(), window.height());
            app.render(window, device, target, lastFps, imGuiFrame);

            surface.present();
            ++frames;

            while (System.currentTimeMillis() - lastTime >= 1000L) {
                lastTime += 1000L;
                lastFps = frames;
                frames = 0;
            }
        }

        app.shutdown(window, device);
        imGuiKgl.shutdown();
        device.close();

        window.close();
    }

    interface ImGuiFrame extends AutoCloseable {
        ImGuiFrame begin();
        @Override
        void close();
    }
}
