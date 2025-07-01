package com.hpfxd.spectatorplus.fabric.client.util;

import com.hpfxd.spectatorplus.fabric.client.SpectatorClientMod;
import com.hpfxd.spectatorplus.fabric.client.sync.ClientSyncController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Unique;

public final class SpecUtil {
    private SpecUtil() {
    }

    public static AbstractClientPlayer getCameraPlayer(Minecraft minecraft) {
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR
                && minecraft.getCameraEntity() instanceof final AbstractClientPlayer spectated
                && spectated != minecraft.player) {
            return spectated;
        }
        return null;
    }
}
