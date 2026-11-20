package io.zershyan.avernus.mixin.avernus.client;

import io.zershyan.avernus.client.realm_render.RealmLighting;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 实体与第一人称手部的天空光：境面以上不受被消隐的山体遮挡。 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "getSkyLightLevel", at = @At("RETURN"), cancellable = true)
    private void avernus$openSky(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(RealmLighting.skyLight(entity.level(), pos, cir.getReturnValueI()));
    }
}
