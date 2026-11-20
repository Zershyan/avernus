package io.zershyan.avernus.mixin.avernus.client;

import io.zershyan.avernus.client.realm_render.RealmEnclosure;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 身处领域时不绘制境外实体。 */
@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void avernus$enclose(E entity, Frustum frustum, double camX, double camY, double camZ,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!RealmEnclosure.keeps(entity)) cir.setReturnValue(false);
    }
}
