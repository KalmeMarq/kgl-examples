package io.github.kalmemarq;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.sdl3.ImGuiImplSdl3;
import io.github.kalmemarq.kgl.api.gpu.GpuBackend;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import io.github.kalmemarq.kgl.api.gpu.GpuSurface;
import io.github.kalmemarq.kgl.api.window.Event;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.api.window.WindowConfig;
import io.github.kalmemarq.kgl.impl.window.SdlWindow;
import org.lwjgl.sdl.SDLKeycode;

public interface App {
    void initialize(Window window, GpuDevice device);
    void shutdown(Window window, GpuDevice device);
    default void update(Window window, GpuDevice device) {}
    void render(Window window, GpuDevice device, int lastFps, ImGuiFrame imGuiFrame);

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

        GpuDevice device = backend.createDevice(window.handle());
        GpuSurface surface = backend.createSurface(window.handle());
        surface.configure(GpuSurface.PresentMode.FIFO);

        ImGuiImplSdl3 imGuiSdl3;
        ImGuiImplGl3 imGuiGl3;

        imGuiSdl3 = new ImGuiImplSdl3();
        imGuiGl3 = new ImGuiImplGl3();

        ImGui.createContext();
        ImGui.getIO().setIniFilename("");
        ImGui.getIO().setLogFilename("");
        imGuiSdl3.init(window.handle());
        imGuiGl3.init("#version 450");

        if (window instanceof SdlWindow sdlWindow) {
            sdlWindow.addSdlEventHandler(sdlEvent -> {
                imGuiSdl3.processEvent(sdlEvent.address());
            });
        }

        app.initialize(window, device);

        window.show();

        ImGuiFrame imGuiFrame = new ImGuiFrame() {
            boolean started = false;

            @Override
            public void begin() {
                imGuiSdl3.newFrame();
                imGuiGl3.newFrame();
                ImGui.newFrame();
                this.started = true;
            }

            @Override
            public void end() {
                if (!this.started) {
                    return;
                }
                ImGui.render();
                imGuiGl3.renderDrawData(ImGui.getDrawData());
                this.started = false;
            }
        };

        long lastTime = System.currentTimeMillis();
        int frames = 0;
        int lastFps = 0;

        while (running[0]) {
            window.pollEvents();

            app.update(window, device);
            app.render(window, device, lastFps, imGuiFrame);

            surface.present();
            ++frames;

            while (System.currentTimeMillis() - lastTime >= 1000L) {
                lastTime += 1000L;
                lastFps = frames;
                IO.println(frames + " FPS");
                frames = 0;
            }
        }

        app.shutdown(window, device);
        window.close();
    }

    interface ImGuiFrame {
        void begin();
        void end();
    }
}
