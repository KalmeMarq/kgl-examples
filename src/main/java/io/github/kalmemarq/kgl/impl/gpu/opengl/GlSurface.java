package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuSurface;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.EnumSet;

class GlSurface implements GpuSurface {
    private final long windowHandle;
    private final EnumSet<PresentMode> supportedPresentModes;

    public GlSurface(long windowHandle) {
        this.windowHandle = windowHandle;

        this.supportedPresentModes = EnumSet.noneOf(PresentMode.class);

        if (checkPresentMode(0)) {
            this.supportedPresentModes.add(PresentMode.IMMEDIATE);
        }

        if (checkPresentMode(-1)) {
            this.supportedPresentModes.add(PresentMode.FIFO_RELAXED);
        }

        if (checkPresentMode(1)) {
            this.supportedPresentModes.add(PresentMode.FIFO);
        }
    }

    private static boolean checkPresentMode(int index) {
        SDLVideo.SDL_GL_SetSwapInterval(index);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pInterval = stack.nmalloc(Integer.BYTES);

            if (SDLVideo.nSDL_GL_GetSwapInterval(pInterval)) {
                return MemoryUtil.memGetInt(pInterval) == index;
            }
        }

        return false;
    }

    @Override
    public void configure(PresentMode presentMode) {
        switch (presentMode) {
            case IMMEDIATE, MAILBOX -> SDLVideo.SDL_GL_SetSwapInterval(0);
            case FIFO_RELAXED ->
                    SDLVideo.SDL_GL_SetSwapInterval(this.supportedPresentModes.contains(PresentMode.FIFO_RELAXED) ? -1 : 0);
            case FIFO -> SDLVideo.SDL_GL_SetSwapInterval(this.supportedPresentModes.contains(PresentMode.FIFO) ? 1 : 0);
        }
    }

    @Override
    public void present() {
        SDLVideo.SDL_GL_SwapWindow(this.windowHandle);
    }
}
