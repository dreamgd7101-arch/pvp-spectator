package com.pvpspectator;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: switch to next (+1) or previous (-1) player. */
public record CyclePayload(int direction) implements CustomPayload {
    public static final Id<CyclePayload> ID = new Id<>(Identifier.of("pvpspectator", "cycle"));
    public static final PacketCodec<RegistryByteBuf, CyclePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CyclePayload::direction,
            CyclePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
