package dev.fashion.gfx;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class Projection {
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static final Vector4f TMP = new Vector4f();
    private static Vec3d camera = Vec3d.ZERO;
    private static float tickProgress;
    private static boolean valid;

    private Projection() {
    }

    public static void capture(Camera cam, Matrix4f view, Matrix4f projection, float progress) {
        VIEW_PROJ.set(projection).mul(view);
        camera = cam.getCameraPos();
        tickProgress = progress;
        valid = true;
    }

    public static float tickProgress() {
        return tickProgress;
    }

    public static boolean project(double x, double y, double z, int fbW, int fbH, float[] out) {
        if (!valid) {
            return false;
        }
        TMP.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1f);
        VIEW_PROJ.transform(TMP);
        if (TMP.w <= 0.05f) {
            return false;
        }
        float nx = TMP.x / TMP.w;
        float ny = TMP.y / TMP.w;
        out[0] = (nx * 0.5f + 0.5f) * fbW;
        out[1] = (1f - (ny * 0.5f + 0.5f)) * fbH;
        out[2] = TMP.w;
        return true;
    }
}
