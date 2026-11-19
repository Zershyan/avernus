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
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;

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

    static void buildSurface(AddSectionGeometryEvent.SectionRenderingContext context, BlockPos origin,
                             RealmMeshKey style, Material material) {
        BlockAndTintGetter region = context.getRegion();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        int ry = style.layerY() - origin.getY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                pos.set(origin.getX() + x, style.layerY(), origin.getZ() + z);
                RealmTerrain.Kind kind = style.kind(region.getBlockState(pos));
                if (kind != RealmTerrain.Kind.SOLID && kind != RealmTerrain.Kind.WATER) continue;
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
                if (water) {
                    face(buffer, region, Direction.DOWN, x, ry + WATER_TOP, z, 0.0F, material.still,
                            material.waterColor, light(region, pos), 255);
                } else if (!region.getBlockState(side.setWithOffset(pos, Direction.DOWN)).isSolidRender(region, side)) {
                    face(buffer, region, Direction.DOWN, x, ry, z, 0.0F, material.solid,
                            0xFFFFFF, light(region, side), 255);
                }
            }
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
            buffer.addVertex(x + c[0], y + c[1] * top, z + c[2]).setColor(r, g, b, alpha)
                    .setUv(sprite.getU(u), sprite.getV(v)).setLight(light).setNormal(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        }
    }

    private static void fluidBoundary(AddSectionGeometryEvent.SectionRenderingContext context, BlockPos pos, BlockPos origin,
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
        var sprites = FluidSpriteCache.getFluidSprites(region, pos, fluid);
        int color = IClientFluidTypeExtensions.of(fluid).getTintColor(fluid, region, pos);
        var layer = ItemBlockRenderTypes.getRenderLayer(fluid);
        var buffer = context.getOrCreateChunkBuffer(layer);
        float top = fluid.getHeight(region, pos);
        for (Direction dir : DIRECTIONS) {
            if ((hidden & (1 << dir.ordinal())) == 0) continue;
            face(buffer, region, dir, pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(),
                    top, sprites[dir.getAxis().isHorizontal() ? 1 : 0], color, LevelRenderer.getLightColor(region, pos), color >>> 24);
        }
    }

    static void buildBoundary(AddSectionGeometryEvent.SectionRenderingContext context, BlockPos origin) {
        BlockAndTintGetter region = context.getRegion();
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
            var data = model.getModelData(region, fixed, state, region.getModelData(fixed));
            long seed = state.getSeed(fixed);
            random.setSeed(seed);
            PoseStack pose = context.getPoseStack();
            pose.pushPose();
            pose.translate(fixed.getX() - origin.getX(), fixed.getY() - origin.getY(), fixed.getZ() - origin.getZ());
            QuadLighter lighter = null;
            try {
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    for (Direction dir : DIRECTIONS) {
                        if ((hidden & (1 << dir.ordinal())) == 0) continue;
                        random.setSeed(seed);
                        var quads = model.getQuads(state, dir, random, data, type);
                        if (quads.isEmpty()) continue;
                        if (lighter == null) {
                            lighter = context.getQuadLighter(true);
                            lighter.setup(region, fixed, state);
                        }
                        VertexConsumer buffer = context.getOrCreateChunkBuffer(type);
                        for (var quad : quads) lighter.process(buffer, pose.last(), quad, OverlayTexture.NO_OVERLAY);
                    }
                }
            } finally {
                if (lighter != null) lighter.reset();
                pose.popPose();
            }
        }
    }
}
