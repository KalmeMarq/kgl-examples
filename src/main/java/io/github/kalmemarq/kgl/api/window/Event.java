package io.github.kalmemarq.kgl.api.window;

public interface Event {
    record CloseRequested() implements Event {
        public static final Event INSTANCE = new CloseRequested();
    }

    record Quit() implements Event {
        public static final Event INSTANCE = new Quit();
    }

    interface FramebufferResize extends Event {
        int width();
        int height();
    }

    interface Key extends Event {
        int key();
        boolean pressed();
    }

    interface MouseButton extends Event {
        float x();
        float y();
        int button();
        boolean doubleClick();
        boolean pressed();
    }

    interface MouseMove extends Event {
        float x();
        float y();
        float xrel();
        float yrel();
    }

    interface MouseWheel extends Event {
        float x();
        float y();
        float amountX();
        float amountY();
    }

    interface TextInput extends Event {
        String text();
    }
}
