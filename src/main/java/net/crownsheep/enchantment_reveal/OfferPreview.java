package net.crownsheep.enchantment_reveal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record OfferPreview(int containerId, ItemStack input, int seedBits, List<Offer> offers)
        implements CustomPacketPayload {
    public static final int OFFER_COUNT = 3;
    public static final int MAX_ENCHANTMENTS_PER_OFFER = 256;
    public static final Type<OfferPreview> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("enchantment_reveal", "offers_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OfferPreview> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OfferPreview::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC, OfferPreview::input,
            ByteBufCodecs.VAR_INT, OfferPreview::seedBits,
            Offer.STREAM_CODEC.apply(ByteBufCodecs.list(OFFER_COUNT)), OfferPreview::offers,
            OfferPreview::new
    );

    public OfferPreview {
        offers = List.copyOf(offers);
    }

    @Override
    public Type<OfferPreview> type() {
        return TYPE;
    }

    public boolean matches(EnchantmentMenu menu) {
        if (containerId != menu.containerId || offers.size() != OFFER_COUNT || input.isEmpty()
                || seedBits != (menu.getEnchantmentSeed() & 0xffff)
                || !ItemStack.matches(input, menu.getSlot(0).getItem())) {
            return false;
        }

        for (int row = 0; row < OFFER_COUNT; row++) {
            Offer offer = offers.get(row);
            if (offer.cost() != menu.costs[row] || offer.clueId() != menu.enchantClue[row]
                    || offer.clueLevel() != menu.levelClue[row]) {
                return false;
            }
        }
        return true;
    }

    public record Offer(int cost, int clueId, int clueLevel, List<Entry> enchantments) {
        private static final StreamCodec<ByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Offer::cost,
                ByteBufCodecs.VAR_INT, Offer::clueId,
                ByteBufCodecs.VAR_INT, Offer::clueLevel,
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENCHANTMENTS_PER_OFFER)), Offer::enchantments,
                Offer::new
        );

        public Offer {
            enchantments = List.copyOf(enchantments);
        }

        public boolean hasValidEntries() {
            if (cost <= 0 || enchantments.isEmpty() || enchantments.size() > MAX_ENCHANTMENTS_PER_OFFER) {
                return false;
            }

            Set<Integer> ids = new HashSet<>();
            boolean containsClue = false;
            for (Entry entry : enchantments) {
                if (entry.enchantmentId() < 0 || entry.level() < 1 || entry.level() > Enchantment.MAX_LEVEL
                        || !ids.add(entry.enchantmentId())) {
                    return false;
                }
                containsClue |= entry.enchantmentId() == clueId && entry.level() == clueLevel;
            }
            return containsClue;
        }
    }

    public record Entry(int enchantmentId, int level) {
        private static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::enchantmentId,
                ByteBufCodecs.VAR_INT, Entry::level,
                Entry::new
        );
    }
}
