package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.zershyan.avernus.realm.RealmTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.lighting.QuadLighter;

import java.util.Map;

/** 只烘焙与波纹无关的网格；实际可见面在绘制时由相邻列决定。 */
final class RealmSectionMesher {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float WATER_TOP = RealmTerrain.WATER_FLUID.getOwnHeight();
    private static final int[][][] CORNERS = {
            {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
            {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
            {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
            {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
            {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
            {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}
    };

    record Material(TextureAtlasSprite solid, TextureAtlasSprite still, TextureAtlasSprite flow, int waterColor) { }

    private RealmSectionMesher() {
    }

    /** 境面层与其下的底板层：底板只在境面空洞或水面下露出顶面，侧面由着色器裁到领域外缘，遮住下方真实地形。 */
    static void buildSurface(RealmGeometryContext context, BlockPos origin,
                             RealmMeshKey style, Material material) {
        BlockAndTintGetter region = context.getRegion();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        boolean hasSurface = SectionPos.blockToSectionCoord(style.layerY()) == SectionPos.blockToSectionCoord(origin.getY());
        boolean hasFloor = !style.weakWater() && SectionPos.blockToSectionCoord(style.floorY()) == SectionPos.blockToSectionCoord(origin.getY());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                pos.set(origin.getX() + x, style.layerY(), origin.getZ() + z);
                RealmTerrain.Kind kind = style.kind(region.getBlockState(pos));
                if (hasSurface) surfaceBlock(context, region, pos, side, kind, x, z, style.layerY() - origin.getY(), material);
                if (hasFloor) floorBlock(context, region, pos.setY(style.floorY()), side, kind != RealmTerrain.Kind.SOLID, x, z,
                        style.floorY() - origin.getY(), material);
            }
        }
    }

    private static void surfaceBlock(RealmGeometryContext context, BlockAndTintGetter region, BlockPos pos,
                                     BlockPos.MutableBlockPos side, RealmTerrain.Kind kind, int x, int z, int ry, Material material) {
        if (kind != RealmTerrain.Kind.SOLID && kind != RealmTerrain.Kind.WATER) return;
        boolean water = kind == RealmTerrain.Kind.WATER;
        VertexConsumer buffer = context.getOrCreateChunkBuffer(water ? RenderType.translucent() : RenderType.solid());
        for (Direction dir : DIRECTIONS) {
            if (dir == Direction.DOWN) continue;
            side.setWithOffset(pos, dir);
            boolean hiddenByReal = dir.getAxis().isHorizontal() && region.getBlockState(side).isSolidRender(region, side);
            face(buffer, region, dir, x, ry, z, water ? WATER_TOP : 1.0F,
                    water ? (dir.getAxis().isHorizontal() ? material.flow : material.still) : material.solid, water ? material.waterColor : 0xFFFFFF,
                    light(region, side), hiddenByReal ? 0 : 255);
        }
        if (water) face(buffer, region, Direction.DOWN, x, ry + WATER_TOP, z, 0.0F, material.still, material.waterColor, light(region, pos), 255);
    }

    private static void floorBlock(RealmGeometryContext context, BlockAndTintGetter region, BlockPos pos,
                                   BlockPos.MutableBlockPos side, boolean exposed, int x, int z, int ry, Material material) {
        VertexConsumer buffer = context.getOrCreateChunkBuffer(RenderType.solid());
        for (Direction dir : DIRECTIONS) {
            if (dir == Direction.UP && !exposed) continue;
            side.setWithOffset(pos, dir);
            boolean hiddenByReal = dir != Direction.UP && region.getBlockState(side).isSolidRender(region, side);
            if (dir == Direction.DOWN && hiddenByReal) continue;
            face(buffer, region, dir, x, ry, z, 1.0F, material.solid, 0xFFFFFF, light(region, dir == Direction.UP ? side : pos),
                    hiddenByReal ? 0 : 255);
        }
    }

    private static int light(BlockAndTintGetter region, BlockPos pos) {
        return LightTexture.pack(LightTexture.block(LevelRenderer.getLightColor(region, pos)), 15);
    }

    private static void face(VertexConsumer buffer, BlockAndTintGetter region, Direction dir,
                             float x, float y, float z, float top, TextureAtlasSprite sprite,
                             int color, int light, int alpha) {
        float shade = region.getShade(dir, true);
        int r = (int) ((color >> 16 & 255) * shade);
        int g = (int) ((color >> 8 & 255) * shade);
        int b = (int) ((color & 255) * shade);
        for (int[] c : CORNERS[dir.ordinal()]) {
            float u = dir.getAxis() == Direction.Axis.X ? c[2] : c[0];
            float v = dir.getAxis() == Direction.Axis.Y ? c[2] : 1.0F - c[1] * top;
            buffer.vertex(x + c[0], y + c[1] * top, z + c[2]).color(r, g, b, alpha)
                    .uv(sprite.getU(u * 16.0), sprite.getV(v * 16.0)).uv2(light).normal(dir.getStepX(), dir.getStepY(), dir.getStepZ()).endVertex();
        }
    }

    private static void fluidBoundary(RealmGeometryContext context, BlockPos pos, BlockPos origin,
                                      BlockState state) {
        var region = context.getRegion();
        var fluid = state.getFluidState();
        var side = new BlockPos.MutableBlockPos();
        int hidden = 0;
        for (Direction dir : DIRECTIONS) {
            side.setWithOffset(pos, dir);
            BlockState neighbor = region.getBlockState(side);
            if (fluid.getType().isSame(neighbor.getFluidState().getType()) || neighbor.isSolidRender(region, side)) hidden |= 1 << dir.ordinal();
        }
        if (hidden == 0) return;
        var extensions = IClientFluidTypeExtensions.of(fluid);
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var still = atlas.apply(extensions.getStillTexture(fluid, region, pos));
        var flow = atlas.apply(extensions.getFlowingTexture(fluid, region, pos));
        int color = extensions.getTintColor(fluid, region, pos);
        var layer = ItemBlockRenderTypes.getRenderLayer(fluid);
        var buffer = context.getOrCreateChunkBuffer(layer);
        float top = fluid.getHeight(region, pos);
        for (Direction dir : DIRECTIONS) {
            if ((hidden & (1 << dir.ordinal())) == 0) continue;
            face(buffer, region, dir, pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(),
                    top, dir.getAxis().isHorizontal() ? flow : still, color, LevelRenderer.getLightColor(region, pos), color >>> 24);
        }
    }

    static void buildBoundary(RealmGeometryContext context, BlockPos origin, Map<BlockPos, ModelData> modelData) {
        var region = context.getRegion();
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var random = RandomSource.create();
        var side = new BlockPos.MutableBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(15, 15, 15))) {
            BlockState state = region.getBlockState(pos);
            if (!state.getFluidState().isEmpty() && RealmTerrain.isPureFluid(state)) fluidBoundary(context, pos, origin, state);
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            int hidden = 0;
            for (Direction dir : DIRECTIONS) {
                if (!Block.shouldRenderFace(state, region, pos, dir, side.setWithOffset(pos, dir))) hidden |= 1 << dir.ordinal();
            }
            if (hidden == 0) continue;
            BlockPos fixed = pos.immutable();
            var model = dispatcher.getBlockModel(state);
            var data = model.getModelData(region, fixed, state, modelData.getOrDefault(fixed, ModelData.EMPTY));
            long seed = state.getSeed(fixed);
            random.setSeed(seed);
            PoseStack pose = context.getPoseStack();
            pose.pushPose();
            var offset = state.getOffset(region, fixed);
            pose.translate(fixed.getX() - origin.getX() + offset.x, fixed.getY() - origin.getY() + offset.y,
                    fixed.getZ() - origin.getZ() + offset.z);
            try {
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    boolean smooth = Minecraft.useAmbientOcclusion() && state.getLightEmission(region, fixed) == 0
                            && model.useAmbientOcclusion(state, type);
                    QuadLighter lighter = null;
                    try {
                        for (Direction dir : DIRECTIONS) {
                            if ((hidden & (1 << dir.ordinal())) == 0) continue;
                            random.setSeed(seed);
                            var quads = model.getQuads(state, dir, random, data, type);
                            if (quads.isEmpty()) continue;
                            if (lighter == null) {
                                lighter = context.getQuadLighter(smooth);
                                lighter.setup(region, fixed, state);
                            }
                            VertexConsumer buffer = context.getOrCreateChunkBuffer(type);
                            for (var quad : quads) lighter.process(buffer, pose.last(), quad, OverlayTexture.NO_OVERLAY);
                        }
                    } finally {
                        if (lighter != null) lighter.reset();
                    }
                }
            } finally {
                pose.popPose();
            }
        }
    }
}
