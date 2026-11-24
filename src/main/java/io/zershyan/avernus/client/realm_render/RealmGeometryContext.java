package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.client.model.lighting.FlatQuadLighter;
import net.minecraftforge.client.model.lighting.QuadLighter;
import net.minecraftforge.client.model.lighting.SmoothQuadLighter;

import java.util.HashMap;
import java.util.Map;

/** 1.20.1 的区块几何构建上下文：按渲染层累积顶点，并提供模型光照明暗计算。 */
final class RealmGeometryContext {
    private final RenderChunkRegion region;
    private final PoseStack pose = new PoseStack();
    private final Map<RenderType, BufferBuilder> buffers = new HashMap<>();
    private FlatQuadLighter flat;
    private SmoothQuadLighter smooth;

    RealmGeometryContext(RenderChunkRegion region) {
        this.region = region;
    }

    BlockAndTintGetter getRegion() {
        return region;
    }

    PoseStack getPoseStack() {
        return pose;
    }

    Map<RenderType, BufferBuilder> buffers() {
        return buffers;
    }

    VertexConsumer getOrCreateChunkBuffer(RenderType type) {
        return buffers.computeIfAbsent(type, ignored -> {
            BufferBuilder builder = new BufferBuilder(4096);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            return builder;
        });
    }

    QuadLighter getQuadLighter(boolean smoothLighting) {
        var colors = Minecraft.getInstance().getBlockColors();
        if (smoothLighting) return smooth != null ? smooth : (smooth = new SmoothQuadLighter(colors));
        return flat != null ? flat : (flat = new FlatQuadLighter(colors));
    }
}
