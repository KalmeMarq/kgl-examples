package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.GpuBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record UboLayout(int byteSize, Map<String, Member> members) {
    public void put(GpuDevice device, GpuBuffer buffer, String name, Matrix4fc matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), matrix.get(stack.malloc(64)));
        }
    }

    public void put(GpuDevice device, GpuBuffer buffer, String name, float value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), stack.malloc(4).putFloat(value).flip());
        }
    }

    public void put(GpuDevice device, GpuBuffer buffer, String name, int value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), stack.malloc(4).putInt(value).flip());
        }
    }

    public void put(GpuDevice device, GpuBuffer buffer, String name, int value0, int value1, int value2) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), stack.malloc(12).putInt(value0).putInt(value1).putInt(value2).flip());
        }
    }

    public void put(GpuDevice device, GpuBuffer buffer, String name, float value0, float value1) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), stack.malloc(8).putFloat(value0).putFloat(value1).flip());
        }
    }

    public void put(GpuDevice device, GpuBuffer buffer, String name, float value0, float value1, float value2) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), stack.malloc(12).putFloat(value0).putFloat(value1).putFloat(value2).flip());
        }
    }

    public void put(GpuDevice device, GpuBuffer buffer, String name, float value0, float value1, float value2, float value3) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Member member = this.members.get(name);

            device.writeToBuffer(buffer.slice(member.offset, member.type.byteSize), stack.malloc(16).putFloat(value0).putFloat(value1).putFloat(value2).putFloat(value3).flip());
        }
    }

    public record Member(Type type, int offset) {
    }

    public enum Type {
        FLOAT32(4), FLOAT32X2(8), FLOAT32X3(12), FLOAT32X4(16), INT32(4), INT32X3(12), MAT4x4(64);

        public final int byteSize;

        Type(int byteSize) {
            this.byteSize = byteSize;
        }
    }

    public static class Builder {
        private final Map<String, Member> members;
        private int byteSize;

        public Builder() {
            this.members = new HashMap<>();
        }

        public Builder add(UboLayout layout) {
            for (var entry : layout.members.entrySet()) {
                this.add(entry.getKey(), entry.getValue().type);
            }
            return this;
        }

        public Builder add(String name, Type type) {
            this.members.put(name, new Member(type, this.byteSize));
            this.byteSize += type.byteSize;
            return this;
        }

        public UboLayout build() {
            return new UboLayout(this.byteSize, Collections.unmodifiableMap(this.members));
        }
    }
}
