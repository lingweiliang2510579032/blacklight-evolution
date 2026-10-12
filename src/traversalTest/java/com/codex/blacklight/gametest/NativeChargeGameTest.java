package com.codex.blacklight.gametest;

import com.codex.blacklight.combat.LocalWeaponMotion;
import com.codex.blacklight.combat.WeaponCombatSystem;
import com.codex.blacklight.combat.WeaponMove;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

/** Native-profile cases require hammer_jab, hammer_jab_charge and hammer_jab_charged. */
@GameTestHolder("blacklight") @PrefixGameTestTemplate(false)
public final class NativeChargeGameTest {
    private static LocalWeaponMotion.Definition definition() {
        return LocalWeaponMotion.find("hammer_jab").filter(d -> d.charge().isPresent()).orElse(null);
    }

    private static ServerPlayer player(GameTestHelper h, String name) {
        var p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setPos(h.absoluteVec(new Vec3(4, 2, 3))); p.setYRot(0); p.setOnGround(true); p.tickCount=100;
        var state=p.getData(ModAttachments.PLAYER_STATE);
        state.infect(); state.setPrototypeMode(true); state.unlock(PowerForm.HAMMERFISTS); state.activate(PowerForm.HAMMERFISTS);
        p.getData(ModAttachments.MOVEMENT_STATE).reset();
        return p;
    }

    private static void tick(ServerPlayer p) {
        p.tickCount++; WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));
    }

    private static int windup(LocalWeaponMotion.Definition d) {
        return d.charge().orElseThrow().windupTicks()+(int)Math.ceil(d.transitionSeconds()*20-.00001);
    }

    @GameTest(template="wall_test", timeoutTicks=40)
    public static void shortPressCompletesWindupBeforeReleaseWithoutEarlyDamage(GameTestHelper h) {
        var d=definition(); if(d==null){h.succeed();return;}
        var p=player(h,"charge-short");
        var target=h.spawn(EntityType.IRON_GOLEM,4,2,4);target.setNoAi(true);
        target.setPos(p.position().add(0,0,1));float health=target.getHealth();
        WeaponCombatSystem.acceptInput(p,true,false);
        h.assertTrue(p.getData(ModAttachments.WEAPON_VISUAL).charging(),"physical press enters held windup");
        WeaponCombatSystem.acceptInput(p,false,false);
        for(int t=1;t<windup(d);t++){
            tick(p);
            h.assertTrue(p.getData(ModAttachments.WEAPON_VISUAL).charging(),"early release cannot skip minimum windup");
            h.assertTrue(target.getHealth()==health,"held windup has no damage even against a close target");
        }
        tick(p);var visual=p.getData(ModAttachments.WEAPON_VISUAL);
        h.assertTrue(!visual.charging()&&visual.spec()==WeaponMove.HAMMER_JAB,"short release starts uncharged strike");
        h.assertTrue(Math.abs(visual.clipOffset()-d.charge().orElseThrow().releaseTime())<.0001,"release seeks past completed windup");
        h.assertTrue(target.getHealth()==health,"switching charge to release does not itself deal damage");
        WeaponCombatSystem.cancelHeldInput(p);WeaponCombatSystem.cancel(p);target.discard();h.succeed();
    }

    @GameTest(template="wall_test", timeoutTicks=40)
    public static void fullChargeAutoReleasesAndHeldHeartbeatCannotRepeat(GameTestHelper h) {
        var d=definition(); if(d==null){h.succeed();return;}
        var p=player(h,"charge-full");var charge=d.charge().orElseThrow();
        h.assertTrue(charge.chargedMove().isPresent(),"native hammer fixture includes a distinct charged release");
        WeaponCombatSystem.acceptInput(p,true,false);
        long started=p.getData(ModAttachments.WEAPON_VISUAL).started();
        int full=windup(d)+charge.holdTicks();
        for(int t=1;t<full;t++){
            WeaponCombatSystem.acceptInput(p,true,false);tick(p);
            var visual=p.getData(ModAttachments.WEAPON_VISUAL);
            h.assertTrue(visual.charging()&&visual.started()==started,"held heartbeat cannot restart the charge clock");
        }
        WeaponCombatSystem.acceptInput(p,true,false);tick(p);
        var released=p.getData(ModAttachments.WEAPON_VISUAL);
        h.assertTrue(!released.charging()&&released.spec()!=null&&released.spec().id.equals(charge.chargedMove().orElseThrow()),"maximum hold automatically chooses charged release");
        int recovery=released.spec().duration;
        for(int t=0;t<recovery+3;t++){WeaponCombatSystem.acceptInput(p,true,false);tick(p);}
        h.assertTrue(!WeaponCombatSystem.busy(p),"continued held heartbeat cannot queue or restart an unrequested action");
        WeaponCombatSystem.cancelHeldInput(p);WeaponCombatSystem.cancel(p);h.succeed();
    }

    @GameTest(template="wall_test", timeoutTicks=40)
    public static void focusLossAndMissingHeartbeatCancelHeldState(GameTestHelper h) {
        var d=definition(); if(d==null){h.succeed();return;}
        var p=player(h,"charge-cancel");
        WeaponCombatSystem.acceptInput(p,true,false);WeaponCombatSystem.cancelHeldInput(p);
        h.assertTrue(!WeaponCombatSystem.busy(p)&&p.getData(ModAttachments.WEAPON_VISUAL).spec()==null,"focus-loss cancellation removes held pose and movement lock");
        WeaponCombatSystem.acceptInput(p,true,false);
        h.assertTrue(p.getData(ModAttachments.WEAPON_VISUAL).charging(),"fresh press works after cancellation");
        // Advance the authoritative input clock without supplying a keepalive. This
        // models a disconnected/stalled client before the next combat dispatch.
        p.tickCount+=26;WeaponCombatSystem.tick(new PlayerTickEvent.Post(p));
        h.assertTrue(!WeaponCombatSystem.busy(p),"stale held input cancels instead of firing after reconnect");
        WeaponCombatSystem.cancelHeldInput(p);WeaponCombatSystem.cancel(p);h.succeed();
    }
}
