package com.codex.blacklight.power;

import com.codex.blacklight.combat.LocalVisualMotion;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Replicated cosmetic long transformation; movement and combat can always interrupt it. */
@EventBusSubscriber(modid="blacklight")
public record PowerMorph(int formId,long started,float facing,Vec3 origin) {
    public static final PowerMorph NONE=new PowerMorph(-1,0,0,Vec3.ZERO);
    public static final StreamCodec<RegistryFriendlyByteBuf,PowerMorph> CODEC=new StreamCodec<>() {
        public PowerMorph decode(RegistryFriendlyByteBuf b){return new PowerMorph(b.readVarInt(),b.readVarLong(),b.readFloat(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
        public void encode(RegistryFriendlyByteBuf b,PowerMorph v){b.writeVarInt(v.formId);b.writeVarLong(v.started);b.writeFloat(v.facing);b.writeDouble(v.origin.x);b.writeDouble(v.origin.y);b.writeDouble(v.origin.z);}
    };
    public static String key(PowerForm form) {
        return switch(form){case CLAWS,BLADE,HAMMERFISTS,MUSCLE_MASS,WHIPFIST,THERMAL_VISION -> "morph_"+form.id();default->"";};
    }
    public String key(){return formId<0||formId>=PowerForm.values().length?"":key(PowerForm.values()[formId]);}
    public float seconds(Player player,float partial){return (player.level().getGameTime()-started+partial)/20F;}
    public boolean visible(Player p,float partial) {
        String key=key();
        if(key.isEmpty()||!com.codex.blacklight.mode.PrototypeModeSystem.isActive(p)||!p.isAlive()||p.isSpectator()
                ||!p.onGround()||p.isPassenger()||p.hurtTime>0||p.isUsingItem()||p.getDeltaMovement().horizontalDistanceSqr()>.0025
                ||p.getData(ModAttachments.WEAPON_VISUAL).spec()!=null||p.getData(ModAttachments.GRAB_STAGE)!=0
                ||p.getData(ModAttachments.CONSUMING)||p.getData(ModAttachments.JUMP_CHARGE)>0
                ||p.getData(ModAttachments.CONSUME_RECOVERY).active()
                ||p.getData(ModAttachments.HIT_REACTION).weight(p.level().getGameTime(),partial)>0)return false;
        var form=PowerForm.values()[formId];
        boolean active=UtilityPowerSystem.supported(form)?UtilityPowerSystem.active(p,form):p.getData(ModAttachments.PLAYER_STATE).getActivePower()==form;
        return active&&seconds(p,partial)>=0&&LocalVisualMotion.find(key).map(m->seconds(p,partial)<m.durationTicks()/20F).orElse(false);
    }
    public static void begin(ServerPlayer player,PowerForm form) {
        String key=key(form);
        if(key.isEmpty()||LocalVisualMotion.find(key).isEmpty())return;
        var morph=new PowerMorph(form.ordinal(),player.level().getGameTime(),player.yBodyRot,player.position());
        if(!morph.visible(player,0))return;
        player.setData(ModAttachments.POWER_MORPH,morph);player.syncData(ModAttachments.POWER_MORPH);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer p) {
            var morph=p.getData(ModAttachments.POWER_MORPH);
            if(morph.formId>=0&&!morph.visible(p,0)){p.setData(ModAttachments.POWER_MORPH,NONE);p.syncData(ModAttachments.POWER_MORPH);}
        }
    }
}
