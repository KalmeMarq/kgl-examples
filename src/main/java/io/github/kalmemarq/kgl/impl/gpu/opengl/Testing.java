package io.github.kalmemarq.kgl.impl.gpu.opengl;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

public class Testing {
    static void main() {
        Arena arena = Arena.ofConfined();
        MemorySegment segment = arena.allocate(1024);
        ByteBuffer buffer = segment.asByteBuffer();

        // setViewport
        buffer.put((byte) 4);
        buffer.putInt(1);
        buffer.putInt(1);
        buffer.putInt(1);
        buffer.putInt(1);

        // setScissorRect
        buffer.put((byte) 5);
        buffer.putInt(1);
        buffer.putInt(1);
        buffer.putInt(1);
        buffer.putInt(1);

        // draw
        buffer.put((byte) 8);
        buffer.putInt(1);

        // drawIndexed
        buffer.put((byte) 9);
        buffer.putInt(1);

        buffer.flip();

        CommandBuffer commandBuffer = new CommandBuffer();
        commandBuffer.buffer = buffer;

        CommandEncoder encoder = new CommandEncoder();
        encoder.buffer = commandBuffer;

        try (RenderPass renderPass = encoder.beginRenderPass()) {
            renderPass.setViewport(0, 0, 1280, 720);
            renderPass.setScissorRect(0, 0, 200, 200);
            renderPass.setViewport(0, 0, 180, 720);
        }

        CommandBuffer buffer1 = encoder.finish();

        byte type;
        while (buffer1.buffer.hasRemaining()) {
            type = buffer1.buffer.get();

            if (type == 4) {
                IO.println("SetViewport " + buffer1.buffer.getInt() + "," + buffer1.buffer.getInt() + "," + buffer1.buffer.getInt() + "," + buffer1.buffer.getInt());
            }
            else if (type == 5) {
                IO.println("SetScissorRect " + buffer1.buffer.getInt() + "," + buffer1.buffer.getInt() + "," + buffer1.buffer.getInt() + "," + buffer1.buffer.getInt());
            }
        }
    }

    public static class CommandBuffer {
        ByteBuffer buffer;
    }

    public static class CommandEncoder {
        CommandBuffer buffer;
        boolean inRenderPass;

        RenderPass beginRenderPass() {
            this.inRenderPass = true;
            return new RenderPass(this, this.buffer);
        }

        public CommandBuffer finish() {
            this.buffer.buffer.flip();
            return this.buffer;
        }
    }

    public static class RenderPass implements AutoCloseable {
        CommandEncoder encoder;
        CommandBuffer buffer;

        public RenderPass(CommandEncoder encoder, CommandBuffer buffer) {
            this.encoder = encoder;
            this.buffer = buffer;
        }

        public void setViewport(int x, int y, int w, int h) {
            this.buffer.buffer.put((byte) 4);
            this.buffer.buffer.putInt(x);
            this.buffer.buffer.putInt(y);
            this.buffer.buffer.putInt(w);
            this.buffer.buffer.putInt(h);
        }

        public void setScissorRect(int x, int y, int w, int h) {
            this.buffer.buffer.put((byte) 5);
            this.buffer.buffer.putInt(x);
            this.buffer.buffer.putInt(y);
            this.buffer.buffer.putInt(w);
            this.buffer.buffer.putInt(h);
        }

        @Override
        public void close() {
            this.encoder.inRenderPass = false;
        }
    }
}
