package com.codex.blacklight.client.model;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.animation.PrototypeAvatar;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class PrototypeAvatarModel extends GeoModel<PrototypeAvatar> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            BlacklightNeoMod.MOD_ID, "geo/prototype_avatar.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BlacklightNeoMod.MOD_ID, "textures/entity/prototype_avatar.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            BlacklightNeoMod.MOD_ID, "animations/prototype_avatar.animation.json");

    @Override
    public ResourceLocation getModelResource(PrototypeAvatar animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PrototypeAvatar animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PrototypeAvatar animatable) {
        return ANIMATION;
    }
}
