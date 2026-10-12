package com.codex.blacklight.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;

/** Independently sampled local bone deltas; no game entity or rendering engine owns this data. */
public final class BlacklightPose {
    private final Map<String, BoneTransform> transforms = new LinkedHashMap<>();
    public Map<String, BoneTransform> getJointTransformData() { return transforms; }
    public BoneTransform orElseEmpty(String bone) { return transforms.computeIfAbsent(bone, ignored -> new BoneTransform()); }
    public BoneTransform get(String bone) { return transforms.get(bone); }
    public boolean hasTransform(String bone) { return transforms.containsKey(bone); }
    public BlacklightPose copy() {
        var result = new BlacklightPose();
        transforms.forEach((name, value) -> result.transforms.put(name, value.copy()));
        return result;
    }
    public static BlacklightPose interpolatePose(BlacklightPose from, BlacklightPose to, float weight) {
        var result = new BlacklightPose();
        var names = new HashSet<>(from.transforms.keySet()); names.addAll(to.transforms.keySet());
        for (String name : names) result.transforms.put(name, BoneTransform.blend(
                from.transforms.getOrDefault(name, new BoneTransform()),
                to.transforms.getOrDefault(name, new BoneTransform()), weight));
        return result;
    }
}
