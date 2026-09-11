package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;

/** Server acknowledgement that guarantees the controlling client starts the visual action. */
public record ClawAnimationPayload(Animation animation) implements CustomPacketPayload {
    public static final Type<ClawAnimationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "claw_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClawAnimationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.map(Animation::byId, Animation::id),
                    ClawAnimationPayload::animation,
                    ClawAnimationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Animation {
        TRANSFORM(0),
        AUTO_1(1),
        AUTO_2(2),
        AUTO_3(3),
        GROUND_SPIKE(4),
        SPRINT_ATTACK(5),
        DASHING_SLICE(6),
        CHARGED_JUMP(7);

        private final int id;

        Animation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Animation byId(int id) {
            return switch (id) {
                case 1 -> AUTO_1;
                case 2 -> AUTO_2;
                case 3 -> AUTO_3;
                case 4 -> GROUND_SPIKE;
                case 5 -> SPRINT_ATTACK;
                case 6 -> DASHING_SLICE;
                case 7 -> CHARGED_JUMP;
                default -> TRANSFORM;
            };
        }

        public static Animation from(
                AnimationAccessor<? extends StaticAnimation> accessor) {
            if (accessor == BlacklightEpicFightAnimations.TRANSFORM_CLAWS) return TRANSFORM;
            if (accessor == BlacklightEpicFightAnimations.CLAW_AUTO_1) return AUTO_1;
            if (accessor == BlacklightEpicFightAnimations.CLAW_AUTO_2) return AUTO_2;
            if (accessor == BlacklightEpicFightAnimations.CLAW_AUTO_3) return AUTO_3;
            if (accessor == BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE) return GROUND_SPIKE;
            if (accessor == BlacklightEpicFightAnimations.CLAW_SPRINT_ATTACK) return SPRINT_ATTACK;
            if (accessor == BlacklightEpicFightAnimations.CLAW_DASHING_SLICE) return DASHING_SLICE;
            if (accessor == BlacklightEpicFightAnimations.CHARGED_JUMP) return CHARGED_JUMP;
            return null;
        }
    }
}
