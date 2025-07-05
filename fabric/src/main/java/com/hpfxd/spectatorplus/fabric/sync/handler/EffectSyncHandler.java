package com.hpfxd.spectatorplus.fabric.sync.handler;

import com.hpfxd.spectatorplus.fabric.sync.ServerSyncController;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundEffectsSyncPacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectSyncHandler {
    private static final Map<UUID, Map<Holder<MobEffect>, MobEffectInstance>> PLAYER_EFFECTS = new HashMap<>();

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PLAYER_EFFECTS.remove(handler.getPlayer().getUUID()));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PLAYER_EFFECTS.clear());

        ServerTickEvents.END_WORLD_TICK.register(EffectSyncHandler::tick);
    }

    private static void tick(ServerLevel level) {
        for (final ServerPlayer player : level.players()) {
            final Map<Holder<MobEffect>, MobEffectInstance> currentEffects = new HashMap<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                currentEffects.put(effect.getEffect(), effect);
            }

            final Map<Holder<MobEffect>, MobEffectInstance> lastEffects = PLAYER_EFFECTS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());

            boolean updated = false;

            // Check if effects have changed
            if (currentEffects.size() != lastEffects.size()) {
                updated = true;
            } else {
                for (Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : currentEffects.entrySet()) {
                    MobEffectInstance lastEffect = lastEffects.get(entry.getKey());
                    MobEffectInstance currentEffect = entry.getValue();
                    
                    if (lastEffect == null || 
                        lastEffect.getAmplifier() != currentEffect.getAmplifier() ||
                        lastEffect.getDuration() != currentEffect.getDuration() ||
                        lastEffect.isAmbient() != currentEffect.isAmbient() ||
                        lastEffect.isVisible() != currentEffect.isVisible() ||
                        lastEffect.showIcon() != currentEffect.showIcon()) {
                        updated = true;
                        break;
                    }
                }
            }

            if (updated) {
                lastEffects.clear();
                lastEffects.putAll(currentEffects);
                
                ServerSyncController.broadcastPacketToSpectators(player, new ClientboundEffectsSyncPacket(player.getUUID(), currentEffects));
            }
        }
    }
}
