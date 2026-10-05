package io.github.kalmemarq.kgl.api.gpu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE_USE)
public @interface GpuBufferUsage {
    /// The buffer can be used as the source of a copy operation.
    int COPY_SRC = 0x0004;
    /// The buffer can be used as the destination of a copy or write operation.
    int COPY_DST = 0x0008;
    /// The buffer can be used as an index buffer.
    int INDEX = 0x0010;
    /// The buffer can be used as a vertex buffer.
    int VERTEX = 0x0020;
    // The buffer can be used as a uniform buffer.
    int UNIFORM = 0x0040;
    /// The buffer can be used as to store indirect command arguments.
    int INDIRECT = 0x0100;
}
