package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponComboTimingTest {
    @Test void basicLinksKeepActiveFramesAndVanillaHurtResistance() {
        for(var move:WeaponMove.values()) {
            assertTrue(move.comboBufferTick()>0&&move.comboBufferTick()<=move.start,move.id);
            assertTrue(move.comboLinkTick()>=move.end,move.id+" cannot skip contact");
            var next=move.nextAttack()!=null?move.nextAttack():move.nextSpecial();
            if(next==null){assertEquals(move.duration,move.comboLinkTick());continue;}
            assertTrue(move.comboLinkTick()<=move.duration,move.id+" link cannot outlive recovery");
            assertTrue(move.comboLinkTick()+next.start-(move.end-1)>=10,
                    move.id+" late hit must permit the next full-damage contact");
            int source=SourceComboTiming.linkTick(move.id,LocalWeaponMotion.find(move.id).orElse(null));
            if(source>=0) {
                int safe=Math.max(move.end+1,move.end-1+10-next.start);
                assertTrue(source-move.comboLinkTick()<=Math.ceil(Math.max(0,source-safe)*.15),
                        move.id+" cannot trim more than fifteen percent of source recovery");
            }
            assertNotEquals(move,next);
            assertTrue(move.form==next.form||next.form==com.codex.blacklight.power.PowerForm.NONE
                            ||move==WeaponMove.COMBO_KICK1&&next==WeaponMove.MUSCLE_COMBO_C,
                    move.id+" can link only to its own form or a shared body combo");
        }
    }

    @Test void bundledBladeCutExtendsAnEarlySourceHookToTheSafeLink() {
        var move=WeaponMove.BLADE_CUT;
        assertEquals(10,move.end);
        assertEquals(7,move.nextAttack().start);
        assertEquals(11,SourceComboTiming.linkTick(move.id,LocalWeaponMotion.find(move.id).orElseThrow()));
        assertEquals(12,move.comboLinkTick());
        assertTrue(move.comboLinkTick()>=move.end&&move.comboLinkTick()<=move.duration);
        assertEquals(10,move.comboLinkTick()+move.nextAttack().start-(move.end-1));
    }

    @Test void nativeAttackChainsEndInsteadOfLoopingTwoPoses(){
        assertChain(WeaponMove.BLADE_CUT,WeaponMove.BLADE_RETURN,WeaponMove.BLADE_COMBO_C,WeaponMove.BLADE_COMBO_C2,WeaponMove.BLADE_COMBO_D);
        assertChain(WeaponMove.HAMMER_JAB,WeaponMove.HAMMER_HOOK,WeaponMove.HAMMER_COMBO_C,WeaponMove.HAMMER_COMBO_D);
        assertChain(WeaponMove.WHIP_LASH,WeaponMove.WHIP_RETURN,WeaponMove.WHIP_COMBO_C);
        assertEquals(WeaponMove.WHIP_COMBO_C,WeaponMove.WHIP_RETURN_CHARGED.nextAttack());
    }
    private static void assertChain(WeaponMove... moves){
        for(int i=0;i<moves.length-1;i++)assertEquals(moves[i+1],moves[i].nextAttack());
        assertNull(moves[moves.length-1].nextAttack());
    }

    @Test void specialActionsRetainTheirCompleteCommitment() {
        for(var move:WeaponMove.values())if(!move.basicCombo())
            assertEquals(move.duration,move.comboLinkTick(),move.id);
    }
}
