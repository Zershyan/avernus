package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPresenceTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** 在实际实体 tick 入口追踪，避免子类覆写 tick 后遗漏恢复检查。 */
@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {
    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void avernus$tickEntity(Entity entity, Operation<Void> original) {
        avernus$trackTick(entity, original);
    }

    @WrapOperation(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void avernus$tickPassenger(Entity entity, Operation<Void> original) {
        avernus$trackTick(entity, original);
    }

    @Unique
    private static void avernus$trackTick(Entity entity, Operation<Void> original) {
        boolean track = entity instanceof LivingEntity || entity instanceof Boat || entity instanceof AbstractMinecart;
        if (track) RealmPresenceTracker.tick(entity);
        original.call(entity);
        if (track) RealmPresenceTracker.tick(entity);
    }
}
