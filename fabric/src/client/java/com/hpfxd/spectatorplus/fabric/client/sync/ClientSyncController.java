package com.hpfxd.spectatorplus.fabric.client.sync;

import com.hpfxd.spectatorplus.fabric.client.sync.screen.ScreenSyncController;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundExperienceSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundFoodSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundHotbarSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundInventorySyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundSelectedSlotSyncPacket;
import java.util.List;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundEffectsSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.SyncedEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

public class ClientSyncController {
    public static ClientSyncData syncData;
    private static Minecraft minecraft = Minecraft.getInstance();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundExperienceSyncPacket.TYPE, ClientSyncController::handle);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundFoodSyncPacket.TYPE, ClientSyncController::handle);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundHotbarSyncPacket.TYPE, ClientSyncController::handle);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSelectedSlotSyncPacket.TYPE, ClientSyncController::handle);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundEffectsSyncPacket.TYPE, ClientSyncController::handle);
        ClientLoginConnectionEvents.INIT.register((handler, client) -> setSyncData(null));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> setSyncData(null));

        ScreenSyncController.init();
    }

    private static void handle(ClientboundEffectsSyncPacket packet, ClientPlayNetworking.Context context) {
        setSyncData(packet.playerId());
        syncData.effects = packet.effects();

        var client = Minecraft.getInstance();
        if (client.player != null) {
            // 移除客户端玩家当前的所有效果
            List<Holder<MobEffect>> toRemove = new ArrayList<>(client.player.getActiveEffectsMap().keySet());
            for (Holder<MobEffect> effect : toRemove) {
                client.player.removeEffect(effect);
            }

            // 添加所有同步的效果到客户端玩家
            for (SyncedEffect synced : syncData.effects) {
                ResourceLocation effectLocation = ResourceLocation.tryParse(synced.effectKey);
                if (effectLocation != null) {
                    java.util.Optional<Holder.Reference<MobEffect>> optHolder = BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                    if (optHolder.isPresent()) {
                        MobEffect effect = optHolder.get().value();
                        Holder<MobEffect> holder = optHolder.get();
                        MobEffectInstance instance = new MobEffectInstance(holder, synced.duration, synced.amplifier);
                        client.player.forceAddEffect(instance, client.player);
                    }
                }
            }
        }
    }

    private static void handle(ClientboundExperienceSyncPacket packet, ClientPlayNetworking.Context context) {
        setSyncData(packet.playerId());

        var player = minecraft.player;
        if (player != null && (packet.progress() != player.experienceProgress || packet.level() != player.experienceLevel))
            player.experienceDisplayStartTick = player.tickCount;

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
