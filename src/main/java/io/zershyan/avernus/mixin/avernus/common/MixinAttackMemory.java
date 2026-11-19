package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Zoglin.class, Warden.class})
public abstract class MixinAttackMemory {
    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void avernus$guardTargetMemory(LivingEntity target, CallbackInfo ci) {
        if (RealmTraitEffects.forbidsTarget((Mob) (Object) this, target)) ci.cancel();
    }
}
