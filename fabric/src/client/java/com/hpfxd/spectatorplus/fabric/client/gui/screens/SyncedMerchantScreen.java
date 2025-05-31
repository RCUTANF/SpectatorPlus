package com.hpfxd.spectatorplus.fabric.client.gui.screens;

import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundMerchantSyncPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SyncedMerchantScreen extends Screen {
    private final List<ClientboundMerchantSyncPacket.TradeOfferData> offers;
    private final int selectedOffer;

    public SyncedMerchantScreen(List<ClientboundMerchantSyncPacket.TradeOfferData> offers, int selectedOffer) {
        super(Component.translatable("spectatorplus.synced_merchant.title"));
        this.offers = offers;
        this.selectedOffer = selectedOffer;
    }

    @Override
    protected void init() {
        // You can add buttons or widgets here if needed (for navigation, etc.)
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);

        int y = 30;
        graphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 10, 0xFFFFFF);

        for (int i = 0; i < offers.size(); i++) {
            ClientboundMerchantSyncPacket.TradeOfferData offer = offers.get(i);
            String offerText = String.format(
                "%s x%d + %s x%d → %s x%d%s",
                getItemName(offer.costA), offer.costA.getCount(),
                getItemName(offer.costB), offer.costB.getCount(),
                getItemName(offer.result), offer.result.getCount(),
                offer.outOfStock ? " (Out of stock)" : ""
            );
            int color = (i == selectedOffer) ? 0x00FF00 : 0xFFFFFF;
            graphics.drawString(this.font, offerText, 20, y, color);
            y += 12;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private String getItemName(ItemStack stack) {
        return stack.isEmpty() ? "-" : stack.getHoverName().getString();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}