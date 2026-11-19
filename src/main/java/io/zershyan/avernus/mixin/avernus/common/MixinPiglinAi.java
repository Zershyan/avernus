package io.zershyan.avernus.mixin.avernus.common;

import io.zershyan.avernus.realm.RealmTraitEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinAi.class)
public abstract class MixinPiglinAi {
    @Inject(method = "setAngerTarget", at = @At("HEAD"), cancellable = true)
    private static void avernus$guardAnger(AbstractPiglin piglin, LivingEntity target, CallbackInfo ci) {
        if (RealmTraitEffects.forbidsTarget(piglin, target)) ci.cancel();
    }
}
