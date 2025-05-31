package com.hpfxd.spectatorplus.fabric.sync.packet;

import com.hpfxd.spectatorplus.fabric.sync.ClientboundSyncPacket;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ClientboundMerchantSyncPacket(
    UUID playerId,
    List<TradeOfferData> offers,
    int selectedOffer
) implements ClientboundSyncPacket {
    public static final StreamCodec<FriendlyByteBuf, ClientboundMerchantSyncPacket> STREAM_CODEC =
        CustomPacketPayload.codec(ClientboundMerchantSyncPacket::write, ClientboundMerchantSyncPacket::new);
    public static final CustomPacketPayload.Type<ClientboundMerchantSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.parse("spectatorplus:merchant_sync"));
    private static final String PERMISSION = "spectatorplus.sync.merchant";

    public ClientboundMerchantSyncPacket(FriendlyByteBuf buf) {
        this(
            buf.readUUID(),
            TradeOfferData.readList(buf),
            buf.readVarInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        TradeOfferData.writeList(buf, this.offers);
        buf.writeVarInt(this.selectedOffer);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean canSend(ServerPlayer receiver) {
        return Permissions.check(receiver, PERMISSION, true);
    }

    public static class TradeOfferData {
        public final ItemStack costA;
        public final ItemStack costB;
        public final ItemStack result;
        public final boolean outOfStock;

        public TradeOfferData(ItemStack costA, ItemStack costB, ItemStack result, boolean outOfStock) {
            this.costA = costA == null ? ItemStack.EMPTY : costA;
            this.costB = costB == null ? ItemStack.EMPTY : costB;
            this.result = result == null ? ItemStack.EMPTY : result;
            this.outOfStock = outOfStock;
        }

        public TradeOfferData(FriendlyByteBuf buf) {
            this(
                readItemStack(buf),
                readItemStack(buf),
                readItemStack(buf),
                buf.readBoolean()
            );
        }

        public void write(FriendlyByteBuf buf) {
            writeItemStack(buf, costA);
            writeItemStack(buf, costB);
            writeItemStack(buf, result);
            buf.writeBoolean(outOfStock);
        }

        public static List<TradeOfferData> readList(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<TradeOfferData> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(new TradeOfferData(buf));
            }
            return list;
        }

        public static void writeList(FriendlyByteBuf buf, List<TradeOfferData> list) {
            buf.writeVarInt(list.size());
            for (TradeOfferData offer : list) {
                offer.write(buf);
            }
        }

    }

    private static ItemStack readItemStack(FriendlyByteBuf buf) {
        // Reads an ItemStack from the buffer using NBT
        return ItemStack.STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
    }

    private static void writeItemStack(FriendlyByteBuf buf, ItemStack stack) {
        // Writes an ItemStack to the buffer using NBT
        ItemStack.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, stack == null ? ItemStack.EMPTY : stack);
    }

}