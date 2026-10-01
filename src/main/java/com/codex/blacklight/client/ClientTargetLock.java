package com.codex.blacklight.client;

import com.codex.blacklight.combat.CombatTargeting;
import com.codex.blacklight.network.TargetLockPayload;
import com.codex.blacklight.power.PrototypeMetabolism;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class ClientTargetLock {
    private static net.minecraft.client.player.LocalPlayer previousPlayer;
    private static int previousId=-1;
    private record Focus(net.minecraft.client.player.LocalPlayer player,LivingEntity target,long tick,
            net.minecraft.world.phys.Vec3 eye,net.minecraft.world.phys.Vec3 targetPosition,net.minecraft.world.phys.Vec3 point) {}
    private static Focus cachedFocus;
    public static boolean active(){return PrototypeMetabolism.active(Minecraft.getInstance().player);}
    public static boolean valid(LivingEntity target){
        var p=Minecraft.getInstance().player;
        return p!=null && CombatTargeting.selectable(p,target);
    }
    public static boolean visibleInRange(LivingEntity target){
        var p=Minecraft.getInstance().player;
        double range=EpicFightCameraAPI.getInstance().getFocusingEntityPickRange();
        return valid(target)&&CombatTargeting.visibleWithinRange(p,target,range);
    }
    public static net.minecraft.world.phys.Vec3 focusPoint(LivingEntity target){
        var p=Minecraft.getInstance().player;
        if(p==null||!CombatTargeting.multipart(target))return target.getBoundingBox().getCenter();
        long tick=p.level().getGameTime();var eye=p.getEyePosition();var position=target.position();
        var cached=cachedFocus;
        if(cached!=null&&cached.player==p&&cached.target==target&&cached.tick==tick
                &&cached.eye.equals(eye)&&cached.targetPosition.equals(position))return cached.point;
        var point=CombatTargeting.lockPoint(p,target);
        cachedFocus=new Focus(p,target,tick,eye,position,point);
        return point;
    }
    public static net.minecraft.world.phys.AABB focusBounds(LivingEntity target){
        if(!CombatTargeting.multipart(target))return target.getBoundingBox();
        var point=focusPoint(target);
        return new net.minecraft.world.phys.AABB(point,point).inflate(.4);
    }
    /** A stale EF lock flag must never reserve the camera without a live target. */
    public static boolean hasValidLock(){
        var camera=EpicFightCameraAPI.getInstance();
        return active()&&camera.isLockingOnTarget()&&valid(camera.getFocusingEntity());
    }
    /** EF stores LivingEntity focus, while Minecraft's ray can hit a giant limb.
     * Promote that one hit to its parent without replacing an explicit lock. */
    public static void focusHitPart(){
        var minecraft=Minecraft.getInstance();var camera=EpicFightCameraAPI.getInstance();
        if(!active()||camera.isLockingOnTarget()&&valid(camera.getFocusingEntity()))return;
        // EF's detached camera traces farther than Minecraft's short interaction ray.
        var result=camera.getCrosshairHitResult();
        if(!(result instanceof net.minecraft.world.phys.EntityHitResult))result=minecraft.hitResult;
        if(result instanceof net.minecraft.world.phys.EntityHitResult hit
                &&hit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?>){
            var parent=CombatTargeting.resolveTarget(hit.getEntity());
            if(visibleInRange(parent)&&camera.getFocusingEntity()!=parent)((TargetFocusAccess)(Object)camera).blacklight$focusEntity(parent);
        }
    }
    public static void sync(){
        var p=Minecraft.getInstance().player;
        var camera=EpicFightCameraAPI.getInstance();
        if(p==null){
            previousPlayer=null;previousId=-1;cachedFocus=null;
            if(camera.isLockingOnTarget())camera.setLockOn(false);
            return;
        }
        boolean replaced=p!=previousPlayer;
        if(replaced){previousPlayer=p;previousId=-2;cachedFocus=null;}
        var target=camera.getFocusingEntity();
        if(camera.isLockingOnTarget()&&(replaced||!active()||!valid(target))){
            // setLockOn invokes sync through our mixin. The flag is already cleared
            // for that nested call, and previousId deduplicates its packet.
            camera.setLockOn(false);
        }
        int id=active()&&camera.isLockingOnTarget()&&valid(target)?target.getId():-1;
        if(id!=previousId){previousId=id;PacketDistributor.sendToServer(new TargetLockPayload(id));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){focusHitPart();sync();}
}
