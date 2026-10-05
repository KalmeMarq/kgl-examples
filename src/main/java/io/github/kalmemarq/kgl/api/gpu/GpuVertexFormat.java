package io.github.kalmemarq.kgl.api.gpu;

public enum GpuVertexFormat {
    UINT8(1, 1, true),
    UINT8X2(2, 2, true),
    UINT8X4(4, 4, true),
    SINT8(1, 1, true),
    SINT8X2(2, 2, true),
    SINT8X4(4, 4, true),
    UNORM8(true, 1, 1),
    UNORM8X2(true, 2, 2),
    UNORM8X4(true, 4, 4),
    SNORM8(true, 1, 1),
    SNORM8X2(true, 2, 2),
    SNORM8X4(true, 4, 4),
    UINT16(1, 2, true),
    UINT16X2(2, 4, true),
    UINT16X4(4, 8, true),
    SINT16(1, 2, true),
    SINT16X2(2, 4, true),
    SINT16X4(4, 8, true),
    UNORM16(true, 1, 2),
    UNORM16X2(true, 2, 4),
    UNORM16X4(true, 4, 8),
    SNORM16(true, 1, 2),
    SNORM16X2(true, 2, 4),
    SNORM16X4(true, 4, 8),
    FLOAT32(1, 4),
    FLOAT32X2(2, 8),
    FLOAT32X3(3, 12),
    FLOAT32X4(4, 16),
    UINT32(1, 4, true),
    UINT32X2(2, 8, true),
    UINT32X3(3, 12, true),
    UINT32X4(4, 16, true),
    SINT32(1, 4, true),
    SINT32X2(2, 8, true),
    SINT32X3(3, 12, true),
    SINT32X4(4, 16, true);

    public final int componentCount;
    public final int byteSize;
    public final boolean normalized;
    public final boolean isInt;

    GpuVertexFormat(int componentCount, int byteSize) {
        this(componentCount, byteSize, false);
    }

    GpuVertexFormat(boolean normalized, int componentCount, int byteSize) {
        this.componentCount = componentCount;
        this.byteSize = byteSize;
        this.normalized = normalized;
        this.isInt = false;
    }

    GpuVertexFormat(int componentCount, int byteSize, boolean isInt) {
        this.componentCount = componentCount;
        this.byteSize = byteSize;
        this.normalized = false;
        this.isInt = isInt;
    }
}
