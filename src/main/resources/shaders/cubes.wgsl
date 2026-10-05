struct Uniforms {
    proj: mat4x4f,
    view: mat4x4f,
    model: mat4x4f,
}

struct VertexInput {
    @location(0) position: vec3f,
    @location(1) color: vec4f,
}

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
}

@group(0) @binding(0) var<uniform> uniforms: Uniforms;

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
    var out : VertexOutput;
    out.position = uniforms.proj * uniforms.view * uniforms.model * vec4f(in.position, 1.0);
    out.color = in.color;
    return out;
}

@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {
    return in.color;
}