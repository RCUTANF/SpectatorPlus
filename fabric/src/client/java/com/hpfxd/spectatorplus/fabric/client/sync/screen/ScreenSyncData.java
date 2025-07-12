package com.hpfxd.spectatorplus.fabric.client.sync.screen;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class ScreenSyncData {
    /**
     * inventoryItems layout:
     *   0-35: main inventory (including hotbar)
     *   36-39: armor (helmet, chestplate, leggings, boots)
     */
    public NonNullList<ItemStack> inventoryItems;

    public boolean isSurvivalInventory;
    public boolean isClientRequested;
    public boolean hasDummySlots;

    public ItemStack cursorItem = ItemStack.EMPTY;
    public int cursorItemSlot = -1;
}
