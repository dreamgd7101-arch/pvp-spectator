package com.pvpspectator;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Server -> spectator: inventory snapshot (41 slots: 36 main, 4 armor, 1 offhand) of the watched player. */
public record InventoryPayload(int entityId, int selectedSlot, int food, List<ItemStack> stacks) implements CustomPayload {
    public static final Id<InventoryPayload> ID = new Id<>(Identifier.of("pvpspectator", "inventory"));
    public static final PacketCodec<RegistryByteBuf, InventoryPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, InventoryPayload::entityId,
            PacketCodecs.VAR_INT, InventoryPayload::selectedSlot,
            PacketCodecs.VAR_INT, InventoryPayload::food,
            ItemStack.OPTIONAL_LIST_PACKET_CODEC, InventoryPayload::stacks,
            InventoryPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
