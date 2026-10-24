struct Uniforms {
    proj_view_model: mat4x4f,
    resolution: vec2f,
    line_width: f32,
}

struct Line {
    sx: f32, sy: f32, sz: f32,
    ex: f32, ey: f32, ez: f32,
    color: u32,
}

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) color: vec4f,
}

@group(0) @binding(0) var<uniform> uniforms: Uniforms;
@group(0) @binding(1) var<storage, read> lines: array<Line>;

@vertex
fn vs_main(@builtin(vertex_index) id: u32) -> VertexOutput {
    let lline = lines[id / 6u];   // vertex_index = index value = 4 * lineIndex + corner
    let corners = array<u32, 6>(0u, 1u, 2u, 2u, 1u, 3u);
    let corner = corners[id % 6u];        // 0 = A+, 1 = A-, 2 = B+, 3 = B-

    let start = uniforms.proj_view_model * vec4f(lline.sx, lline.sy, lline.sz, 1.0);
    let end = uniforms.proj_view_model * vec4f(lline.ex, lline.ey, lline.ez, 1.0);

    let ndc1 = start.xy / start.w;
    let ndc2 = end.xy / end.w;

    let dir = normalize((ndc2 - ndc1) * uniforms.resolution);

    let isEnd = (corner >> 1u) == 1u;
    let side = f32(corner & 1u) * -2.0 + 1.0;
    let offset = vec2f(-dir.y, dir.x) * uniforms.line_width / uniforms.resolution * side;

    let p = select(start, end, isEnd);
    let ndc = select(ndc1, ndc2, isEnd);

    var out: VertexOutput;
    out.position = vec4f((ndc + offset) * p.w, p.z, p.w);
    out.color = unpack4x8unorm(lline.color);
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4f {
    return in.color;
}
