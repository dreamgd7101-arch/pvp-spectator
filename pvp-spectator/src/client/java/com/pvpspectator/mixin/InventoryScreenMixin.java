package com.pvpspectator.mixin;

import com.pvpspectator.client.SpectateInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** In the spectate inventory, draw the watched player's model instead of our own. */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @ModifyArg(
            method = "drawBackground",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V"),
            index = 9)
    private LivingEntity pvpspectator$useTarget(LivingEntity original) {
        if ((Object) this instanceof SpectateInventoryScreen screen) return screen.target;
        return original;
    }
}
