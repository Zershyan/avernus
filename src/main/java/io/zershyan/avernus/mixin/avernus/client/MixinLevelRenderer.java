package io.zershyan.avernus.mixin.avernus.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmLighting;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusShaders;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinRenderChunkInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
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

    /** Forge 1.20.1 的 AFTER_LEVEL 事件传入投影矩阵栈，此处使用实际的世界视图矩阵。 */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void avernus$renderRealmEffects(PoseStack view, float partialTick, long finishTimeNano, boolean renderBlockOutline,
                                           Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection,
                                           CallbackInfo ci) {
        RealmEnvironment.render(camera, view.last().pose(), projection, partialTick, ((LevelRenderer) (Object) this).getTicks());
    }

    @Inject(method = "compileChunks", at = @At("HEAD"))
    private void avernus$prepare(Camera camera, CallbackInfo ci) {
        if (level == null || viewArea == null) return;
        Frustum frustum = ((LevelRenderer) (Object) this).getFrustum();
        if (frustum == null) return;
        List<ChunkRenderDispatcher.RenderChunk> vanilla = new ArrayList<>();
        if (!RealmSets.of(level).isEmpty()) {
            for (Object info : renderChunksInFrustum) vanilla.add(((IMixinRenderChunkInfo) info).avernus$chunk());
        }
        RealmRenderState.prepare(level, viewArea, camera, frustum, vanilla);
    }

    @Inject(method = "compileChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadAllPendingUploads()V"))
    private void avernus$compileSupplement(Camera camera, CallbackInfo ci,
                                           @Local List<ChunkRenderDispatcher.RenderChunk> pending,
                                           @Local LevelLightEngine lightEngine) {
        for (var section : RealmRenderState.supplement()) {
            if (section.isDirty() && lightEngine.lightOnInSection(SectionPos.of(section.getOrigin()))) pending.add(section);
        }
    }

    @ModifyExpressionValue(method = "renderChunkLayer", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;getShader()Lnet/minecraft/client/renderer/ShaderInstance;"))
    private ShaderInstance avernus$realmShader(ShaderInstance vanilla, @Local(argsOnly = true) RenderType layer) {
        ShaderInstance shader = RealmRenderState.active() ? AvernusShaders.realmChunk(layer) : null;
        if (shader == null) return vanilla;
        RealmRenderState.setupShader(shader);
        return shader;
    }

    @WrapOperation(method = "renderChunkLayer", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;listIterator(I)Lit/unimi/dsi/fastutil/objects/ObjectListIterator;", remap = false))
    private ObjectListIterator<?> avernus$sectionIterator(ObjectArrayList<?> sections, int start,
                                                         Operation<ObjectListIterator<?>> original,
                                                         @Local(argsOnly = true) RenderType layer) {
        if (RealmRenderState.active() && AvernusShaders.realmChunk(layer) != null) return ObjectIterators.EMPTY_ITERATOR;
        return original.call(sections, start);
    }

    /** 沿用原版的着色器初始化，统一绘制原版分区与补充分区。 */
    @Inject(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"))
    private void avernus$drawRealmLayer(RenderType type, PoseStack poseStack, double camX, double camY, double camZ,
                                       Matrix4f projection, CallbackInfo ci, @Local ShaderInstance shader) {
        if (RealmRenderState.active() && shader == AvernusShaders.realmChunk(type)) {
            RealmRenderState.drawLayer(type, shader, camX, camY, camZ);
        }
    }
}
