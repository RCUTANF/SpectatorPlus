package com.hpfxd.spectatorplus.paper.util;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class MinimalItemUtil {
    public static void writeMinimalItems(ByteArrayDataOutput out, ItemStack[] items) {
        out.writeInt(items.length);
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                out.writeUTF("");
                out.writeInt(0);
            } else {
                out.writeUTF(item.getType().getKey().toString());
                out.writeInt(item.getAmount());
            }
        }
    }
}