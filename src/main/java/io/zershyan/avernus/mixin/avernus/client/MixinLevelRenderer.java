package io.zershyan.avernus.mixin.avernus.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.zershyan.avernus.client.realm_render.RealmEnvironment;
import io.zershyan.avernus.client.realm_render.RealmRenderState;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusShaders;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.CompiledSection;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow private @Nullable ClientLevel level;
    @Shadow private @Nullable ViewArea viewArea;
    @Shadow @Final private ObjectArrayList<RenderSection> visibleSections;

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

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;compileSections(Lnet/minecraft/client/Camera;)V"))
    private void avernus$compileSupplement(LevelRenderer renderer, Camera camera, Operation<Void> original) {
        ClientLevel current = level;
        ViewArea view = viewArea;
        var frustum = renderer.getFrustum();
        if (current == null || view == null || frustum == null) {
            original.call(renderer, camera);
            return;
        }
        RealmRenderState.prepare(current, view, camera, frustum, visibleSections);
        var added = RealmRenderState.supplement();
        if (added.isEmpty()) {
            original.call(renderer, camera);
            return;
        }
        int vanillaSize = visibleSections.size();
        visibleSections.addAll(added);
        try {
            original.call(renderer, camera);
        } finally {
            visibleSections.removeElements(vanillaSize, visibleSections.size());
        }
    }

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;listIterator(I)Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<RenderSection> avernus$sectionIterator(ObjectArrayList<RenderSection> sections, int start,
                                                                    Operation<ObjectListIterator<RenderSection>> original,
                                                                    @Local(argsOnly = true) RenderType layer) {
        return RealmRenderState.active() ? RealmRenderState.iterator(layer) : original.call(sections, start);
    }

    @ModifyExpressionValue(method = "renderSectionLayer", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;getShader()Lnet/minecraft/client/renderer/ShaderInstance;"))
    private ShaderInstance avernus$realmShader(ShaderInstance vanilla, RenderType layer) {
        ShaderInstance shader = RealmRenderState.active() ? AvernusShaders.realmChunk(layer) : null;
        if (shader == null) return vanilla;
        RealmRenderState.setupShader(shader);
        return shader;
    }

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$CompiledSection;isEmpty(Lnet/minecraft/client/renderer/RenderType;)Z"))
    private boolean avernus$hasGeometry(CompiledSection compiled, RenderType layer, Operation<Boolean> original,
                                       @Local RenderSection section, @Local ShaderInstance shader) {
        return original.call(compiled, layer) && !(RealmRenderState.active() && shader == AvernusShaders.realmChunk(layer)
                && RealmRenderState.hasAdditional(section, layer));
    }

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"))
    private void avernus$drawAdditional(VertexBuffer vertices, Operation<Void> original, @Local RenderSection section,
                                         @Local ShaderInstance shader, @Local(argsOnly = true) RenderType layer) {
        if (RealmRenderState.active() && shader == AvernusShaders.realmChunk(layer)) {
            RealmRenderState.draw(section, layer, shader, () -> original.call(vertices));
        } else {
            original.call(vertices);
        }
    }
}
