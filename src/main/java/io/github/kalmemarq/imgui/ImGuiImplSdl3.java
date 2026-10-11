package io.github.kalmemarq.imgui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class ImGuiImplSdl3 {
    protected static class Data {
        protected long window;
        protected int windowID;
        protected long time;
        protected boolean wantUpdateMonitors;

        protected int mouseWindowID;
        protected int mouseButtonsDown;
        protected final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];
        protected long mouseLastCursor;
        protected int mousePendingLeaveFrame;
        protected boolean mouseCanUseGlobalState;
    }

    protected Data data = null;

    protected ImStrSupplier getClipboardTextFn() {
        return new ImStrSupplier() {
            @Override
            public String get() {
                if (SDLClipboard.SDL_HasClipboardText()) {
                    final String clipboardString = SDLClipboard.SDL_GetClipboardText();
                    return clipboardString != null ? clipboardString : "";
                }
                return null;
            }
        };
    }

    protected ImStrConsumer setClipboardTextFn() {
        return new ImStrConsumer() {
            @Override
            public void accept(final String text) {
                if (text != null) {
                    SDLClipboard.SDL_SetClipboardText(text);
                }
            }
        };
    }

    public boolean init(final long sdlWindow) {
        final ImGuiIO io = ImGui.getIO();

        io.addBackendFlags(ImGuiBackendFlags.HasMouseCursors);
        io.addBackendFlags(ImGuiBackendFlags.HasSetMousePos);

        this.data = new Data();
        this.data.window = sdlWindow;
        this.data.windowID = SDLVideo.SDL_GetWindowID(sdlWindow);
        this.data.mouseCanUseGlobalState = false;

        this.data.mouseCursors[ImGuiMouseCursor.Arrow] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT);
        this.data.mouseCursors[ImGuiMouseCursor.TextInput] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_TEXT);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeAll] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_MOVE);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeNS] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeEW] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeNESW] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE);
        this.data.mouseCursors[ImGuiMouseCursor.ResizeNWSE] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE);
        this.data.mouseCursors[ImGuiMouseCursor.Hand] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_POINTER);
        this.data.mouseCursors[ImGuiMouseCursor.Wait] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_WAIT);
        this.data.mouseCursors[ImGuiMouseCursor.Progress] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_PROGRESS);
        this.data.mouseCursors[ImGuiMouseCursor.NotAllowed] = SDLMouse.SDL_CreateSystemCursor(SDLMouse.SDL_SYSTEM_CURSOR_NOT_ALLOWED);

        io.setGetClipboardTextFn(this.getClipboardTextFn());
        io.setSetClipboardTextFn(this.setClipboardTextFn());

        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_MOUSE_FOCUS_CLICKTHROUGH, "1");
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_MOUSE_AUTO_CAPTURE, "0");
        SDLHints.SDL_SetHint("SDL_BORDERLESS_WINDOWED_STYLE", "0");

        return true;
    }

    public void shutdown() {
        final ImGuiIO io = ImGui.getIO();

        for (int i = 0; i < ImGuiMouseCursor.COUNT; i++) {
            if (this.data.mouseCursors[i] != 0L) {
                SDLMouse.SDL_DestroyCursor(this.data.mouseCursors[i]);
                this.data.mouseCursors[i] = 0L;
            }
        }

        io.removeBackendFlags(ImGuiBackendFlags.HasMouseCursors
                | ImGuiBackendFlags.HasSetMousePos
                | ImGuiBackendFlags.HasMouseHoveredViewport
                | ImGuiBackendFlags.HasParentViewport);

        this.data = null;
    }

    public boolean processEvent(final long sdlEventPtr) {
        final SDL_Event event = SDL_Event.create(sdlEventPtr);
        final ImGuiIO io = ImGui.getIO();
        final int type = event.type();

        return switch (type) {
            case SDLEvents.SDL_EVENT_MOUSE_MOTION -> this.processMouseMotion(event, io);
            case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> this.processMouseWheel(event, io);
            case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP ->
                    this.processMouseButton(event, io, type);
            case SDLEvents.SDL_EVENT_TEXT_INPUT -> this.processTextInput(event, io);
            case SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP -> this.processKey(event, io, type);
            case SDLEvents.SDL_EVENT_DISPLAY_ORIENTATION, SDLEvents.SDL_EVENT_DISPLAY_ADDED,
                 SDLEvents.SDL_EVENT_DISPLAY_REMOVED, SDLEvents.SDL_EVENT_DISPLAY_MOVED,
                 SDLEvents.SDL_EVENT_DISPLAY_CONTENT_SCALE_CHANGED,
                 SDLEvents.SDL_EVENT_DISPLAY_USABLE_BOUNDS_CHANGED -> {
                this.data.wantUpdateMonitors = true;
                yield true;
            }
            case SDLEvents.SDL_EVENT_WINDOW_MOUSE_ENTER -> this.processWindowMouseEnter(event);
            case SDLEvents.SDL_EVENT_WINDOW_MOUSE_LEAVE -> this.processWindowMouseLeave(event);
            case SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED, SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST ->
                    this.processWindowFocus(event, io, type);
            case SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED, SDLEvents.SDL_EVENT_WINDOW_MOVED,
                 SDLEvents.SDL_EVENT_WINDOW_RESIZED ->
                // Single-viewport: nothing to forward to ImGui; user code reacts via SDL events directly.
                // TODO: not ported — viewport->PlatformRequestClose / Move / Resize hooks are multi-viewport only.
                    true;
            default -> false;
        };
    }

    private boolean processMouseMotion(final SDL_Event event, final ImGuiIO io) {
        final SDL_MouseMotionEvent motion = event.motion();
        if (motion.windowID() != this.data.windowID) {
            return false;
        }
        float mx = motion.x();
        float my = motion.y();
        if ((io.getConfigFlags() & ImGuiConfigFlags.ViewportsEnable) != 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                final IntBuffer pwx = stack.mallocInt(1);
                final IntBuffer pwy = stack.mallocInt(1);
                SDLVideo.SDL_GetWindowPosition(this.data.window, pwx, pwy);
                mx += pwx.get(0);
                my += pwy.get(0);
            }
        }
        io.addMouseSourceEvent(motion.which() == SDLTouch.SDL_TOUCH_MOUSEID ? ImGuiMouseSource.TouchScreen : ImGuiMouseSource.Mouse);
        io.addMousePosEvent(mx, my);
        return true;
    }

    private boolean processMouseWheel(final SDL_Event event, final ImGuiIO io) {
        final SDL_MouseWheelEvent wheel = event.wheel();
        if (wheel.windowID() != this.data.windowID) {
            return false;
        }
        final float wheelX = -wheel.x();
        final float wheelY = wheel.y();
        io.addMouseSourceEvent(wheel.which() == SDLTouch.SDL_TOUCH_MOUSEID ? ImGuiMouseSource.TouchScreen : ImGuiMouseSource.Mouse);
        io.addMouseWheelEvent(wheelX, wheelY);
        return true;
    }

    private boolean processMouseButton(final SDL_Event event, final ImGuiIO io, final int type) {
        final SDL_MouseButtonEvent button = event.button();
        if (button.windowID() != this.data.windowID) {
            return false;
        }
        int mouseButton = -1;
        final int btn = button.button() & 0xFF;
        if (btn == SDLMouse.SDL_BUTTON_LEFT) {
            mouseButton = 0;
        } else if (btn == SDLMouse.SDL_BUTTON_RIGHT) {
            mouseButton = 1;
        } else if (btn == SDLMouse.SDL_BUTTON_MIDDLE) {
            mouseButton = 2;
        } else if (btn == SDLMouse.SDL_BUTTON_X1) {
            mouseButton = 3;
        } else if (btn == SDLMouse.SDL_BUTTON_X2) {
            mouseButton = 4;
        }
        if (mouseButton == -1) {
            return false;
        }
        final boolean down = type == SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
        io.addMouseSourceEvent(button.which() == SDLTouch.SDL_TOUCH_MOUSEID ? ImGuiMouseSource.TouchScreen : ImGuiMouseSource.Mouse);
        io.addMouseButtonEvent(mouseButton, down);
        if (down) {
            this.data.mouseButtonsDown |= 1 << mouseButton;
        } else {
            this.data.mouseButtonsDown &= ~(1 << mouseButton);
        }
        return true;
    }

    private boolean processTextInput(final SDL_Event event, final ImGuiIO io) {
        final SDL_TextInputEvent text = event.text();
        if (text.windowID() != this.data.windowID) {
            return false;
        }
        io.addInputCharactersUTF8(text.textString());
        return true;
    }

    private boolean processKey(final SDL_Event event, final ImGuiIO io, final int type) {
        final SDL_KeyboardEvent key = event.key();
        if (key.windowID() != this.data.windowID) {
            return false;
        }
        updateKeyModifiers(key.mod());
        final int imguiKey = keyEventToImGuiKey(key.key(), key.scancode());
        io.addKeyEvent(imguiKey, type == SDLEvents.SDL_EVENT_KEY_DOWN);
        io.setKeyEventNativeData(imguiKey, key.key(), key.scancode(), key.scancode());
        return true;
    }

    private boolean processWindowMouseEnter(final SDL_Event event) {
        final SDL_WindowEvent win = event.window();
        if (win.windowID() != this.data.windowID) {
            return false;
        }
        this.data.mouseWindowID = win.windowID();
        this.data.mousePendingLeaveFrame = 0;
        return true;
    }

    private boolean processWindowMouseLeave(final SDL_Event event) {
        final SDL_WindowEvent win = event.window();
        if (win.windowID() != this.data.windowID) {
            return false;
        }
        this.data.mousePendingLeaveFrame = ImGui.getFrameCount() + 1;
        return true;
    }

    private boolean processWindowFocus(final SDL_Event event, final ImGuiIO io, final int type) {
        final SDL_WindowEvent win = event.window();
        if (win.windowID() != this.data.windowID) {
            return false;
        }
        io.addFocusEvent(type == SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED);
        return true;
    }

    public void newFrame() {
        final ImGuiIO io = ImGui.getIO();

        // Setup main viewport size (every frame to accommodate for window resizing)
        final int[] wh = this.getWindowSize();
        final float[] fbScale = this.getFramebufferScale(wh[0], wh[1]);
        io.setDisplaySize((float) wh[0], (float) wh[1]);
        io.setDisplayFramebufferScale(fbScale[0], fbScale[1]);

        // Setup time step
        if (this.frequency == 0L) {
            this.frequency = SDLTimer.SDL_GetPerformanceFrequency();
        }
        long currentTime = SDLTimer.SDL_GetPerformanceCounter();
        if (currentTime <= this.data.time) {
            currentTime = this.data.time + 1;
        }
        io.setDeltaTime(this.data.time > 0L ? (float) ((double) (currentTime - this.data.time) / (double) this.frequency) : (1.0f / 60.0f));
        this.data.time = currentTime;

        if (this.data.mousePendingLeaveFrame != 0 && this.data.mousePendingLeaveFrame >= ImGui.getFrameCount() && this.data.mouseButtonsDown == 0) {
            this.data.mouseWindowID = 0;
            this.data.mousePendingLeaveFrame = 0;
            io.addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE);
        }

        this.updateMouseData(io);
        this.updateMouseCursor(io);
    }

    private long frequency;

    private int[] getWindowSize() {
        final int[] wh = new int[2];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pw = stack.mallocInt(1);
            final IntBuffer ph = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSize(this.data.window, pw, ph);
            wh[0] = pw.get(0);
            wh[1] = ph.get(0);
        }
        // SDL_WINDOW_MINIMIZED — when minimized, report 0,0
        if ((SDLVideo.SDL_GetWindowFlags(this.data.window) & SDLVideo.SDL_WINDOW_MINIMIZED) != 0L) {
            wh[0] = 0;
            wh[1] = 0;
        }
        return wh;
    }

    private float[] getFramebufferScale(final int w, final int h) {
        final int dw;
        final int dh;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pdw = stack.mallocInt(1);
            final IntBuffer pdh = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSizeInPixels(this.data.window, pdw, pdh);
            dw = pdw.get(0);
            dh = pdh.get(0);
        }
        final float fx = w > 0 ? (float) dw / (float) w : 1.0f;
        final float fy = h > 0 ? (float) dh / (float) h : 1.0f;
        return new float[]{fx, fy};
    }

    private void updateMouseData(final ImGuiIO io) {
        final boolean isAppFocused = (SDLVideo.SDL_GetWindowFlags(this.data.window) & SDLVideo.SDL_WINDOW_INPUT_FOCUS) != 0L;
        if (isAppFocused) {
            if (io.getWantSetMousePos()) {
                SDLMouse.SDL_WarpMouseInWindow(this.data.window, io.getMousePosX(), io.getMousePosY());
            }
            // TODO: not ported — global-state fallback (SDL_GetGlobalMouseState) for unhovered focus
            // because LWJGL's PointerBuffer/IntBuffer translation is non-trivial and the path
            // is only required when mouseCanUseGlobalState is true (multi-viewport regime).
        }
    }

    private void updateMouseCursor(final ImGuiIO io) {
        if ((io.getConfigFlags() & ImGuiConfigFlags.NoMouseCursorChange) != 0) {
            return;
        }
        final int imguiCursor = ImGui.getMouseCursor();
        if (io.getMouseDrawCursor() || imguiCursor == ImGuiMouseCursor.None) {
            SDLMouse.SDL_HideCursor();
        } else {
            final long expected = this.data.mouseCursors[imguiCursor] != 0L
                    ? this.data.mouseCursors[imguiCursor]
                    : this.data.mouseCursors[ImGuiMouseCursor.Arrow];
            if (this.data.mouseLastCursor != expected) {
                SDLMouse.SDL_SetCursor(expected);
                this.data.mouseLastCursor = expected;
            }
            SDLMouse.SDL_ShowCursor();
        }
    }

    private static void updateKeyModifiers(final int sdlKeyMods) {
        final ImGuiIO io = ImGui.getIO();
        io.addKeyEvent(ImGuiKey.ImGuiMod_Ctrl, (sdlKeyMods & SDLKeycode.SDL_KMOD_CTRL) != 0);
        io.addKeyEvent(ImGuiKey.ImGuiMod_Shift, (sdlKeyMods & SDLKeycode.SDL_KMOD_SHIFT) != 0);
        io.addKeyEvent(ImGuiKey.ImGuiMod_Alt, (sdlKeyMods & SDLKeycode.SDL_KMOD_ALT) != 0);
        io.addKeyEvent(ImGuiKey.ImGuiMod_Super, (sdlKeyMods & SDLKeycode.SDL_KMOD_GUI) != 0);
    }

    public static int keyEventToImGuiKey(final int keycode, final int scancode) {
        return switch (scancode) {
            case SDLScancode.SDL_SCANCODE_KP_0 -> ImGuiKey.Keypad0;
            case SDLScancode.SDL_SCANCODE_KP_1 -> ImGuiKey.Keypad1;
            case SDLScancode.SDL_SCANCODE_KP_2 -> ImGuiKey.Keypad2;
            case SDLScancode.SDL_SCANCODE_KP_3 -> ImGuiKey.Keypad3;
            case SDLScancode.SDL_SCANCODE_KP_4 -> ImGuiKey.Keypad4;
            case SDLScancode.SDL_SCANCODE_KP_5 -> ImGuiKey.Keypad5;
            case SDLScancode.SDL_SCANCODE_KP_6 -> ImGuiKey.Keypad6;
            case SDLScancode.SDL_SCANCODE_KP_7 -> ImGuiKey.Keypad7;
            case SDLScancode.SDL_SCANCODE_KP_8 -> ImGuiKey.Keypad8;
            case SDLScancode.SDL_SCANCODE_KP_9 -> ImGuiKey.Keypad9;
            case SDLScancode.SDL_SCANCODE_KP_PERIOD -> ImGuiKey.KeypadDecimal;
            case SDLScancode.SDL_SCANCODE_KP_DIVIDE -> ImGuiKey.KeypadDivide;
            case SDLScancode.SDL_SCANCODE_KP_MULTIPLY -> ImGuiKey.KeypadMultiply;
            case SDLScancode.SDL_SCANCODE_KP_MINUS -> ImGuiKey.KeypadSubtract;
            case SDLScancode.SDL_SCANCODE_KP_PLUS -> ImGuiKey.KeypadAdd;
            case SDLScancode.SDL_SCANCODE_KP_ENTER -> ImGuiKey.KeypadEnter;
            case SDLScancode.SDL_SCANCODE_KP_EQUALS -> ImGuiKey.KeypadEqual;
            default -> switch (keycode) {
                case SDLKeycode.SDLK_TAB -> ImGuiKey.Tab;
                case SDLKeycode.SDLK_LEFT -> ImGuiKey.LeftArrow;
                case SDLKeycode.SDLK_RIGHT -> ImGuiKey.RightArrow;
                case SDLKeycode.SDLK_UP -> ImGuiKey.UpArrow;
                case SDLKeycode.SDLK_DOWN -> ImGuiKey.DownArrow;
                case SDLKeycode.SDLK_PAGEUP -> ImGuiKey.PageUp;
                case SDLKeycode.SDLK_PAGEDOWN -> ImGuiKey.PageDown;
                case SDLKeycode.SDLK_HOME -> ImGuiKey.Home;
                case SDLKeycode.SDLK_END -> ImGuiKey.End;
                case SDLKeycode.SDLK_INSERT -> ImGuiKey.Insert;
                case SDLKeycode.SDLK_DELETE -> ImGuiKey.Delete;
                case SDLKeycode.SDLK_BACKSPACE -> ImGuiKey.Backspace;
                case SDLKeycode.SDLK_SPACE -> ImGuiKey.Space;
                case SDLKeycode.SDLK_RETURN -> ImGuiKey.Enter;
                case SDLKeycode.SDLK_ESCAPE -> ImGuiKey.Escape;
                case SDLKeycode.SDLK_COMMA -> ImGuiKey.Comma;
                case SDLKeycode.SDLK_PERIOD -> ImGuiKey.Period;
                case SDLKeycode.SDLK_SEMICOLON -> ImGuiKey.Semicolon;
                case SDLKeycode.SDLK_CAPSLOCK -> ImGuiKey.CapsLock;
                case SDLKeycode.SDLK_SCROLLLOCK -> ImGuiKey.ScrollLock;
                case SDLKeycode.SDLK_NUMLOCKCLEAR -> ImGuiKey.NumLock;
                case SDLKeycode.SDLK_PRINTSCREEN -> ImGuiKey.PrintScreen;
                case SDLKeycode.SDLK_PAUSE -> ImGuiKey.Pause;
                case SDLKeycode.SDLK_LCTRL -> ImGuiKey.LeftCtrl;
                case SDLKeycode.SDLK_LSHIFT -> ImGuiKey.LeftShift;
                case SDLKeycode.SDLK_LALT -> ImGuiKey.LeftAlt;
                case SDLKeycode.SDLK_LGUI -> ImGuiKey.LeftSuper;
                case SDLKeycode.SDLK_RCTRL -> ImGuiKey.RightCtrl;
                case SDLKeycode.SDLK_RSHIFT -> ImGuiKey.RightShift;
                case SDLKeycode.SDLK_RALT -> ImGuiKey.RightAlt;
                case SDLKeycode.SDLK_RGUI -> ImGuiKey.RightSuper;
                case SDLKeycode.SDLK_APPLICATION -> ImGuiKey.Menu;
                case SDLKeycode.SDLK_0 -> ImGuiKey._0;
                case SDLKeycode.SDLK_1 -> ImGuiKey._1;
                case SDLKeycode.SDLK_2 -> ImGuiKey._2;
                case SDLKeycode.SDLK_3 -> ImGuiKey._3;
                case SDLKeycode.SDLK_4 -> ImGuiKey._4;
                case SDLKeycode.SDLK_5 -> ImGuiKey._5;
                case SDLKeycode.SDLK_6 -> ImGuiKey._6;
                case SDLKeycode.SDLK_7 -> ImGuiKey._7;
                case SDLKeycode.SDLK_8 -> ImGuiKey._8;
                case SDLKeycode.SDLK_9 -> ImGuiKey._9;
                case SDLKeycode.SDLK_A -> ImGuiKey.A;
                case SDLKeycode.SDLK_B -> ImGuiKey.B;
                case SDLKeycode.SDLK_C -> ImGuiKey.C;
                case SDLKeycode.SDLK_D -> ImGuiKey.D;
                case SDLKeycode.SDLK_E -> ImGuiKey.E;
                case SDLKeycode.SDLK_F -> ImGuiKey.F;
                case SDLKeycode.SDLK_G -> ImGuiKey.G;
                case SDLKeycode.SDLK_H -> ImGuiKey.H;
                case SDLKeycode.SDLK_I -> ImGuiKey.I;
                case SDLKeycode.SDLK_J -> ImGuiKey.J;
                case SDLKeycode.SDLK_K -> ImGuiKey.K;
                case SDLKeycode.SDLK_L -> ImGuiKey.L;
                case SDLKeycode.SDLK_M -> ImGuiKey.M;
                case SDLKeycode.SDLK_N -> ImGuiKey.N;
                case SDLKeycode.SDLK_O -> ImGuiKey.O;
                case SDLKeycode.SDLK_P -> ImGuiKey.P;
                case SDLKeycode.SDLK_Q -> ImGuiKey.Q;
                case SDLKeycode.SDLK_R -> ImGuiKey.R;
                case SDLKeycode.SDLK_S -> ImGuiKey.S;
                case SDLKeycode.SDLK_T -> ImGuiKey.T;
                case SDLKeycode.SDLK_U -> ImGuiKey.U;
                case SDLKeycode.SDLK_V -> ImGuiKey.V;
                case SDLKeycode.SDLK_W -> ImGuiKey.W;
                case SDLKeycode.SDLK_X -> ImGuiKey.X;
                case SDLKeycode.SDLK_Y -> ImGuiKey.Y;
                case SDLKeycode.SDLK_Z -> ImGuiKey.Z;
                case SDLKeycode.SDLK_F1 -> ImGuiKey.F1;
                case SDLKeycode.SDLK_F2 -> ImGuiKey.F2;
                case SDLKeycode.SDLK_F3 -> ImGuiKey.F3;
                case SDLKeycode.SDLK_F4 -> ImGuiKey.F4;
                case SDLKeycode.SDLK_F5 -> ImGuiKey.F5;
                case SDLKeycode.SDLK_F6 -> ImGuiKey.F6;
                case SDLKeycode.SDLK_F7 -> ImGuiKey.F7;
                case SDLKeycode.SDLK_F8 -> ImGuiKey.F8;
                case SDLKeycode.SDLK_F9 -> ImGuiKey.F9;
                case SDLKeycode.SDLK_F10 -> ImGuiKey.F10;
                case SDLKeycode.SDLK_F11 -> ImGuiKey.F11;
                case SDLKeycode.SDLK_F12 -> ImGuiKey.F12;
                case SDLKeycode.SDLK_F13 -> ImGuiKey.F13;
                case SDLKeycode.SDLK_F14 -> ImGuiKey.F14;
                case SDLKeycode.SDLK_F15 -> ImGuiKey.F15;
                case SDLKeycode.SDLK_F16 -> ImGuiKey.F16;
                case SDLKeycode.SDLK_F17 -> ImGuiKey.F17;
                case SDLKeycode.SDLK_F18 -> ImGuiKey.F18;
                case SDLKeycode.SDLK_F19 -> ImGuiKey.F19;
                case SDLKeycode.SDLK_F20 -> ImGuiKey.F20;
                case SDLKeycode.SDLK_F21 -> ImGuiKey.F21;
                case SDLKeycode.SDLK_F22 -> ImGuiKey.F22;
                case SDLKeycode.SDLK_F23 -> ImGuiKey.F23;
                case SDLKeycode.SDLK_F24 -> ImGuiKey.F24;
                case SDLKeycode.SDLK_AC_BACK -> ImGuiKey.AppBack;
                case SDLKeycode.SDLK_AC_FORWARD -> ImGuiKey.AppForward;
                default -> switch (scancode) {
                    case SDLScancode.SDL_SCANCODE_GRAVE -> ImGuiKey.GraveAccent;
                    case SDLScancode.SDL_SCANCODE_MINUS -> ImGuiKey.Minus;
                    case SDLScancode.SDL_SCANCODE_EQUALS -> ImGuiKey.Equal;
                    case SDLScancode.SDL_SCANCODE_LEFTBRACKET -> ImGuiKey.LeftBracket;
                    case SDLScancode.SDL_SCANCODE_RIGHTBRACKET -> ImGuiKey.RightBracket;
                    case SDLScancode.SDL_SCANCODE_NONUSBACKSLASH -> ImGuiKey.Oem102;
                    case SDLScancode.SDL_SCANCODE_BACKSLASH -> ImGuiKey.Backslash;
                    case SDLScancode.SDL_SCANCODE_SEMICOLON -> ImGuiKey.Semicolon;
                    case SDLScancode.SDL_SCANCODE_APOSTROPHE -> ImGuiKey.Apostrophe;
                    case SDLScancode.SDL_SCANCODE_COMMA -> ImGuiKey.Comma;
                    case SDLScancode.SDL_SCANCODE_PERIOD -> ImGuiKey.Period;
                    case SDLScancode.SDL_SCANCODE_SLASH -> ImGuiKey.Slash;
                    default -> ImGuiKey.None;
                };
            };
        };
    }
}
