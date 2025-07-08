
package com.hpfxd.spectatorplus.paper;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import com.hpfxd.spectatorplus.paper.util.ReflectionUtil;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// For tracking which entity a spectator was spectating before logout
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorWorkarounds implements Listener {
    private final SpectatorPlugin plugin;

    private final Map<UUID, UUID> tempTargets = new HashMap<>();
    private boolean directTeleportFailed;
    private boolean cameraPacketFailed;

    // Tracks which spectators were spectating a player before a world change/portal
    private final Map<UUID, java.util.Set<UUID>> preWorldChangeSpectators = new HashMap<>();

    // Tracks which spectators were spectating a player before logout
    private final Map<UUID, java.util.Set<UUID>> preLogoutSpectators = new HashMap<>();

    // Tracks which entity a spectator was spectating before they logged out
    private final Map<UUID, UUID> preLogoutSpectatorTargets = new ConcurrentHashMap<>();
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnWorldChange) return;
        Player player = event.getPlayer();
        java.util.Set<UUID> spectators = new java.util.HashSet<>();
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) {
                spectators.add(spectator.getUniqueId());
            }
        }
        if (!spectators.isEmpty()) {
            preWorldChangeSpectators.put(player.getUniqueId(), spectators);
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Tracked spectators for portal: " + player.getName() + " <- " + spectators.size());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnWorldChange) return;
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) return; // Only track if world is changing
        Player player = event.getPlayer();
        java.util.Set<UUID> spectators = new java.util.HashSet<>();
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) {
                spectators.add(spectator.getUniqueId());
            }
        }
        if (!spectators.isEmpty()) {
            preWorldChangeSpectators.put(player.getUniqueId(), spectators);
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Tracked spectators for world-change teleport: " + player.getName() + " <- " + spectators.size());
        }
    }

    public SpectatorWorkarounds(SpectatorPlugin plugin) {
        this.plugin = plugin;

        if (plugin.getServerConfig().workaroundTeleportTicker) {
            Bukkit.getScheduler().runTaskTimer(plugin, this::updateSpectatorPositions, 20, 20);
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void updateSpectatorPositions() {
        for (final Player spectator : Bukkit.getOnlinePlayers()) {
            final Entity target = spectator.getSpectatorTarget();

            if (target != null) {
                if (spectator.getWorld().equals(target.getWorld())) {
                    if (!this.directTeleportFailed) {
                        try {
                            ReflectionUtil.directTeleport(spectator, target.getLocation());
                        } catch (Throwable e) {
                            this.directTeleportFailed = true;
                            this.plugin.getSLF4JLogger().warn("auto-update-position workaround: Failed to call directTeleport, will not try again", e);
                            if (this.plugin.getServerConfig().workaroundsAllowFallback) {
                                this.plugin.getSLF4JLogger().warn("\"allow-fallback\" is enabled in the plugin configuration. This has a few drawbacks, it is recommended to view the notes in the config about this option.");
                            }
                        }
                    }

                    if (this.directTeleportFailed && this.plugin.getServerConfig().workaroundsAllowFallback) {
                        spectator.setSpectatorTarget(null);
                        spectator.setSpectatorTarget(target);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUntrack(PlayerUntrackEntityEvent event) {
        if (!this.plugin.getServerConfig().workaroundTeleportOnUntrack || !event.getPlayer().isConnected()) {
            return;
        }

        final Player spectator = event.getPlayer();
        final Entity target = event.getEntity();

        if (!target.equals(spectator.getSpectatorTarget())) {
            return;
        }

        // the target has been untracked by the spectator. this is usually caused by the target teleporting a long
        // distance. so here, we need to teleport the spectator to the target, and wait for the PlayerTrackEntityEvent
        // and re-apply the spectator target.
        // this would be a lot simpler if Paper let us cancel the PlayerUntrackEntityEvent

        this.tempTargets.put(spectator.getUniqueId(), target.getUniqueId());

        if (!this.directTeleportFailed) {
            try {
                ReflectionUtil.directTeleport(spectator, target.getLocation());
            } catch (Throwable e) {
                this.directTeleportFailed = true;
            }
        }

        if (this.directTeleportFailed && this.plugin.getServerConfig().workaroundsAllowFallback) {
            spectator.teleport(target, PlayerTeleportEvent.TeleportCause.SPECTATE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (!this.plugin.getServerConfig().workaroundTeleportOnUntrack) {
            return;
        }

        final Player spectator = event.getPlayer();
        final Entity target = event.getEntity();

        if (this.tempTargets.remove(spectator.getUniqueId(), target.getUniqueId()) && !event.isCancelled()) {
            // we need to schedule the re-apply for a tick later, as the target is not actually tracked yet when
            // PlayerTrackEntityEvent is called.
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (!this.cameraPacketFailed) {
                    try {
                        // attempt to send ClientboundSetCameraPacket directly to the spectator as that's all that is really
                        // needed, and we can try to skip the logic in setSpectatorTarget() which includes teleporting and
                        // calling PlayerStartSpectatingEntityEvent.
                        ReflectionUtil.sendCameraPacket(spectator, target);
                    } catch (Throwable e) {
                        this.cameraPacketFailed = true;
                        this.plugin.getSLF4JLogger().warn("auto-teleport-on-untrack workaround: Failed to send ClientboundSetCameraPacket directly", e);
                        if (this.plugin.getServerConfig().workaroundsAllowFallback) {
                            this.plugin.getSLF4JLogger().warn("\"allow-fallback\" is enabled in the plugin configuration, falling back to Bukkit setSpectatorTarget(). This is unlikely to cause issues.");
                        }
                    }
                }

                if (this.cameraPacketFailed) {
                    spectator.setSpectatorTarget(null);
                    spectator.setSpectatorTarget(target);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartSpectating(PlayerStartSpectatingEntityEvent event) {
        if (!this.plugin.getServerConfig().workaroundTeleportOnUntrack) {
            return;
        }

        final Player spectator = event.getPlayer();
        final Entity target = event.getNewSpectatorTarget();

        if (!target.getTrackedBy().contains(spectator)) {
            this.tempTargets.put(spectator.getUniqueId(), target.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        if (!this.plugin.getServerConfig().workaroundTeleportOnUntrack) {
            return;
        }

        final Player spectator = event.getPlayer();
        final Entity target = event.getSpectatorTarget();

        // the spectator has stopped spectating the target. so we don't want to re-apply if the target is tracked again.
        this.tempTargets.remove(spectator.getUniqueId(), target.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.tempTargets.remove(event.getPlayer().getUniqueId());

        if (!this.plugin.getServerConfig().workaroundReattachOnPlayerLogout) return;
        Player player = event.getPlayer();
        java.util.Set<UUID> spectators = new java.util.HashSet<>();
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) {
                spectators.add(spectator.getUniqueId());
            }
        }
        if (!spectators.isEmpty()) {
            preLogoutSpectators.put(player.getUniqueId(), spectators);
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Tracked spectators for logout: " + player.getName() + " <- " + spectators.size());
        }

        // --- New: Track if the quitting player is a spectator and who they were spectating ---
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
            Entity target = player.getSpectatorTarget();
            preLogoutSpectatorTargets.put(player.getUniqueId(), target.getUniqueId());
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Tracked spectator logout: " + player.getName() + " was spectating " + (target instanceof Player ? ((Player)target).getName() : target.getType().name()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnPlayerLogout) return;
        Player player = event.getPlayer();
        java.util.Set<UUID> spectators = preLogoutSpectators.remove(player.getUniqueId());
        if (spectators != null && !spectators.isEmpty()) {
            for (UUID spectatorId : spectators) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null && spectator.isOnline() && spectator.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] (Logout) Scheduling reattach attempts for spectator: " + spectator.getName() + " -> " + player.getName());
                    final int maxAttempts = 80;
                    final int[] attempts = {0};
                    final Runnable[] task = new Runnable[1];
                    task[0] = () -> {
                        if (!spectator.isOnline() || !player.isOnline()) return;
                        if (spectator.getGameMode() != org.bukkit.GameMode.SPECTATOR) return;
                        this.plugin.getSLF4JLogger().debug("[SpectatorWorkarounds] Attempting to reattach spectator " + spectator.getName() + " to " + player.getName() + " after login (attempt " + (attempts[0]+1) + ")");
                        spectator.setSpectatorTarget(null);
                        spectator.setSpectatorTarget(player);
                        attempts[0]++;
                        if ((spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) || attempts[0] >= maxAttempts) {
                            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Finished reattach attempts for " + spectator.getName() + " after login (success=" + (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) + ")");
                            return;
                        }
                        Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 1L);
                    };
                    Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 2L);
                }
            }
        } else {
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] No tracked spectators for player after login: " + player.getName());
        }

        // --- New: If the joining player was a spectator, try to reattach them to their previous target ---
        UUID lastTargetId = preLogoutSpectatorTargets.remove(player.getUniqueId());
        if (lastTargetId != null) {
            // Try to find the entity (prefer player, fallback to any entity)
            Entity target = Bukkit.getPlayer(lastTargetId);
            if (target == null) {
                // Try to find any entity with that UUID in any world
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    target = world.getEntity(lastTargetId);
                    if (target != null) break;
                }
            }
            if (target != null && player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] (Spectator) Scheduling reattach attempts for " + player.getName() + " -> " + (target instanceof Player ? ((Player)target).getName() : target.getType().name()));
                final Entity reattachTarget = target;
                final int maxAttempts = 80;
                final int[] attempts = {0};
                final Runnable[] task = new Runnable[1];
                task[0] = () -> {
                    if (!player.isOnline()) return;
                    if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) return;
                    this.plugin.getSLF4JLogger().debug("[SpectatorWorkarounds] Attempting to reattach " + player.getName() + " to previous target after login (attempt " + (attempts[0]+1) + ")");
                    player.setSpectatorTarget(null);
                    player.setSpectatorTarget(reattachTarget);
                    attempts[0]++;
                    if ((player.getSpectatorTarget() != null && player.getSpectatorTarget().equals(reattachTarget)) || attempts[0] >= maxAttempts) {
                        this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Finished reattach attempts for " + player.getName() + " after login (success=" + (player.getSpectatorTarget() != null && player.getSpectatorTarget().equals(reattachTarget)) + ")");
                        return;
                    }
                    Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 1L);
                };
                Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 2L);
            } else {
                this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] No valid previous target for spectator after login: " + player.getName());
            }
        }
    }
    // --- New workaround event handlers ---

    // Re-attach spectator after world change (portal, respawn)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] onPlayerChangedWorld called for player: " + event.getPlayer().getName() + " (from " + event.getFrom().getName() + " to " + event.getPlayer().getWorld().getName() + ")");
        if (!this.plugin.getServerConfig().workaroundReattachOnWorldChange) {
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] workaroundReattachOnWorldChange is disabled in config");
            return;
        }
        Player player = event.getPlayer();
        java.util.Set<UUID> spectators = preWorldChangeSpectators.remove(player.getUniqueId());
        if (spectators != null && !spectators.isEmpty()) {
            for (UUID spectatorId : spectators) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null && spectator.isOnline() && spectator.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] (Tracked) Scheduling reattach attempts for spectator: " + spectator.getName() + " -> " + player.getName());
                    final int maxAttempts = 80;
                    final int[] attempts = {0};
                    final Runnable[] task = new Runnable[1];
                    task[0] = () -> {
                        if (!spectator.isOnline() || !player.isOnline()) return;
                        if (spectator.getGameMode() != org.bukkit.GameMode.SPECTATOR) return;
                        this.plugin.getSLF4JLogger().debug("[SpectatorWorkarounds] Attempting to reattach spectator " + spectator.getName() + " to " + player.getName() + " (attempt " + (attempts[0]+1) + ")");
                        spectator.setSpectatorTarget(null);
                        spectator.setSpectatorTarget(player);
                        attempts[0]++;
                        if ((spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) || attempts[0] >= maxAttempts) {
                            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Finished reattach attempts for " + spectator.getName() + " (success=" + (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) + ")");
                            return;
                        }
                        Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 1L);
                    };
                    Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 2L);
                }
            }
        } else {
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] No tracked spectators for player: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnWorldChange) return;
        Player player = event.getPlayer();
        // Track all spectators who were spectating this player before death
        java.util.Set<UUID> spectators = new java.util.HashSet<>();
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) {
                spectators.add(spectator.getUniqueId());
            }
        }
        if (!spectators.isEmpty()) {
            // Schedule reattach attempts for each spectator, robust to world changes
            for (UUID spectatorId : spectators) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null && spectator.isOnline() && spectator.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] (Respawn) Scheduling reattach attempts for spectator: " + spectator.getName() + " -> " + player.getName());
                    final int maxAttempts = 80;
                    final int[] attempts = {0};
                    final Runnable[] task = new Runnable[1];
                    task[0] = () -> {
                        if (!spectator.isOnline() || !player.isOnline()) return;
                        if (spectator.getGameMode() != org.bukkit.GameMode.SPECTATOR) return;
                        this.plugin.getSLF4JLogger().debug("[SpectatorWorkarounds] Attempting to reattach spectator " + spectator.getName() + " to " + player.getName() + " after respawn (attempt " + (attempts[0]+1) + ")");
                        spectator.setSpectatorTarget(null);
                        spectator.setSpectatorTarget(player);
                        attempts[0]++;
                        if ((spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) || attempts[0] >= maxAttempts) {
                            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] Finished reattach attempts for " + spectator.getName() + " after respawn (success=" + (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(player)) + ")");
                            return;
                        }
                        Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 1L);
                    };
                    Bukkit.getScheduler().runTaskLater(this.plugin, task[0], 2L);
                }
            }
        } else {
            this.plugin.getSLF4JLogger().info("[SpectatorWorkarounds] No tracked spectators for player after respawn: " + player.getName());
        }
    }

    // Re-attach spectator after chunk unload (for non-player entities)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnChunkUnload) return;
        for (Entity entity : event.getChunk().getEntities()) {
            for (Player spectator : Bukkit.getOnlinePlayers()) {
                if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(entity)) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        spectator.setSpectatorTarget(null);
                        spectator.setSpectatorTarget(entity);
                    });
                }
            }
        }
    }

    // Re-attach spectator after entity removal/despawn
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnEntityRemoval) return;
        Entity entity = event.getEntity();
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(entity)) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    spectator.setSpectatorTarget(null);
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        if (!this.plugin.getServerConfig().workaroundReattachOnEntityRemoval) return;
        Entity entity = event.getEntity();
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (spectator.getSpectatorTarget() != null && spectator.getSpectatorTarget().equals(entity)) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    spectator.setSpectatorTarget(null);
                });
            }
        }
    }
}
