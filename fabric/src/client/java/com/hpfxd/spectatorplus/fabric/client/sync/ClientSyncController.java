package com.hpfxd.spectatorplus.fabric.client.sync;

import com.hpfxd.spectatorplus.fabric.client.sync.screen.ScreenSyncController;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundExperienceSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundFoodSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundHotbarSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundSelectedSlotSyncPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class ClientSyncController {
    public static ClientSyncData syncData;

    public static void init() {
        // Experience Sync
        ClientPlayNetworking.registerGlobalReceiver(ClientboundExperienceSyncPacket.TYPE, (packet, context) -> {
            if (packet instanceof ClientboundExperienceSyncPacket p) {
                // Log packet type and raw bytes
                if (context instanceof FriendlyByteBuf buf) {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.getBytes(buf.readerIndex(), data);
                    System.out.println("[Fabric] Received ClientboundExperienceSyncPacket: size=" + data.length + ", bytes=" + java.util.Arrays.toString(data));
                } else {
                    System.out.println("[Fabric] Received ClientboundExperienceSyncPacket");
                }
            }
            handle(packet, context);
        });

        // Food Sync
        ClientPlayNetworking.registerGlobalReceiver(ClientboundFoodSyncPacket.TYPE, (packet, context) -> {
            if (context instanceof FriendlyByteBuf buf) {
                byte[] data = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), data);
                System.out.println("[Fabric] Received ClientboundFoodSyncPacket: size=" + data.length + ", bytes=" + java.util.Arrays.toString(data));
            } else {
                System.out.println("[Fabric] Received ClientboundFoodSyncPacket");
            }
            handle(packet, context);
        });

        // Hotbar Sync
        ClientPlayNetworking.registerGlobalReceiver(ClientboundHotbarSyncPacket.TYPE, (packet, context) -> {
            if (context instanceof FriendlyByteBuf buf) {
                byte[] data = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), data);
                System.out.println("[Fabric] Received ClientboundHotbarSyncPacket: size=" + data.length + ", bytes=" + java.util.Arrays.toString(data));
            } else {
                System.out.println("[Fabric] Received ClientboundHotbarSyncPacket");
            }
            handle(packet, context);
        });

        // Selected Slot Sync
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSelectedSlotSyncPacket.TYPE, (packet, context) -> {
            if (context instanceof FriendlyByteBuf buf) {
                byte[] data = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), data);
                System.out.println("[Fabric] Received ClientboundSelectedSlotSyncPacket: size=" + data.length + ", bytes=" + java.util.Arrays.toString(data));
            } else {
                System.out.println("[Fabric] Received ClientboundSelectedSlotSyncPacket");
            }
            handle(packet, context);
        });

        ClientLoginConnectionEvents.INIT.register((handler, client) -> setSyncData(null));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> setSyncData(null));

        ScreenSyncController.init();
    }

    private static void handle(ClientboundExperienceSyncPacket packet, ClientPlayNetworking.Context context) {
        setSyncData(packet.playerId());

        syncData.experienceProgress = packet.progress();
        syncData.experienceLevel = packet.level();
        syncData.experienceNeededForNextLevel = packet.neededForNextLevel();
    }

    private static void handle(ClientboundFoodSyncPacket packet, ClientPlayNetworking.Context context) {
        setSyncData(packet.playerId());

        if (syncData.foodData == null) {
            syncData.foodData = new FoodData();
        }
        syncData.foodData.setFoodLevel(packet.food());
        syncData.foodData.setSaturation(packet.saturation());
    }

    private static void handle(ClientboundHotbarSyncPacket packet, ClientPlayNetworking.Context context) {
        setSyncData(packet.playerId());

        final ItemStack[] items = packet.items();
        for (int slot = 0; slot < items.length; slot++) {
            final ItemStack item = items[slot];

            if (item != null) {
                syncData.hotbarItems.set(slot, item);
            }
        }
    }

    private static void handle(ClientboundSelectedSlotSyncPacket packet, ClientPlayNetworking.Context context) {
        setSyncData(packet.playerId());

        syncData.selectedHotbarSlot = packet.selectedSlot();
    }

    public static void setSyncData(UUID playerId) {
        if (playerId == null) {
            syncData = null;
        } else if (syncData == null || !syncData.playerId.equals(playerId)) {
            syncData = new ClientSyncData(playerId);
        }
    }
}
