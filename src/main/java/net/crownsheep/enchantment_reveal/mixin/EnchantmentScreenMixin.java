package net.crownsheep.enchantment_reveal.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen {
    private final EnchantmentScreen enchantmentScreen = (EnchantmentScreen) (Object) this;

    private int x;
    private int y;

    public EnchantmentScreenMixin(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    private boolean addToList(List<Component> tooltip, Object element) {
        for (int i = 0; i < 3; i++) {
            if (this.isHovering(60, 14 + 19 * i, 108, 17, x, y)) {
                List<EnchantmentInstance> enchantments = getServerSideEnchantmentList(i);

                for (EnchantmentInstance enchantmentInstance : enchantments) {
                    tooltip.add(
                            Enchantment.getFullname(
                                    enchantmentInstance.enchantment(),
                                    enchantmentInstance.level()
                            ).copy().withStyle(ChatFormatting.GRAY)
                    );
                }
            }
        }

        return true;
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentScreen;isHovering(IIIIDD)Z"))
    private void isHoveringGetter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        x = mouseX;
        y = mouseY;
    }

    private List<EnchantmentInstance> getServerSideEnchantmentList(int enchantmentSlot) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!minecraft.isSingleplayer() || minecraft.getSingleplayerServer() == null || minecraft.player == null) {
            return List.of();
        }

        ServerPlayer serverPlayer = minecraft.getSingleplayerServer()
                .getPlayerList()
                .getPlayer(minecraft.player.getUUID());

        if (serverPlayer == null || !(serverPlayer.containerMenu instanceof EnchantmentMenu serverMenu)) {
            return List.of();
        }

        ItemStack itemStack = serverMenu.getSlot(0).getItem();
        int cost = serverMenu.costs[enchantmentSlot];

        if (itemStack.isEmpty() || cost <= 0) {
            return List.of();
        }

        return ((EnchantmentMenuInvoker) serverMenu).enchantmentReveal$getEnchantmentList(
                serverPlayer.level().registryAccess(),
                itemStack,
                enchantmentSlot,
                cost
        );
    }
}
