package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.hoglin.HoglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoglinAi.class)
public abstract class MixinHoglinAi {
    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private static void avernus$guardTargetMemory(Hoglin hoglin, LivingEntity target, CallbackInfo ci) {
        if (RealmTraitEffects.forbidsTarget(hoglin, target)) ci.cancel();
    }
}
