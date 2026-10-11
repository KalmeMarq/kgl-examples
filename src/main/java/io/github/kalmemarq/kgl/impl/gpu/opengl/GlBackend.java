package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuBackend;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import io.github.kalmemarq.kgl.api.gpu.GpuSurface;
import io.github.kalmemarq.kgl.api.window.WindowHints;
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
    public void setWindowHints(WindowHints hints) {
        hints.set(WindowHints.Hint.GL, true);
        hints.set(WindowHints.Hint.GL_DEBUG, true);
        hints.set(WindowHints.Hint.GL_MAJOR, 4);
        hints.set(WindowHints.Hint.GL_MINOR, 5);
        hints.set(WindowHints.Hint.GL_DOUBLEBUFFER, true);
        hints.set(WindowHints.Hint.GL_DEPTH_BITS, 24);
        hints.set(WindowHints.Hint.GL_STENCIL_BITS, 8);

        hints.set(WindowHints.Hint.GL_CORE, true);
        hints.set(WindowHints.Hint.GL_FORWARD_COMPATIBLE, true);
    }

    @Override
    public GpuDevice createDevice(long windowHandle, boolean validation) {
        return new GlDevice(windowHandle, validation);
    }

    @Override
    public GpuSurface createSurface(long windowHandle) {
        return new GlSurface(windowHandle);
    }
}
