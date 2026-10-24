struct Uniforms {
    proj_view_model: mat4x4f,
    resolution: vec2f,
    line_width: f32,
}

struct VertexInput {
    @builtin(vertex_index) id: u32,
    @location(0) position: vec3f,
    @location(1) normal: vec3f,
    @location(2) color: vec4f,
}

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) color: vec4f,
}

@group(0) @binding(0) var<uniform> uniforms: Uniforms;

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
    let start = uniforms.proj_view_model * vec4<f32>(in.position, 1.0);
    let end = uniforms.proj_view_model * vec4<f32>(in.position + in.normal, 1.0);

    let ndc1 = start.xy / start.w;
    let ndc2 = end.xy / end.w;

    var dir = ndc2 - ndc1;
    dir *= uniforms.resolution;
    dir = normalize(dir);

    var offset = vec2f(-dir.y, dir.x) * uniforms.line_width / uniforms.resolution;

    if (in.id % 2u != 0u) { offset = -offset; }

    var out: VertexOutput;
    out.position = vec4f((ndc1 + offset) * start.w, start.z, start.w);
    out.color = in.color;
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4f {
    return in.color;
}
