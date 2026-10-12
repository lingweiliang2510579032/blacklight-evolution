package com.codex.blacklight.animation;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Blacklight's mutable local bone delta. Each sampled pose owns its transforms. */
public final class BoneTransform {
    private final Vector3f translation;
    private final Quaternionf rotation;
    private final Vector3f scale;
    public BoneTransform() { this(new Vector3f(), new Quaternionf(), new Vector3f(1)); }
    public BoneTransform(Vector3f translation, Quaternionf rotation, Vector3f scale) {
        this.translation = new Vector3f(translation);
        this.rotation = new Quaternionf(rotation);
        this.scale = new Vector3f(scale);
    }
    public Vector3f translation() { return translation; }
    public Quaternionf rotation() { return rotation; }
    public Vector3f scale() { return scale; }
    public BoneTransform copy() { return new BoneTransform(translation, rotation, scale); }
    public Matrix4f toMatrix() { return new Matrix4f().translationRotateScale(translation, rotation, scale); }
    public static BoneTransform translation(Vector3f value) { return new BoneTransform(value, new Quaternionf(), new Vector3f(1)); }
    public static BoneTransform fromMatrix(Matrix4f matrix) {
        var scale = matrix.getScale(new Vector3f());
        if (matrix.determinant3x3() < 0) scale.x = -scale.x;
        var normalized = new Matrix4f(matrix).scale(1 / scale.x, 1 / scale.y, 1 / scale.z);
        return new BoneTransform(matrix.getTranslation(new Vector3f()),
                normalized.getNormalizedRotation(new Quaternionf()).normalize(), scale);
    }
    public static BoneTransform blend(BoneTransform from, BoneTransform to, float weight) {
        float t = Math.clamp(weight, 0, 1);
        return new BoneTransform(new Vector3f(from.translation).lerp(to.translation, t),
                new Quaternionf(from.rotation).slerp(to.rotation, t).normalize(),
                new Vector3f(from.scale).lerp(to.scale, t));
    }
}
