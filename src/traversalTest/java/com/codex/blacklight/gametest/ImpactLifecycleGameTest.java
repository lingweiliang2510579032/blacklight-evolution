package com.codex.blacklight.gametest;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("blacklight") @PrefixGameTestTemplate(false)
public final class ImpactLifecycleGameTest {
    @GameTest(template="wall_test",timeoutTicks=90)
    public static void knockdownRecoversWithoutChangingSavedAi(GameTestHelper h){
        var recovery=new HitReaction(100,16,HitReaction.GETUP,0,ImpactProfile.RISE);
        h.assertTrue(recovery.weight(115,0)==1&&recovery.weight(116,0)==0,"recovery pose holds until exact end");
        for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
        var pig=h.spawn(EntityType.PIG,3,2,3);pig.setOnGround(true);
        ImpactControl.begin(pig,new ImpactProfile(HitReaction.HEAVY,ImpactProfile.FALL,45,0,0,true),0);
        h.assertTrue(ImpactControl.stunned(pig)&&!pig.isNoAi(),"stun must pause decisions without persisting NoAI");
        h.runAtTickTime(7,()->h.assertTrue(pig.getData(ModAttachments.HIT_REACTION).kind()==HitReaction.DOWN,"grounded finisher enters down phase"));
        h.runAtTickTime(25,()->h.assertTrue(pig.getData(ModAttachments.HIT_REACTION).kind()==HitReaction.GETUP,"down victim actually gets up"));
        h.runAtTickTime(45,()->{
            h.assertTrue(!ImpactControl.stunned(pig)&&!pig.isNoAi()&&pig.getData(ModAttachments.HIT_REACTION).duration()==0,"full recovery restores AI eligibility and clears pose");pig.discard();h.succeed();
        });
    }
    @GameTest(template="wall_test",timeoutTicks=30)
    public static void captureAndRemovalCancelStunWithoutChangingNoAi(GameTestHelper h){
        var pig=h.spawn(EntityType.PIG,3,2,3);pig.setNoAi(true);
        ImpactControl.begin(pig,ImpactProfile.of(WeaponMove.HAMMER_COMBO_D),0);
        ImpactControl.cancel(pig);
        h.assertTrue(pig.isNoAi()&&!ImpactControl.stunned(pig),"existing NoAI is untouched by cancellation");
        ImpactControl.begin(pig,ImpactProfile.of(WeaponMove.HAMMER_JAB),0);pig.discard();
        h.assertTrue(!ImpactControl.stunned(pig),"removed entity cannot retain control");h.succeed();
    }
    @GameTest(template="wall_test",timeoutTicks=80)
    public static void launchDoesNotBecomeGroundedUntilActualLanding(GameTestHelper h){
        for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
        var cow=h.spawn(EntityType.COW,3,7,3);cow.setOnGround(false);cow.setDeltaMovement(0,.5,0);
        ImpactControl.begin(cow,ImpactProfile.of(WeaponMove.MUSCLE_UPPERCUT),0);
        h.runAtTickTime(5,()->h.assertTrue(cow.getData(ModAttachments.HIT_REACTION).kind()==HitReaction.LAUNCH,"airborne victim retains launch phase"));
        h.runAtTickTime(32,()->h.assertTrue(cow.getData(ModAttachments.HIT_REACTION).kind()==HitReaction.DOWN,"landing enters prone phase y="+cow.getY()+" ground="+cow.onGround()+" hit="+cow.getData(ModAttachments.HIT_REACTION)));
        h.runAtTickTime(70,()->{h.assertTrue(!ImpactControl.stunned(cow)&&!cow.isNoAi(),"normal AI eligibility returns after airborne recovery");cow.discard();h.succeed();});
    }
}
