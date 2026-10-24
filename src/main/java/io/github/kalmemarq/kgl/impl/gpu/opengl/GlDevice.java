package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.joml.Vector4ic;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

class GlDevice implements GpuDevice {
    private final long context;
    private final GpuLimits limits;
    final boolean validation;

    private final int[] _boundTextures;
    private final int[] _boundSamplers;
    private GlRenderPipeline _lastPipeline;
    private final Map<GpuVertexState, Integer> vertexStateToVao;
    private int lastColorCount;
    private final int fbo;
    private final Reference2ObjectMap<GpuRenderPipeline, Object2IntMap<String>> pipelineToUniforms;
    private final ArrayDeque<GlCommandBuffer> freeCommandBuffers = new ArrayDeque<>();
    private final List<GlCommandBuffer> allCommandBuffers = new ArrayList<>();
    private final int zeroBufferSource;

    public GlDevice(long windowHandle, boolean validation) {
        this.validation = validation;
        this.context = SDLVideo.SDL_GL_CreateContext(windowHandle);
        SDLVideo.SDL_GL_MakeCurrent(windowHandle, this.context);
        GL.create(SDLVideo::SDL_GL_GetProcAddress);
        GL.createCapabilities();

        GLUtil.setupDebugMessageCallback(System.err);
        GL45C.glDebugMessageControl(GL45C.GL_DEBUG_SOURCE_API, GL45C.GL_DEBUG_TYPE_OTHER, GL45C.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);

        this.limits = new GpuLimits(
                GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_SIZE),
                GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_SIZE),
                GL45C.glGetInteger(GL45C.GL_MAX_3D_TEXTURE_SIZE),
                GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_ATTRIBS)
        );

        this.zeroBufferSource = GL45C.glCreateBuffers();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL45C.glNamedBufferStorage(this.zeroBufferSource, stack.calloc(1), GL45C.GL_DYNAMIC_STORAGE_BIT);
        }

        this.vertexStateToVao = new HashMap<>();
        this._boundTextures = new int[]{-1, -1, -1};
        this._boundSamplers = new int[]{-1, -1, -1};
        this.fbo = GL45C.glCreateFramebuffers();
        this.pipelineToUniforms = new Reference2ObjectOpenHashMap<>();
    }

    @Override
    public GpuLimits limits() {
        return this.limits;
    }

    @Override
    public GpuTexture createTexture(GpuTextureDescriptor descriptor) {
        if (this.validation) {
            GlValidator.validateCreateTexture(this, descriptor);
        }

        int handle = GL45C.glCreateTextures(descriptor.textureBindingViewDimension() == null ? GlUtils.toGlEnum(descriptor.dimension()) : GlUtils.toGlEnum(descriptor.textureBindingViewDimension()));
        if (descriptor.label() != null) KHRDebug.glObjectLabel(GL45C.GL_TEXTURE, handle, descriptor.label());

        switch (descriptor.dimension()) {
            case D1 -> GL45C.glTextureStorage1D(handle, 1, GlUtils.toGlEnum(descriptor.format()), descriptor.width());
            case D2 -> GL45C.glTextureStorage2D(handle, 1, GlUtils.toGlEnum(descriptor.format()), descriptor.width(), descriptor.height());
            case D3 -> GL45C.glTextureStorage3D(handle, 1, GlUtils.toGlEnum(descriptor.format()), descriptor.width(), descriptor.height(), descriptor.depthOrLayers());
        }
        return new GlTexture(handle, descriptor);
    }

    @Override
    public GpuSampler createSampler(GpuSamplerDescriptor descriptor) {
        if (this.validation) {
            GlValidator.validateCreateSampler(this, descriptor);
        }

        int handle = GL45C.glCreateSamplers();
        if (descriptor.label() != null) KHRDebug.glObjectLabel(GL45C.GL_SAMPLER, handle, descriptor.label());

        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_S, GlUtils.toGlEnum(descriptor.addressModeU()));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_T, GlUtils.toGlEnum(descriptor.addressModeV()));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_R, GlUtils.toGlEnum(descriptor.addressModeW()));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_MIN_FILTER, GlUtils.toGlEnum(descriptor.minFilter()));
        GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_MAG_FILTER, GlUtils.toGlEnum(descriptor.magFilter()));

        return new GlSampler(handle, descriptor);
    }

    @Override
    public GpuBuffer createBuffer(GpuBufferDescriptor descriptor) {
        if (this.validation) {
            GlValidator.validateCreateBuffer(this, descriptor);
        }

        int handle = GL45C.glCreateBuffers();
        if (descriptor.label() != null) KHRDebug.glObjectLabel(KHRDebug.GL_BUFFER, handle, descriptor.label());

        GL45C.glNamedBufferStorage(handle, descriptor.size(), GL45C.GL_DYNAMIC_STORAGE_BIT);

        return new GlBuffer(handle, descriptor.size(), descriptor.usage());
    }

    @Override
    public GpuRenderPipeline createRenderPipeline(GpuRenderPipelineDescriptor descriptor) {
        int vertexShader = GL45C.glCreateShaderProgramv(GL45C.GL_VERTEX_SHADER, descriptor.source().vertex());
        int fragmentShader = GL45C.glCreateShaderProgramv(GL45C.GL_FRAGMENT_SHADER, descriptor.source().fragment());
        int shaderPipeline = GL45C.glCreateProgramPipelines();

        if (descriptor.label() != null) KHRDebug.glObjectLabel(KHRDebug.GL_PROGRAM_PIPELINE, shaderPipeline, descriptor.label());

        GL45C.glUseProgramStages(shaderPipeline, GL45C.GL_VERTEX_SHADER_BIT, vertexShader);
        GL45C.glUseProgramStages(shaderPipeline, GL45C.GL_FRAGMENT_SHADER_BIT, fragmentShader);
        if (!GL45C.glGetProgramInfoLog(vertexShader).isEmpty()) IO.println(GL45C.glGetProgramInfoLog(vertexShader));
        if (!GL45C.glGetProgramInfoLog(fragmentShader).isEmpty()) IO.println(GL45C.glGetProgramInfoLog(fragmentShader));
        return new GlRenderPipeline(shaderPipeline, vertexShader, fragmentShader, descriptor.primitive(), descriptor.depthStencil(), descriptor.vertex(), descriptor.bindGroupLayouts()[0], descriptor.fragment().targets());
    }

    @Override
    public GpuCommandEncoder createCommandEncoder() {
        return new GlCommandEncoder(this, this.acquireCommandBuffer());
    }

    @Override
    public GpuRenderPass createRenderPass(GpuTexture colorAttachment, @Nullable GpuTexture depthStencilAttachment) {
        return this.createRenderPass(new GpuRenderPassDescriptor(
                colorAttachment == null ? null : new GpuRenderPassDescriptor.ColorAttachment(colorAttachment),
                depthStencilAttachment == null ? null : new GpuRenderPassDescriptor.DepthStencilAttachment(depthStencilAttachment)
        ));
    }

    @Override
    public GpuRenderPass createRenderPass(GpuRenderPassDescriptor descriptor) {
        return new GlRenderPass(this, descriptor);
    }

    @Override
    public void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data) {
        if (this.validation) {
            GlValidator.validateWriteToBuffer(bufferSlice, data);
        }

        GlBuffer buffer = (GlBuffer) bufferSlice.buffer();
        GL45C.glNamedBufferSubData(buffer.handle(), bufferSlice.offset(), data);
    }

    @Override
    public void writeToTexture(GpuTexture texture, ByteBuffer data) {
        GL45C.glTextureSubImage2D(((GlTexture) texture).handle(), 0, 0, 0, texture.width(), texture.height(), GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, data);
    }

    public void writeToTexture(GpuTexture texture, ByteBuffer data, int dstX, int dstY, int dstWidth, int dstHeight) {
        GL45C.glTextureSubImage2D(((GlTexture) texture).handle(), 0, dstX, dstY, dstWidth, dstHeight, GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, data);
    }

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
    public void submit(GpuCommandBuffer commandBuffer) {
        GlCommandBuffer glCommandBuffer = (GlCommandBuffer) commandBuffer;

        ByteBuffer buffer = glCommandBuffer.data.flip();
        while (buffer.hasRemaining()) {
            int type = buffer.getInt();
            switch (type) {
                case GlCommandBuffer.COPY_BUFFER_TO_BUFFER -> {
                    GlBuffer srcBuffer = glCommandBuffer.buffers.get(buffer.getInt());
                    GlBuffer dstBuffer = glCommandBuffer.buffers.get(buffer.getInt());
                    long srcOffset = buffer.getLong();
                    long dstOffset = buffer.getLong();
                    long size = buffer.getLong();

                    GL45C.glCopyNamedBufferSubData(srcBuffer.handle(), dstBuffer.handle(), srcOffset, dstOffset, size);
                }
                case GlCommandBuffer.COPY_TEXTURE_TO_TEXTURE -> {
                    GlTexture src = glCommandBuffer.textures.get(buffer.getInt());
                    GlTexture dst = glCommandBuffer.textures.get(buffer.getInt());

                    int srcOriginX = buffer.getInt();
                    int srcOriginY = buffer.getInt();
                    int srcOriginZ = buffer.getInt();

                    int dstOriginX = buffer.getInt();
                    int dstOriginY = buffer.getInt();
                    int dstOriginZ = buffer.getInt();

                    int copySizeX = buffer.getInt();
                    int copySizeY = buffer.getInt();
                    int copySizeZ = buffer.getInt();

                    GL45C.glCopyImageSubData(
                            src.handle(), GlUtils.toGlEnum(src.dimension()), 0,
                            srcOriginX, srcOriginY, srcOriginZ,
                            dst.handle(), GlUtils.toGlEnum(dst.dimension()), 0,
                            dstOriginX, dstOriginY, dstOriginZ,
                            copySizeX, copySizeY, copySizeZ
                    );
                }
                case GlCommandBuffer.CLEAR_BUFFER -> {
                    GlBuffer buf = glCommandBuffer.buffers.get(buffer.getInt());
                    long offset = buffer.getLong();
                    long size = buffer.getLong();

                    GL45C.glCopyNamedBufferSubData(
                            this.zeroBufferSource,
                            buf.handle(),
                            0,
                            offset,
                            size
                    );
                }
                case GlCommandBuffer.CLEAR_COLOR -> {
                    GlTexture texture = glCommandBuffer.textures.get(buffer.getInt());
                    int color = buffer.getInt();

                    float a = (color >> 24 & 0xFF) / 255f;
                    float r = (color >> 16 & 0xFF) / 255f;
                    float g = (color >> 8 & 0xFF) / 255f;
                    float b = (color & 0xFF) / 255f;

                    GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, texture.handle(), 0);
                    GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);

                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_COLOR, 0, stack.floats(r, g, b, a));
                    }
                }
                case GlCommandBuffer.CLEAR_DEPTH -> {
                    GlTexture texture = glCommandBuffer.textures.get(buffer.getInt());
                    float clearDepth = buffer.getFloat();

                    GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, texture.handle(), 0);

                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_DEPTH, 0, stack.floats(clearDepth));
                    }
                }
            }
        }

        glCommandBuffer.reset();
        this.freeCommandBuffers.add(glCommandBuffer);
    }

    @Override
    public void blit(GpuTexture texture) {
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, ((GlTexture) texture).handle(), 0);
        GL45C.glNamedFramebufferReadBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);
        GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);
        GL45C.glBlitNamedFramebuffer(this.fbo, 0, 0, 0, texture.width(), texture.height(), 0, 0, texture.width(), texture.height(), GL45C.GL_COLOR_BUFFER_BIT, GL45C.GL_NEAREST);
    }

    @Override
    public void close() {
        for (GlCommandBuffer commandBuffer : this.allCommandBuffers) {
            commandBuffer.close();
        }

        SDLVideo.SDL_GL_DestroyContext(this.context);
    }

    void beginRenderPass(GlRenderPass renderPass) {
        GpuRenderPassDescriptor.ColorAttachment[] colors = renderPass.descriptor.colorAttachments();
        GpuRenderPassDescriptor.DepthStencilAttachment depth = renderPass.descriptor.depthStencilAttachment();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer colorBufs = stack.mallocInt(colors.length);

            for (int i = 0; i < colors.length; i++) {
                GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0 + i, ((GlTexture) colors[i].texture()).handle(), 0);
                colorBufs.put(GL45C.GL_COLOR_ATTACHMENT0 + i);
            }
            colorBufs.flip();

            if (colors.length == 0) {
                GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_NONE);
                GL45C.glNamedFramebufferReadBuffer(this.fbo, GL45C.GL_NONE);
            } else {
                GL45C.glNamedFramebufferDrawBuffers(this.fbo, colorBufs);
                GL45C.glNamedFramebufferReadBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);
            }

            GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, depth != null ? ((GlTexture) depth.texture()).handle() : 0, 0);
        }

        for (int i = colors.length; i < this.lastColorCount; i++) {
            GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0 + i, 0, 0);
        }
        this.lastColorCount = colors.length;

        GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, this.fbo);

        this.applyLoadOps(renderPass);
    }

    private void applyLoadOps(GlRenderPass renderPass) {
        var colors = renderPass.descriptor.colorAttachments();
        var depth = renderPass.descriptor.depthStencilAttachment();

        GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
        GL45C.glColorMask(true, true, true, true);
        GL45C.glDepthMask(true);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i].loadOp() != GpuLoadOp.CLEAR) continue;

                int color = colors[i].color();
                GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_COLOR, i, stack.floats((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, (color >> 24 & 0xFF) / 255f));
            }

            if (depth != null && depth.loadOp() == GpuLoadOp.CLEAR) {
                GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_DEPTH, 0, stack.floats(1.0f));
            }
        }
    }

    void endRenderPass() {
        GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, 0, 0);
        GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, 0);
        GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
        GL45C.glColorMask(true, true, true, true);
        GL45C.glDepthMask(true);
        this._lastPipeline = null;
    }

    void drawIndexed(GlRenderPass renderPass, int indexCount, int instanceCount, int baseIndex, int baseVertex, int baseInstance) {
        this.setupResourcesAndPipelines(renderPass);

        int vao = this.getVertexArrayBuffer(renderPass.pipeline.vertexState());
        for (int i = 0; i < renderPass.pipeline.vertexState().buffers().length; ++i) {
            GpuVertexBufferLayout layout = renderPass.pipeline.vertexState().buffers()[i];

            if (renderPass.vertexBuffers[i] != null) {
                GL45C.glVertexArrayVertexBuffer(vao, i, ((GlBuffer) renderPass.vertexBuffers[i].buffer()).handle(), renderPass.vertexBuffers[i].offset(), layout.arrayStride());
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
            GL45C.glDrawElements(GlUtils.toGlEnum(renderPass.pipeline.primitiveState().topology()), indexCount, GlUtils.toGlEnum(renderPass.indexFormat), (long) baseIndex * (renderPass.indexFormat == GpuIndexFormat.UINT16 ? 2 : 4));
        } else if (baseInstance == 0) {
            GL45C.glDrawElementsInstancedBaseVertex(GlUtils.toGlEnum(renderPass.pipeline.primitiveState().topology()), indexCount, GlUtils.toGlEnum(renderPass.indexFormat), (long) baseIndex * (renderPass.indexFormat == GpuIndexFormat.UINT16 ? 2 : 4), instanceCount, baseVertex);
        } else {
            GL45C.glDrawElementsInstancedBaseVertexBaseInstance(GlUtils.toGlEnum(renderPass.pipeline.primitiveState().topology()), indexCount, GlUtils.toGlEnum(renderPass.indexFormat), (long) baseIndex * (renderPass.indexFormat == GpuIndexFormat.UINT16 ? 2 : 4), instanceCount, baseVertex, baseInstance);
        }
    }

    void draw(GlRenderPass renderPass, int count, int instanceCount, int baseVertex, int baseInstance) {
        this.setupResourcesAndPipelines(renderPass);

        int vao = this.getVertexArrayBuffer(renderPass.pipeline.vertexState());
        for (int i = 0; i < renderPass.pipeline.vertexState().buffers().length; ++i) {
            GpuVertexBufferLayout layout = renderPass.pipeline.vertexState().buffers()[i];

            if (renderPass.vertexBuffers[i] != null) {
                GL45C.glVertexArrayVertexBuffer(vao, i, ((GlBuffer) renderPass.vertexBuffers[i].buffer()).handle(), renderPass.vertexBuffers[i].offset(), layout.arrayStride());
            } else {
                GL45C.glVertexArrayVertexBuffer(vao, i, 0, 0, 0);
            }
        }
        GL45C.glBindVertexArray(vao);

        if (instanceCount == 1) {
            GL45C.glDrawArrays(GlUtils.toGlEnum(renderPass.pipeline.primitiveState().topology()), baseVertex, count);
        } else if (baseInstance == 0) {
            GL45C.glDrawArraysInstanced(GlUtils.toGlEnum(renderPass.pipeline.primitiveState().topology()), baseVertex, count, instanceCount);
        } else {
            GL45C.glDrawArraysInstancedBaseInstance(GlUtils.toGlEnum(renderPass.pipeline.primitiveState().topology()), baseVertex, count, instanceCount, baseInstance);
        }
    }

    private void setupResourcesAndPipelines(GlRenderPass renderPass) {
        if (renderPass.viewport != null) {
            Vector4ic viewport = renderPass.viewport;
            GL45C.glViewport(viewport.x(), viewport.y(), viewport.z(), viewport.w());
        } else {
            GL45C.glViewport(0, 0, renderPass.width(), renderPass.height());
        }

        if (this._lastPipeline != renderPass.pipeline && !this.pipelineToUniforms.containsKey(renderPass.pipeline)) {
            GpuBindGroupLayout layout = renderPass.pipeline.bindGroupLayout();
            Object2IntMap<String> uniformToBinding = new Object2IntOpenHashMap<>();

            List<Map.Entry<String, GpuBindGroupLayoutEntry>> ubo = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.UniformBuffer).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();
            List<Map.Entry<String, GpuBindGroupLayoutEntry>> ssbo = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.StorageBuffer).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();
            List<Map.Entry<String, GpuBindGroupLayoutEntry>> txr = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.Texture).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();
            List<Map.Entry<String, GpuBindGroupLayoutEntry>> smp = layout.entries().entrySet().stream().filter(it -> it.getValue() instanceof GpuBindGroupLayoutEntry.Sampler).sorted(Comparator.comparingInt(a -> a.getValue().binding())).toList();

            int uboIdx = 0;

            for (var entry : ubo) {
                String name = entry.getKey();
                uniformToBinding.put(name, uboIdx);
                ++uboIdx;
            }

            int ssboIdx = 0;

            for (var entry : ssbo) {
                String name = entry.getKey();
                uniformToBinding.put(name, ssboIdx + 300);
                ++ssboIdx;
            }

            int txrIdx = 0;

            for (var entry : txr) {
                String name = entry.getKey();
                uniformToBinding.put(name, txrIdx + 100);
                ++txrIdx;
            }

            int smpIdx = 0;

            for (var entry : smp) {
                String name = entry.getKey();
                uniformToBinding.put(name, smpIdx + 200);
                ++smpIdx;
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
                    if (entry.getValue() instanceof GlBufferSlice(GlBuffer buffer, long offset, long size)) {
                        GL45C.glBindBufferRange(GL45C.GL_UNIFORM_BUFFER, binding, buffer.handle(), offset, size);
                    } else if (entry.getValue() instanceof GlBuffer glBuffer) {
                        GL45C.glBindBufferBase(GL45C.GL_UNIFORM_BUFFER, binding, glBuffer.handle());
                    }
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
                } else if (binding >= 300) {
                    if (entry.getValue() instanceof GlBufferSlice(GlBuffer buffer, long offset, long size)) {
                        GL45C.glBindBufferRange(GL45C.GL_SHADER_STORAGE_BUFFER, binding - 300, buffer.handle(), offset, size);
                    } else if (entry.getValue() instanceof GlBuffer glBuffer) {
                        GL45C.glBindBufferBase(GL45C.GL_SHADER_STORAGE_BUFFER, binding - 300, glBuffer.handle());
                    }
                }
            }
        }

        if (this._lastPipeline != renderPass.pipeline) {
            this.applyPipelineState(renderPass.pipeline);
            GL45C.glBindProgramPipeline(renderPass.pipeline.handle());
            this._lastPipeline = renderPass.pipeline;
        }

        if (renderPass.scissorRect != null) {
            GL45C.glEnable(GL45C.GL_SCISSOR_TEST);

            Vector4ic scissorRect = renderPass.scissorRect;
            GL45C.glScissor(scissorRect.x(), scissorRect.y(), scissorRect.z(), scissorRect.w());
        } else {
            GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
        }

        if (renderPass.pipeline.colorTargets() != null) {
            GpuColorTargetState[] targets = renderPass.pipeline.colorTargets();
            for (int i = 0; i < targets.length; i++) {
                GpuColorTargetState state = targets[i];

                if (state == null) {
                    GL45C.glColorMaski(i, false, false, false, false);
                    GL45C.glDisablei(GL45C.GL_BLEND, i);
                    continue;
                }

                int mask = state.writeMask();
                GL45C.glColorMaski(i,
                        (mask & GpuColorWrite.RED) != 0,
                        (mask & GpuColorWrite.GREEN) != 0,
                        (mask & GpuColorWrite.BLUE) != 0,
                        (mask & GpuColorWrite.ALPHA) != 0);

                GpuBlendState blend = state.blendState();
                if (blend == null) {
                    GL45C.glDisablei(GL45C.GL_BLEND, i);
                } else {
                    GpuBlendComponent c = blend.color();
                    GpuBlendComponent a = blend.alpha();
                    GL45C.glEnablei(GL45C.GL_BLEND, i);
                    GL45C.glBlendEquationSeparatei(i, GlUtils.toGlEnum(c.operation()), GlUtils.toGlEnum(a.operation()));
                    GL45C.glBlendFuncSeparatei(i,
                            GlUtils.toGlEnum(c.srcFactor()), GlUtils.toGlEnum(c.dstFactor()),
                            GlUtils.toGlEnum(a.srcFactor()), GlUtils.toGlEnum(a.dstFactor()));
                }
            }
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
                        GL45C.glVertexArrayAttribIFormat(handle, location, attribute.format().componentCount, GlUtils.toGlEnum(attribute.format()), attribute.offset());
                    } else {
                        GL45C.glVertexArrayAttribFormat(handle, location, attribute.format().componentCount, GlUtils.toGlEnum(attribute.format()), attribute.format().normalized, attribute.offset());
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
            GL45C.glEnable(GL45C.GL_DEPTH_TEST);
            GL45C.glDepthFunc(GlUtils.toGlEnum(depthStencilState.depthCompare()));
            GL45C.glDepthMask(depthStencilState.depthWriteEnabled());
        } else {
            GL45C.glDisable(GL45C.GL_DEPTH_TEST);
            GL45C.glDepthMask(false);
        }

        switch (pipeline.primitiveState().cullMode()) {
            case NONE -> GL45C.glDisable(GL11C.GL_CULL_FACE);
            case FRONT -> {
                GL11C.glEnable(GL11C.GL_CULL_FACE);
                GL11C.glCullFace(GL11C.GL_FRONT);
            }
            case BACK -> {
                GL11C.glEnable(GL11C.GL_CULL_FACE);
                GL11C.glCullFace(GL11C.GL_BACK);
            }
        }

        GL11C.glFrontFace(pipeline.primitiveState().frontFace() == GpuFrontFace.CW ? GL45C.GL_CW : GL45C.GL_CCW);
    }

    GlCommandBuffer acquireCommandBuffer() {
        GlCommandBuffer commandBuffer = this.freeCommandBuffers.poll();
        if (commandBuffer == null) {
            commandBuffer = new GlCommandBuffer();
            this.allCommandBuffers.add(commandBuffer);
        }
        return commandBuffer;
    }
}
