package com.codex.blacklight.combat;

/** Physical input arbitration shared by the live controller and focused routing tests. */
public record WeaponButtons(boolean attack, boolean special, boolean action, boolean jump) {
    public WeaponMove.Input intent() {
        if(attack&&special)return WeaponMove.Input.GROUND_POUND;
        if(attack&&action)return WeaponMove.Input.PAIN;
        if(special&&action)return WeaponMove.Input.CLAP;
        if(attack)return WeaponMove.Input.ATTACK;
        if(special)return WeaponMove.Input.SPECIAL;
        return null;
    }
    public boolean held(WeaponMove.Input input) {
        return switch(input){
            case ATTACK -> attack;
            case SPECIAL -> special;
            case GROUND_POUND -> attack&&special;
            case PAIN -> attack&&action;
            case CLAP -> special&&action;
        };
    }
    public boolean pressedSince(WeaponButtons before) {
        return attack&&!before.attack||special&&!before.special||action&&!before.action;
    }
}
