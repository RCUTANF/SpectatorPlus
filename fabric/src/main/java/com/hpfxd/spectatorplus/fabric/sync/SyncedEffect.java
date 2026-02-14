package com.hpfxd.spectatorplus.fabric.sync;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class SyncedEffect {
    /**
     * Maximum number of effects allowed in a single sync packet.
     * Vanilla Minecraft has ~35 mob effects; 128 provides generous headroom
     * while preventing malicious packets from causing OutOfMemoryError.
     */
    private static final int MAX_EFFECTS = 128;

    /**
     * Maximum length for an effect key string (e.g. "minecraft:speed").
     * Resource identifiers should never exceed this length.
     */
    private static final int MAX_EFFECT_KEY_LENGTH = 256;

    public final String effectKey;
    public final int amplifier;
    public final int duration;

    public SyncedEffect(String effectKey, int amplifier, int duration) {
        this.effectKey = effectKey;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    public static SyncedEffect read(FriendlyByteBuf buf) {
        String effectKey = buf.readUtf(MAX_EFFECT_KEY_LENGTH);
        int amplifier = buf.readVarInt();
        int duration = buf.readVarInt();
        return new SyncedEffect(effectKey, amplifier, duration);
    }

    public static List<SyncedEffect> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_EFFECTS) {
            throw new DecoderException(
                    "Effect list size " + size + " is outside allowed range [0, " + MAX_EFFECTS + "]");
        }
        List<SyncedEffect> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(SyncedEffect.read(buf));
        }
        return list;
    }
}
