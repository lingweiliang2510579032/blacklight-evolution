package com.codex.blacklight.client.model;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.entity.GroundSpikeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GroundSpikeModel extends GeoModel<GroundSpikeEntity> {
    @Override
    public ResourceLocation getModelResource(GroundSpikeEntity animatable) {
        return new ResourceLocation(BlacklightNeoMod.MOD_ID, "geo/ground_spike.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GroundSpikeEntity animatable) {
        return new ResourceLocation(BlacklightNeoMod.MOD_ID, "textures/entity/claw_biomass.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GroundSpikeEntity animatable) {
        return new ResourceLocation(BlacklightNeoMod.MOD_ID, "animations/ground_spike.animation.json");
    }
}
