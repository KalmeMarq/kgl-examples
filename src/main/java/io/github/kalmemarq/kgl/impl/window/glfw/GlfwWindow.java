package io.github.kalmemarq.kgl.impl.window.glfw;

import io.github.kalmemarq.kgl.api.gpu.GpuBackend;
import io.github.kalmemarq.kgl.api.window.*;
import io.github.kalmemarq.kgl.util.image.Image;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public class GlfwWindow implements Window {
    private long handle;
    private int width;
    private int height;
    private boolean isMinimized;
    private boolean isFocused;
    private final List<EventHandler> handlers = new ArrayList<>();

    @Override
    public void init(WindowConfig config, GpuBackend backend) {
        GLFW.glfwInit();
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, config.resizable() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

        WindowHints hints = new WindowHints();
        backend.setWindowHints(hints);

        if (hints.get(WindowHints.Hint.GL).orElse(false)) {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);

            hints.get(WindowHints.Hint.GL_DEBUG).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_DEBUG, value ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE));
            hints.get(WindowHints.Hint.GL_MAJOR).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, value));
            hints.get(WindowHints.Hint.GL_MINOR).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, value));

            hints.get(WindowHints.Hint.GL_DOUBLEBUFFER).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_DOUBLEBUFFER, value ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE));
            hints.get(WindowHints.Hint.GL_DEPTH_BITS).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, value));
            hints.get(WindowHints.Hint.GL_STENCIL_BITS).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, value));

            if (hints.get(WindowHints.Hint.GL_CORE).orElse(false)) {
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            }

            hints.get(WindowHints.Hint.GL_FORWARD_COMPATIBLE).ifPresent(value -> GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, value ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE));
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        }

        this.width = config.width();
        this.height = config.height();
        this.handle = GLFW.glfwCreateWindow(config.width(), config.height(), config.title(), MemoryUtil.NULL, MemoryUtil.NULL);

        GLFW.glfwSetWindowCloseCallback(this.handle, _ -> {
            for (EventHandler handler : this.handlers) {
                handler.onEvent(Event.Quit.INSTANCE);
            }
        });
    }

    @Override
    public void setIcon(Image... images) {
    }

    @Override
    public void addEventHandler(EventHandler handler) {
        this.handlers.add(handler);
    }

    @Override
    public void removeEventHandler(EventHandler handler) {
        this.handlers.remove(handler);
    }

    @Override
    public void show() {
        GLFW.glfwShowWindow(this.handle);
    }

    @Override
    public void hide() {
        GLFW.glfwHideWindow(this.handle);
    }

    @Override
    public void toggleFullscreen() {
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
    }

    @Override
    public void lockMouse() {
    }

    @Override
    public void unlockMouse() {
    }

    @Override
    public boolean isMouseLocked() {
        return false;
    }

    @Override
    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    @Override
    public long handle() {
        return this.handle;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public boolean isMinimized() {
        return GLFW.glfwGetWindowAttrib(this.handle, GLFW.GLFW_ICONIFIED) != 0;
    }

    @Override
    public boolean isFocused() {
        return GLFW.glfwGetWindowAttrib(this.handle, GLFW.GLFW_FOCUSED) != 0;
    }

    @Override
    public void setClipboardText(String text) {
        GLFW.glfwSetClipboardString(this.handle, text);
    }

    @Override
    public String getClipboardText() {
        return GLFW.glfwGetClipboardString(this.handle);
    }

    @Override
    public void close() {
        Callbacks.glfwFreeCallbacks(this.handle);
        GLFW.glfwDestroyWindow(this.handle);
        GLFW.glfwDestroyWindow(this.handle);
    }
}
