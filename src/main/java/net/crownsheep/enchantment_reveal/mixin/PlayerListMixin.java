package net.crownsheep.enchantment_reveal.mixin;

import net.crownsheep.enchantment_reveal.PreviewMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow public abstract List<ServerPlayer> getPlayers();

    @Inject(method = "reloadResources", at = @At("RETURN"))
    private void enchantmentReveal$invalidateOpenPreviews(CallbackInfo ci) {
        for (ServerPlayer player : getPlayers()) {
            if (player.containerMenu instanceof PreviewMenu menu) {
                menu.enchantmentReveal$invalidateServerPreview();
            }
        }
    }
}
