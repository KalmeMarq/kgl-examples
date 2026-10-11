package io.github.kalmemarq.kgl.api.window;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WindowHints {
    private final Map<Hint<?>, Object> hints = new HashMap<>();

    public <T> void set(Hint<T> hint, T value) {
        this.hints.put(hint, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Hint<T> hint) {
        return Optional.ofNullable((T) this.hints.get(hint));
    }

    public static class Hint<T> {
        public Hint() {
        }

        public static final Hint<Boolean> GL = new Hint<>();
        public static final Hint<Boolean> GL_DEBUG = new Hint<>();
        public static final Hint<Boolean> GL_DOUBLEBUFFER = new Hint<>();
        public static final Hint<Boolean> GL_FORWARD_COMPATIBLE = new Hint<>();
        public static final Hint<Integer> GL_MAJOR = new Hint<>();
        public static final Hint<Integer> GL_MINOR = new Hint<>();
        public static final Hint<Boolean> GL_CORE = new Hint<>();
        public static final Hint<Integer> GL_DEPTH_BITS = new Hint<>();
        public static final Hint<Integer> GL_STENCIL_BITS = new Hint<>();
    }
}
