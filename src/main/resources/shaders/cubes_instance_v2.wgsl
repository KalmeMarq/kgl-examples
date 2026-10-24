struct Uniforms {
    proj_view: mat4x4f,
    model: mat4x4f,
}

struct GridUniforms {
    size: i32,
    time: f32,
    scale: f32,
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
@group(0) @binding(1) var<uniform> grid_uniforms: GridUniforms;

// @include("common")

@vertex
fn vs_main(in: VertexInput, @builtin(instance_index) idx: u32) -> VertexOutput {
    let side = u32(grid_uniforms.size);
    let x = i32(idx % side);
    let y = i32(idx / side);

    let px = -15.0 * (1.0 / grid_uniforms.scale) + f32(x) * 3.0;
    let py = -15.0 * (1.0 / grid_uniforms.scale) + f32(y) * 3.0;

    let pos = vec3f(px, py, 0.0);
    let ax = f32(x) * 0.21 + grid_uniforms.time;
    let ay = f32(y) * 0.37 + grid_uniforms.time;
    let az = 0.0;

    let local = rotate_xyz(in.position, vec3f(ax, ay, az));
    let world = local + pos;

    var out: VertexOutput;
    out.position = uniforms.proj_view * uniforms.model * vec4f(world, 1.0);
    out.color = in.color;
    return out;
}

@fragment
fn fs_main(in: VertexOutput) -> @location(0) vec4f {
    return in.color;
}