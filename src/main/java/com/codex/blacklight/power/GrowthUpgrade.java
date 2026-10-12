package com.codex.blacklight.power;
public enum GrowthUpgrade {
    VITALITY("vitality",1,200),REGENERATION("regeneration",2,400),ASSIMILATION("assimilation",4,300),
    REACH_I("reach_1",8,300),REACH_II("reach_2",16,600),REACH_III("reach_3",32,900),
    VITALITY_II("vitality_2",64,400),VITALITY_III("vitality_3",128,650),VITALITY_IV("vitality_4",256,900);
    public final String id;public final int bit,cost;
    GrowthUpgrade(String id,int bit,int cost){this.id=id;this.bit=bit;this.cost=cost;}
    public static GrowthUpgrade byId(String id){for(var g:values())if(g.id.equals(id))return g;return null;}
    public GrowthUpgrade requires(){return switch(this){case REACH_III->REACH_II;case REACH_II->REACH_I;
        case VITALITY_II->VITALITY;case VITALITY_III->VITALITY_II;case VITALITY_IV->VITALITY_III;default->null;};}
}
