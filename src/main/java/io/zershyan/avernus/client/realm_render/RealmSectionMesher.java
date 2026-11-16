package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.zershyan.avernus.realm.RealmTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;

import java.util.List;

/**
 * 在区块网格编译线程上为 section 追加领域几何：H−1 层的境相立方体/虚拟水，以及圆柱边缘被原版邻面剔除省略的真实侧面。
 * 显示与否全部交给领域区块 Shader 按波面裁剪，因此网格不随波纹半径重建。
 */
public final class RealmSectionMesher {
    private static final float WATER_TOP = 14.0F / 16.0F;
    /** 弱水底面略高于格界，使 Shader 将其归入虚拟层所在格。 */
    private static final float FLOOR_LIFT = 0.02F;

    /** 主线程捕获的构建任务。 */
    public record Job(RealmMeshKey key, TextureAtlasSprite solid, TextureAtlasSprite water, int waterColor) {
    }

    private RealmSectionMesher() {
    }

    public static void build(AddSectionGeometryEvent.SectionRenderingContext context, BlockPos origin, List<Job> jobs) {
        BlockAndTintGetter region = context.getRegion();
        int originSection = SectionPos.blockToSectionCoord(origin.getY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Job job : jobs) {
            RealmMeshKey key = job.key();
            boolean layerHere = SectionPos.blockToSectionCoord(key.layerY()) == originSection;
            int boundaryFrom = Math.max(key.layerY(), origin.getY());
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    if (layerHere && key.inDisc(x, z)) layer(context, region, job, pos.set(x, key.layerY(), z), origin);
                    if (!key.needsBoundary(x, z)) continue;
                    for (int y = boundaryFrom; y < origin.getY() + 16; y++) boundary(context, region, pos.set(x, y, z), origin);
                }
            }
        }
    }

    private static RealmTerrain.Kind kind(RealmMeshKey key, BlockAndTintGetter region, BlockPos pos) {
        return RealmTerrain.classify(key.height(), key.weakWater(), key.boundless(), region.getBlockState(pos), pos.getY());
    }

    // region 虚拟层

    private static void layer(AddSectionGeometryEvent.SectionRenderingContext context, BlockAndTintGetter region,
                              Job job, BlockPos pos, BlockPos origin) {
        RealmMeshKey key = job.key();
        RealmTerrain.Kind kind = kind(key, region, pos);
        if (kind != RealmTerrain.Kind.SOLID && kind != RealmTerrain.Kind.WATER) return;
        boolean water = kind == RealmTerrain.Kind.WATER;
        VertexConsumer consumer = consumer(context, water);
        PoseStack.Pose pose = context.getPoseStack().last();
        float rx = pos.getX() - origin.getX(), ry = pos.getY() - origin.getY(), rz = pos.getZ() - origin.getZ();
        float top = water ? WATER_TOP : 1.0F;
        TextureAtlasSprite sprite = water ? job.water() : job.solid();
        int color = water ? job.waterColor() : 0xFFFFFF;
        if (water && kind(key, region, pos.below()) == RealmTerrain.Kind.SOLID && !region.getBlockState(pos).isAir()) {
            face(consumer(context, false), pose, region, Direction.UP, rx, ry - 1.0F + FLOOR_LIFT, rz, 1.0F, job.solid(), 0xFFFFFF,
                    layerLight(region, pos), key.label());
        }
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            BlockPos side = pos.relative(dir);
            if (dir.getAxis().isHorizontal() && key.inDisc(side.getX(), side.getZ())) {
                RealmTerrain.Kind neighbor = kind(key, region, side);
                if (neighbor == RealmTerrain.Kind.SOLID || neighbor == kind) continue;
            }
            int light = layerLight(region, side);
            face(consumer, pose, region, dir, rx, ry, rz, top, sprite, color, light, key.label());
            if (water && dir == Direction.UP) face(consumer, pose, region, Direction.DOWN, rx, ry + top, rz, 0.0F, sprite, color, light, key.label());
        }
    }

    /** 真实天光可能被已消隐的高处遮挡，虚拟层按露天处理，仅保留方块光。 */
    private static int layerLight(BlockAndTintGetter region, BlockPos pos) {
        return LightTexture.pack(LightTexture.block(LevelRenderer.getLightColor(region, pos)), 15);
    }

    private static VertexConsumer consumer(AddSectionGeometryEvent.SectionRenderingContext context, boolean water) {
        return context.getOrCreateChunkBuffer(water ? RenderType.translucent() : RenderType.solid());
    }

    /** 绘制单位立方体（顶高 top）在 dir 方向的面；DOWN 面位于 ry 平面。 */
    private static void face(VertexConsumer consumer, PoseStack.Pose pose, BlockAndTintGetter region, Direction dir,
                             float rx, float ry, float rz, float top, TextureAtlasSprite sprite, int color, int light, int marker) {
        float shade = region.getShade(dir, true);
        int r = (int) ((color >> 16 & 0xFF) * shade), g = (int) ((color >> 8 & 0xFF) * shade), b = (int) ((color & 0xFF) * shade);
        float[][] corners = corners(dir, top);
        for (float[] c : corners) {
            float u = dir.getAxis() == Direction.Axis.X ? c[2] : c[0];
            float v = dir.getAxis() == Direction.Axis.Y ? c[2] : 1.0F - c[1];
            consumer.addVertex(pose, rx + c[0], ry + c[1], rz + c[2])
                    .setColor(r, g, b, marker)
                    .setUv(sprite.getU(u), sprite.getV(v))
                    .setLight(light)
                    .setNormal(pose, dir.getStepX(), dir.getStepY(), dir.getStepZ());
        }
    }

    /** 以面外侧观察逆时针排列的四角。 */
    private static float[][] corners(Direction dir, float top) {
        return switch (dir) {
            case DOWN -> new float[][]{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}};
            case UP -> new float[][]{{0, top, 0}, {0, top, 1}, {1, top, 1}, {1, top, 0}};
            case NORTH -> new float[][]{{1, top, 0}, {1, 0, 0}, {0, 0, 0}, {0, top, 0}};
            case SOUTH -> new float[][]{{0, top, 1}, {0, 0, 1}, {1, 0, 1}, {1, top, 1}};
            case WEST -> new float[][]{{0, top, 0}, {0, 0, 0}, {0, 0, 1}, {0, top, 1}};
            case EAST -> new float[][]{{1, top, 1}, {1, 0, 1}, {1, 0, 0}, {1, top, 0}};
        };
    }

    // endregion

    // region 边界补面

    /** 补回该方块被水平邻块剔除的面；Shader 只在本格可见而邻格已消隐时显示。 */
    private static void boundary(AddSectionGeometryEvent.SectionRenderingContext context, BlockAndTintGetter region,
                                 BlockPos pos, BlockPos origin) {
        BlockState state = region.getBlockState(pos);
        if (state.getRenderShape() != RenderShape.MODEL) return;
        BlockPos immutable = pos.immutable();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        ModelData data = model.getModelData(region, immutable, state, region.getModelData(immutable));
        long seed = state.getSeed(immutable);
        RandomSource random = RandomSource.create(seed);
        QuadLighter lighter = null;
        PoseStack poseStack = context.getPoseStack();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = immutable.relative(dir);
            if (Block.shouldRenderFace(state, region, immutable, dir, side)) continue;
            for (RenderType type : model.getRenderTypes(state, random, data)) {
                random.setSeed(seed);
                var quads = model.getQuads(state, dir, random, data, type);
                if (quads.isEmpty()) continue;
                if (lighter == null) {
                    lighter = context.getQuadLighter(true);
                    lighter.setup(region, immutable, state);
                }
                VertexConsumer consumer = new MarkerVertexConsumer(context.getOrCreateChunkBuffer(type), MarkerVertexConsumer.BOUNDARY);
                poseStack.pushPose();
                poseStack.translate(immutable.getX() - origin.getX(), immutable.getY() - origin.getY(), immutable.getZ() - origin.getZ());
                for (var quad : quads) lighter.process(consumer, poseStack.last(), quad, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
        if (lighter != null) lighter.reset();
    }

    // endregion
}
