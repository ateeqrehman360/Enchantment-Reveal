package net.crownsheep.enchantment_reveal.mixin;

import net.crownsheep.enchantment_reveal.PreviewMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMixin {
    @Shadow private boolean suppressRemoteUpdates;

    // Inventory clicks can recalculate offers with remote updates suppressed. Wait for the real sync.
    @Inject(method = "broadcastChanges", at = @At("RETURN"))
    private void enchantmentReveal$afterIncrementalSync(CallbackInfo ci) {
        if (!suppressRemoteUpdates && (Object) this instanceof PreviewMenu menu) {
            menu.enchantmentReveal$sendPreview(false);
        }
    }

    @Inject(method = "sendAllDataToRemote", at = @At("RETURN"))
    private void enchantmentReveal$afterFullSync(CallbackInfo ci) {
        if ((Object) this instanceof PreviewMenu menu) {
            menu.enchantmentReveal$sendPreview(true);
        }
    }

    @Inject(method = "setData", at = @At("RETURN"))
    private void enchantmentReveal$afterDataChange(int index, int value, CallbackInfo ci) {
        if ((Object) this instanceof PreviewMenu menu) {
            menu.enchantmentReveal$discardChangedPreview();
        }
    }
}
