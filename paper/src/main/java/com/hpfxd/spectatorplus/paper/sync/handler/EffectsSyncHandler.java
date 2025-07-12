package com.hpfxd.spectatorplus.paper.sync.handler;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.hpfxd.spectatorplus.paper.SpectatorPlugin;
import com.hpfxd.spectatorplus.paper.sync.packet.ClientboundEffectsSyncPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.stream.Collectors;
import com.hpfxd.spectatorplus.paper.effect.EffectType;

public class EffectsSyncHandler implements Listener {
    private static final String PERMISSION = "spectatorplus.sync.effects";

    private final SpectatorPlugin plugin;

    public EffectsSyncHandler(SpectatorPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private java.util.List<com.hpfxd.spectatorplus.paper.effect.SyncedEffect> getSyncedEffects(Player player) {
        java.util.List<PotionEffect> effects = java.util.List.copyOf(player.getActivePotionEffects());
        if (effects.isEmpty()) {
            this.plugin.getSLF4JLogger().info("[SpectatorPlus] No active effects for {}", player.getName());
        } else {
            for (PotionEffect effect : effects) {
                this.plugin.getSLF4JLogger().info("[SpectatorPlus] Effect for {}: type={}, key={}, amplifier={}, duration={}", player.getName(), effect.getType().getName(), effect.getType().getKey(), effect.getAmplifier(), effect.getDuration());
            }
        }
        return effects.stream()
            .map(pe -> new com.hpfxd.spectatorplus.paper.effect.SyncedEffect(
                pe.getType().getKey().toString(),
                pe.getAmplifier(),
                pe.getDuration()
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartSpectatingEntity(PlayerStartSpectatingEntityEvent event) {
        final Player spectator = event.getPlayer();
        if (event.getNewSpectatorTarget() instanceof final Player target && spectator.hasPermission(PERMISSION)) {
            java.util.List<com.hpfxd.spectatorplus.paper.effect.SyncedEffect> effects = getSyncedEffects(target);
            this.plugin.getSLF4JLogger().info("[SpectatorPlus] Sending effects sync packet to {}: {}", spectator.getName(), effects);
            this.plugin.getSyncController().sendPacket(spectator,
                    new ClientboundEffectsSyncPacket(target.getUniqueId(), effects));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                java.util.List<com.hpfxd.spectatorplus.paper.effect.SyncedEffect> effects = getSyncedEffects(player);
                this.plugin.getSLF4JLogger().info("[SpectatorPlus] Broadcasting effects sync packet for {}: {}", player.getName(), effects);
                this.plugin.getSyncController().broadcastPacketToSpectators(player, PERMISSION,
                    new ClientboundEffectsSyncPacket(player.getUniqueId(), effects));
            });
        }
    }
}
