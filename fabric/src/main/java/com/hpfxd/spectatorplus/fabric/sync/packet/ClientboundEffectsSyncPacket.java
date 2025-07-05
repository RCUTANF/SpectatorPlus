package com.hpfxd.spectatorplus.fabric.sync.packet;

import com.hpfxd.spectatorplus.fabric.sync.ClientboundSyncPacket;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ClientboundEffectsSyncPacket(
        UUID playerId,
        Map<Holder<MobEffect>, MobEffectInstance> effects
) implements ClientboundSyncPacket {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundEffectsSyncPacket> STREAM_CODEC = CustomPacketPayload.codec(ClientboundEffectsSyncPacket::write, ClientboundEffectsSyncPacket::new);
    public static final CustomPacketPayload.Type<ClientboundEffectsSyncPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("spectatorplus:effects_sync"));
    private static final String PERMISSION = "spectatorplus.sync.effects";

    public static ClientboundEffectsSyncPacket initializing(ServerPlayer target) {
        Map<Holder<MobEffect>, MobEffectInstance> effects = new HashMap<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            effects.put(effect.getEffect(), effect);
        }
        return new ClientboundEffectsSyncPacket(target.getUUID(), effects);
    }

    public ClientboundEffectsSyncPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID(), readEffects(buf));
    }
    
    private static Map<Holder<MobEffect>, MobEffectInstance> readEffects(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<Holder<MobEffect>, MobEffectInstance> effects = new HashMap<>();
        for (int i = 0; i < size; i++) {
            MobEffectInstance effect = MobEffectInstance.STREAM_CODEC.decode(buf);
            effects.put(effect.getEffect(), effect);
        }
        return effects;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeVarInt(this.effects.size());
        for (MobEffectInstance effect : this.effects.values()) {
            MobEffectInstance.STREAM_CODEC.encode(buf, effect);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean canSend(ServerPlayer receiver) {
        return Permissions.check(receiver, PERMISSION, true);
    }
}