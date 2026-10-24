package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuRenderPassDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuSamplerDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuTextureDescriptor;

import java.nio.ByteBuffer;

class GlValidator {
    static void validateCreateTexture(GlDevice device, GpuTextureDescriptor descriptor) throws GpuValidationException {
        if (descriptor.usage() == 0) {
            throw new GpuValidationException("Texture usage must be specified");
        }

        if (descriptor.width() <= 0) {
            throw new GpuValidationException("Texture width (" + descriptor.width() + ") must be > 0");
        }

        if (descriptor.height() <= 0) {
            throw new GpuValidationException("Texture height (" + descriptor.height() + ") must be > 0");
        }

        if (descriptor.depthOrLayers() <= 0) {
            throw new GpuValidationException("Texture depthOrLayers (" + descriptor.depthOrLayers() + ") must be > 0");
        }

        if (descriptor.dimension() == GpuTextureDimension.D1) {
            if (descriptor.width() > device.limits().maxTextureDimension1D()) {
                throw new GpuValidationException("Texture width (" + descriptor.width() + ") exceeds max 1D texture dimension");
            }

            if (descriptor.height() != 1 || descriptor.depthOrLayers() != 1) {
                throw new GpuValidationException("Texture height (" + descriptor.height() + ") and depthOrLayers (" + descriptor.depthOrLayers() + ") must be 1");
            }

            if (descriptor.format() == GpuTextureFormat.DEPTH32FLOAT) {
                throw new GpuValidationException("Texture format must not be a depth or stencil format");
            }
        } else if (descriptor.dimension() == GpuTextureDimension.D2) {
            if (descriptor.width() > device.limits().maxTextureDimension2D()) {
                throw new GpuValidationException("Texture width (" + descriptor.width() + ") exceeds max 2D texture dimension (" + device.limits().maxTextureDimension2D() + ")");
            }

            if (descriptor.height() > device.limits().maxTextureDimension2D()) {
                throw new GpuValidationException("Texture height (" + descriptor.height() + ") exceeds max 2D texture dimension (" + device.limits().maxTextureDimension2D() + ")");
            }

            if (descriptor.textureBindingViewDimension() != null && descriptor.textureBindingViewDimension() != GpuTextureViewDimension.D2 && descriptor.depthOrLayers() == 1) {
                throw new GpuValidationException("2D texture may only have a 2D texture view if depthOrLayers equals 1");
            }
        } else if (descriptor.dimension() == GpuTextureDimension.D3) {
            if (descriptor.width() > device.limits().maxTextureDimension3D()) {
                throw new GpuValidationException("Texture width (" + descriptor.width() + ") exceeds max 3D texture dimension (" + device.limits().maxTextureDimension3D() + ")");
            }

            if (descriptor.height() > device.limits().maxTextureDimension3D()) {
                throw new GpuValidationException("Texture height (" + descriptor.width() + ") exceeds max 3D texture dimension (" + device.limits().maxTextureDimension3D() + ")");
            }

            if (descriptor.depthOrLayers() > device.limits().maxTextureDimension3D()) {
                throw new GpuValidationException("Texture depthOrLayers (" + descriptor.width() + ") exceeds max 3D texture dimension (" + device.limits().maxTextureDimension3D() + ")");
            }

            if (descriptor.textureBindingViewDimension() != null && descriptor.textureBindingViewDimension() != GpuTextureViewDimension.D3) {
                throw new GpuValidationException("3D texture may only have a 3D texture view");
            }
        }
    }

    static void validateCreateSampler(GlDevice device, GpuSamplerDescriptor descriptor) throws GpuValidationException {
    }

    static void validateCreateBuffer(GlDevice device, GpuBufferDescriptor descriptor) throws GpuValidationException {
        if (descriptor.usage() == 0) {
            throw new GpuValidationException("Buffer usage must be specified");
        }
    }

    static void validateWriteToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data) throws GpuValidationException {
        if ((bufferSlice.buffer().usage() & GpuBufferUsage.COPY_DST) == 0) {
            throw new GpuValidationException("Buffer usage does not include COPY_DST");
        }

        if (data.remaining() > bufferSlice.size()) {
            throw new GpuValidationException("Write exceeds buffer bounds");
        }
    }

    static void validateCopyBufferToBuffer(GpuBufferSlice src, GpuBufferSlice dst) throws GpuValidationException {
        if ((src.buffer().usage() & GpuBufferUsage.COPY_DST) == 0) {
            throw new GpuValidationException("Source buffer usage does not include COPY_SRC");
        }

        if ((dst.buffer().usage() & GpuBufferUsage.COPY_DST) == 0) {
            throw new GpuValidationException("Destination buffer usage does not include COPY_DST");
        }
    }

    static void validateSetPipeline(GlRenderPass pass, GpuRenderPipeline pipeline) throws GpuValidationException {
        GpuColorTargetState[] targets = pipeline.colorTargets();
        GpuRenderPassDescriptor.ColorAttachment[] colors = pass.descriptor.colorAttachments();

        int targetCount = targets == null ? 0 : targets.length;
        if (targetCount != colors.length) {
            throw new GpuValidationException("Pipeline has " + targetCount + " color targets but pass has " + colors.length);
        }
    }
}
