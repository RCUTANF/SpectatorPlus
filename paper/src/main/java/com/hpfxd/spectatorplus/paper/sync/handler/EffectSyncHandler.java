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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectSyncHandler implements Listener {
    private static final String PERMISSION = "spectatorplus.sync.effects";

    private final SpectatorPlugin plugin;
    private final Map<UUID, Collection<PotionEffect>> playerEffects = new HashMap<>();

    public EffectSyncHandler(SpectatorPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Run a task to periodically sync effects (in case some changes are missed by events)
        Bukkit.getScheduler().runTaskTimer(plugin, this::syncAllPlayerEffects, 0, 20); // Every second
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof final Player player) {
            // Delay the sync by one tick to ensure the effect change has been applied
            Bukkit.getScheduler().runTask(plugin, () -> syncPlayerEffects(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartSpectatingEntity(PlayerStartSpectatingEntityEvent event) {
        final Player spectator = event.getPlayer();

        if (event.getNewSpectatorTarget() instanceof final Player target && spectator.hasPermission(PERMISSION)) {
            this.plugin.getSyncController().sendPacket(spectator, new ClientboundEffectsSyncPacket(target.getUniqueId(), target.getActivePotionEffects()));
        }
    }
    
    private void syncAllPlayerEffects() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            syncPlayerEffects(player);
        }
    }
    
    private void syncPlayerEffects(Player player) {
        final Collection<PotionEffect> currentEffects = player.getActivePotionEffects();
        final Collection<PotionEffect> lastEffects = this.playerEffects.get(player.getUniqueId());
        
        // Check if effects have changed
        if (!effectsEqual(currentEffects, lastEffects)) {
            this.playerEffects.put(player.getUniqueId(), currentEffects);
            this.plugin.getSyncController().broadcastPacketToSpectators(player, PERMISSION, new ClientboundEffectsSyncPacket(player.getUniqueId(), currentEffects));
        }
    }
    
    private boolean effectsEqual(Collection<PotionEffect> effects1, Collection<PotionEffect> effects2) {
        if (effects1 == null && effects2 == null) return true;
        if (effects1 == null || effects2 == null) return false;
        if (effects1.size() != effects2.size()) return false;
        
        for (PotionEffect effect1 : effects1) {
            boolean found = false;
            for (PotionEffect effect2 : effects2) {
                if (effect1.getType().equals(effect2.getType()) &&
                    effect1.getAmplifier() == effect2.getAmplifier() &&
                    effect1.getDuration() == effect2.getDuration() &&
                    effect1.isAmbient() == effect2.isAmbient() &&
                    effect1.hasParticles() == effect2.hasParticles() &&
                    effect1.hasIcon() == effect2.hasIcon()) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
