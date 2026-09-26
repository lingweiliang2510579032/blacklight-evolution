package com.codex.blacklight.client;

import com.codex.blacklight.movement.WallTraversal;
import com.codex.blacklight.network.WallMotionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import java.util.HashMap;
import java.util.Map;

/** Explicit wall modes for observers; the controlling client predicts the same state machine. */
public final class ClientWallMotion {
    private record Entry(WallTraversal.Mode mode, long expires) { }
    private static final Map<Integer,Entry> MODES=new HashMap<>();
    private static ClientLevel level;
    private ClientWallMotion() { }
    private static void checkLevel() {
        if(level!=Minecraft.getInstance().level) { MODES.clear();level=Minecraft.getInstance().level; }
    }
    public static void accept(WallMotionPayload payload) {
        checkLevel();if(level==null)return;
        var entity=level.getEntity(payload.entityId());
        if(entity instanceof AbstractClientPlayer player && player!=Minecraft.getInstance().player)
            com.codex.blacklight.movement.WallPoseState.set(player,payload.wallMode(),payload.normalX(),payload.normalZ());
        if(payload.wallMode()==WallTraversal.Mode.NONE)MODES.remove(payload.entityId());
        else MODES.put(payload.entityId(),new Entry(payload.wallMode(),level.getGameTime()+40));
    }
    public static WallTraversal.Mode modeFor(AbstractClientPlayer player) {
        checkLevel();
        if(player==Minecraft.getInstance().player)return ClientMovementEvents.wallMode();
        Entry entry=MODES.get(player.getId());
        if(entry==null || level==null || level.getGameTime()>entry.expires())return WallTraversal.Mode.NONE;
        return entry.mode();
    }
}
