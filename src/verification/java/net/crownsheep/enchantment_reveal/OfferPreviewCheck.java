package net.crownsheep.enchantment_reveal;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public final class OfferPreviewCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var clue = new OfferPreview.Entry(12, 4);
        var bonus = new OfferPreview.Entry(20, 1);
        var offer = new OfferPreview.Offer(30, 12, 4, List.of(clue, bonus));
        // The game binds item defaults as part of data pack loading. This check only needs a patch.
        var book = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(Items.BOOK)).orElseThrow();
        book.bindComponents(DataComponentMap.EMPTY);
        var input = new ItemStack(book);
        input.set(DataComponents.CUSTOM_NAME, Component.literal("Test book"));
        var preview = new OfferPreview(17, input, 0xabcd, List.of(offer, offer, offer));

        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            OfferPreview.STREAM_CODEC.encode(buffer, preview);
            var decoded = OfferPreview.STREAM_CODEC.decode(buffer);
            require(decoded.containerId() == 17 && decoded.seedBits() == 0xabcd && ItemStack.matches(decoded.input(), input)
                    && decoded.offers().equals(preview.offers()) && !buffer.isReadable(), "offer round trip");

            buffer.clear();
            OfferPreview.STREAM_CODEC.encode(buffer, new OfferPreview(17, ItemStack.EMPTY, 0, List.of()));
            var cleared = OfferPreview.STREAM_CODEC.decode(buffer);
            require(cleared.containerId() == 17 && cleared.input().isEmpty()
                    && cleared.offers().isEmpty() && !buffer.isReadable(), "invalidation round trip");

            buffer.clear();
            buffer.writeVarInt(17).writeVarInt(0).writeVarInt(0xabcd).writeVarInt(3);
            buffer.writeVarInt(30).writeVarInt(12).writeVarInt(4);
            buffer.writeVarInt(OfferPreview.MAX_ENCHANTMENTS_PER_OFFER + 1);
            try {
                OfferPreview.STREAM_CODEC.decode(buffer);
                throw new AssertionError("oversized row was accepted");
            } catch (DecoderException expected) {
                require(!buffer.isReadable(), "oversized row rejected at its count");
            }
        } finally {
            buffer.release();
        }

        require(offer.hasValidEntries(), "valid offer");
        require(!new OfferPreview.Offer(30, 12, 4, List.of(bonus)).hasValidEntries(), "missing clue");
        require(!new OfferPreview.Offer(30, 12, 4, List.of(clue, clue)).hasValidEntries(), "duplicate enchantment");
        require(!new OfferPreview.Offer(30, 12, 4, List.of(clue,
                new OfferPreview.Entry(20, Enchantment.MAX_LEVEL + 1))).hasValidEntries(), "unsupported enchantment level");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
