package net.crownsheep.enchantment_reveal;

public interface PreviewMenu {
    void enchantmentReveal$sendPreview(boolean fullSync);

    void enchantmentReveal$invalidateServerPreview();

    void enchantmentReveal$receivePreview(OfferPreview preview);

    void enchantmentReveal$discardChangedPreview();

    OfferPreview enchantmentReveal$getPreview();
}
