package com.hpfxd.spectatorplus.paper.sync.packet;

import com.google.common.io.ByteArrayDataOutput;
import com.hpfxd.spectatorplus.paper.util.SerializationUtil;
import com.hpfxd.spectatorplus.paper.sync.ClientboundSyncPacket;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public record ClientboundMerchantSyncPacket(
    UUID playerId,
    List<TradeOfferData> offers,
    int selectedOffer
) implements ClientboundSyncPacket {
    public static final NamespacedKey ID = new NamespacedKey("spectatorplus", "merchant_sync");

    @Override
    public void write(ByteArrayDataOutput buf) {
        buf.writeUTF(playerId.toString());
        buf.writeInt(offers.size());
        for (TradeOfferData offer : offers) {
            offer.write(buf);
        }
        buf.writeInt(selectedOffer);
    }

    @Override
    public NamespacedKey channel() {
        return ID;
    }

    public static class TradeOfferData {
        public final ItemStack costA;
        public final ItemStack costB;
        public final ItemStack result;
        public final boolean outOfStock;

        public TradeOfferData(ItemStack costA, ItemStack costB, ItemStack result, boolean outOfStock) {
            this.costA = costA == null ? ItemStack.empty() : costA;
            this.costB = costB == null ? ItemStack.empty() : costB;
            this.result = result == null ? ItemStack.empty() : result;
            this.outOfStock = outOfStock;
        }

        public void write(ByteArrayDataOutput buf) {
            // Use your existing SerializationUtil.writeItem for each ItemStack
            SerializationUtil.writeItem(buf, costA);
            SerializationUtil.writeItem(buf, costB);
            SerializationUtil.writeItem(buf, result);
            buf.writeBoolean(outOfStock);
        }
    }
}