package net.crownsheep.enchantment_reveal.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.crownsheep.enchantment_reveal.OfferPreview;
import net.crownsheep.enchantment_reveal.PreviewMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"),
            index = 1)
    private List<Component> enchantmentReveal$showFullOffer(List<Component> vanilla, @Local(name = "i") int row) {
        OfferPreview preview = ((PreviewMenu) menu).enchantmentReveal$getPreview();
        if (preview == null || minecraft.level == null || vanilla.isEmpty()
                || !(vanilla.getFirst().getContents() instanceof TranslatableContents clue)
                || !clue.getKey().equals("container.enchant.clue")) {
            return vanilla;
        }

        OfferPreview.Offer offer = preview.offers().get(row);
        if (!offer.hasValidEntries()) {
            return vanilla;
        }

        var registry = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Component> lines = new ArrayList<>();
        for (OfferPreview.Entry entry : offer.enchantments()) {
            var enchantment = registry.get(entry.enchantmentId());
            if (enchantment.isEmpty()) {
                return vanilla;
            }
            lines.add(Enchantment.getFullname(enchantment.get(), entry.level()).copy().withStyle(ChatFormatting.GRAY));
        }

        lines.addAll(vanilla.subList(1, vanilla.size()));
        return lines;
    }
}
