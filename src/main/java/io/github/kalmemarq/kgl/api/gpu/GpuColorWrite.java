package io.github.kalmemarq.kgl.api.gpu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE_USE)
public @interface GpuColorWrite {
    int NONE = 0x0;
    int RED = 0x1;
    int GREEN = 0x2;
    int BLUE = 0x4;
    int ALPHA = 0x8;
    int ALL = 0xF;
}
