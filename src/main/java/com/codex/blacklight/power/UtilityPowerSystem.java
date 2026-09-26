package com.codex.blacklight.power;

import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.*;

/** Defence and vision coexist with a weapon; each category has one active power. */
public final class UtilityPowerSystem {
    private static final Map<ServerPlayer,Integer> SHIELD_RECOVERY=new WeakHashMap<>();
    public static boolean supported(PowerForm f){return f==PowerForm.SHIELD||f==PowerForm.ARMOR||f==PowerForm.THERMAL_VISION||f==PowerForm.INFECTED_VISION;}
    public static boolean active(net.minecraft.world.entity.player.Player p,PowerForm form){return p.getData(ModAttachments.PLAYER_STATE).isPrototypeMode()&&(p.getData(ModAttachments.UTILITY_POWERS)&form.bit())!=0;}
    public static void toggle(ServerPlayer p,PowerForm form){
        int mask=p.getData(ModAttachments.UTILITY_POWERS),group=form==PowerForm.SHIELD||form==PowerForm.ARMOR?PowerForm.SHIELD.bit()|PowerForm.ARMOR.bit():PowerForm.THERMAL_VISION.bit()|PowerForm.INFECTED_VISION.bit();
        mask=(mask&form.bit())!=0?mask&~form.bit():(mask&~group)|form.bit();
        p.setData(ModAttachments.UTILITY_POWERS,mask);p.syncData(ModAttachments.UTILITY_POWERS);attributes(p);
    }
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> type,String name,double value,AttributeModifier.Operation operation,boolean enabled){
        var attribute=p.getAttribute(type);if(attribute==null)return;var id=ResourceLocation.fromNamespaceAndPath("blacklight",name);
        if(enabled&&!attribute.hasModifier(id))attribute.addTransientModifier(new AttributeModifier(id,value,operation));else if(!enabled)attribute.removeModifier(id);
    }
    private static void attributes(ServerPlayer p){
        boolean armor=active(p,PowerForm.ARMOR);
        modifier(p,Attributes.ARMOR,"armor_plate",12,AttributeModifier.Operation.ADD_VALUE,armor);
        modifier(p,Attributes.ARMOR_TOUGHNESS,"armor_toughness",4,AttributeModifier.Operation.ADD_VALUE,armor);
        modifier(p,Attributes.MOVEMENT_SPEED,"armor_weight",-.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,armor);
        modifier(p,Attributes.KNOCKBACK_RESISTANCE,"armor_stability",.6,AttributeModifier.Operation.ADD_VALUE,armor);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p){
        if(!p.isAlive()||!p.getData(ModAttachments.PLAYER_STATE).isPrototypeMode()){
            if(p.getData(ModAttachments.UTILITY_POWERS)!=0){p.setData(ModAttachments.UTILITY_POWERS,0);p.syncData(ModAttachments.UTILITY_POWERS);}SHIELD_RECOVERY.remove(p);
        }
        if(p.tickCount%5==0)attributes(p);
    }}
    @SubscribeEvent public static void damage(LivingDamageEvent.Pre e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!active(p,PowerForm.SHIELD)||e.getNewDamage()<=0||p.tickCount<SHIELD_RECOVERY.getOrDefault(p,0))return;
        var source=e.getSource();var pos=source.getSourcePosition();if(pos==null||source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))return;
        var toward=pos.subtract(p.position()).multiply(1,0,1).normalize();var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,p.getYRot());
        if(forward.dot(toward)<.35)return;
        e.setNewDamage(e.getNewDamage()*.2F);SHIELD_RECOVERY.put(p,p.tickCount+40);
        p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,net.minecraft.sounds.SoundSource.PLAYERS,1,.7F);
    }
}
