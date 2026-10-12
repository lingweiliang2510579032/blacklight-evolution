package com.codex.blacklight.client;

import com.codex.blacklight.animation.PlayerAction;
import com.codex.blacklight.animation.PlayerMotionController;
import com.codex.blacklight.network.ClawAnimationPayload;
import net.minecraft.client.Minecraft;

/** Start the controlling client's server-approved action on Blacklight's own timeline. */
public final class ClientClawAnimation {
    private ClientClawAnimation() { }
    public static void accept(ClawAnimationPayload payload) {
        var player=Minecraft.getInstance().player;
        if(player==null)return;
        PlayerAction action=switch(payload.animation()) {
            case TRANSFORM -> PlayerAction.TRANSFORM_CLAWS;
            case AUTO_1 -> PlayerAction.CLAW_AUTO_1;
            case AUTO_2 -> PlayerAction.CLAW_AUTO_2;
            case AUTO_3 -> PlayerAction.CLAW_AUTO_3;
            case GROUND_SPIKE -> PlayerAction.CLAW_GROUNDSPIKE;
            case SPRINT_ATTACK -> PlayerAction.CLAW_SPRINT_ATTACK;
            case DASHING_SLICE -> PlayerAction.CLAW_DASHING_SLICE;
            case CHARGED_JUMP -> PlayerAction.CHARGED_JUMP;
        };
        if(!PlayerMotionController.isPlaying(player,action))PlayerMotionController.request(player,action,0);
    }
}
