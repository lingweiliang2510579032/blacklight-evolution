package com.codex.blacklight.gametest;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("blacklight") @PrefixGameTestTemplate(false)
public final class UnarmedHammerGameTest {
    static void tick(ServerPlayer p){p.tickCount++;WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));}
    @GameTest(template="wall_test",timeoutTicks=40)
    public static void ordinaryPunchesDealDamageAndPreserveTheOrdinaryForm(GameTestHelper h){
        var p=MovingCombatGameTest.player(h,PowerForm.NONE);var target=h.spawn(EntityType.IRON_GOLEM,3,25,5);target.setNoAi(true);float hp=target.getHealth();
        WeaponCombatSystem.request(p,false);
        h.assertTrue(p.getData(ModAttachments.WEAPON_VISUAL).spec()==WeaponMove.MUSCLE_JAB,"ordinary fists use native punch motion");
        for(int i=0;i<WeaponMove.MUSCLE_JAB.duration+1;i++)tick(p);
        h.assertTrue(target.getHealth()<hp,"ordinary punches must deal authoritative damage");
        h.assertTrue(p.getData(ModAttachments.PLAYER_STATE).getActivePower()==PowerForm.NONE,"punching cannot enable muscle mass");
        WeaponCombatSystem.cancel(p);target.discard();h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40)
    public static void ordinaryModeDoesNotStealItemUseOrSurvivalAttacks(GameTestHelper h){
        var p=MovingCombatGameTest.player(h,PowerForm.NONE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_PICKAXE));
        WeaponCombatSystem.request(p,false);h.assertTrue(!WeaponCombatSystem.busy(p),"tool use stays with vanilla");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.getData(ModAttachments.PLAYER_STATE).setPrototypeMode(false);
        WeaponCombatSystem.request(p,false);h.assertTrue(!WeaponCombatSystem.busy(p),"ordinary survival mode stays with vanilla");h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40)
    public static void earlyReleasedHammerTapQueuesWithoutAnotherHeldPause(GameTestHelper h){
        MovingCombatGameTest.supportFloor(h,24);
        var p=MovingCombatGameTest.player(h,PowerForm.HAMMERFISTS);
        WeaponCombatSystem.acceptInput(p,true,false);tick(p);WeaponCombatSystem.acceptInput(p,false,false);
        WeaponCombatSystem.acceptInput(p,true,false);WeaponCombatSystem.acceptInput(p,false,false);
        boolean second=false;
        for(int i=0;i<WeaponMove.HAMMER_JAB.duration+15;i++){
            tick(p);var visual=p.getData(ModAttachments.WEAPON_VISUAL);
            if(visual.spec()==WeaponMove.HAMMER_HOOK){second=true;h.assertTrue(!visual.charging(),"released buffered click must not enter a fresh held charge");break;}
        }
        h.assertTrue(second,"a fast second tap during first windup must survive release");WeaponCombatSystem.cancelHeldInput(p);WeaponCombatSystem.cancel(p);h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40)
    public static void ordinaryMixedComboAndJumpHaveUsableRoutes(GameTestHelper h){
        MovingCombatGameTest.supportFloor(h,24);
        var p=MovingCombatGameTest.player(h,PowerForm.NONE);WeaponCombatSystem.request(p,false);tick(p);
        WeaponCombatSystem.request(p,true);
        for(int i=0;i<WeaponMove.MUSCLE_JAB.comboLinkTick();i++)tick(p);
        h.assertTrue(p.getData(ModAttachments.WEAPON_VISUAL).spec()==WeaponMove.COMBO_KICK1,"ordinary punch must branch into a kick");
        WeaponCombatSystem.cancel(p);p.setOnGround(false);WeaponCombatSystem.request(p,false);
        h.assertTrue(p.getData(ModAttachments.WEAPON_VISUAL).spec()==WeaponMove.AIR_AUTO1,"ordinary mode must have air combo");WeaponCombatSystem.cancel(p);h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40)
    public static void chainedAttacksKeepFacingTheirTargetAfterPassingIt(GameTestHelper h){
        for(var form:new PowerForm[]{PowerForm.NONE,PowerForm.CLAWS,PowerForm.BLADE,PowerForm.HAMMERFISTS,PowerForm.MUSCLE_MASS,PowerForm.WHIPFIST}){
            var p=MovingCombatGameTest.player(h,form);var target=h.spawn(EntityType.IRON_GOLEM,3,25,5);target.setNoAi(true);
            WeaponCombatSystem.request(p,false);var first=p.getData(ModAttachments.WEAPON_VISUAL).spec();
            for(int i=0;i<first.comboBufferTick();i++)tick(p);WeaponCombatSystem.request(p,false);
            p.setPos(target.position().add(0,0,1.5));p.setYRot(0);
            for(int i=0;i<first.duration+2&&p.getData(ModAttachments.WEAPON_VISUAL).spec()==first;i++)tick(p);
            var visual=p.getData(ModAttachments.WEAPON_VISUAL);
            h.assertTrue(visual.spec()!=null&&visual.spec()!=first&&visual.targetId()==target.getId(),"combo retains original target: "+form);
            var toward=target.position().subtract(p.position()).multiply(1,0,1).normalize();
            h.assertTrue(net.minecraft.world.phys.Vec3.directionFromRotation(0,visual.yaw()).dot(toward)>.95,"next attack turns toward target behind actor: "+form);
            WeaponCombatSystem.cancel(p);target.discard();
        }h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40)
    public static void leavingContactRangeCannotDamageTheOldPosition(GameTestHelper h){
        for(var form:new PowerForm[]{PowerForm.NONE,PowerForm.CLAWS,PowerForm.BLADE,PowerForm.HAMMERFISTS,PowerForm.MUSCLE_MASS}){
            var p=MovingCombatGameTest.player(h,form);var target=h.spawn(EntityType.IRON_GOLEM,3,25,5);target.setNoAi(true);float hp=target.getHealth();
            WeaponCombatSystem.request(p,false);var move=p.getData(ModAttachments.WEAPON_VISUAL).spec();
            for(int i=0;i<move.start-1;i++)tick(p);
            p.setPos(p.position().add(0,0,10));
            for(int i=0;i<move.duration;i++)tick(p);
            h.assertTrue(target.getHealth()==hp,"old swept path cannot reach victim after moving away: "+form);
            WeaponCombatSystem.cancel(p);target.discard();
        }h.succeed();
    }
}
