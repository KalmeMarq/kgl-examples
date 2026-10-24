struct Uniforms {
    proj_view: mat4x4f,
    model: mat4x4f,
}

struct VertexInput {
    @location(0) position: vec3f,
    @location(1) color: vec4f,
    @location(2) inst_offset_rotation: vec4f,
}

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
}

@group(0) @binding(0) var<uniform> uniforms: Uniforms;

// @include("common")

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
    let pos = vec3f(in.inst_offset_rotation.xy, 0.0);

    let local_pos = rotate_xyz(in.position, vec3f(in.inst_offset_rotation.zw, 0.0));
    let world_pos = local_pos + pos;

    var out: VertexOutput;
    out.position = uniforms.proj_view * uniforms.model * vec4f(world_pos, 1.0);
    out.color = in.color;
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4f {
    return in.color;
}