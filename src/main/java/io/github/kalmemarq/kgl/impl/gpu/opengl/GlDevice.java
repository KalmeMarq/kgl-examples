package io.github.kalmemarq.kgl.impl.gpu.opengl;

import io.github.kalmemarq.kgl.api.gpu.*;
import io.github.kalmemarq.kgl.api.gpu.descriptor.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
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

    GlDevice(long windowHandle, boolean validation) {
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
                GL45C.glGetInteger(GL45C.GL_MAX_ARRAY_TEXTURE_LAYERS),
                Math.min(GL45C.glGetInteger(GL45C.GL_MAX_COLOR_ATTACHMENTS), GL45C.glGetInteger(GL45C.GL_MAX_DRAW_BUFFERS)),
                Integer.MAX_VALUE,
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
        return new GlTexture(this, handle, descriptor);
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

        return new GlSampler(this, handle, descriptor);
    }

    @Override
    public GpuBuffer createBuffer(GpuBufferDescriptor descriptor) {
        if (this.validation) {
            GlValidator.validateCreateBuffer(this, descriptor);
        }

        int handle = GL45C.glCreateBuffers();
        if (descriptor.label() != null) KHRDebug.glObjectLabel(KHRDebug.GL_BUFFER, handle, descriptor.label());

        GL45C.glNamedBufferStorage(handle, descriptor.size(), GL45C.GL_DYNAMIC_STORAGE_BIT);

        return new GlBuffer(this, handle, descriptor.size(), descriptor.usage());
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

        ByteBuffer dataBuffer = glCommandBuffer.data.flip();
        while (dataBuffer.hasRemaining()) {
            int type = dataBuffer.getInt();
            switch (type) {
                case GlCommandBuffer.COPY_BUFFER_TO_BUFFER -> {
                    GlBuffer srcBuffer = glCommandBuffer.buffers.get(dataBuffer.getInt());
                    GlBuffer dstBuffer = glCommandBuffer.buffers.get(dataBuffer.getInt());
                    long srcOffset = dataBuffer.getLong();
                    long dstOffset = dataBuffer.getLong();
                    long size = dataBuffer.getLong();

                    GL45C.glCopyNamedBufferSubData(srcBuffer.handle(), dstBuffer.handle(), srcOffset, dstOffset, size);
                }
                case GlCommandBuffer.COPY_TEXTURE_TO_TEXTURE -> {
                    GlTexture src = glCommandBuffer.textures.get(dataBuffer.getInt());
                    GlTexture dst = glCommandBuffer.textures.get(dataBuffer.getInt());

                    int srcOriginX = dataBuffer.getInt();
                    int srcOriginY = dataBuffer.getInt();
                    int srcOriginZ = dataBuffer.getInt();

                    int dstOriginX = dataBuffer.getInt();
                    int dstOriginY = dataBuffer.getInt();
                    int dstOriginZ = dataBuffer.getInt();

                    int copySizeX = dataBuffer.getInt();
                    int copySizeY = dataBuffer.getInt();
                    int copySizeZ = dataBuffer.getInt();

                    GL45C.glCopyImageSubData(
                            src.handle(), GlUtils.toGlEnum(src.dimension()), 0,
                            srcOriginX, srcOriginY, srcOriginZ,
                            dst.handle(), GlUtils.toGlEnum(dst.dimension()), 0,
                            dstOriginX, dstOriginY, dstOriginZ,
                            copySizeX, copySizeY, copySizeZ
                    );
                }
                case GlCommandBuffer.CLEAR_BUFFER -> {
                    GlBuffer buf = glCommandBuffer.buffers.get(dataBuffer.getInt());
                    long offset = dataBuffer.getLong();
                    long size = dataBuffer.getLong();

                    GL45C.glCopyNamedBufferSubData(
                            this.zeroBufferSource,
                            buf.handle(),
                            0,
                            offset,
                            size
                    );
                }
                case GlCommandBuffer.CLEAR_COLOR -> {
                    GlTexture texture = glCommandBuffer.textures.get(dataBuffer.getInt());
                    int color = dataBuffer.getInt();

                    float a = (color >> 24 & 0xFF) / 255f;
                    float r = (color >> 16 & 0xFF) / 255f;
                    float g = (color >> 8 & 0xFF) / 255f;
                    float b = (color & 0xFF) / 255f;

                    GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
                    GL45C.glColorMask(true, true, true, true);
                    GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_COLOR_ATTACHMENT0, texture.handle(), 0);
                    GL45C.glNamedFramebufferReadBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);
                    GL45C.glNamedFramebufferDrawBuffer(this.fbo, GL45C.GL_COLOR_ATTACHMENT0);

                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_COLOR, 0, stack.floats(r, g, b, a));
                    }
                }
                case GlCommandBuffer.CLEAR_DEPTH -> {
                    GlTexture texture = glCommandBuffer.textures.get(dataBuffer.getInt());
                    float clearDepth = dataBuffer.getFloat();

                    GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
                    GL45C.glDepthMask(true);
                    GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, texture.handle(), 0);

                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        GL45C.glClearNamedFramebufferfv(this.fbo, GL45C.GL_DEPTH, 0, stack.floats(clearDepth));
                    }
                }
                //
                case GlCommandBuffer.BEGIN_RENDER_PASS -> {
                    GpuRenderPassDescriptor descriptor = glCommandBuffer.renderPassDescriptors.get(dataBuffer.getInt());
                    {
                        GpuRenderPassDescriptor.ColorAttachment[] colors = descriptor.colorAttachments();
                        GpuRenderPassDescriptor.DepthStencilAttachment depth = descriptor.depthStencilAttachment();

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
                    }

                    {
                        var colors = descriptor.colorAttachments();
                        var depth = descriptor.depthStencilAttachment();

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
                }
                case GlCommandBuffer.SET_PIPELINE -> {
                    GlRenderPipeline pipeline = glCommandBuffer.renderPipelines.get(dataBuffer.getInt());

                    if (this._lastPipeline != pipeline && !this.pipelineToUniforms.containsKey(pipeline)) {
                        GpuBindGroupLayout layout = pipeline.bindGroupLayout();
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

                        this.pipelineToUniforms.put(pipeline, uniformToBinding);
                    }

                    if (this._lastPipeline != pipeline) {
                        this.applyPipelineState(pipeline);
                        GL45C.glBindProgramPipeline(pipeline.handle());
                        this._lastPipeline = pipeline;
                    }

                    if (pipeline.colorTargets() != null) {
                        GpuColorTargetState[] targets = pipeline.colorTargets();
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
                case GlCommandBuffer.SET_RESOURCES -> {
                    GlRenderPipeline pipeline = glCommandBuffer.renderPipelines.get(dataBuffer.getInt());
                    Map<String, Object> resources = glCommandBuffer.resources.get(dataBuffer.getInt());

                    Object2IntMap<String> uniformToBinding = this.pipelineToUniforms.get(pipeline);

                    for (var entry : resources.entrySet()) {
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
                            GlCommandEncoder.TextureSampler texSamp = (GlCommandEncoder.TextureSampler) entry.getValue();

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
                case GlCommandBuffer.SET_SCISSOR -> {
                    int x = dataBuffer.getInt();
                    int y = dataBuffer.getInt();
                    int w = dataBuffer.getInt();
                    int h = dataBuffer.getInt();

                    GL45C.glEnable(GL45C.GL_SCISSOR_TEST);
                    GL45C.glScissor(x, y, w, h);
                }
                case GlCommandBuffer.SET_VIEWPORT -> {
                    int x = dataBuffer.getInt();
                    int y = dataBuffer.getInt();
                    int w = dataBuffer.getInt();
                    int h = dataBuffer.getInt();

                    GL45C.glViewport(x, y, w, h);
                }
                case GlCommandBuffer.SET_VERTEX_BUFFERS -> {
                    GlRenderPipeline pipeline = glCommandBuffer.renderPipelines.get(dataBuffer.getInt());
                    GpuBufferSlice[] slots = new GpuBufferSlice[dataBuffer.getInt()];
                    for (int i = 0; i < slots.length; i++) {
                        int bufId = dataBuffer.getInt();
                        long offset = dataBuffer.getLong();
                        long size = dataBuffer.getLong();

                        slots[i] = bufId == -1 ? null : new GlBufferSlice(glCommandBuffer.buffers.get(bufId), offset, size);
                    }

                    int vao = this.getVertexArrayBuffer(pipeline.vertexState());
                    for (int i = 0; i < pipeline.vertexState().buffers().length; ++i) {
                        GpuVertexBufferLayout layout = pipeline.vertexState().buffers()[i];

                        if (slots[i] != null) {
                            GL45C.glVertexArrayVertexBuffer(vao, i, ((GlBuffer) slots[i].buffer()).handle(), slots[i].offset(), layout.arrayStride());
                        } else {
                            GL45C.glVertexArrayVertexBuffer(vao, i, 0, 0, 0);
                        }
                    }
                    GL45C.glBindVertexArray(vao);
                }
                case GlCommandBuffer.SET_INDEX_BUFFER -> {
                    GlRenderPipeline pipeline = glCommandBuffer.renderPipelines.get(dataBuffer.getInt());
                    GlBuffer buf = glCommandBuffer.buffers.get(dataBuffer.getShort());
                    dataBuffer.getShort();
                    int vao = this.getVertexArrayBuffer(pipeline.vertexState());
                    if (buf != null) {
                        GL45C.glVertexArrayElementBuffer(vao, buf.handle());
                    } else {
                        GL45C.glVertexArrayElementBuffer(vao, 0);
                    }
                    GL45C.glBindVertexArray(vao);
                }
                case GlCommandBuffer.DRAW -> {
                    int glTopology = dataBuffer.getInt();
                    int count = dataBuffer.getInt();
                    int instanceCount = dataBuffer.getInt();
                    int baseVertex = dataBuffer.getInt();
                    int baseInstance = dataBuffer.getInt();

                    if (instanceCount == 1) {
                        GL45C.glDrawArrays(glTopology, baseVertex, count);
                    } else if (baseInstance == 0) {
                        GL45C.glDrawArraysInstanced(glTopology, baseVertex, count, instanceCount);
                    } else {
                        GL45C.glDrawArraysInstancedBaseInstance(glTopology, baseVertex, count, instanceCount, baseInstance);
                    }
                }
                case GlCommandBuffer.DRAW_INDEXED -> {
                    int glTopology = dataBuffer.getInt();
                    int glIndexFormat = dataBuffer.getInt();
                    int indexCount = dataBuffer.getInt();
                    int instanceCount = dataBuffer.getInt();
                    int baseIndex = dataBuffer.getInt();
                    int baseVertex = dataBuffer.getInt();
                    int baseInstance = dataBuffer.getInt();

                    if (instanceCount == 1 && baseVertex == 0 && baseInstance == 0) {
                        GL45C.glDrawElements(glTopology, indexCount, glIndexFormat, (long) baseIndex * (glIndexFormat == GL11C.GL_UNSIGNED_SHORT ? 2 : 4));
                    } else if (baseInstance == 0) {
                        GL45C.glDrawElementsInstancedBaseVertex(glTopology, indexCount, glIndexFormat, (long) baseIndex * (glIndexFormat == GL11C.GL_UNSIGNED_SHORT ? 2 : 4), instanceCount, baseVertex);
                    } else {
                        GL45C.glDrawElementsInstancedBaseVertexBaseInstance(glTopology, indexCount, glIndexFormat, (long) baseIndex * (glIndexFormat == GL11C.GL_UNSIGNED_SHORT ? 2 : 4), instanceCount, baseVertex, baseInstance);
                    }
                }
                case GlCommandBuffer.END_RENDER_PASS -> {
                    GL45C.glNamedFramebufferTexture(this.fbo, GL45C.GL_DEPTH_ATTACHMENT, 0, 0);
                    GL45C.glBindFramebuffer(GL45C.GL_FRAMEBUFFER, 0);
                    GL45C.glDisable(GL45C.GL_SCISSOR_TEST);
                    GL45C.glColorMask(true, true, true, true);
                    GL45C.glDepthMask(true);
                    GL45C.glBindVertexArray(0);
                    this._lastPipeline = null;
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
            GL11C.glEnable(GL11C.GL_DEPTH_TEST);
            GL11C.glDepthFunc(GlUtils.toGlEnum(depthStencilState.depthCompare()));
            GL11C.glDepthMask(depthStencilState.depthWriteEnabled());
        } else {
            GL11C.glDisable(GL11C.GL_DEPTH_TEST);
            GL11C.glDepthMask(false);
        }

        switch (pipeline.primitiveState().cullMode()) {
            case NONE -> GL11C.glDisable(GL11C.GL_CULL_FACE);
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

    void deleteBuffer(GlBuffer buffer) {
        GL15C.glDeleteBuffers(buffer.handle());
    }

    void deleteTexture(GlTexture texture) {
        GL11C.glDeleteTextures(texture.handle());
    }

    void deleteSampler(GlSampler sampler) {
        GL33C.glDeleteSamplers(sampler.handle());
    }
}
