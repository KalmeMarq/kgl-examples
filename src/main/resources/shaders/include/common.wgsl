fn rotate_xyz(pos: vec3f, angles: vec3f) -> vec3f {
    let c = cos(angles);
    let s = sin(angles);

    let rotX = mat3x3f(
        1.0, 0.0,  0.0,
        0.0, c.x,  s.x,
        0.0, -s.x, c.x,
    );

    let rotY = mat3x3f(
        c.y, 0.0, -s.y,
        0.0, 1.0,  0.0,
        s.y, 0.0,  c.y,
    );

    let rotZ = mat3x3f(
        c.z,  s.z, 0.0,
        -s.z, c.z, 0.0,
        0.0,  0.0, 1.0,
    );

    return rotX * rotY * rotZ * pos;
}