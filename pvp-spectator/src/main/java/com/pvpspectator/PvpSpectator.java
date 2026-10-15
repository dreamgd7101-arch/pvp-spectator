package com.pvpspectator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PvpSpectator implements ModInitializer {
    /** A player counts as "in a fight" for this many ticks after hitting / being hit by another player. */
    private static final int COMBAT_TICKS = 20 * 30;
    private static final Map<UUID, Integer> LAST_COMBAT = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(InventoryPayload.ID, InventoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CyclePayload.ID, CyclePayload.CODEC);

        // Track PvP combat
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (entity instanceof ServerPlayerEntity victim && source.getAttacker() instanceof ServerPlayerEntity attacker
                    && victim != attacker) {
                int now = victim.getServer().getTicks();
                LAST_COMBAT.put(victim.getUuid(), now);
                LAST_COMBAT.put(attacker.getUuid(), now);
            }
        });

        // Switch player when the spectator presses the key
        ServerPlayNetworking.registerGlobalReceiver(CyclePayload.ID, (payload, context) -> {
            ServerPlayerEntity spectator = context.player();
            if (!spectator.isSpectator()) return;
            cycle(context.server(), spectator, payload.direction() >= 0 ? 1 : -1);
        });

        // Send the watched player's inventory to every spectator (every 2 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 2 != 0) return;
            for (ServerPlayerEntity spectator : server.getPlayerManager().getPlayerList()) {
                if (!spectator.isSpectator()) continue;
                if (spectator.getCameraEntity() instanceof ServerPlayerEntity target && target != spectator) {
                    sendInventory(spectator, target);
                }
            }
        });
    }

    private static void sendInventory(ServerPlayerEntity spectator, ServerPlayerEntity target) {
        PlayerInventory inv = target.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inv.size());
        for (int i = 0; i < inv.size(); i++) stacks.add(inv.getStack(i).copy());
        ServerPlayNetworking.send(spectator, new InventoryPayload(
                target.getId(), inv.selectedSlot, target.getHungerManager().getFoodLevel(), stacks));
    }

    private static void cycle(MinecraftServer server, ServerPlayerEntity spectator, int dir) {
        List<ServerPlayerEntity> candidates = candidates(server, spectator);
        if (candidates.isEmpty()) return;

        Entity current = spectator.getCameraEntity();
        int idx = candidates.indexOf(current);
        int next = idx < 0 ? (dir > 0 ? 0 : candidates.size() - 1) : Math.floorMod(idx + dir, candidates.size());
        spectator.setCameraEntity(candidates.get(next));
    }

    /** Players currently fighting; if nobody is fighting, every non-spectator player. */
    private static List<ServerPlayerEntity> candidates(MinecraftServer server, ServerPlayerEntity spectator) {
        int now = server.getTicks();
        List<ServerPlayerEntity> all = new ArrayList<>();
        List<ServerPlayerEntity> fighting = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p == spectator || p.isSpectator() || !p.isAlive()) continue;
            all.add(p);
            Integer last = LAST_COMBAT.get(p.getUuid());
            if (last != null && now - last <= COMBAT_TICKS) fighting.add(p);
        }
        List<ServerPlayerEntity> result = fighting.isEmpty() ? all : fighting;
        result.sort(Comparator.comparing(p -> p.getGameProfile().getName().toLowerCase()));
        return result;
    }
}
