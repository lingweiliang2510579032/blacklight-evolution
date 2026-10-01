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
    public static boolean active(){return PrototypeMetabolism.active(Minecraft.getInstance().player);}
    public static boolean valid(LivingEntity target){
        var p=Minecraft.getInstance().player;
        return p!=null && CombatTargeting.selectable(p,target);
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
        if(minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                &&hit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?>){
            var parent=CombatTargeting.resolveTarget(hit.getEntity());
            if(valid(parent)&&camera.getFocusingEntity()!=parent)((TargetFocusAccess)(Object)camera).blacklight$focusEntity(parent);
        }
    }
    public static void sync(){
        var p=Minecraft.getInstance().player;
        var camera=EpicFightCameraAPI.getInstance();
        if(p==null){
            previousPlayer=null;previousId=-1;
            if(camera.isLockingOnTarget())camera.setLockOn(false);
            return;
        }
        boolean replaced=p!=previousPlayer;
        if(replaced){previousPlayer=p;previousId=-2;}
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
