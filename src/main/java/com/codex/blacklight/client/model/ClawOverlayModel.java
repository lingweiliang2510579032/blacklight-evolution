package com.codex.blacklight.client.model;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.animation.ClawOverlay;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ClawOverlayModel extends GeoModel<ClawOverlay> {
    @Override
    public ResourceLocation getModelResource(ClawOverlay animatable) {
        return new ResourceLocation(BlacklightNeoMod.MOD_ID, "geo/claw_overlay.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ClawOverlay animatable) {
        return new ResourceLocation(BlacklightNeoMod.MOD_ID, "textures/entity/prototype_avatar.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ClawOverlay animatable) {
        return new ResourceLocation(BlacklightNeoMod.MOD_ID, "animations/prototype_avatar.animation.json");
    }
}
