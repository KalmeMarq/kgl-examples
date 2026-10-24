package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import org.lwjgl.opengl.*;

class GlUtils {

    static int toGlEnum(GpuAddressMode mode) {
        return switch (mode) {
            case REPEAT -> GL11C.GL_REPEAT;
            case CLAMP_TO_EDGE -> GL12C.GL_CLAMP_TO_EDGE;
        };
    }

    static int toGlEnum(GpuFilterMode mode) {
        return switch (mode) {
            case NEAREST -> GL11C.GL_NEAREST;
            case LINEAR -> GL11C.GL_LINEAR;
        };
    }

    static int toGlEnum(GpuPrimitiveTopology topology) {
        return switch (topology) {
            case LINE_LIST -> GL11C.GL_LINES;
            case LINE_STRIP -> GL11C.GL_LINE_STRIP;
            case TRIANGLE_LIST -> GL11C.GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL11C.GL_TRIANGLE_STRIP;
        };
    }

    static int toGlEnum(GpuTextureDimension dimension) {
        return switch (dimension) {
            case D1 -> GL11C.GL_TEXTURE_1D;
            case D2 -> GL11C.GL_TEXTURE_2D;
            case D3 -> GL12C.GL_TEXTURE_3D;
        };
    }

    static int toGlEnum(GpuTextureViewDimension textureViewDimension) {
        return switch (textureViewDimension) {
            case D1 -> GL11C.GL_TEXTURE_1D;
            case D2 -> GL11C.GL_TEXTURE_2D;
            case D2_ARRAY -> GL30C.GL_TEXTURE_2D_ARRAY;
            case CUBE -> GL13C.GL_TEXTURE_CUBE_MAP;
            case CUBE_ARRAY -> GL40C.GL_TEXTURE_CUBE_MAP_ARRAY;
            case D3 -> GL12C.GL_TEXTURE_3D;
        };
    }

    static int toGlEnum(GpuTextureFormat format) {
        return switch (format) {
            case RGBA8UNORM -> GL11C.GL_RGBA8;
            case DEPTH32FLOAT -> GL30C.GL_DEPTH_COMPONENT32F;
        };
    }

    static int toGlEnum(GpuVertexFormat format) {
        return switch (format) {
            case UINT8, UINT8X2, UINT8X4, UNORM8, UNORM8X2, UNORM8X4 -> GL11C.GL_UNSIGNED_BYTE;
            case SINT8, SINT8X2, SINT8X4, SNORM8, SNORM8X2, SNORM8X4 -> GL11C.GL_BYTE;
            case UINT16, UINT16X2, UINT16X4, UNORM16, UNORM16X2, UNORM16X4 -> GL11C.GL_UNSIGNED_SHORT;
            case SINT16, SINT16X2, SINT16X4, SNORM16, SNORM16X2, SNORM16X4 -> GL11C.GL_SHORT;
            case FLOAT32, FLOAT32X2, FLOAT32X3, FLOAT32X4 -> GL11C.GL_FLOAT;
            case UINT32, UINT32X2, UINT32X3, UINT32X4 -> GL11C.GL_UNSIGNED_INT;
            case SINT32, SINT32X2, SINT32X3, SINT32X4 -> GL11C.GL_INT;
        };
    }

    static int toGlEnum(GpuCompareFunction function) {
        return switch (function) {
            case NEVER -> GL11C.GL_NEVER;
            case LESS -> GL11C.GL_LESS;
            case EQUAL -> GL11C.GL_EQUAL;
            case LESS_EQUAL -> GL11C.GL_LEQUAL;
            case GREATER -> GL11C.GL_GREATER;
            case NOT_EQUAL -> GL11C.GL_NOTEQUAL;
            case GREATER_EQUAL -> GL11C.GL_GEQUAL;
            case ALWAYS -> GL11C.GL_ALWAYS;
        };
    }

    static int toGlEnum(GpuBlendOperation operation) {
        return switch (operation) {
            case ADD -> GL14C.GL_FUNC_ADD;
            case SUBTRACT -> GL14C.GL_FUNC_SUBTRACT;
            case REVERSE_SUBTRACT -> GL14C.GL_FUNC_REVERSE_SUBTRACT;
        };
    }

    static int toGlEnum(GpuBlendFactor factor) {
        return switch (factor) {
            case ZERO -> GL11C.GL_ZERO;
            case ONE -> GL11C.GL_ONE;
            case SRC_ALPHA -> GL11C.GL_SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA -> GL11C.GL_ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA -> GL11C.GL_DST_ALPHA;
            case ONE_MINUS_DST_ALPHA -> GL11C.GL_ONE_MINUS_DST_ALPHA;
        };
    }

    static int toGlEnum(GpuIndexFormat format) {
        return switch (format) {
            case UINT16 -> GL11C.GL_UNSIGNED_SHORT;
            case UINT32 -> GL11C.GL_UNSIGNED_INT;
        };
    }
}
