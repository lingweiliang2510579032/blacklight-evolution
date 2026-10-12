package com.codex.blacklight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ClientPoseMatrices {
    private ClientPoseMatrices() {}
    public static void multiply(PoseStack stack, Matrix4f matrix) {
        stack.mulPoseMatrix(matrix);
        stack.last().normal().mul(new Matrix3f(matrix).invert().transpose());
    }
}
