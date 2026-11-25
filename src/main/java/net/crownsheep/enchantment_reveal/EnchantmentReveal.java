package net.crownsheep.enchantment_reveal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class EnchantmentReveal implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(OfferPreview.TYPE, OfferPreview.STREAM_CODEC);
    }
}
