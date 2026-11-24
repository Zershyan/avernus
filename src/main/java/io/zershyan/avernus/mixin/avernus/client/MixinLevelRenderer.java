package io.zershyan.avernus.mixin.avernus.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmLighting;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusShaders;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinRenderChunkInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow private @Nullable ClientLevel level;
    @Shadow private @Nullable ViewArea viewArea;
    @Shadow @Final private ObjectArrayList<?> renderChunksInFrustum;

    /** 粒子与方块实体读取的光照：境面以上天空光取满，区块网格线程传入的不是 ClientLevel，不受影响。 */
    @WrapOperation(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"))
    private static int avernus$openSky(BlockAndTintGetter getter, LightLayer layer, BlockPos pos, Operation<Integer> original) {
        return RealmLighting.skyLight(getter, pos, original.call(getter, layer, pos));
    }

    @Inject(method = "allChanged", at = @At("HEAD"))
    private void avernus$resetOnRebuild(CallbackInfo ci) {
        RealmRenderState.reset();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void avernus$resetOnWorldChange(@Nullable ClientLevel next, CallbackInfo ci) {
        ClientLevel previous = level;
        if (next == previous) return;
        if (previous != null) RealmSets.of(previous).endUpdates();
        RealmRenderState.reset();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void avernus$hideRealmClouds(CallbackInfo ci) {
        if (RealmEnvironment.hidesClouds()) ci.cancel();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void avernus$invalidateMesh(int x, int y, int z, boolean urgent, CallbackInfo ci) {
        RealmRenderState.dirty(level, x, y, z);
    }

    /** 每帧第一层绘制前准备可见分区与网格。 */
    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void avernus$prepare(RenderType type, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projection,
                                 CallbackInfo ci) {
        if (type != RenderType.solid() || level == null || viewArea == null) return;
        Frustum frustum = ((LevelRenderer) (Object) this).getFrustum();
        List<ChunkRenderDispatcher.RenderChunk> vanilla = new ArrayList<>(renderChunksInFrustum.size());
        for (Object info : renderChunksInFrustum) vanilla.add(((IMixinRenderChunkInfo) info).avernus$chunk());
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        RealmRenderState.prepare(level, viewArea, camera, frustum, vanilla);
    }

    @ModifyExpressionValue(method = "renderChunkLayer", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;getShader()Lnet/minecraft/client/renderer/ShaderInstance;"))
    private ShaderInstance avernus$realmShader(ShaderInstance vanilla, @Local(argsOnly = true) RenderType layer) {
        ShaderInstance shader = RealmRenderState.active() ? AvernusShaders.realmChunk(layer) : null;
        if (shader == null) return vanilla;
        RealmRenderState.setupShader(shader);
        return shader;
    }

    @WrapOperation(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"))
    private void avernus$drawAdditional(VertexBuffer vertices, Operation<Void> original, @Local(argsOnly = true) RenderType layer,
                                        @Local ChunkRenderDispatcher.RenderChunk section, @Local ShaderInstance shader) {
        if (RealmRenderState.active() && shader == AvernusShaders.realmChunk(layer)) {
            RealmRenderState.draw(section, layer, shader, () -> original.call(vertices));
        } else {
            original.call(vertices);
        }
    }

    /** 原版没有几何、因而不在可见列表里的补充分区，在着色器清理前单独绘制。 */
    @Inject(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"))
    private void avernus$drawSupplement(RenderType type, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projection,
                                        CallbackInfo ci) {
        if (!RealmRenderState.active()) return;
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != AvernusShaders.realmChunk(type)) return;
        var offset = shader.CHUNK_OFFSET;
        for (ChunkRenderDispatcher.RenderChunk section : RealmRenderState.supplement()) {
            BlockPos origin = section.getOrigin();
            if (offset != null) {
                offset.set((float) ((double) origin.getX() - camX), (float) ((double) origin.getY() - camY), (float) ((double) origin.getZ() - camZ));
                offset.upload();
            }
            RealmRenderState.draw(section, type, shader, () -> {
            });
        }
        if (offset != null) {
            offset.set(0.0F, 0.0F, 0.0F);
            offset.upload();
        }
    }
}
