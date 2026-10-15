package com.pvpspectator;

import com.pvpspectator.client.SpectateHud;
import com.pvpspectator.client.SpectateInventoryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PvpSpectatorClient implements ClientModInitializer {
    private static KeyBinding nextKey;
    private static KeyBinding prevKey;
    private static long lastPayloadTime = -1000;

    @Override
    public void onInitializeClient() {
        nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpspectator.next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.pvpspectator"));
        prevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpspectator.prev", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.pvpspectator"));

        // MODO SERVIDOR: snapshot completo del inventario enviado por un servidor que tenga el mod
        ClientPlayNetworking.registerGlobalReceiver(InventoryPayload.ID, (payload, context) -> {
            if (context.client().world == null) return;
            Entity e = context.client().world.getEntityById(payload.entityId());
            if (e instanceof PlayerEntity target) {
                PlayerInventory inv = target.getInventory();
                inv.selectedSlot = Math.max(0, Math.min(8, payload.selectedSlot()));
                for (int i = 0; i < payload.stacks().size() && i < inv.size(); i++) {
                    inv.setStack(i, payload.stacks().get(i).copy());
                }
                target.getHungerManager().setFoodLevel(payload.food());
                lastPayloadTime = context.client().world.getTime();
            }
        });

        HudRenderCallback.EVENT.register(SpectateHud::render);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (nextKey.wasPressed()) cycle(client, 1);
            while (prevKey.wasPressed()) cycle(client, -1);

            PlayerEntity target = watchedPlayer(client);
            if (target != null && client.currentScreen == null) {
                while (client.options.inventoryKey.wasPressed()) {
                    client.setScreen(new SpectateInventoryScreen(target));
                }
            }

            // MODO CLIENTE (cualquier servidor): la camara la manejamos nosotros
            Entity cam = client.getCameraEntity();
            if (!serverMode(client) && cam != null && cam != client.player) {
                if (cam.isRemoved() || !cam.isAlive()) {
                    client.setCameraEntity(client.player);
                } else if (!client.player.isSpectator()) {
                    freezeInput(client); // no te muevas ni pegues mientras miras a otro
                }
            }
        });
    }

    /** True si estamos en espectador Y el servidor tiene este mod. */
    private static boolean serverMode(MinecraftClient client) {
        return client.player != null && client.player.isSpectator()
                && ClientPlayNetworking.canSend(CyclePayload.ID);
    }

    private static void cycle(MinecraftClient client, int dir) {
        if (serverMode(client)) {
            ClientPlayNetworking.send(new CyclePayload(dir));
            return;
        }
        // Modo cliente: rotar entre los jugadores cargados cerca de ti y, al final, volver a tu vista
        List<PlayerEntity> others = new ArrayList<>();
        for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
            if (p != client.player && !p.isSpectator() && p.isAlive()) others.add(p);
        }
        others.sort(Comparator.comparing(p -> p.getGameProfile().getName().toLowerCase()));
        if (others.isEmpty()) {
            client.setCameraEntity(client.player);
            return;
        }
        List<Entity> list = new ArrayList<>(others);
        list.add(client.player);
        int idx = list.indexOf(client.getCameraEntity());
        if (idx < 0) idx = list.size() - 1;
        client.setCameraEntity(list.get(Math.floorMod(idx + dir, list.size())));
    }

    private static void freezeInput(MinecraftClient client) {
        List<KeyBinding> keys = new ArrayList<>(List.of(
                client.options.forwardKey, client.options.backKey, client.options.leftKey,
                client.options.rightKey, client.options.jumpKey, client.options.sneakKey,
                client.options.sprintKey, client.options.attackKey, client.options.useKey,
                client.options.dropKey, client.options.swapHandsKey, client.options.pickItemKey));
        keys.addAll(List.of(client.options.hotbarKeys));
        for (KeyBinding k : keys) {
            k.setPressed(false);
            while (k.wasPressed()) { /* descartar */ }
        }
    }

    /** El jugador que estamos viendo en primera persona, o null. */
    public static PlayerEntity watchedPlayer(MinecraftClient client) {
        if (client.player != null && client.getCameraEntity() instanceof PlayerEntity p && p != client.player) {
            return p;
        }
        return null;
    }

    /** True si hemos recibido hace poco datos completos (inventario/hambre) de un servidor con el mod. */
    public static boolean hasRecentServerData(MinecraftClient client) {
        return client.world != null && client.world.getTime() - lastPayloadTime < 20;
    }
}
