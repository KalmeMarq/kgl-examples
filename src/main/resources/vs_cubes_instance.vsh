#version 450

layout(std140, binding = 0) uniform Uniforms {
    mat4 proj;
    mat4 view;
    mat4 model;
} uniforms;

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec4 inst_pos_rot;

layout(location = 0) out vec4 v_color;

out gl_PerVertex {
    vec4 gl_Position;
};

vec3 rotateXYZ(vec3 v, float ax, float ay, float az) {
    float sx = sin(ax), cx = cos(ax);
    float sy = sin(ay), cy = cos(ay);
    float sz = sin(az), cz = cos(az);

    // This is exactly R * v where R is JOML's rotationXYZ matrix
    float x =  v.x * (cy*cz)  + v.y * (cy*-sz) + v.z * sy;
    float y =  v.x * (sx*sy*cz + cx*sz)
    + v.y * (-sx*sy*sz + cx*cz)
    + v.z * (-sx*cy);
    float z =  v.x * (-cx*sy*cz + sx*sz)
    + v.y * (cx*sy*sz + sx*cz)
    + v.z * (cx*cy);

    return vec3(x, y, z);
}

void main() {
    vec3 pos = vec3(inst_pos_rot.xy, 0.0);
    float ax = inst_pos_rot.z;
    float ay = inst_pos_rot.w;
    float az = 0.0;               // you currently don’t store a Z angle

    vec3 local = rotateXYZ(position, ax, ay, az);
    vec3 world = local + pos;

    gl_Position = uniforms.proj * uniforms.view * uniforms.model * vec4(world, 1.0);
    v_color = color;
}