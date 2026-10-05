package io.github.kalmemarq.kgl.api.gpu;

public record GpuVertexBufferLayout(int arrayStride, GpuVertexStepMode stepMode, GpuVertexAttribute... attributes) {
    public GpuVertexBufferLayout(GpuVertexStepMode stepMode, GpuVertexFormat... attributeFormats) {
        GpuVertexAttribute[] attributes = new GpuVertexAttribute[attributeFormats.length];
        int arrayStride = 0;

        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = new GpuVertexAttribute(attributeFormats[i], arrayStride, i);
            arrayStride += attributes[i].format().byteSize;
        }

        this(arrayStride, stepMode, attributes);
    }

    public GpuVertexBufferLayout(GpuVertexFormat... attributeFormats) {
        this(GpuVertexStepMode.VERTEX, attributeFormats);
    }
}
