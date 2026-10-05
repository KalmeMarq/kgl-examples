package io.github.kalmemarq.kgl.util;

public class ColorUtils {
    public static int argb(float r, float g, float b, float a) {
        return (int) (r * 255f) << 16 | (int) (g * 255f) << 8 | (int) (b * 255f) | (int) (a * 255f) << 24;
    }
}
