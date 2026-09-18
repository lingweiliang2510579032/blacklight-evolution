package com.codex.blacklight.client.model;

import com.codex.blacklight.entity.BlacklightHunter;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BlacklightHunterModel extends GeoModel<BlacklightHunter> {
    private static ResourceLocation resource(String path) { return ResourceLocation.fromNamespaceAndPath("blacklight", path); }
    @Override public ResourceLocation getModelResource(BlacklightHunter entity) { return resource("geo/blacklight_hunter.geo.json"); }
    @Override public ResourceLocation getTextureResource(BlacklightHunter entity) { return resource("textures/entity/claw_biomass.png"); }
    @Override public ResourceLocation getAnimationResource(BlacklightHunter entity) { return resource("animations/blacklight_hunter.animation.json"); }

    @Override public void setCustomAnimations(BlacklightHunter entity, long instanceId,
            software.bernie.geckolib.animation.AnimationState<BlacklightHunter> state) {
        super.setCustomAnimations(entity, instanceId, state);
        var visual = entity.getData(com.codex.blacklight.registry.ModAttachments.CONSUME_VISUAL);
        float t = visual.active() ? visual.progress(entity.level().getGameTime(), state.getPartialTick()) : 0;
        if (visual.active()) {
            var reaction = com.codex.blacklight.client.animation.ConsumeVictimPose.reaction(entity, state.getPartialTick());
            applyReaction("torso", reaction.bend()*.55F, reaction.twist(), 0);
            applyReaction("head", -reaction.head()*.65F, -reaction.twist(), reaction.twist()*.4F);
            for(String side:new String[]{"right","left"}) {
                float sign=side.equals("right")?1:-1;
                applyReaction(side+"_arm", -reaction.brace()*1.2F, sign*.12F, sign*.28F);
                applyReaction(side+"_forearm", -.4F-reaction.brace()*.6F, 0, 0);
                applyReaction(side+"_thigh", -reaction.kick()*.6F, 0, sign*.10F);
                applyReaction(side+"_shin", reaction.kick()*.95F, 0, 0);
            }
        }
        // Finish converting the remaining trunk into the tendril mass before recovery;
        // parent scaling must not leave a miniature or needle-shaped whole creature.
        var root = getAnimationProcessor().getBone("root");
        if (root != null) { root.setHidden(t >= .73F); root.setChildrenHidden(t >= .73F); }
        String[] names = {"right_forearm","left_forearm","right_shin","left_shin","head",
                "right_arm","left_arm","right_thigh","left_thigh","torso","hips"};
        float[] starts = {.36F,.4F,.44F,.46F,.5F,.55F,.58F,.62F,.64F,.69F,.73F};
        for (int i=0;i<names.length;i++) {
            var bone = getAnimationProcessor().getBone(names[i]); if (bone == null) continue;
            float phase = Math.max(0, Math.min(1, (t-starts[i])/.19F));
            float size = 1 - phase*phase*(3-2*phase);
            bone.setHidden(size < .15F); bone.setChildrenHidden(size < .15F);
            bone.setScaleX(Math.max(.01F,size)); bone.setScaleY(1); bone.setScaleZ(Math.max(.01F,size));
        }
    }
    private void applyReaction(String name, float x, float y, float z) {
        var bone=getAnimationProcessor().getBone(name);if(bone==null)return;
        bone.setRotX(x);bone.setRotY(y);bone.setRotZ(z);
    }
}
