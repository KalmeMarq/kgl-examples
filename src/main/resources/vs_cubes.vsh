#version 450

layout(std140, binding = 0) uniform Uniforms {
    mat4 proj;
    mat4 view;
    mat4 model;
} uniforms;

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;

layout(location = 0) out vec4 v_color;

out gl_PerVertex {
    vec4 gl_Position;
};

void main() {
    gl_Position = uniforms.proj * uniforms.view * uniforms.model * vec4(position, 1.0);
    v_color = color;
}