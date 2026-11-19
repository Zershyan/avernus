package io.zershyan.avernus.mixin.avernus.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.zershyan.avernus.realm.RealmPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** 实体方块碰撞读取有效地形：消隐高处、规整 H−1 层、为下方补保底碰撞。 */
@Mixin(BlockCollisions.class)
public abstract class MixinBlockCollisions {
    @Shadow
    @Final
    private CollisionGetter collisionGetter;
    @Shadow
    @Final
    private CollisionContext context;

    @WrapOperation(method = "computeNext", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState avernus$effective(BlockGetter getter, BlockPos pos, Operation<BlockState> original) {
        return RealmPhysics.collision(RealmPhysics.levelOf(collisionGetter), RealmPhysics.subjectOf(context), pos, original.call(getter, pos));
    }
}
