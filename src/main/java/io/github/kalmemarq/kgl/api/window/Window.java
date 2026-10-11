package io.github.kalmemarq.kgl.api.window;

import io.github.kalmemarq.kgl.api.gpu.GpuBackend;
import io.github.kalmemarq.kgl.util.image.Image;

import java.util.Optional;
import java.util.ServiceLoader;

public interface Window extends AutoCloseable {
    void init(WindowConfig config, GpuBackend backend);

    void setIcon(Image... images);
    void addEventHandler(EventHandler handler);
    void removeEventHandler(EventHandler handler);
    void show();
    void hide();
    void toggleFullscreen();
    void setFullscreen(boolean fullscreen);
    void lockMouse();
    void unlockMouse();
    boolean isMouseLocked();
    void pollEvents();
    long handle();
    int width();
    int height();
    boolean isMinimized();
    boolean isFocused();

    void setClipboardText(String text);
    String getClipboardText();

    @Override
    void close();

    static Optional<Window> create(WindowConfig config, GpuBackend backend) {
        ServiceLoader<Window> providers = ServiceLoader.load(Window.class);

        for (Window provider : providers) {
            provider.init(config, backend);
            return Optional.of(provider);
        }

        return Optional.empty();
    }
}
