package com.codex.blacklight.forge;

import com.codex.blacklight.forge.codec.StreamCodec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Explicit directions and bounded decode for every Blacklight SimpleChannel message. */
public final class ForgePayloadRegistrar {
    public static final int MAX_PACKET_BYTES = 65_536;
    private static final Map<Class<?>, NetworkDirection> DIRECTIONS = new LinkedHashMap<>();
    private final SimpleChannel channel;
    private int nextId;

    public ForgePayloadRegistrar(SimpleChannel channel) { this.channel = channel; }

    public <T extends CustomPacketPayload> ForgePayloadRegistrar playToClient(Class<T> type,
            StreamCodec<FriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
        return register(type, codec, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    public <T extends CustomPacketPayload> ForgePayloadRegistrar playToServer(Class<T> type,
            StreamCodec<FriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
        return register(type, codec, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    private <T extends CustomPacketPayload> ForgePayloadRegistrar register(Class<T> type,
            StreamCodec<FriendlyByteBuf, T> codec, BiConsumer<T, Context> handler, NetworkDirection direction) {
        if (DIRECTIONS.putIfAbsent(type, direction) != null) throw new IllegalStateException("Duplicate Blacklight packet: " + type.getName());
        channel.messageBuilder(type, nextId++, direction)
                .encoder((packet, buffer) -> {
                    int start = buffer.writerIndex();
                    codec.encode(buffer, packet);
                    if (buffer.writerIndex() - start > MAX_PACKET_BYTES) throw new EncoderException("Blacklight packet exceeds size limit");
                })
                .decoder(buffer -> {
                    if (buffer.readableBytes() > MAX_PACKET_BYTES) throw new DecoderException("Blacklight packet exceeds size limit");
                    T packet = codec.decode(buffer);
                    if (buffer.isReadable()) throw new DecoderException("Trailing bytes in Blacklight packet");
                    return packet;
                })
                .consumerMainThread((packet, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    if (context.getDirection() != direction) throw new IllegalStateException("Wrong Blacklight packet direction");
                    handler.accept(packet, new Context(context.getSender()));
                }).add();
        return this;
    }

    public static void requireDirection(CustomPacketPayload packet, NetworkDirection direction) {
        if (DIRECTIONS.get(packet.getClass()) != direction) throw new IllegalArgumentException("Unregistered or wrong-direction Blacklight packet");
    }

    /** All consumers are already on the receiving game's main thread. */
    public record Context(ServerPlayer sender) {
        public Object player() { return sender; }
        public void enqueueWork(Runnable work) { work.run(); }
    }
}
