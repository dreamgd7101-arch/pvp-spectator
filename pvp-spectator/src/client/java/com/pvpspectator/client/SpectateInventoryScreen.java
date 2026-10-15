package com.pvpspectator.client;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/** Read-only view of another player's inventory. All interaction is disabled so nothing is sent to the server. */
public class SpectateInventoryScreen extends InventoryScreen {
    public final PlayerEntity target;

    public SpectateInventoryScreen(PlayerEntity target) {
        super(target);
        this.target = target;
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) { }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return true; }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return true; }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) { return true; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
        }
        return true;
    }
}
