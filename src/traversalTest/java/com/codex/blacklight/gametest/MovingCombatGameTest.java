package com.codex.blacklight.gametest;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("blacklight") @PrefixGameTestTemplate(false)
public final class MovingCombatGameTest {
    static ServerPlayer player(GameTestHelper h,PowerForm form){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"moving-qa"));
        p.setPos(h.absoluteVec(new Vec3(3,25,3)));p.setYRot(0);p.setXRot(0);p.setOnGround(true);p.tickCount=100;
        var state=p.getData(ModAttachments.PLAYER_STATE);state.infect();state.setPrototypeMode(true);state.unlock(form);state.activate(form);
        return p;
    }
    static void moving(GameTestHelper h,boolean retreat,boolean air){
        for(var form:new PowerForm[]{PowerForm.CLAWS,PowerForm.BLADE,PowerForm.HAMMERFISTS,PowerForm.MUSCLE_MASS,PowerForm.WHIPFIST}){
            var p=player(h,form);p.setOnGround(!air);
            var mob=h.spawn(EntityType.IRON_GOLEM,3,25,6);mob.setNoAi(true);
            mob.setPos(p.position().add(0,air?.25:0,form==PowerForm.WHIPFIST&&!air?4:2.15));
            float hp=mob.getHealth();var origin=p.position();
            WeaponCombatSystem.request(p,false);var move=p.getData(ModAttachments.WEAPON_VISUAL).spec();
            for(int t=1;t<=move.duration;t++){
                if(mob.getHealth()==hp)mob.setPos(mob.position().add(retreat?0:.105,0,retreat?.08:0));
                p.tickCount++;WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));
            }
            h.assertTrue(mob.getHealth()<hp,"must contact moving target "+form+" retreat="+retreat+" air="+air+" target="+mob.position().subtract(origin)+" player="+p.position().subtract(origin));
            WeaponCombatSystem.cancel(p);mob.discard();
        }h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40) public static void strafingTargetsRemainInAttackPath(GameTestHelper h){moving(h,false,false);}
    @GameTest(template="wall_test",timeoutTicks=40) public static void retreatingTargetsReceiveBoundedPursuit(GameTestHelper h){moving(h,true,false);}
    @GameTest(template="wall_test",timeoutTicks=40) public static void airCombosFollowMovingVictims(GameTestHelper h){moving(h,false,true);}
    @GameTest(template="wall_test",timeoutTicks=40) public static void movingSmallBodiesAreNotIgnored(GameTestHelper h){
        for(var type:new EntityType<?>[]{EntityType.PIG,EntityType.CHICKEN})for(var form:new PowerForm[]{PowerForm.CLAWS,PowerForm.BLADE,PowerForm.HAMMERFISTS,PowerForm.MUSCLE_MASS}){
            var p=player(h,form);var mob=(Mob)h.spawn(type,3,25,5);mob.setNoAi(true);mob.setPos(p.position().add(0,0,1.7));float hp=mob.getHealth();
            WeaponCombatSystem.request(p,false);var move=p.getData(ModAttachments.WEAPON_VISUAL).spec();
            for(int t=1;t<=move.duration;t++){if(mob.getHealth()==hp)mob.setPos(mob.position().add(.055,0,0));p.tickCount++;WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));}
            h.assertTrue(mob.getHealth()<hp,"small moving body requires visible contact: "+type+" / "+form);WeaponCombatSystem.cancel(p);mob.discard();
        }h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40) public static void pursuitCannotTurnBehindOrChaseEscapingTarget(GameTestHelper h){
        var p=player(h,PowerForm.BLADE);var mob=h.spawn(EntityType.IRON_GOLEM,3,25,6);mob.setNoAi(true);mob.setPos(p.position().add(0,0,2.2));
        float hp=mob.getHealth();var origin=p.position();WeaponCombatSystem.request(p,false);
        mob.setPos(origin.add(0,0,-5));
        for(int t=0;t<WeaponMove.BLADE_CUT.duration+1;t++){p.tickCount++;WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));}
        h.assertTrue(mob.getHealth()==hp&&p.position().distanceTo(origin)<.1,"escape behind cannot turn into guaranteed damage");
        WeaponCombatSystem.cancel(p);mob.discard();h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=40) public static void pursuitCannotCrossSolidWall(GameTestHelper h){
        var p=player(h,PowerForm.CLAWS);var mob=h.spawn(EntityType.IRON_GOLEM,3,25,6);mob.setNoAi(true);mob.setPos(p.position().add(0,0,3));
        float hp=mob.getHealth();var origin=p.position();WeaponCombatSystem.request(p,false);
        for(int x=1;x<=5;x++)for(int y=24;y<=29;y++)h.setBlock(x,y,5,Blocks.STONE);
        for(int t=0;t<WeaponMove.CLAW_AUTO1.duration+1;t++){p.tickCount++;WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));}
        h.assertTrue(mob.getHealth()==hp&&p.position().distanceTo(origin)<.1,"obstruction cancels pursuit and blocks damage");
        WeaponCombatSystem.cancel(p);mob.discard();h.succeed();
    }
}
