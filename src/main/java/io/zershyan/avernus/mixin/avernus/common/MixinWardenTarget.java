package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 监守者直接写攻击目标记忆，需在此拦住幻化境印保护的目标。 */
@Mixin(Warden.class)
public abstract class MixinWardenTarget {
    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void avernus$guardTargetMemory(LivingEntity target, CallbackInfo ci) {
        if (RealmTraitEffects.forbidsTarget((Warden) (Object) this, target)) ci.cancel();
    }
}
