struct Uniforms {
    proj: mat4x4f,
}

struct VertexInput {
    @location(0) position: vec2f,
    @location(1) texcoord: vec2f,
    @location(2) color: vec4f,
}

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) texcoord: vec2f,
    @location(1) color: vec4f,
}

@group(0) @binding(0) var<uniform> uniforms: Uniforms;

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
    var out: VertexOutput;
    out.position = uniforms.proj * vec4f(in.position, 0.0, 1.0);
    out.texcoord = in.texcoord;
    out.color = in.color;
    return out;
}

@group(0) @binding(1) var txr: texture_2d<f32>;
@group(0) @binding(2) var smp: sampler;

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4f {
    return in.color * textureSample(txr, smp, in.texcoord);
}