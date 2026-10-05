package io.github.kalmemarq.kgl.api.gpu;

import java.util.Optional;
import java.util.ServiceLoader;

public interface GpuBackend {
    String getName();
    boolean isSupported();

    void setWindowHints(int props);

    GpuDevice createDevice(long windowHandle);
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
