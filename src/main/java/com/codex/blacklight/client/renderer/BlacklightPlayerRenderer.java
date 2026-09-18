package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.renderer.layer.BlacklightClawLayer;
import com.codex.blacklight.client.tuning.ClawTransformSettings;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/** Epic Fight player renderer that replaces the biological limbs at model-preparation time. */
public final class BlacklightPlayerRenderer extends PPlayerRenderer {
    public BlacklightPlayerRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
        super(context, entityType);
        addCustomLayer(new BlacklightClawLayer());
    }

    @Override
    protected void prepareModel(HumanoidMesh mesh,
                                AbstractClientPlayer player,
                                AbstractClientPlayerPatch<AbstractClientPlayer> entityPatch,
                                PlayerRenderer renderer) {
        super.prepareModel(mesh, player, entityPatch, renderer);
        var state = player.getData(ModAttachments.PLAYER_STATE);
        var form = state.getActivePower();
        boolean weapon = state.isPrototypeMode() && (form == PowerForm.HAMMERFISTS || form == PowerForm.BLADE || form == PowerForm.WHIPFIST);
        if (weapon) {
            mesh.rightArm.setHidden(true); mesh.rightSleeve.setHidden(true);
            if (form == PowerForm.HAMMERFISTS) { mesh.leftArm.setHidden(true); mesh.leftSleeve.setHidden(true); }
            return;
        }
        float morphProgress = ClawTransformSettings.preview()
                ? 1.0F : ClawMorphTracker.progress(player, 0.0F);
        if (morphProgress < 0.72F) return;

        // Hide both the base skin limbs and their second-layer sleeves. This happens after
        // PPlayerRenderer has copied vanilla visibility, so Epic Fight cannot overwrite it.
        mesh.leftArm.setHidden(true);
        mesh.rightArm.setHidden(true);
        mesh.leftSleeve.setHidden(true);
        mesh.rightSleeve.setHidden(true);
    }
}
