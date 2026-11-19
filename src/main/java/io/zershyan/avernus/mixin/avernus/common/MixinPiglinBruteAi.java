package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinBruteAi.class)
public abstract class MixinPiglinBruteAi {
    @Inject(method = "setAngerTarget", at = @At("HEAD"), cancellable = true)
    private static void avernus$guardAnger(PiglinBrute piglin, LivingEntity target, CallbackInfo ci) {
        if (RealmTraitEffects.forbidsTarget(piglin, target)) ci.cancel();
    }
}
