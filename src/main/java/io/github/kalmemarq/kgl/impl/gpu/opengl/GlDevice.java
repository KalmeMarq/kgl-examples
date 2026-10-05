package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import it.unimi.dsi.fastutil.objects.*;
import org.joml.Vector4ic;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlDevice implements GpuDevice {
    private final long context;
    private final GpuDeviceLimits limits;

    private final int[] _boundTextures;
    private final int[] _boundSamplers;
    private boolean _scissorEnabled;
    private boolean _cullEnabled;
    private boolean _blendEnabled;
    private boolean _depthTestEnabled;
    private boolean _depthMaskEnabled;
    private GlRenderPipeline _lastPipeline;
    private final Map<GpuVertexState, Integer> vertexStateToVao;
    private final int fbo;

    private final Reference2ObjectMap<GpuRenderPipeline, Object2IntMap<String>> pipelineToUniforms;

    public GlDevice(long windowHandle) {
        this.context = SDLVideo.SDL_GL_CreateContext(windowHandle);
        SDLVideo.SDL_GL_MakeCurrent(windowHandle, this.context);
        GL.create(SDLVideo::SDL_GL_GetProcAddress);
        GL.createCapabilities();

        GLUtil.setupDebugMessageCallback(System.err);
        GL45C.glDebugMessageControl(GL45C.GL_DEBUG_SOURCE_API, GL45C.GL_DEBUG_TYPE_OTHER, GL45C.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);

        this.limits = new GpuDeviceLimits(
                GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_SIZE),
                GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_SIZE),
                GL45C.glGetInteger(GL45C.GL_MAX_3D_TEXTURE_SIZE),
                GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_ATTRIBS)
        );

        this.vertexStateToVao = new HashMap<>();
        this._boundTextures = new int[]{-1, -1, -1};
        this._boundSamplers = new int[]{-1, -1, -1};
        this.fbo = GL45C.glCreateFramebuffers();
        this.pipelineToUniforms = new Reference2ObjectOpenHashMap<>();
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, @GpuTextureUsage int usage) {
        return this.createTexture(label, dimension, format, width, 1, 1, usage, null);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, @GpuTextureUsage int usage) {
        return this.createTexture(label, dimension, format, width, height, 1, usage, null);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, int depthOrLayers, @GpuTextureUsage int usage) {
        return this.createTexture(label, dimension, format, width, height, depthOrLayers, usage, null);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, GpuTextureDimension dimension, GpuTextureFormat format, int width, int height, int depthOrLayers, @GpuTextureUsage int usage, @Nullable GpuTextureViewDimension textureBindingViewDimension) {
        if (width <= 0) {
            throw new GpuValidationException("Texture width must be > 0");
        }

        if (height <= 0) {
            throw new GpuValidationException("Texture height must be > 0");
        }

        switch (dimension) {
            case D1 -> {
                if (height != 1)
                    throw new GpuValidationException("1D texture height must be 1");

                if (width > this.limits.maxTextureDimension1D())
                    throw new GpuValidationException("1D texture width must be > 0 and <= " + this.limits.maxTextureDimension1D());

                if (textureBindingViewDimension != null && textureBindingViewDimension != GpuTextureViewDimension.D1) {
                    throw new GpuValidationException("1D texture may only have a 1D texture view");
                }
            }
            case D2 -> {
                if (width > this.limits.maxTextureDimension2D())
                    throw new GpuValidationException("2D texture width must be > 0 and <= " + this.limits.maxTextureDimension2D());

                if (height > this.limits.maxTextureDimension2D())
                    throw new GpuValidationException("2D texture height must be > 0 and <= " + this.limits.maxTextureDimension2D());

                if (textureBindingViewDimension != null && textureBindingViewDimension != GpuTextureViewDimension.D2 && depthOrLayers == 1) {
                    throw new GpuValidationException("2D texture may only have a 2D texture view if depthOrLayers equals 1");
                }
            }
            case D3 -> {
                if (width > this.limits.maxTextureDimension3D())
                    throw new GpuValidationException("3D texture width must be > 0 and <= " + this.limits.maxTextureDimension3D());

                if (height > this.limits.maxTextureDimension3D())
                    throw new GpuValidationException("3D texture height must be > 0 and <= " + this.limits.maxTextureDimension3D());

                if (textureBindingViewDimension != null && textureBindingViewDimension != GpuTextureViewDimension.D3) {
                    throw new GpuValidationException("3D texture may only have a 3D texture view");
                }
            }
        }

        int handle = GL45C.glCreateTextures(textureBindingViewDimension == null ? toGlEnum(dimension) : toGlEnum(textureBindingViewDimension));

        if (label != null) KHRDebug.glObjectLabel(GL45C.GL_TEXTURE, handle, label);

        switch (dimension) {
            case D1 -> GL45C.glTextureStorage1D(handle, 1, toGlEnum(format), width);
            case D2 -> GL45C.glTextureStorage2D(handle, 1, toGlEnum(format), width, height);
            case D3 -> GL45C.glTextureStorage3D(handle, 1, toGlEnum(format), width, height, depthOrLayers);
        }
        return new GlTexture(handle, dimension, format, usage, width, height, depthOrLayers, textureBindingViewDimension);
    }

    @Override
    public GpuSampler createSampler(@Nullable String label, GpuAddressMode addressModeUV, GpuFilterMode minMagFilter) {
        return this.createSampler(label, addressModeUV, addressModeUV, minMagFilter, minMagFilter);
    }

    @Override
    public GpuSampler createSampler(@Nullable String label, GpuAddressMode addressModeU, GpuAddressMode addressModeV, GpuFilterMode minFilter, GpuFilterMode magFilter) {
        int handle = GL45C.glCreateSamplers();
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_S, toGlEnum(addressModeU));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_T, toGlEnum(addressModeV));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_MIN_FILTER, toGlEnum(minFilter));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_MAG_FILTER, toGlEnum(magFilter));

        if (label != null) KHRDebug.glObjectLabel(GL45C.GL_SAMPLER, handle, label);

        return new GlSampler(handle, addressModeU, addressModeV, minFilter, magFilter);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable String label, long size, @GpuBufferUsage int usage) {
        int handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, size, GL45C.GL_DYNAMIC_STORAGE_BIT);

        if (label != null) KHRDebug.glObjectLabel(KHRDebug.GL_BUFFER, handle, label);

        return new GlBuffer(handle, size, usage);
    }

    @Override
    public GpuRenderPipeline createRenderPipeline(String vertexSrc, String fragmentSrc, GpuPrimitiveState primitiveState, GpuDepthStencilState depthStencilState, GpuVertexState vertexState, GpuBindGroupLayout bindGroupLayout, GpuColorTargetState[] colorTargets) {
        int vertexShader = GL45C.glCreateShaderProgramv(GL45C.GL_VERTEX_SHADER, vertexSrc);
        int fragmentShader = GL45C.glCreateShaderProgramv(GL45C.GL_FRAGMENT_SHADER, fragmentSrc);
        int shaderPipeline = GL45C.glCreateProgramPipelines();
        GL45C.glUseProgramStages(shaderPipeline, GL45C.GL_VERTEX_SHADER_BIT, vertexShader);
        GL45C.glUseProgramStages(shaderPipeline, GL45C.GL_FRAGMENT_SHADER_BIT, fragmentShader);
        if (!GL45C.glGetProgramInfoLog(vertexShader).isEmpty()) IO.println(GL45C.glGetProgramInfoLog(vertexShader));
        if (!GL45C.glGetProgramInfoLog(fragmentShader).isEmpty()) IO.println(GL45C.glGetProgramInfoLog(fragmentShader));
        return new GlRenderPipeline(shaderPipeline, vertexShader, fragmentShader, primitiveState, depthStencilState, vertexState, bindGroupLayout, colorTargets);
    }

    @Override
    public GpuRenderPass createRenderPass(GpuTexture colorTexture, @Nullable GpuTexture depthStencilTexture) {
        return new GlRenderPass(this, (GlTexture) colorTexture, (GlTexture) depthStencilTexture);
    }

    @Override
    public void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data) {
        GlBuffer buffer = (GlBuffer) bufferSlice.buffer();

        if ((buffer.usage() & GpuBufferUsage.COPY_DST) == 0) {
            throw new GpuValidationException("Buffer usage does not include COPY_DST");
        }

        if (data.remaining() > bufferSlice.size()) {
            throw new GpuValidationException("Write exceeds buffer bounds");
        }

        GL45C.glNamedBufferSubData(buffer.handle(), bufferSlice.offset(), data);
    }

    @Override
    public void copyToBuffer(GpuBufferSlice src, GpuBufferSlice dst) {
        GlBuffer srcBuffer = (GlBuffer) src.buffer();
        GlBuffer dstBuffer = (GlBuffer) dst.buffer();

        if ((srcBuffer.usage() & GpuBufferUsage.COPY_DST) == 0) {
            throw new GpuValidationException("Source buffer usage does not include COPY_SRC");
        }

        if ((dstBuffer.usage() & GpuBufferUsage.COPY_DST) == 0) {
            throw new GpuValidationException("Destination buffer usage does not include COPY_DST");
        }

        GL45C.glCopyNamedBufferSubData(srcBuffer.handle(), dstBuffer.handle(), src.offset(), dst.offset(), src.size());
    }

    @Override
    public void writeToTexture(GpuTexture texture, ByteBuffer data) {
        GL45C.glTextureSubImage2D(((GlTexture) texture).handle(), 0, 0, 0, texture.width(), texture.height(), GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, data);
    }

    @Override
    public void writeToTexture(GpuTexture texture, ByteBuffer data, int dstX, int dstY, int dstWidth, int dstHeight) {
        GL45C.glTextureSubImage2D(((GlTexture) texture).handle(), 0, dstX, dstY, dstWidth, dstHeight, GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, data);
    }

    @Override
    public void copyBufferToTexture(GpuBufferSlice src, int srcX, int srcY, int srcWidth, int srcHeight, GpuTexture dst, int dstX, int dstY, int dstWidth, int dstHeight, int layer) {
        GlTexture glTexture = (GlTexture) dst;

        GL45C.glPixelStorei(GL45C.GL_UNPACK_ROW_LENGTH, srcWidth);
        GL45C.glPixelStorei(GL45C.GL_UNPACK_IMAGE_HEIGHT, srcHeight);
        GL45C.glPixelStorei(GL45C.GL_UNPACK_SKIP_PIXELS, 0);
        GL45C.glPixelStorei(GL45C.GL_UNPACK_SKIP_ROWS, 0);
        GL45C.glPixelStorei(GL45C.GL_UNPACK_ALIGNMENT, 4);

        // Bind PBO (still required for pixel transfers)
        GL45C.glBindBuffer(GL45C.GL_PIXEL_UNPACK_BUFFER, ((GlBuffer) src).handle());

        if (dst.dimension() == GpuTextureDimension.D3 || dst.textureBindingViewDimension() == GpuTextureViewDimension.CUBE) {
            GL45C.glTextureSubImage3D(glTexture.handle(), 0, dstX, dstY, layer, dstWidth, dstHeight, 1, GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, 0);
        } else if (dst.dimension() == GpuTextureDimension.D2) {
            GL45C.glTextureSubImage2D(glTexture.handle(), 0, dstX, dstY, dstWidth, dstHeight, GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, 0);
        }

        GL45C.glBindBuffer(GL45C.GL_PIXEL_UNPACK_BUFFER, 0);
    }

    @Override
    public void clearColorTexture(GpuTexture texture, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, ((GlTexture) texture).handle(), 0);
        GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_COLOR, 0, stack.floats(r, g, b, a));
        }
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, double clearDepth) {
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, ((GlTexture) texture).handle(), 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_DEPTH, 0, stack.floats((float) clearDepth));
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, double clearDepth) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;

        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, ((GlTexture) colorTexture).handle(), 0);
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, ((GlTexture) depthTexture).handle(), 0);

        GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_COLOR, 0, stack.floats(r, g, b, a));
            GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_DEPTH, 0, stack.floats((float) clearDepth));
        }
    }

    @Override
    public void blit(GpuTexture texture) {
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, ((GlTexture) texture).handle(), 0);
        GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);
        GL45C.glBlitNamedFramebuffer(this.fbo, 0, 0, 0, texture.width(), texture.height(), 0, 0, texture.width(), texture.height(), GL45C.GL_COLOR_BUFFER_BIT, GL45C.GL_NEAREST);
    }

    @Override
    public void close() {
        SDLVideo.SDL_GL_DestroyContext(this.context);
    }

    void drawIndexed(GlRenderPass renderPass, int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance) {
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, renderPass.color.handle(), 0);
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, renderPass.depth != null ? renderPass.depth.handle() : 0, 0);
        GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);
        GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, this.fbo);

        this.setupResourcesAndPipelines(renderPass);

        int vao = this.getVertexArrayBuffer(renderPass.pipeline.vertexState());
        for (int i = 0; i < renderPass.pipeline.vertexState().buffers().length; ++i) {
            GpuVertexBufferLayout layout = renderPass.pipeline.vertexState().buffers()[i];

            if (renderPass.vertexBuffers[i] != null) {
                GL45C.glVertexArrayVertexBuffer(vao, i, renderPass.vertexBuffers[i].handle(), 0, layout.arrayStride());
            } else {
                GL45C.glVertexArrayVertexBuffer(vao, i, 0, 0, 0);
            }
        }
        if (renderPass.indexBuffer != null) {
            GL45C.glVertexArrayElementBuffer(vao, renderPass.indexBuffer.handle());
        } else {
            GL45C.glVertexArrayElementBuffer(vao, 0);
        }
        GL45C.glBindVertexArray(vao);

        if (instanceCount == 1 && baseVertex == 0 && baseInstance == 0) {
            GL45C.glDrawElements(toGlEnum(renderPass.pipeline.primitiveState().topology()), indexCount, GL45C.GL_UNSIGNED_INT, baseIndex);
        } else if (baseInstance == 0) {
            GL45C.glDrawElementsInstancedBaseVertex(toGlEnum(renderPass.pipeline.primitiveState().topology()), indexCount, GL45C.GL_UNSIGNED_INT, baseIndex, instanceCount, baseVertex);
        } else {
            GL45C.glDrawElementsInstancedBaseVertexBaseInstance(toGlEnum(renderPass.pipeline.primitiveState().topology()), indexCount, GL45C.GL_UNSIGNED_INT, baseIndex, instanceCount, baseVertex, baseInstance);
        }

        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, 0, 0);
        GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, 0);
        GL45C.glColorMask(true, true, true, true);
    }

    void draw(GlRenderPass renderPass, int count) {
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, renderPass.color.handle(), 0);
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, renderPass.depth != null ? renderPass.depth.handle() : 0, 0);
        GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);

        GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, this.fbo);

        this.setupResourcesAndPipelines(renderPass);

        int vao = this.getVertexArrayBuffer(renderPass.pipeline.vertexState());
        for (int i = 0; i < renderPass.pipeline.vertexState().buffers().length; ++i) {
            GpuVertexBufferLayout layout = renderPass.pipeline.vertexState().buffers()[i];

            if (renderPass.vertexBuffers[i] != null) {
                GL45C.glVertexArrayVertexBuffer(vao, i, renderPass.vertexBuffers[i].handle(), 0, layout.arrayStride());
            } else {
                GL45C.glVertexArrayVertexBuffer(vao, i, 0, 0, 0);
            }
        }
        GL45C.glBindVertexArray(vao);

        GL45C.glDrawArrays(toGlEnum(renderPass.pipeline.primitiveState().topology()), 0, count);

        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, 0, 0);
        GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, 0);
        GL45C.glColorMask(true, true, true, true);
    }

    private void setupResourcesAndPipelines(GlRenderPass renderPass) {
        if (this._lastPipeline != renderPass.pipeline && !this.pipelineToUniforms.containsKey(renderPass.pipeline)) {
            GpuBindGroupLayout layout = renderPass.pipeline.bindGroupLayout();
            Object2IntMap<String> uniformToBinding = new Object2IntOpenHashMap<>();

            List<Map.Entry<String, GpuBindGroupLayoutEntry>> ubo = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.Buffer).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();
            List<Map.Entry<String, GpuBindGroupLayoutEntry>> txr = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.Texture).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();
            List<Map.Entry<String, GpuBindGroupLayoutEntry>> smp = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.Sampler).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();

            int uboIdx = -1;

            for (var entry : ubo) {
                String name = entry.getKey();
                GpuBindGroupLayoutEntry l = entry.getValue();
                uboIdx = uboIdx == -1 ? l.binding() : uboIdx + 1;

                uniformToBinding.put(name, uboIdx);
            }

            int txrIdx = -1;

            for (var entry : txr) {
                String name = entry.getKey();
                GpuBindGroupLayoutEntry l = entry.getValue();
                txrIdx = txrIdx == -1 ? 0 : txrIdx + 1;

                uniformToBinding.put(name, txrIdx + 100);
            }

            int smpIdx = -1;

            for (var entry : smp) {
                String name = entry.getKey();
                GpuBindGroupLayoutEntry l = entry.getValue();
                smpIdx = smpIdx == -1 ? 0 : smpIdx + 1;

                uniformToBinding.put(name, smpIdx + 200);
            }

            this.pipelineToUniforms.put(renderPass.pipeline, uniformToBinding);
        }

        if (renderPass.resources != null) {
            Object2IntMap<String> uniformToBinding = this.pipelineToUniforms.get(renderPass.pipeline);

            for (var entry : renderPass.resources.entrySet()) {
                int binding = uniformToBinding.getOrDefault(entry.getKey(), -1);

                if (binding == -1) {
                    continue;
                }

                if (binding < 100) {
                    GL45C.glBindBufferBase(GL45C.GL_UNIFORM_BUFFER, binding, ((GlBuffer) entry.getValue()).handle());
                } else if (binding < 200) {
                    binding -= 100;
                    GlRenderPass.TextureAndSampler texSamp = (GlRenderPass.TextureAndSampler) entry.getValue();

                    int textureId = texSamp.texture().handle();
                    int samplerId = texSamp.sampler().handle();

                    if (this._boundTextures[binding] != textureId) {
                        GL45C.glBindTextureUnit(binding, texSamp.texture().handle());
                        this._boundTextures[binding] = textureId;
                    }

                    if (this._boundSamplers[binding] != samplerId) {
                        GL45C.glBindSampler(binding, samplerId);
                        this._boundSamplers[binding] = samplerId;
                    }
                }
            }
        }

        if (this._lastPipeline != renderPass.pipeline) {
            this.applyPipelineState(renderPass.pipeline);
            GL45C.glBindProgramPipeline(renderPass.pipeline.handle());
            this._lastPipeline = renderPass.pipeline;
        }

        if (renderPass.viewport != null) {
            Vector4ic viewport = renderPass.viewport;
            GL45C.glViewport(viewport.x(), viewport.y(), viewport.z(), viewport.w());
        } else {
            GL45C.glViewport(0, 0, renderPass.color.width(), renderPass.color.height());
        }

        if (renderPass.scissorRect != null) {
            if (!this._scissorEnabled) {
                GL45C.glEnable(GL45C.GL_SCISSOR_TEST);
                this._scissorEnabled = true;
            }
            Vector4ic scissorRect = renderPass.scissorRect;
            GL45C.glScissor(scissorRect.x(), scissorRect.y(), scissorRect.z(), scissorRect.w());
        } else {
            if (this._scissorEnabled) {
                GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
                this._scissorEnabled = false;
            }
        }

        if (renderPass.pipeline.colorTargets() != null) {
            for (GpuColorTargetState colorTargetState : renderPass.pipeline.colorTargets()) {
                GpuBlendState blendState = colorTargetState.blendState();
                if (blendState != null) {
                    if (!this._blendEnabled) {
                        GL45C.glEnable(GL45C.GL_BLEND);
                        this._blendEnabled = true;
                    }
                    GpuBlendComponent color = blendState.color();
                    GpuBlendComponent alpha = blendState.alpha();

                    GL45C.glBlendEquationSeparate(toGlEnum(color.operation()), toGlEnum(alpha.operation()));
                    GL45C.glBlendFuncSeparate(toGlEnum(color.srcFactor()), toGlEnum(color.dstFactor()), toGlEnum(alpha.srcFactor()), toGlEnum(alpha.dstFactor()));
                    return;
                }

                GL45C.glColorMask((colorTargetState.writeMask() & GpuColorWrite.RED) != 0,
                        (colorTargetState.writeMask() & GpuColorWrite.GREEN) != 0,
                        (colorTargetState.writeMask() & GpuColorWrite.BLUE) != 0,
                        (colorTargetState.writeMask() & GpuColorWrite.ALL) != 0);

                break;
            }
        } else {
            GL45C.glColorMask(true, true, true, true);
        }

        if (this._blendEnabled) {
            GL45C.glDisable(GL45C.GL_BLEND);
            this._blendEnabled = false;
        }
    }

    private int getVertexArrayBuffer(GpuVertexState vertexState) {
        return this.vertexStateToVao.computeIfAbsent(vertexState, state -> {
            int handle = GL45C.glCreateVertexArrays();

            int bindingIndex = 0;

            for (GpuVertexBufferLayout bufferLayout : state.buffers()) {
                for (GpuVertexAttribute attribute : bufferLayout.attributes()) {
                    int location = attribute.shaderLocation();

                    GL45C.glVertexArrayAttribBinding(handle, location, bindingIndex);
                    if (attribute.format().isInt) {
                        GL45C.glVertexArrayAttribIFormat(handle, location, attribute.format().componentCount, toGlEnum(attribute.format()), attribute.offset());
                    } else {
                        GL45C.glVertexArrayAttribFormat(handle, location, attribute.format().componentCount, toGlEnum(attribute.format()), attribute.format().normalized, attribute.offset());
                    }

                    GL45C.glEnableVertexArrayAttrib(handle, attribute.shaderLocation());
                }

                int divisor = bufferLayout.stepMode() == GpuVertexStepMode.INSTANCE ? 1 : 0;
                GL45C.glVertexArrayBindingDivisor(handle, bindingIndex, divisor);
                ++bindingIndex;
            }

            return handle;
        });
    }

    private void applyPipelineState(GlRenderPipeline pipeline) {
        GpuDepthStencilState depthStencilState = pipeline.depthStencilState();

        if (depthStencilState != null) {
            if (!this._depthTestEnabled) {
                GL45C.glEnable(GL45C.GL_DEPTH_TEST);
                this._depthTestEnabled = true;
            }

            GL45C.glDepthFunc(toGlEnum(depthStencilState.depthCompare()));

            if (!this._depthMaskEnabled) {
                GL45C.glDepthMask(depthStencilState.depthWriteEnabled());
                this._depthMaskEnabled = true;
            }
        } else {
            if (this._depthTestEnabled) {
                GL45C.glDisable(GL45C.GL_DEPTH_TEST);
                this._depthTestEnabled = false;
            }
            if (this._depthMaskEnabled) {
                GL45C.glDepthMask(false);
                this._depthMaskEnabled = false;
            }
        }

        if (pipeline.primitiveState().cullMode() == GpuCullMode.NONE) {
            if (this._cullEnabled) {
                GL45C.glDisable(GL45C.GL_CULL_FACE);
                this._cullEnabled = false;
            }
        } else {
            if (!this._cullEnabled) {
                GL45C.glEnable(GL45C.GL_CULL_FACE);
                this._cullEnabled = true;
            }
        }
    }

    private static int toGlEnum(GpuAddressMode mode) {
        return switch (mode) {
            case REPEAT -> GL45C.GL_REPEAT;
            case CLAMP_TO_EDGE -> GL45C.GL_CLAMP_TO_EDGE;
        };
    }

    private static int toGlEnum(GpuFilterMode mode) {
        return switch (mode) {
            case NEAREST -> GL45C.GL_NEAREST;
            case LINEAR -> GL45C.GL_LINEAR;
        };
    }

    private static int toGlEnum(GpuPrimitiveTopology topology) {
        return switch (topology) {
            case LINE_LIST -> GL45C.GL_LINES;
            case LINE_STRIP -> GL45C.GL_LINE_STRIP;
            case TRIANGLE_LIST -> GL45C.GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL45C.GL_TRIANGLE_STRIP;
        };
    }

    private static int toGlEnum(GpuTextureDimension dimension) {
        return switch (dimension) {
            case D1 -> GL45C.GL_TEXTURE_1D;
            case D2 -> GL45C.GL_TEXTURE_2D;
            case D3 -> GL45C.GL_TEXTURE_3D;
        };
    }

    private static int toGlEnum(GpuTextureViewDimension textureViewDimension) {
        return switch (textureViewDimension) {
            case D1 -> GL45C.GL_TEXTURE_1D;
            case D2 -> GL45C.GL_TEXTURE_2D;
            case D2_ARRAY -> GL45C.GL_TEXTURE_2D_ARRAY;
            case CUBE -> GL45C.GL_TEXTURE_CUBE_MAP;
            case CUBE_ARRAY -> GL45C.GL_TEXTURE_CUBE_MAP_ARRAY;
            case D3 -> GL45C.GL_TEXTURE_3D;
        };
    }

    private static int toGlEnum(GpuTextureFormat format) {
        return switch (format) {
            case RGBA8UINT -> GL45C.GL_RGBA8;
            case DEPTH32FLOAT -> GL45C.GL_DEPTH_COMPONENT32F;
        };
    }

    private static int toGlEnum(GpuVertexFormat format) {
        return switch (format) {
            case UINT8, UINT8X2, UINT8X4, UNORM8, UNORM8X2, UNORM8X4 -> GL45C.GL_UNSIGNED_BYTE;
            case SINT8, SINT8X2, SINT8X4, SNORM8, SNORM8X2, SNORM8X4 -> GL45C.GL_BYTE;
            case UINT16, UINT16X2, UINT16X4, UNORM16, UNORM16X2, UNORM16X4 -> GL45C.GL_UNSIGNED_SHORT;
            case SINT16, SINT16X2, SINT16X4, SNORM16, SNORM16X2, SNORM16X4 -> GL45C.GL_SHORT;
            case FLOAT32, FLOAT32X2, FLOAT32X3, FLOAT32X4 -> GL45C.GL_FLOAT;
            case UINT32, UINT32X2, UINT32X3, UINT32X4 -> GL45C.GL_UNSIGNED_INT;
            case SINT32, SINT32X2, SINT32X3, SINT32X4 -> GL45C.GL_INT;
        };
    }

    private static int toGlEnum(GpuCompareFunction function) {
        return switch (function) {
            case NEVER -> GL45C.GL_NEVER;
            case LESS -> GL45C.GL_LESS;
            case EQUAL -> GL45C.GL_EQUAL;
            case LESS_EQUAL -> GL45C.GL_LEQUAL;
            case GREATER -> GL45C.GL_GREATER;
            case NOT_EQUAL -> GL45C.GL_NOTEQUAL;
            case GREATER_EQUAL -> GL45C.GL_GEQUAL;
            case ALWAYS -> GL45C.GL_ALWAYS;
        };
    }

    private static int toGlEnum(GpuBlendOperation operation) {
        return switch (operation) {
            case ADD -> GL45C.GL_FUNC_ADD;
            case SUBTRACT -> GL45C.GL_FUNC_SUBTRACT;
            case REVERSE_SUBTRACT -> GL45C.GL_FUNC_REVERSE_SUBTRACT;
        };
    }

    private static int toGlEnum(GpuBlendFactor factor) {
        return switch (factor) {
            case ZERO -> GL45C.GL_ZERO;
            case ONE -> GL45C.GL_ONE;
            case SRC_ALPHA -> GL45C.GL_SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA -> GL45C.GL_ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA -> GL45C.GL_DST_ALPHA;
            case ONE_MINUS_DST_ALPHA -> GL45C.GL_ONE_MINUS_DST_ALPHA;
        };
    }
}
