package com.codex.blacklight.client.animation;

import com.codex.blacklight.power.PowerForm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WeaponMorphTimelineTest {
    @Test void everyWeaponFormsAndReturnsToExactlyNoMaterial() {
        for(var form:PowerForm.values())if(WeaponMorphTiming.weapon(form)) {
            var clock=new WeaponMorphTimeline(PowerForm.NONE,100);
            clock.retarget(form,100,100,16,false);
            assertEquals(0,clock.weights(100)[form.ordinal()]);
            assertEquals(.5F,clock.weights(108)[form.ordinal()],1E-6);
            assertEquals(1,clock.weights(116)[form.ordinal()]);
            assertFalse(clock.transitioning(116));
            clock.retarget(PowerForm.NONE,116,116,14,false);
            assertEquals(1,clock.weights(116)[form.ordinal()]);
            for(float weight:clock.weights(130))assertEquals(0,weight);
        }
    }
    @Test void rapidRetargetKeepsAllExistingAmountsWithoutAFramePop() {
        var clock=new WeaponMorphTimeline(PowerForm.NONE,0);
        double now=0;
        for(var target:new PowerForm[]{PowerForm.CLAWS,PowerForm.BLADE,PowerForm.HAMMERFISTS,PowerForm.MUSCLE_MASS,PowerForm.WHIPFIST,PowerForm.NONE}) {
            var before=clock.weights(now);
            clock.retarget(target,now,now,18,false);
            assertArrayEquals(before,clock.weights(now),0);
            for(double t=now;t<=now+18;t+=.25)for(float w:clock.weights(t))assertTrue(Float.isFinite(w)&&w>=0&&w<=1);
            now+=5;
        }
        for(float weight:clock.weights(now+18))assertEquals(0,weight);
    }
    @Test void longBodyWaitsForTheActualSourceMarkerThenForms() {
        for(var form:PowerForm.values())if(WeaponMorphTiming.weapon(form)) {
            var clock=new WeaponMorphTimeline(PowerForm.NONE,1000);
            double onset=1000+WeaponMorphTiming.onset(form);
            clock.retarget(form,1000,onset,18,true);
            assertEquals(0,clock.weights(onset-.01)[form.ordinal()]);
            assertEquals(.5F,clock.weights(onset+9)[form.ordinal()],1E-6);
            assertEquals(1,clock.weights(onset+18)[form.ordinal()]);
        }
    }
    @Test void interruptingALongPreparationResumesFromCurrentMaterial() {
        var clock=new WeaponMorphTimeline(PowerForm.CLAWS,10);
        clock.retarget(PowerForm.BLADE,10,50,18,true);
        var held=clock.weights(30);
        assertEquals(1,held[PowerForm.CLAWS.ordinal()]);
        assertEquals(0,held[PowerForm.BLADE.ordinal()]);
        clock.retarget(PowerForm.BLADE,30,30,16,false);
        assertArrayEquals(held,clock.weights(30),0);
        assertEquals(1,clock.weights(46)[PowerForm.BLADE.ordinal()]);
        assertEquals(0,clock.weights(46)[PowerForm.CLAWS.ordinal()]);
    }
    @Test void quickToLongSettlesItsOldPulseBeforeTheOriginalCue() {
        var clock=new WeaponMorphTimeline(PowerForm.NONE,0);
        clock.retarget(PowerForm.CLAWS,0,0,16,false);
        var amounts=clock.weights(10);float flow=clock.flow(10);
        double onset=10+WeaponMorphTiming.onset(PowerForm.BLADE);
        clock.retarget(PowerForm.BLADE,10,onset,18,true);
        assertArrayEquals(amounts,clock.weights(10),0);
        assertEquals(flow,clock.flow(10),0,"switch must preserve the current surface");
        assertTrue(clock.flow(13)>0&&clock.flow(13)<flow);
        assertEquals(0,clock.flow(16));
        assertEquals(0,clock.flow(onset));
        assertEquals(0,clock.weights(onset)[PowerForm.BLADE.ordinal()]);
        assertEquals(1,clock.flow(onset+9));
    }
    @Test void rapidRetargetPreservesActualLiquidDisplacementAndColor() {
        var clock=new WeaponMorphTimeline(PowerForm.NONE,0);
        clock.retarget(PowerForm.CLAWS,0,0,16,false);
        var vertex=BiomassSurface.vertex(.14F,.32F,-.045F,.8F,true);
        var before=new BiomassSurface.Frame(clock.weights(8)[PowerForm.CLAWS.ordinal()],clock.phase(8),clock.flow(8),false);
        float[] a=new float[6],b=new float[6];
        BiomassSurface.deform(vertex,before,81.5F,vertex.x(),vertex.y(),vertex.z(),a,0);
        clock.retarget(PowerForm.BLADE,8,8,18,false);
        var after=new BiomassSurface.Frame(clock.weights(8)[PowerForm.CLAWS.ordinal()],clock.phase(8),clock.flow(8),true);
        BiomassSurface.deform(vertex,after,81.5F,vertex.x(),vertex.y(),vertex.z(),b,0);
        assertArrayEquals(a,b,0,"retarget must keep the same liquid surface, not only its amount");
        assertEquals(BiomassSurface.color(0xFF302C2E,a[3],a[4],a[5]),BiomassSurface.color(0xFF302C2E,b[3],b[4],b[5]));
        assertEquals(0,clock.flow(26));
    }
}
