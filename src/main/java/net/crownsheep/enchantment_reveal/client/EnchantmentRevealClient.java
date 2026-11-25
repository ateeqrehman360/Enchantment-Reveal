package net.crownsheep.enchantment_reveal.client;

import net.crownsheep.enchantment_reveal.OfferPreview;
import net.crownsheep.enchantment_reveal.PreviewMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.EnchantmentMenu;

public class EnchantmentRevealClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OfferPreview.TYPE, (preview, context) -> {
            if (context.player().containerMenu instanceof EnchantmentMenu menu
                    && menu.containerId == preview.containerId()) {
                ((PreviewMenu) menu).enchantmentReveal$receivePreview(preview);
            }
        });
    }
}
