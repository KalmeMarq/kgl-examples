package io.github.kalmemarq.kgl.util;

import io.github.kalmemarq.kgl.api.gpu.GpuBuffer;
import io.github.kalmemarq.kgl.api.gpu.GpuDevice;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.util.Map;

public record UboLayout(int byteSize, Map<String, Member> members) {
    public UboLayout(String member0, MemberType memberType0) {
        this(memberType0.byteSize, Map.of(member0, new Member(memberType0, 0)));
    }

    public UboLayout(String member0, MemberType memberType0, String member1, MemberType memberType1) {
        this(memberType0.byteSize + memberType1.byteSize, Map.of(member0, new Member(memberType0, 0), member1, new Member(memberType1, memberType0.byteSize)));
    }

    public UboLayout(String member0, MemberType memberType0, String member1, MemberType memberType1, String member2, MemberType memberType2) {
        this(memberType0.byteSize + memberType1.byteSize + memberType2.byteSize, Map.of(member0, new Member(memberType0, 0), member1, new Member(memberType1, memberType0.byteSize), member2, new Member(memberType2, memberType0.byteSize + memberType1.byteSize)));
    }

    public UboLayout(String member0, MemberType memberType0, String member1, MemberType memberType1, String member2, MemberType memberType2, String member3, MemberType memberType3) {
        this(memberType0.byteSize + memberType1.byteSize + memberType2.byteSize + memberType3.byteSize, Map.of(member0, new Member(memberType0, 0), member1, new Member(memberType1, memberType0.byteSize), member2, new Member(memberType2, memberType0.byteSize + memberType1.byteSize), member3, new Member(memberType3, memberType0.byteSize + memberType1.byteSize + memberType2.byteSize)));
    }

    public UboLayout(String member0, MemberType memberType0, String member1, MemberType memberType1, String member2, MemberType memberType2, String member3, MemberType memberType3, String member4, MemberType memberType4) {
        this(memberType0.byteSize + memberType1.byteSize + memberType2.byteSize + memberType3.byteSize + memberType4.byteSize, Map.of(member0, new Member(memberType0, 0), member1, new Member(memberType1, memberType0.byteSize), member2, new Member(memberType2, memberType0.byteSize + memberType1.byteSize), member3, new Member(memberType3, memberType0.byteSize + memberType1.byteSize + memberType2.byteSize), member4, new Member(memberType3, memberType0.byteSize + memberType1.byteSize + memberType2.byteSize + memberType3.byteSize)));
    }

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

    public record Member(MemberType type, int offset) {
    }

    public enum MemberType {
        FLOAT32(4), FLOAT32X2(8), FLOAT32X3(12), FLOAT32X4(16), INT32(4), INT32X3(12), MAT4x4(64);

        public final int byteSize;

        MemberType(int byteSize) {
            this.byteSize = byteSize;
        }
    }
}
