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
    public static void sync(){
        var p=Minecraft.getInstance().player;
        if(p==null){previousPlayer=null;previousId=-1;return;}
        if(p!=previousPlayer){previousPlayer=p;previousId=-2;}
        var camera=EpicFightCameraAPI.getInstance();var target=camera.getFocusingEntity();
        int id=active()&&camera.isLockingOnTarget()&&valid(target)?target.getId():-1;
        if(id!=previousId){previousId=id;PacketDistributor.sendToServer(new TargetLockPayload(id));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){sync();}
}
