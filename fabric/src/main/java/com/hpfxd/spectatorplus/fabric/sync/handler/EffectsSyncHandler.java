package com.hpfxd.spectatorplus.fabric.sync.handler;

import com.hpfxd.spectatorplus.fabric.sync.ServerSyncController;
import com.hpfxd.spectatorplus.fabric.sync.SyncedEffect;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundEffectsSyncPacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.*;

public class EffectsSyncHandler {
    private static final Map<UUID, List<SyncedEffect>> EFFECTS = new HashMap<>();

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EFFECTS.remove(handler.getPlayer().getUUID()));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EFFECTS.clear());

        ServerTickEvents.END_WORLD_TICK.register(EffectsSyncHandler::tick);
    }

    private static void tick(ServerLevel level) {
        for (final ServerPlayer player : level.players()) {
            final List<SyncedEffect> cachedEffects = EFFECTS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());

            final List<SyncedEffect> currentEffects = new ArrayList<>();
            for (MobEffectInstance effectInstance : player.getActiveEffects()) {
                String effectKey = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect().value()).toString();
                currentEffects.add(new SyncedEffect(
                    effectKey,
                    effectInstance.getAmplifier(),
                    effectInstance.getDuration()
                ));
            }

            if (!effectsEqual(currentEffects, cachedEffects)) {
                cachedEffects.clear();
                cachedEffects.addAll(currentEffects);

                ServerSyncController.broadcastPacketToSpectators(player, new ClientboundEffectsSyncPacket(player.getUUID(), new ArrayList<>(currentEffects)));
            }
        }
    }

    private static boolean effectsEqual(List<SyncedEffect> list1, List<SyncedEffect> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }

        final Map<String, SyncedEffect> map1 = new HashMap<>();
        final Map<String, SyncedEffect> map2 = new HashMap<>();

        for (SyncedEffect effect : list1) {
            map1.put(effect.effectKey, effect);
        }

        for (SyncedEffect effect : list2) {
            map2.put(effect.effectKey, effect);
        }

        return map1.equals(map2);
    }
}