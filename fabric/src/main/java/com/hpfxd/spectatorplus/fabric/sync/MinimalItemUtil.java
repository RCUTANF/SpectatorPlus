package com.hpfxd.spectatorplus.fabric.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class MinimalItemUtil {
    public static ItemStack[] readMinimalItems(FriendlyByteBuf buf) {
        int len = buf.readInt();
        ItemStack[] items = new ItemStack[len];
        for (int i = 0; i < len; i++) {
            String id = buf.readUtf();
            int count = buf.readInt();
            if (id.isEmpty() || count <= 0) {
                items[i] = ItemStack.EMPTY;
            } else {
                var resourceLocation = ResourceLocation.tryParse(id);
                Holder<Item> item = resourceLocation != null ? BuiltInRegistries.ITEM.get(resourceLocation).orElse(null) : null;
                items[i] = item != null ? new ItemStack(item, count) : ItemStack.EMPTY;
            }
        }
        return items;
    }
    public static void writeMinimalItems(FriendlyByteBuf buf, ItemStack[] items) {
        buf.writeInt(items.length);
        for (ItemStack item : items) {
            if (item == null || item.isEmpty()) {
                buf.writeUtf("");
                buf.writeInt(0);
            } else {
                buf.writeUtf(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
                buf.writeInt(item.getCount());
            }
        }
    }
}