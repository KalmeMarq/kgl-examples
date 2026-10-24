package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.GpuTextureDimension;
import io.github.kalmemarq.kgl.api.gpu.GpuTextureFormat;
import io.github.kalmemarq.kgl.api.gpu.GpuTextureViewDimension;
import io.github.kalmemarq.kgl.api.gpu.GpuValidationException;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuBufferDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuSamplerDescriptor;
import io.github.kalmemarq.kgl.api.gpu.descriptor.GpuTextureDescriptor;

public class GlValidator {
    public static void validateCreateTexture(GlDevice device, GpuTextureDescriptor descriptor) throws GpuValidationException {
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

    public static void validateCreateSampler(GlDevice device, GpuSamplerDescriptor descriptor) throws GpuValidationException {
    }

    public static void validateCreateBuffer(GlDevice device, GpuBufferDescriptor descriptor) throws GpuValidationException {
        if (descriptor.usage() == 0) {
            throw new GpuValidationException("Buffer usage must be specified");
        }
    }
}
