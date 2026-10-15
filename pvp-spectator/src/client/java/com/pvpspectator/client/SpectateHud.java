package com.pvpspectator.client;

import com.pvpspectator.PvpSpectatorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Draws the watched player's hotbar, health, armor and hunger as if you were playing. */
public class SpectateHud {
    private static final Identifier HOTBAR = Identifier.ofVanilla("hud/hotbar");
    private static final Identifier SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
    private static final Identifier OFFHAND = Identifier.ofVanilla("hud/hotbar_offhand_left");
    private static final Identifier HEART_BG = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier FOOD_EMPTY = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");
    private static final Identifier ARMOR_EMPTY = Identifier.ofVanilla("hud/armor_empty");
    private static final Identifier ARMOR_HALF = Identifier.ofVanilla("hud/armor_half");
    private static final Identifier ARMOR_FULL = Identifier.ofVanilla("hud/armor_full");

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        PlayerEntity t = PvpSpectatorClient.watchedPlayer(mc);
        if (t == null) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int left = w / 2 - 91;
        PlayerInventory inv = t.getInventory();

        // Name
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("Viendo: " + t.getName().getString()), w / 2, 6, 0xFFFFFF);

        // En supervivencia/aventura el HUD vanilla ya dibuja la hotbar, vida y hambre del jugador observado.
        // Solo en espectador (donde vanilla lo oculta) lo dibujamos nosotros.
        if (!mc.player.isSpectator()) return;

        // Hotbar
        ctx.drawGuiTexture(RenderLayer::getGuiTextured, HOTBAR, left, h - 22, 182, 22);
        ctx.drawGuiTexture(RenderLayer::getGuiTextured, SELECTION, left - 1 + inv.selectedSlot * 20, h - 23, 24, 23);
        for (int i = 0; i < 9; i++) {
            drawStack(ctx, mc, inv.getStack(i), left + 3 + i * 20, h - 19);
        }
        ItemStack off = inv.getStack(40);
        if (!off.isEmpty()) {
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, OFFHAND, left - 29, h - 23, 29, 24);
            drawStack(ctx, mc, off, left - 26, h - 19);
        }

        // Health
        int health = (int) Math.ceil(t.getHealth());
        for (int i = 0; i < 10; i++) {
            int x = left + i * 8;
            int y = h - 39;
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, HEART_BG, x, y, 9, 9);
            if (health >= i * 2 + 2) ctx.drawGuiTexture(RenderLayer::getGuiTextured, HEART_FULL, x, y, 9, 9);
            else if (health == i * 2 + 1) ctx.drawGuiTexture(RenderLayer::getGuiTextured, HEART_HALF, x, y, 9, 9);
        }

        // Armor
        int armor = t.getArmor();
        for (int i = 0; i < 10; i++) {
            int x = left + i * 8;
            int y = h - 49;
            Identifier id = armor >= i * 2 + 2 ? ARMOR_FULL : armor == i * 2 + 1 ? ARMOR_HALF : ARMOR_EMPTY;
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, id, x, y, 9, 9);
        }

        // Hunger (solo si el servidor nos manda el dato real)
        if (!PvpSpectatorClient.hasRecentServerData(mc)) return;
        int food = t.getHungerManager().getFoodLevel();
        for (int i = 0; i < 10; i++) {
            int x = left + 182 - 9 - i * 8;
            int y = h - 39;
            Identifier id = food >= i * 2 + 2 ? FOOD_FULL : food == i * 2 + 1 ? FOOD_HALF : FOOD_EMPTY;
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, id, x, y, 9, 9);
        }
    }

    private static void drawStack(DrawContext ctx, MinecraftClient mc, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        ctx.drawItem(stack, x, y);
        ctx.drawStackOverlay(mc.textRenderer, stack, x, y);
    }
}
