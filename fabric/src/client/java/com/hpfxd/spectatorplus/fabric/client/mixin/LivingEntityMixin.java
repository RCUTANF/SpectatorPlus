package com.hpfxd.spectatorplus.fabric.client.mixin;

import com.hpfxd.spectatorplus.fabric.client.util.SpecUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("TAIL"))
    private void spectatorplus$resetSpectatedAttackStrength(InteractionHand hand, CallbackInfo ci) {
        final AbstractClientPlayer spectated = SpecUtil.getCameraPlayer(Minecraft.getInstance());
        if ((Object) this == spectated) {
            if (!this.isBreakingBlock() && !this.isLookingAtBlock()) {
                spectated.resetAttackStrengthTicker();
            }
        }
    }

    // @Inject(method = "hasEffect(Lnet/minecraft/core/Holder;)Z", at = @At("HEAD"), cancellable = true)
    // private void overrideNauseaEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
    //     System.out.println("[SpectatorPlus] hasEffect called. Entity type: " + this.getClass().getName() + ", effect: " +
    //         effect.value().getDescriptionId() + " player: " + this.getName().getString());
    //     if ((Entity) this instanceof Player) {
    //         var syncData = com.hpfxd.spectatorplus.fabric.client.sync.ClientSyncController.syncData;
    //         if (syncData != null && syncData.effects != null) {
    //             boolean found = syncData.effects.stream().anyMatch(synced -> {
    //                 java.util.Optional<Holder.Reference<MobEffect>> optHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.ResourceLocation.tryParse(synced.effectKey));
    //                 return optHolder.isPresent() && optHolder.get().value().equals(effect.value());
    //             });
    //             if (found) {
    //                 System.out.println("[SpectatorPlus] hasEffect: Synced effect found for player: " + this.getName().getString());
    //                 cir.setReturnValue(true);
    //             }
    //         }
    //     }
    // }

    // @Inject(method = "getActiveEffects", at = @At("HEAD"), cancellable = true)
    // private void overrideActiveEffects(CallbackInfoReturnable<Collection<MobEffectInstance>> cir) {
    //     System.out.println("[SpectatorPlus] getActiveEffects called. Entity type: " + this.getClass().getName());
    //     if ((Entity) this instanceof Player) {
    //         System.out.println("[SpectatorPlus] Injecting effects from syncdata for player: " + this.getName().getString());
    //         var syncData = com.hpfxd.spectatorplus.fabric.client.sync.ClientSyncController.syncData;
    //         if (syncData != null && syncData.effects != null) {
    //             java.util.List<MobEffectInstance> instances = new java.util.ArrayList<>();
    //             for (com.hpfxd.spectatorplus.fabric.sync.SyncedEffect synced : syncData.effects) {
    //                 java.util.Optional<net.minecraft.core.Holder.Reference<MobEffect>> optHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.ResourceLocation.tryParse(synced.effectKey));
    //                 if (optHolder.isPresent()) {
    //                     MobEffect effect = optHolder.get().value();
    //                     net.minecraft.core.Holder<MobEffect> holder = net.minecraft.core.Holder.direct(effect);
    //                     MobEffectInstance instance = new MobEffectInstance(holder, synced.duration, synced.amplifier);
    //                     instances.add(instance);
    //                 }
    //             }
    //             System.out.println("[SpectatorPlus] Injecting synced effects for player: " + this.getName().getString() + " -> " + instances);
    //             cir.setReturnValue(instances);
    //         }
    //     }
    // }

    @Unique
    private boolean isBreakingBlock() {
        return ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getDestroyingBlocks().containsKey(this.getId());
    }

    @Unique
    private boolean isLookingAtBlock() {
        if (!(((Object) this) instanceof final Player player)) {
            return false;
        }

        return ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).invokePick(this, player.blockInteractionRange(), player.entityInteractionRange(), 1F).getType() == HitResult.Type.BLOCK;
    }
}
