package io.github.kalmemarq.kgl.api.gpu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE_USE)
public @interface GpuTextureUsage {
    int COPY_SRC = 0x01;
    int COPY_DST = 0x02;
    int TEXTURE_BINDING = 0x04;
    int RENDER_ATTACHMENT = 0x10;
}
