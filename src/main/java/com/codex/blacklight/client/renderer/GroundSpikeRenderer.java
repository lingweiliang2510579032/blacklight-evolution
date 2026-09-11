package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.model.GroundSpikeModel;
import com.codex.blacklight.entity.GroundSpikeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GroundSpikeRenderer extends GeoEntityRenderer<GroundSpikeEntity> {
    public GroundSpikeRenderer(EntityRendererProvider.Context context) {
        super(context, new GroundSpikeModel());
        withScale(1.65F);
        shadowRadius = 0.0F;
    }
}
