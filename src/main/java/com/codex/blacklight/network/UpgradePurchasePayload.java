package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Upgrade-panel purchase request; the server validates EP, tiers and ownership. */
public record UpgradePurchasePayload(String upgradeId) implements CustomPacketPayload {
    public static final Type<UpgradePurchasePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "upgrade_purchase"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradePurchasePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeUtf(payload.upgradeId),
                    buffer -> new UpgradePurchasePayload(buffer.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
