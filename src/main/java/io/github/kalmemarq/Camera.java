package io.github.kalmemarq;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private static final float MAX_PITCH = (float) Math.toRadians(90);

    public Vector3f position = new Vector3f(50.0f, 22.0f, 45.0f);
    public float yaw;
    public float pitch;

    public void move(float forward, float right, float up) {
        float sin = (float) Math.sin(this.yaw);
        float cos = (float) Math.cos(this.yaw);

        this.position.x += -sin * forward + cos * right;
        this.position.z += -cos * forward - sin * right;
        this.position.y += up;
    }

    public void rotate(float y, float p) {
        this.yaw += y;
        this.pitch = Math.clamp(this.pitch + p, -MAX_PITCH, MAX_PITCH);
    }

    public Matrix4f getView(Matrix4f dest) {
        return dest.identity()
                .rotateX(-this.pitch)
                .rotateY(-this.yaw)
                .translate(-this.position.x, -this.position.y, -this.position.z);
    }
}
