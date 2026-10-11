package io.github.kalmemarq.imgui;

import imgui.ImDrawData;
import imgui.ImGui;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import io.github.kalmemarq.kgl.api.gpu.GpuRenderPass;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.impl.window.sdl.SdlWindow;

public class ImGuiKgl {
    private ImGuiImplSdl3 imGuiImplSdl3;
    private ImGuiImplKgl imGuiImplKgl;

    public void init(Window window, GpuDevice device) {
        ImGui.createContext();
        ImGui.getIO().setIniFilename("");
        ImGui.getIO().setLogFilename("");
        this.imGuiImplSdl3 = new ImGuiImplSdl3();
        this.imGuiImplKgl = new ImGuiImplKgl(device);
        this.imGuiImplSdl3.init(window.handle());
        if (window instanceof SdlWindow sdlWindow) {
            sdlWindow.addSdlEventHandler(sdlEvent -> this.imGuiImplSdl3.processEvent(sdlEvent.address()));
        }
    }

    public void newFrame(Window window) {
        this.imGuiImplSdl3.newFrame();
        this.imGuiImplKgl.newFrame();
        ImGui.newFrame();
    }

    public void shutdown() {
        this.imGuiImplSdl3.shutdown();
        this.imGuiImplKgl.shutdown();
        ImGui.destroyContext();
    }

    public void uploadData(ImDrawData drawData) {
        this.imGuiImplKgl.uploadData(drawData);
    }

    public void renderData(GpuRenderPass renderPass, ImDrawData drawData) {
        this.imGuiImplKgl.renderData(renderPass, drawData);
    }
}
