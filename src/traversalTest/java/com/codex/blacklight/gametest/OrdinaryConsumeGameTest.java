package com.codex.blacklight.gametest;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.power.*;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("blacklight") @PrefixGameTestTemplate(false)
public final class OrdinaryConsumeGameTest {
    private static ServerPlayer player(GameTestHelper h, PowerForm form) {
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"form-consume"));
        p.setPos(h.absoluteVec(new Vec3(3,2,3))); p.setYRot(0);p.setXRot(0);p.setOnGround(true);p.tickCount=100;
        var s=p.getData(ModAttachments.PLAYER_STATE);s.infect();s.setPrototypeMode(true);s.unlock(form);s.activate(form);
        p.setData(ModAttachments.PLAYER_STATE,s);p.setHealth(10);return p;
    }
    private static void tick(ServerPlayer p) { p.tickCount++;ConsumeSystem.tick(new PlayerTickEvent.Post(p)); }

    @GameTest(template="wall_test", timeoutTicks=30)
    public static void everyFormConsumesPassiveAndRewardsOnce(GameTestHelper h) {
        for (var form : new PowerForm[]{PowerForm.NONE,PowerForm.CLAWS,PowerForm.HAMMERFISTS,PowerForm.BLADE,PowerForm.WHIPFIST}) {
            var p=player(h,form);var cow=h.spawn(EntityType.COW,3,2,5);cow.setNoAi(true);
            ConsumeSystem.request(p);var visual=cow.getData(ModAttachments.CONSUME_VISUAL);
            h.assertTrue(visual.active() && visual.form()==form.ordinal(),"full-health cow accepted with form "+form);
            for(int i=0;i<ConsumeVisual.captureDuration(form)+80;i++)tick(p);
            h.assertTrue(ConsumeSystem.holding(p)&&cow.isAlive()&&p.getHealth()==10,"E must hold indefinitely without reward "+form);
            ConsumeSystem.consume(p);ConsumeSystem.consume(p);
            for(int i=0;i<ConsumeVisual.duration(form)-1;i++)tick(p);
            h.assertTrue(!cow.isRemoved()&&p.getHealth()==10,"no early award in "+form);
            tick(p);tick(p);
            h.assertTrue(cow.isRemoved()&&!p.getData(ModAttachments.CONSUMING),"complete releases target and player "+form);
            h.assertTrue(p.getHealth()==12&&p.getData(ModAttachments.PLAYER_STATE).getBiomass()==270
                    &&p.getData(ModAttachments.PLAYER_STATE).getEvolutionPoints()==2,"cow reward once, not hunter reward "+form);
        }
        h.succeed();
    }
    @GameTest(template="wall_test", timeoutTicks=30)
    public static void normalEnemyNeedsWeakeningAndClaimsAreExclusive(GameTestHelper h) {
        var p=player(h,PowerForm.BLADE);var z=h.spawn(EntityType.ZOMBIE,3,2,5);z.setNoAi(true);
        ConsumeSystem.request(p);h.assertTrue(!p.getData(ModAttachments.CONSUMING),"healthy hostile rejected");
        z.setHealth(4);ConsumeSystem.request(p);h.assertTrue(p.getData(ModAttachments.CONSUMING),"weak hostile accepted");
        var other=player(h,PowerForm.CLAWS);ConsumeSystem.request(other);
        h.assertTrue(!other.getData(ModAttachments.CONSUMING),"ordinary victims cannot be stolen");
        ConsumeSystem.cancel(p);z.discard();h.succeed();
    }
    @GameTest(template="wall_test", timeoutTicks=30)
    public static void captureDoesNotPersistNoAiAndDamageRestoresMob(GameTestHelper h) {
        var p=player(h,PowerForm.HAMMERFISTS);var cow=h.spawn(EntityType.COW,3,2,5);
        ConsumeSystem.request(p);var event=new EntityTickEvent.Pre(cow);ConsumeSystem.freeze(event);
        h.assertTrue(event.isCanceled(),"captured AI is frozen");
        var tag=new net.minecraft.nbt.CompoundTag();cow.saveWithoutId(tag);
        var restored=EntityType.COW.create(h.getLevel());restored.load(tag);
        h.assertTrue(!restored.isNoAi()&&!restored.getData(ModAttachments.CONSUME_VISUAL).active(),"reload must not save AI lock or visual capture");
        cow.hurt(cow.damageSources().generic(),1);
        h.assertTrue(!p.getData(ModAttachments.CONSUMING)&&!cow.isNoAi(),"damage frees cow immediately");
        event=new EntityTickEvent.Pre(cow);ConsumeSystem.freeze(event);
        h.assertTrue(!event.isCanceled()&&p.getHealth()==10,"normal ticks resume without reward");cow.discard();h.succeed();
    }
    @GameTest(template="wall_test", timeoutTicks=30)
    public static void whipPullUsesCollisionsAndCannotFinishAcrossWall(GameTestHelper h) {
        var p=player(h,PowerForm.WHIPFIST);var cow=h.spawn(EntityType.COW,3,2,8);cow.setNoAi(true);
        ConsumeSystem.request(p);h.assertTrue(p.getData(ModAttachments.CONSUMING),"whip reaches five blocks");
        Vec3 before=cow.position();for(int i=0;i<13;i++)tick(p);
        h.assertTrue(cow.position().distanceToSqr(before)<.0001,"victim must not slide before the whip arrives");
        for(int i=0;i<17;i++)tick(p);
        h.assertTrue(p.distanceToSqr(cow)<2,"whip reels victim into contact");ConsumeSystem.cancel(p);
        cow.setPos(h.absoluteVec(new Vec3(3,2,8)));ConsumeSystem.request(p);
        for(int y=2;y<5;y++)h.setBlock(3,y,5,net.minecraft.world.level.block.Blocks.STONE);
        tick(p);
        h.assertTrue(!p.getData(ModAttachments.CONSUMING)&&cow.isAlive()&&p.getHealth()==10,"wall interrupts, no remote reward");cow.discard();h.succeed();
    }
    @GameTest(template="wall_test", timeoutTicks=30)
    public static void petsConstructsAndMountedAnimalsAreExcluded(GameTestHelper h) {
        var wolf=h.spawn(EntityType.WOLF,3,2,5);wolf.setTame(true,true);
        var golem=h.spawn(EntityType.IRON_GOLEM,5,2,5);var skeleton=h.spawn(EntityType.SKELETON,6,2,5);
        var cow=h.spawn(EntityType.COW,3,2,6);var chicken=h.spawn(EntityType.CHICKEN,3,2,6);chicken.startRiding(cow,true);
        h.assertTrue(!ConsumeSystem.eligible(wolf)&&!ConsumeSystem.eligible(golem)&&!ConsumeSystem.eligible(skeleton)
                &&!ConsumeSystem.eligible(cow)&&!ConsumeSystem.eligible(chicken),"deny pets, constructs and mounted pairs");
        wolf.discard();golem.discard();skeleton.discard();chicken.discard();cow.discard();h.succeed();
    }
    @GameTest(template="wall_test", timeoutTicks=30)
    public static void formChangeAndVictimRemovalCancelWithoutReward(GameTestHelper h) {
        var p=player(h,PowerForm.HAMMERFISTS);var cow=h.spawn(EntityType.COW,3,2,5);cow.setNoAi(true);
        ConsumeSystem.request(p);
        h.assertTrue(!PowerFormSystem.select(p,PowerForm.CLAWS,false),"cannot switch weapons while consuming");
        p.getData(ModAttachments.PLAYER_STATE).activate(PowerForm.CLAWS);tick(p);
        h.assertTrue(!p.getData(ModAttachments.CONSUMING)&&!cow.getData(ModAttachments.CONSUME_VISUAL).active(),"external form changes release claim");
        ConsumeSystem.request(p);cow.discard();tick(p);
        h.assertTrue(!p.getData(ModAttachments.CONSUMING)&&p.getHealth()==10,"removed victims pay nothing");h.succeed();
    }
}
