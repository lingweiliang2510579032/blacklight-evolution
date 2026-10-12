package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Upgrade-panel purchase request; the server validates EP, tiers and ownership. */
public record UpgradePurchasePayload(String upgradeId) implements CustomPacketPayload {
    public static final Type<UpgradePurchasePayload> TYPE = new Type<>(
            new ResourceLocation(BlacklightNeoMod.MOD_ID, "upgrade_purchase"));
    public static final StreamCodec<FriendlyByteBuf, UpgradePurchasePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeUtf(payload.upgradeId, 64),
                    buffer -> new UpgradePurchasePayload(buffer.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
