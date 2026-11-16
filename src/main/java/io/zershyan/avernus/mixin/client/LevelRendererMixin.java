package io.zershyan.avernus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.registry.AvernusShaders;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 存在领域网格时，把区块图层 Shader 换成带领域覆盖裁剪的对应版本。 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @ModifyExpressionValue(method = "renderSectionLayer",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getShader()Lnet/minecraft/client/renderer/ShaderInstance;"))
    private ShaderInstance avernus$realmShader(ShaderInstance original, RenderType renderType) {
        if (!RealmRenderState.active()) return original;
        ShaderInstance realm = AvernusShaders.realmChunk(renderType);
        if (realm == null) return original;
        RenderSystem.setShader(() -> realm);
        RealmRenderState.prepareFrame(realm);
        return realm;
    }
}
