struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
    @location(1) texcoord: vec2f,
}

struct FragmentOutput {
    @location(0) all: vec4f,
    @location(1) r: vec4f,
    @location(2) g: vec4f,
    @location(3) b: vec4f,
}

@vertex
fn vs_main(@builtin(vertex_index) id: u32) -> VertexOutput {
    let positions = array<vec3f, 3>(
        vec3f(-0.5, -0.5, 0.0),
        vec3f( 0.5, -0.5, 0.0),
        vec3f( 0.0,  0.5, 0.0)
    );

    let texcoords = array<vec2f, 3>(
        vec2f(0.0, 0.0),
        vec2f(1.0, 0.0),
        vec2f(0.5, 1.0)
    );

    var out : VertexOutput;
    out.position = vec4f(positions[id], 1.0);
    out.color = vec4f(1.0, 1.0, 1.0, 1.0);
    out.texcoord = texcoords[id];
    return out;
}

@group(0) @binding(0) var txr: texture_2d<f32>;
@group(0) @binding(1) var smp: sampler;

@fragment
fn fs_main(in : VertexOutput) -> FragmentOutput {
    let color = textureSample(txr, smp, in.texcoord) * in.color;

    var out : FragmentOutput;
    out.all = vec4f(color.rgb, 1.0);
    out.r = vec4f(color.r, 0.0, 0.0, 1.0);
    out.g = vec4f(0.0, color.g, 0.0, 1.0);
    out.b = vec4f(0.0, 0.0, color.b, 1.0);
    return out;
}