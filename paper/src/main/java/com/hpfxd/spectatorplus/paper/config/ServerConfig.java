package com.hpfxd.spectatorplus.paper.config;

import org.bukkit.configuration.ConfigurationSection;

public class ServerConfig {
    public final boolean workaroundTeleportTicker;
    public final boolean workaroundTeleportOnUntrack;
    public final boolean workaroundsAllowFallback;

    public final boolean workaroundReattachOnWorldChange;
    public final boolean workaroundReattachOnChunkUnload;
    public final boolean workaroundReattachOnEntityRemoval;
    public final boolean workaroundReattachOnPlayerLogout;

    public final boolean screensRequireClientMod;

    public ServerConfig(ConfigurationSection config) {
        this.workaroundTeleportTicker = config.getBoolean("workarounds.auto-update-position");
        this.workaroundTeleportOnUntrack = config.getBoolean("workarounds.auto-teleport-on-untrack");
        this.workaroundsAllowFallback = config.getBoolean("workarounds.allow-fallback");

        this.workaroundReattachOnWorldChange = config.getBoolean("workarounds.reattach-on-world-change");
        this.workaroundReattachOnChunkUnload = config.getBoolean("workarounds.reattach-on-chunk-unload");
        this.workaroundReattachOnEntityRemoval = config.getBoolean("workarounds.reattach-on-entity-removal");
        this.workaroundReattachOnPlayerLogout = config.getBoolean("workarounds.reattach-on-player-logout");

        this.screensRequireClientMod = config.getBoolean("screens.require-client-mod");
    }
}
