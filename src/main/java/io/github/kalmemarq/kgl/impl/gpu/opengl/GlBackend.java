package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBackend;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import io.github.kalmemarq.kgl.api.gpu.GpuSurface;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.Configuration;

public class GlBackend implements GpuBackend {
    public GlBackend() {
        Configuration.OPENGL_EXPLICIT_INIT.set(true);
    }

    @Override
    public String getName() {
        return "OpenGL";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void setWindowHints(int props) {
        SDLProperties.SDL_SetBooleanProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_OPENGL_BOOLEAN, true);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_FLAGS, SDLVideo.SDL_GL_CONTEXT_DEBUG_FLAG);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK, SDLVideo.SDL_GL_CONTEXT_PROFILE_COMPATIBILITY);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION, 5);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_DOUBLEBUFFER, 1);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_DEPTH_SIZE, 24);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_STENCIL_SIZE, 8);

        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK, SDLVideo.SDL_GL_CONTEXT_PROFILE_CORE);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_FLAGS, SDLVideo.SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG);
    }

    @Override
    public GpuDevice createDevice(long windowHandle) {
        return new GlDevice(windowHandle);
    }

    @Override
    public GpuSurface createSurface(long windowHandle) {
        return new GlSurface(windowHandle);
    }
}
