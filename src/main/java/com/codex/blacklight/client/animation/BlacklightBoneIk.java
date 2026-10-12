package com.codex.blacklight.client.animation;

import com.codex.blacklight.animation.BlacklightPose;
import com.codex.blacklight.animation.BlacklightSkeleton;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Original constrained CCD solver. Joint lengths and the shared bind rig remain immutable. */
public final class BlacklightBoneIk {
    private BlacklightBoneIk() { }
    public static void reach(BlacklightSkeleton skeleton, BlacklightPose pose, String side,
                             Vector3f target, Vector3f palmLocal, int iterations) {
        var wrist = skeleton.searchJointByName("Tool_" + side);
        if (wrist == null) return;
        for (int i = 0; i < iterations; i++) {
            for (String name : new String[]{"Hand_" + side, "Arm_" + side}) {
                var bone = skeleton.searchJointByName(name);
                if (bone == null) continue;
                Matrix4f world = skeleton.getBoundTransformFor(pose, bone);
                Vector3f center = world.getTranslation(new Vector3f());
                Vector3f palm = skeleton.getBoundTransformFor(pose, wrist).transformPosition(new Vector3f(palmLocal));
                Vector3f current = palm.sub(center, new Vector3f());
                Vector3f desired = target.sub(center, new Vector3f());
                if (current.lengthSquared() < 1e-8F || desired.lengthSquared() < 1e-8F) continue;
                Quaternionf turn = new Quaternionf().rotationTo(current.normalize(), desired.normalize());
                // Damp large corrections to avoid elbow jitter when the target crosses the shoulder.
                turn.set(new Quaternionf().slerp(turn, .7F));
                Quaternionf worldRotation = world.getUnnormalizedRotation(new Quaternionf()).normalize();
                Quaternionf poseRotation = pose.orElseEmpty(name).rotation();
                Quaternionf basis = new Quaternionf(worldRotation).mul(new Quaternionf(poseRotation).invert());
                Quaternionf localTurn = new Quaternionf(basis).invert().mul(turn).mul(basis);
                poseRotation.premul(localTurn).normalize();
            }
            Vector3f palm = skeleton.getBoundTransformFor(pose, wrist).transformPosition(new Vector3f(palmLocal));
            if (palm.distanceSquared(target) < .000025F) break;
        }
    }
}
