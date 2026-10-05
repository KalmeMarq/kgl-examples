package io.github.kalmemarq.kgl.api.gpu;

public interface GpuSurface {
    void configure(PresentMode presentMode);

    void present();

    enum PresentMode {
        IMMEDIATE,
        FIFO,
        FIFO_RELAXED,
        MAILBOX
    }
}
