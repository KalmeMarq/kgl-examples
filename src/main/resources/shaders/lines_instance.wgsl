struct Uniforms {
    proj_view_model: mat4x4f,
    resolution: vec2f,
    line_width: f32,
}

struct VertexInput {
    @builtin(vertex_index) id: u32,
    @location(0) start: vec3f,
    @location(1) end: vec3f,
    @location(2) color: vec4f,
}

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) color: vec4f,
}

@group(0) @binding(0) var<uniform> uniforms: Uniforms;

@vertex
fn vs_main(in: VertexInput) -> VertexOutput {
    let start = uniforms.proj_view_model * vec4<f32>(in.start, 1.0);
    let end = uniforms.proj_view_model * vec4<f32>(in.end, 1.0);

    let ndc1 = start.xy / start.w;
    let ndc2 = end.xy / end.w;

    var dir = ndc2 - ndc1;
    dir *= uniforms.resolution;
    dir = normalize(dir);

    let isEnd = (in.id >> 1u) == 1u;
    let side = f32(in.id & 1u) * -2.0 + 1.0;

    var offset = vec2f(-dir.y, dir.x) * (uniforms.line_width / uniforms.resolution) * side;

    let p = select(start, end, isEnd);
    let ndc = select(ndc1, ndc2, isEnd);

    var out: VertexOutput;
    out.position = vec4f((ndc + offset) * p.w, p.z, p.w);
    out.color = in.color;
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4f {
    return in.color;
}
