package com.hpfxd.spectatorplus.fabric.sync.handler;

import com.hpfxd.spectatorplus.fabric.sync.ServerSyncController;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundInventorySyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ClientboundScreenSyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ServerboundOpenedInventorySyncPacket;
import com.hpfxd.spectatorplus.fabric.sync.packet.ServerboundRequestInventoryOpenPacket;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenSyncHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenSyncHandler.class);

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(ServerboundRequestInventoryOpenPacket.TYPE, ScreenSyncHandler::handle);
        ServerPlayNetworking.registerGlobalReceiver(ServerboundOpenedInventorySyncPacket.TYPE, ScreenSyncHandler::handle);
    }

    private static void handle(ServerboundRequestInventoryOpenPacket packet, ServerPlayNetworking.Context ctx) {
        try {
            final var server = ctx.server();
            final var player = ctx.player();
            final var target = server.getPlayerList().getPlayer(packet.playerId());

            if (target != null) {
                onRequestOpen(player, target);
            } else {
                LOGGER.warn("Player {} requested to view inventory of non-existent player {}",
                    player.getGameProfile().name(), packet.playerId());
            }
        } catch (Exception e) {
            LOGGER.error("Error handling inventory open request", e);
        }
    }

    private static void handle(ServerboundOpenedInventorySyncPacket packet, ServerPlayNetworking.Context ctx) {
        try {
            final var player = ctx.player();
            onPlayerOpenInventory(player);
        } catch (Exception e) {
            LOGGER.error("Error handling player inventory open", e);
        }
    }

    private static void onRequestOpen(ServerPlayer spectator, ServerPlayer target) {
        // Check if spectator has permission to view inventory
        if (canSyncInventory(spectator)) {
            openPlayerInventory(spectator, target);
        }
    }

    /**
     * Called when a player opens their inventory or any container screen.
     * This method syncs the appropriate screen to all spectators who are viewing this player.
     *
     * @param target The player who opened their inventory/container
     */
    private static void onPlayerOpenInventory(ServerPlayer target) {
        try {
            // Get all spectators who are currently viewing this player
            var spectators = ServerSyncController.getSpectators(target);
            if (spectators.isEmpty()) {
                return; // No spectators, no need to sync
            }

            for (ServerPlayer spectator : spectators) {
                if (!canSyncInventory(spectator)) {
                    continue;
                }

                // Determine what type of screen the target player has open
                if (target.containerMenu != target.inventoryMenu) {
                    // Player has a container open (chest, crafting table, furnace, etc.)
                    LOGGER.debug("Syncing container screen for spectator {} viewing {}",
                        spectator.getGameProfile().name(), target.getGameProfile().name());
                    syncContainerScreen(spectator, target);
                } else {
                    // Player has their regular inventory open (survival/creative inventory)
                    LOGGER.debug("Syncing player inventory screen for spectator {} viewing {}",
                        spectator.getGameProfile().name(), target.getGameProfile().name());
                    syncPlayerInventoryScreen(spectator, target);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error syncing player inventory screen for {}", target.getGameProfile().name(), e);
        }
    }

    private static void openPlayerInventory(ServerPlayer spectator, ServerPlayer target) {
        // Send screen sync packet to indicate survival inventory
        int flags = 1; // Survival inventory flag
        flags |= (1 << 1); // Client requested flag
        ServerSyncController.sendPacket(spectator, new ClientboundScreenSyncPacket(target.getUUID(), flags));

        // Send full inventory data
        InventorySyncHandler.sendPacket(spectator, target);
    }

    private static boolean canSyncInventory(ServerPlayer player) {
        return Permissions.check(player, "spectatorplus.sync.inventory", true);
    }

    public static void updatePlayerInventory(ServerPlayer player, ItemStack[] inventorySendSlots) {
        try {
            // Only send to spectators who have permission and are actually spectating this player
            for (ServerPlayer spectator : ServerSyncController.getSpectators(player)) {
                if (canSyncInventory(spectator)) {
                    ServerSyncController.sendPacket(spectator,
                        new ClientboundInventorySyncPacket(player.getUUID(), inventorySendSlots));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error updating inventory for player {}", player.getGameProfile().name(), e);
        }
    }

    /**
     * Syncs a container screen (chest, crafting table, furnace, etc.) to a spectator.
     * This is used when the target player has opened a container that isn't their inventory.
     *
     * @param spectator The spectator to sync the screen to
     * @param target The player whose container screen should be synced
     */
    private static void syncContainerScreen(ServerPlayer spectator, ServerPlayer target) {
        // Send screen sync packet for container screen
        int flags = 0; // Container screen (not survival inventory)
        flags |= (1 << 2); // Has dummy slots flag for container screens
        ServerSyncController.sendPacket(spectator, new ClientboundScreenSyncPacket(target.getUUID(), flags));

        // Send the target's inventory data to the spectator
        InventorySyncHandler.sendPacket(spectator, target);

        // For container screens (chests, crafting tables, etc.), we can send ClientboundOpenScreenPacket
        // because container menus have proper MenuType implementations
        try {
            spectator.connection.send(new ClientboundOpenScreenPacket(
                    target.containerMenu.containerId,
                    target.containerMenu.getType(),
                    target.getDisplayName()
            ));
        } catch (Exception e) {
            LOGGER.warn("Could not sync container screen type for {} to spectator {}: {}",
                target.getGameProfile().name(), spectator.getGameProfile().name(), e.getMessage());
            // Continue without the screen packet - the client will handle it through sync packets
        }
    }

    /**
     * Syncs a player's regular inventory screen (survival/creative inventory) to a spectator.
     * This is used when the target player has opened their own inventory.
     *
     * @param spectator The spectator to sync the screen to
     * @param target The player whose inventory screen should be synced
     */
    private static void syncPlayerInventoryScreen(ServerPlayer spectator, ServerPlayer target) {
        // Send screen sync packet for player inventory (survival inventory)
        int flags = 1; // Survival inventory flag
        ServerSyncController.sendPacket(spectator, new ClientboundScreenSyncPacket(target.getUUID(), flags));

        // Send the target's inventory data to the spectator
        InventorySyncHandler.sendPacket(spectator, target);

        // Note: The client will handle opening the synced inventory screen based on the packets sent above.
        // No need to call openMenu() here as the client-side ScreenSyncController will process the
        // ClientboundScreenSyncPacket and open the appropriate synced screen automatically.
    }
}
