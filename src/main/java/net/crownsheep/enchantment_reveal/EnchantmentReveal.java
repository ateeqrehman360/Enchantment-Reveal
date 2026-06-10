package net.crownsheep.enchantment_reveal;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnchantmentReveal implements ClientModInitializer {
    public static final String MOD_ID = "enchantment_reveal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Enchantment Reveal initialized");
    }
}
