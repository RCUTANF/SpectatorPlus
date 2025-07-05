package com.hpfxd.spectatorplus.paper.sync.packet;

import com.google.common.io.ByteArrayDataOutput;
import com.hpfxd.spectatorplus.paper.sync.ClientboundSyncPacket;
import com.hpfxd.spectatorplus.paper.util.SerializationUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

public record ClientboundEffectsSyncPacket(
        UUID playerId,
        Collection<PotionEffect> effects
) implements ClientboundSyncPacket {
    public static final NamespacedKey ID = new NamespacedKey("spectatorplus", "effects_sync");

    @Override
    public void write(ByteArrayDataOutput buf) {
        SerializationUtil.writeUuid(buf, this.playerId);
        buf.writeInt(this.effects.size());
        for (PotionEffect effect : this.effects) {
            writePotionEffect(buf, effect);
        }
    }
    
    private void writePotionEffect(ByteArrayDataOutput buf, PotionEffect effect) {
        // Write effect type key
        buf.writeUTF(effect.getType().getKey().toString());
        
        // Write effect properties
        buf.writeInt(effect.getAmplifier());
        buf.writeInt(effect.getDuration());
        buf.writeBoolean(effect.isAmbient());
        buf.writeBoolean(effect.hasParticles());
        buf.writeBoolean(effect.hasIcon());
    }

    @Override
    public NamespacedKey channel() {
        return ID;
    }
}
