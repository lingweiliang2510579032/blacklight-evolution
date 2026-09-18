package com.codex.blacklight.gametest;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.network.ClawActionPayload.Action;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("blacklight") @PrefixGameTestTemplate(false)
public final class GrabInteractionGameTest {
    private static ServerPlayer player(GameTestHelper h) {
        for(int x=1;x<10;x++)for(int z=1;z<10;z++)h.setBlock(x,1,z,Blocks.STONE);
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"grab-input"));
        p.setPos(h.absoluteVec(new Vec3(3,2,3)));p.setYRot(0);p.setXRot(0);p.setOnGround(true);p.tickCount=100;
        var s=p.getData(ModAttachments.PLAYER_STATE);s.infect();s.setPrototypeMode(true);s.activate(PowerForm.CLAWS);
        p.setData(ModAttachments.PLAYER_STATE,s);p.setHealth(10);return p;
    }
    private static void tick(ServerPlayer p,int count) {for(int i=0;i<count;i++){p.tickCount++;ConsumeSystem.tick(new PlayerTickEvent.Post(p));}}
    @GameTest(template="wall_test",timeoutTicks=30)
    public static void consumeCannotGrabOrQueueWhileReaching(GameTestHelper h) {
        var p=player(h);var cow=h.spawn(EntityType.COW,3,2,5);cow.setNoAi(true);
        ClawCombatSystem.handle(p,Action.CONSUME);
        h.assertTrue(!p.getData(ModAttachments.CONSUMING),"secondary intent without held target does not auto-grab");
        ClawCombatSystem.handle(p,Action.GRAB);ClawCombatSystem.handle(p,Action.CONSUME);tick(p,120);
        h.assertTrue(ConsumeSystem.holding(p)&&cow.isAlive()&&p.getHealth()==10,"early click cannot queue an automatic consume");
        h.assertTrue(cow.getData(ModAttachments.CONSUME_VISUAL).progress(h.getLevel().getGameTime()+10000,0)==0,"holding never dissolves");
        ClawCombatSystem.handle(p,Action.CONSUME);tick(p,46);
        h.assertTrue(cow.isRemoved()&&p.getHealth()==12,"fresh secondary click executes held target");h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=30)
    public static void primaryAndRepeatedGrabThrowAtReleaseFrameWithoutRewards(GameTestHelper h) {
        for(var action:new Action[]{Action.THROW,Action.GRAB}) {
            var p=player(h);var cow=h.spawn(EntityType.COW,3,2,5);cow.setNoAi(true);
            ClawCombatSystem.handle(p,Action.GRAB);tick(p,12);ClawCombatSystem.handle(p,action);tick(p,5);
            h.assertTrue(p.getData(ModAttachments.GRAB_STAGE)==ConsumeVisual.THROW,"throw waits for hand release");
            tick(p,1);ClawCombatSystem.handle(p,Action.THROW);ClawCombatSystem.handle(p,Action.CONSUME);
            h.assertTrue(cow.isAlive()&&cow.getData(ModAttachments.CONSUME_VISUAL).stage()==ConsumeVisual.FLIGHT&&!p.getData(ModAttachments.CONSUMING),"throw releases hold and begins free flight");
            var freeze=new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(cow);ConsumeSystem.freeze(freeze);
            h.assertTrue(!freeze.isCanceled(),"flight is not held by the capture freeze");
            h.assertTrue(cow.getDeltaMovement().z>1&&cow.getDeltaMovement().y>0,"victim launches in look direction");
            h.assertTrue(p.getHealth()==10&&p.getData(ModAttachments.PLAYER_STATE).getBiomass()==250&&p.getData(ModAttachments.PLAYER_STATE).getEvolutionPoints()==0,"throw and duplicate packets award no consumption resources");
            Vec3 launch=cow.position();
            for(int i=0;i<3;i++)ConsumeSystem.thrown(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(cow));
            h.assertTrue(cow.position().distanceToSqr(launch)>4,"real flight displacement must work even for pre-existing NoAI victims");
            h.assertTrue(cow.isNoAi(),"flight must not change the original persistent NoAI setting");
            cow.discard();
        }
        h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=30)
    public static void heldTargetFollowsWalkingAndTurning(GameTestHelper h) {
        var p=player(h);var cow=h.spawn(EntityType.COW,3,2,5);cow.setNoAi(true);
        ClawCombatSystem.handle(p,Action.GRAB);tick(p,12);Vec3 before=cow.position();
        for(int i=0;i<20;i++){p.setPos(p.position().add(.07,0,0));p.setYRot(i*4);tick(p,1);}
        h.assertTrue(ConsumeSystem.holding(p),"turning while walking must preserve the held state");
        Vec3 forward=Vec3.directionFromRotation(0,p.getYRot());
        Vec3 expected=p.position().add(forward.scale(1.05)).add(forward.z*.28,Math.clamp(1.3-cow.getBbHeight()*.72,0,.65),-forward.x*.28);
        h.assertTrue(cow.position().distanceToSqr(expected)<.01&&cow.position().distanceToSqr(before)>.09,"held body follows the rotated hand anchor after 1.4 blocks of walking");ConsumeSystem.cancel(p);cow.discard();h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=30)
    public static void heldClicksDoNotStartGroundspikeOrClawAttack(GameTestHelper h) {
        var p=player(h);var cow=h.spawn(EntityType.COW,3,2,5);cow.setNoAi(true);
        ClawCombatSystem.handle(p,Action.GRAB);tick(p,12);
        for(var action:new Action[]{Action.ATTACK,Action.GROUND_SPIKE_CHARGE_START,Action.CYCLE_FORM,Action.TOGGLE_CLAWS})ClawCombatSystem.handle(p,action);
        h.assertTrue(ConsumeSystem.holding(p)&&p.getData(ModAttachments.CLAW_COMBAT).action()==ClawCombatState.Action.IDLE,"held actions exclude combat and morph");
        ConsumeSystem.cancel(p);cow.discard();h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=30)
    public static void leavingPrototypeModeReleasesHeldBody(GameTestHelper h) {
        var p=player(h);var cow=h.spawn(EntityType.COW,3,2,5);
        ClawCombatSystem.handle(p,Action.GRAB);tick(p,12);p.getData(ModAttachments.PLAYER_STATE).setPrototypeMode(false);tick(p,1);
        h.assertTrue(!p.getData(ModAttachments.CONSUMING)&&p.getData(ModAttachments.GRAB_STAGE)==0&&!cow.getData(ModAttachments.CONSUME_VISUAL).active()&&!cow.isNoAi(),"mode change frees target without persisting NoAI");
        h.assertTrue(p.getHealth()==10&&cow.isAlive(),"release is not consume");cow.discard();h.succeed();
    }
}
