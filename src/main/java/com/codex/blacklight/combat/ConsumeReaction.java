package com.codex.blacklight.combat;

import com.codex.blacklight.power.PowerForm;

/** Authored victim key poses on the same normalized clock as the consuming player. Angles in radians. */
public final class ConsumeReaction {
    public record Pose(float bend, float twist, float slump, float brace, float kick, float head, float grounded) {}
    // t, torso pitch degrees, twist degrees, lowered body fraction, arm brace, leg flex, head pitch degrees
    private static final float[][] NORMAL={{0,0,0,0,0,0,0},{.12F,-12,-8,0,.8F,.2F,-18},{.24F,28,14,.13F,1,.7F,20},{.40F,-9,-12,.06F,.7F,.35F,-16},{.62F,36,9,.19F,.35F,.9F,27},{1,45,0,.25F,0,1,32}};
    private static final float[][] HAMMER={{0,0,0,0,0,0,0},{.16F,-16,-9,.02F,1,.3F,-25},{.29F,-22,4,0,1,.2F,-28},{.34F,58,14,.29F,.5F,1,40},{.44F,39,-9,.23F,.7F,.8F,23},{.63F,62,5,.32F,.2F,1,35},{1,66,0,.35F,0,1,35}};
    private static final float[][] BLADE={{0,0,0,0,0,0,0},{.15F,-8,-10,0,.6F,.1F,-8},{.24F,-29,23,.035F,1,.35F,-32},{.37F,-18,10,.06F,.8F,.5F,-18},{.51F,37,-18,.19F,.5F,.8F,32},{.70F,47,8,.26F,.15F,1,38},{1,52,0,.29F,0,1,38}};
    private static final float[][] WHIP={{0,0,0,0,0,0,0},{.17F,0,0,0,0,0,0},{.23F,-22,-15,.03F,1,.3F,-20},{.30F,-26,14,.02F,.9F,.7F,-24},{.39F,33,-20,.16F,.8F,.9F,25},{.51F,-8,12,.10F,.5F,.5F,-12},{.66F,39,-9,.24F,.3F,1,30},{1,48,0,.30F,0,1,35}};
    private ConsumeReaction() {}
    public static Pose sample(PowerForm form, float t) {
        var keys=switch(form){case HAMMERFISTS->HAMMER;case BLADE->BLADE;case WHIPFIST->WHIP;default->NORMAL;};
        t=Math.clamp(t,0,1);int i=0;while(i<keys.length-2 && t>keys[i+1][0])i++;
        float a=Math.clamp((t-keys[i][0])/(keys[i+1][0]-keys[i][0]),0,1);a=a*a*(3-2*a);
        float[] v=new float[6];for(int k=0;k<6;k++)v[k]=keys[i][k+1]+(keys[i+1][k+1]-keys[i][k+1])*a;
        float struggle=(float)Math.sin(t*61)*Math.min(t*8,1)*Math.max(0,1-t/.72F);
        float grounded=form==PowerForm.HAMMERFISTS ? Math.clamp((t-.26F)/.10F,0,1) : 0;
        grounded=grounded*grounded*(3-2*grounded);
        return new Pose((float)Math.toRadians(v[0])*(1-grounded*.8F),(float)Math.toRadians(v[1]+struggle*7),v[2]*(1-grounded),v[3],v[4]*(1-grounded*.65F),(float)Math.toRadians(v[5]+struggle*5)*(1-grounded*.8F),grounded);
    }
}
