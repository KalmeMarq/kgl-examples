package io.github.kalmemarq.kgl.impl.window;

import io.github.kalmemarq.kgl.api.gpu.GpuBackend;
import io.github.kalmemarq.kgl.api.window.Event;
import io.github.kalmemarq.kgl.api.window.EventHandler;
import io.github.kalmemarq.kgl.api.window.Window;
import io.github.kalmemarq.kgl.api.window.WindowConfig;
import io.github.kalmemarq.kgl.util.image.Image;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SdlWindow implements Window, AutoCloseable {
    private long handle;
    private int width;
    private int height;
    private boolean isMinimized;
    private boolean isFocused;
    private final List<EventHandler> handlers = new ArrayList<>();
    private final List<Consumer<SDL_Event>> sdlEventHandlers = new ArrayList<>();

    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;

    private boolean fullscreen;

    private int minWidth;
    private int minHeight;

    public void init(WindowConfig config, GpuBackend backend) {
        SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO);

        this.minWidth = config.minWidth();
        this.minHeight = config.minHeight();

        int props = SDLProperties.SDL_CreateProperties();
        SDLProperties.SDL_SetNumberProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_X_NUMBER, SDLVideo.SDL_WINDOWPOS_CENTERED);
        SDLProperties.SDL_SetNumberProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_Y_NUMBER, SDLVideo.SDL_WINDOWPOS_CENTERED);
        SDLProperties.SDL_SetNumberProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_WIDTH_NUMBER, config.width());
        SDLProperties.SDL_SetNumberProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_HEIGHT_NUMBER, config.height());
        SDLProperties.SDL_SetStringProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_TITLE_STRING, config.title());
        SDLProperties.SDL_SetBooleanProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN, true);
        SDLProperties.SDL_SetBooleanProperty(props, SDLVideo.SDL_PROP_WINDOW_CREATE_RESIZABLE_BOOLEAN, config.resizable());

        backend.setWindowHints(props);

        this.handle = SDLVideo.SDL_CreateWindowWithProperties(props);
        SDLProperties.SDL_DestroyProperties(props);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);

            SDLVideo.SDL_GetWindowSizeInPixels(this.handle, width, height);

            this.width = width.get(0);
            this.height = height.get(0);
        }

        SDLVideo.SDL_SetWindowMinimumSize(this.handle, config.minWidth(), config.minHeight());
    }

    @Override
    public void setIcon(Image... images) {
        SDL_Surface[] surfaces = new SDL_Surface[images.length];

        int idx = 0;
        for (Image image : images) {
            surfaces[idx] = SDLSurface.SDL_CreateSurfaceFrom(image.width(), image.height(), SDLPixels.SDL_PIXELFORMAT_RGBA32, MemoryUtil.memByteBuffer(image.pointer(), image.width() * image.height() * 4), image.width() * 4);
            if (idx > 0) {
                SDLSurface.SDL_AddSurfaceAlternateImage(surfaces[0], surfaces[idx]);
            }
            ++idx;
        }

        if (surfaces.length > 0 && surfaces[0] != null) {
            SDLVideo.SDL_SetWindowIcon(this.handle, surfaces[0]);
        }

        for (SDL_Surface surface : surfaces) {
            SDLSurface.SDL_DestroySurface(surface);
        }
    }

    @Override
    public void addEventHandler(EventHandler handler) {
        this.handlers.add(handler);
    }

    @Override
    public void removeEventHandler(EventHandler handler) {
        this.handlers.remove(handler);
    }

    public void addSdlEventHandler(Consumer<SDL_Event> handler) {
        this.sdlEventHandlers.add(handler);
    }

    public void removeSdlEventHandler(Consumer<SDL_Event> handler) {
        this.sdlEventHandlers.remove(handler);
    }

    @Override
    public void show() {
        SDLVideo.SDL_ShowWindow(this.handle);
    }

    @Override
    public void hide() {
        SDLVideo.SDL_HideWindow(this.handle);
    }

    @Override
    public void toggleFullscreen() {
        this.fullscreen = !this.fullscreen;
        this.setFullscreen(this.fullscreen);
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;

        if (fullscreen) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                SDLVideo.SDL_GetWindowSizeInPixels(this.handle, width, height);

                this.windowedWidth = Math.max(width.get(0), this.minWidth);
                this.windowedHeight = Math.max(height.get(0), this.minHeight);

                SDLVideo.SDL_GetWindowPosition(this.handle, width, height);

                this.windowedX = width.get(0);
                this.windowedY = height.get(0);
            }

            SDLVideo.SDL_SetWindowFullscreenMode(this.handle, null);
            SDLVideo.SDL_SetWindowFullscreen(this.handle, true);
        } else {
            SDLVideo.SDL_SetWindowFullscreen(this.handle, false);
            SDLVideo.SDL_SetWindowSize(this.handle, this.windowedWidth, this.windowedHeight);
            SDLVideo.SDL_SetWindowPosition(this.handle, this.windowedX, this.windowedY);
        }
    }

    @Override
    public void lockMouse() {
        SDLMouse.SDL_WarpMouseInWindow(this.handle, this.width / 2f, this.height / 2f);
        SDLMouse.SDL_SetWindowRelativeMouseMode(this.handle, true);
    }

    @Override
    public void unlockMouse() {
        SDLMouse.SDL_SetWindowRelativeMouseMode(this.handle, false);
        SDLMouse.SDL_WarpMouseInWindow(this.handle, this.width / 2f, this.height / 2f);
    }

    @Override
    public boolean isMouseLocked() {
        return SDLMouse.SDL_GetWindowRelativeMouseMode(this.handle);
    }

    private final SDL_Event event = SDL_Event.malloc();
    private final MouseButtonImp mouseButtonEvent = new MouseButtonImp();
    private final FramebufferResizeImpl framebufferResizeEvent = new FramebufferResizeImpl();
    private final MouseMoveImpl mouseMoveEvent = new MouseMoveImpl();
    private final KeyImpl keyEvent = new KeyImpl();
    private final MouseWheelImpl wheelEvent = new MouseWheelImpl();
    private final TextInputImpl inputEvent = new TextInputImpl();

    @Override
    public void pollEvents() {
        SDL_Event event = this.event;

        while (SDLEvents.SDL_PollEvent(event)) {
            for (var handler : this.sdlEventHandlers) {
                handler.accept(event);
            }

            switch (event.type()) {
                case SDLEvents.SDL_EVENT_QUIT -> this.handlers.forEach(it -> it.onEvent(Event.Quit.INSTANCE));
                case SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED ->
                        this.handlers.forEach(it -> it.onEvent(Event.CloseRequested.INSTANCE));
                case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP, SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                    SDL_MouseButtonEvent button = event.button();

                    this.mouseButtonEvent.x = button.x();
                    this.mouseButtonEvent.y = button.y();
                    this.mouseButtonEvent.button = button.button();
                    this.mouseButtonEvent.doubleClick = button.clicks() > 1;
                    this.mouseButtonEvent.pressed = button.down();

                    this.handlers.forEach(it -> it.onEvent(this.mouseButtonEvent));
                }
                case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
                    this.width = event.window().data1();
                    this.height = event.window().data2();

                    this.framebufferResizeEvent.width = this.width;
                    this.framebufferResizeEvent.height = this.height;

                    this.handlers.forEach(it -> it.onEvent(this.framebufferResizeEvent));
                }
                case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP -> {
                    SDL_KeyboardEvent key = event.key();

                    this.keyEvent.key = key.key();
                    this.keyEvent.pressed = key.down();

                    this.handlers.forEach(it -> it.onEvent(this.keyEvent));
                }
                case SDLEvents.SDL_EVENT_MOUSE_MOTION -> {
                    SDL_MouseMotionEvent motion = event.motion();

                    this.mouseMoveEvent.x = motion.x();
                    this.mouseMoveEvent.y = motion.y();
                    this.mouseMoveEvent.xrel = motion.xrel();
                    this.mouseMoveEvent.yrel = motion.yrel();

                    this.handlers.forEach(it -> it.onEvent(this.mouseMoveEvent));
                }
                case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> {
                    SDL_MouseWheelEvent wheel = event.wheel();

                    this.wheelEvent.x = wheel.mouse_x();
                    this.wheelEvent.y = wheel.mouse_y();
                    this.wheelEvent.amountX = wheel.x();
                    this.wheelEvent.amountY = wheel.y();

                    this.handlers.forEach(it -> it.onEvent(this.wheelEvent));
                }
                case SDLEvents.SDL_EVENT_TEXT_INPUT ->  {
                    SDL_TextInputEvent text = event.text();

                    this.inputEvent.text = text.textString();
                    IO.println(text.textString());

                    this.handlers.forEach(it -> it.onEvent(this.inputEvent));
                }
                case SDLEvents.SDL_EVENT_TEXT_EDITING -> {
                    SDL_TextEditingEvent editing = event.edit();

                    IO.println(editing.start());
                    IO.println(editing.length());
                    IO.println(editing.textString());
                }
                case SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED -> this.isFocused = true;
                case SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST -> this.isFocused = false;
                case SDLEvents.SDL_EVENT_WINDOW_MINIMIZED -> this.isMinimized = true;
                case SDLEvents.SDL_EVENT_WINDOW_RESTORED -> this.isMinimized = false;
            }
        }
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
        return this.isMinimized;
    }

    @Override
    public boolean isFocused() {
        return this.isFocused;
    }

    @Override
    public void close() {
        this.event.free();
        SDLVideo.SDL_DestroyWindow(this.handle);
        SDLInit.SDL_Quit();
    }

    static final class FramebufferResizeImpl implements Event.FramebufferResize {
        int width;
        int height;

        @Override
        public int width() {
            return this.width;
        }

        @Override
        public int height() {
            return this.height;
        }
    }

    static final class KeyImpl implements Event.Key {
        int key;
        boolean pressed;

        @Override
        public int key() {
            return this.key;
        }

        @Override
        public boolean pressed() {
            return this.pressed;
        }
    }

    static final class MouseButtonImp implements Event.MouseButton {
        float x;
        float y;
        int button;
        boolean doubleClick;
        boolean pressed;

        @Override
        public float x() {
            return this.x;
        }

        @Override
        public float y() {
            return this.y;
        }

        @Override
        public int button() {
            return this.button;
        }

        @Override
        public boolean doubleClick() {
            return this.doubleClick;
        }

        @Override
        public boolean pressed() {
            return this.pressed;
        }
    }

    static final class MouseMoveImpl implements Event.MouseMove {
        float x;
        float y;
        float xrel;
        float yrel;

        @Override
        public float x() {
            return this.x;
        }

        @Override
        public float y() {
            return this.y;
        }

        @Override
        public float xrel() {
            return this.xrel;
        }

        @Override
        public float yrel() {
            return this.yrel;
        }
    }

    static final class MouseWheelImpl implements Event.MouseWheel {
        float x;
        float y;
        float amountX;
        float amountY;

        @Override
        public float x() {
            return this.x;
        }

        @Override
        public float y() {
            return this.y;
        }

        @Override
        public float amountX() {
            return this.amountX;
        }

        @Override
        public float amountY() {
            return this.amountY;
        }
    }

    static final class TextInputImpl implements Event.TextInput {
        String text;

        @Override
        public String text() {
            return this.text;
        }
    }
}
