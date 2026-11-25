package net.crownsheep.enchantment_reveal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.crownsheep.enchantment_reveal.OfferPreview;
import net.crownsheep.enchantment_reveal.PreviewMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu implements PreviewMenu {
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;
    @Shadow public abstract int getEnchantmentSeed();

    @Unique private ServerPlayer enchantmentReveal$owner;
    @Unique private List<List<OfferPreview.Entry>> enchantmentReveal$capturing;
    @Unique private OfferPreview enchantmentReveal$serverPreview;
    @Unique private int enchantmentReveal$serverSeed;
    @Unique private boolean enchantmentReveal$dirty;
    @Unique private OfferPreview enchantmentReveal$preview;

    protected EnchantmentMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("RETURN"))
    private void enchantmentReveal$rememberOwner(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        if (inventory.player instanceof ServerPlayer player) {
            enchantmentReveal$owner = player;
        }
    }

    @WrapMethod(method = "slotsChanged")
    private void enchantmentReveal$updateOffers(Container container, Operation<Void> original) {
        ServerPlayer player = enchantmentReveal$owner;
        if (container != enchantSlots || player == null || player.connection == null
                || !ServerPlayNetworking.canSend(player, OfferPreview.TYPE)
                || enchantmentReveal$capturing != null) {
            original.call(container);
            if (container == enchantSlots && player == null) {
                enchantmentReveal$discardChangedPreview();
            }
            return;
        }

        ItemStack input = container.getItem(0).copy();
        int seed = getEnchantmentSeed();
        var entries = new ArrayList<List<OfferPreview.Entry>>(Collections.nCopies(OfferPreview.OFFER_COUNT, List.of()));
        enchantmentReveal$capturing = entries;
        try {
            original.call(container);
        } finally {
            enchantmentReveal$capturing = null;
        }

        OfferPreview snapshot = null;
        if (seed == getEnchantmentSeed() && ItemStack.matches(input, container.getItem(0))
                && entries.stream().anyMatch(offer -> !offer.isEmpty())) {
            List<OfferPreview.Offer> offers = new ArrayList<>(OfferPreview.OFFER_COUNT);
            for (int row = 0; row < OfferPreview.OFFER_COUNT; row++) {
                offers.add(new OfferPreview.Offer(costs[row], enchantClue[row], levelClue[row], entries.get(row)));
            }
            snapshot = new OfferPreview(containerId, input, seed & 0xffff, offers);
        }

        enchantmentReveal$dirty |= snapshot != null || enchantmentReveal$serverPreview != null;
        enchantmentReveal$serverPreview = snapshot;
        enchantmentReveal$serverSeed = seed;
    }

    // Read vanilla's result during offer generation only. The actual enchant click also calls this method.
    @Inject(method = "getEnchantmentList", at = @At("RETURN"))
    private void enchantmentReveal$captureOffer(RegistryAccess registries, ItemStack input, int row, int cost,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        if (enchantmentReveal$capturing != null) {
            var result = cir.getReturnValue();
            if (result.size() > OfferPreview.MAX_ENCHANTMENTS_PER_OFFER) {
                return;
            }

            var ids = registries.lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            enchantmentReveal$capturing.set(row, result.stream()
                    .map(entry -> new OfferPreview.Entry(ids.getId(entry.enchantment()), entry.level()))
                    .toList());
        }
    }

    @Override
    public void enchantmentReveal$sendPreview(boolean fullSync) {
        ServerPlayer player = enchantmentReveal$owner;
        if (player == null || player.connection == null || player.containerMenu != (Object) this
                || enchantmentReveal$capturing != null) {
            return;
        }

        OfferPreview snapshot = enchantmentReveal$serverPreview;
        if (snapshot != null && (enchantmentReveal$serverSeed != getEnchantmentSeed()
                || !snapshot.matches((EnchantmentMenu) (Object) this))) {
            enchantmentReveal$serverPreview = null;
            enchantmentReveal$dirty = true;
            snapshot = null;
        }

        if ((!enchantmentReveal$dirty && !(fullSync && snapshot != null))
                || !ServerPlayNetworking.canSend(player, OfferPreview.TYPE)) {
            return;
        }

        enchantmentReveal$dirty = false;
        ServerPlayNetworking.send(player, snapshot != null ? snapshot
                : new OfferPreview(containerId, ItemStack.EMPTY, 0, List.of()));
    }

    @Override
    public void enchantmentReveal$invalidateServerPreview() {
        // A data pack reload can change eligibility while vanilla's existing clues stay on screen.
        if (enchantmentReveal$serverPreview != null) {
            enchantmentReveal$serverPreview = null;
            enchantmentReveal$dirty = true;
        }
        enchantmentReveal$sendPreview(false);
    }

    @Override
    public void enchantmentReveal$receivePreview(OfferPreview preview) {
        enchantmentReveal$preview = preview.matches((EnchantmentMenu) (Object) this) ? preview : null;
    }

    @Override
    public void enchantmentReveal$discardChangedPreview() {
        if (enchantmentReveal$preview != null && !enchantmentReveal$preview.matches((EnchantmentMenu) (Object) this)) {
            enchantmentReveal$preview = null;
        }
    }

    @Override
    public OfferPreview enchantmentReveal$getPreview() {
        enchantmentReveal$discardChangedPreview();
        return enchantmentReveal$preview;
    }
}
