package io.github.kalmemarq.kgl.api.gpu;

import io.github.kalmemarq.kgl.api.window.WindowHints;

import java.util.Optional;
import java.util.ServiceLoader;

public interface GpuBackend {
    String getName();
    boolean isSupported();

    void setWindowHints(WindowHints hints);

    GpuDevice createDevice(long windowHandle, boolean validation);
    GpuSurface createSurface(long windowHandle);

    static Optional<GpuBackend> getFirstSupportedBackend() {
        ServiceLoader<GpuBackend> backends = ServiceLoader.load(GpuBackend.class);

        for (GpuBackend backend : backends) {
            if (backend.isSupported()) {
                return Optional.of(backend);
            }
        }

        return Optional.empty();
    }
}
