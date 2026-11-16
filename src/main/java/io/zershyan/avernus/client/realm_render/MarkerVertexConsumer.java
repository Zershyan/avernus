package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

/** 把顶点 alpha 改写为网格标记（区块顶点 alpha 原本恒为 255），供领域区块 Shader 区分网格来源。 */
public record MarkerVertexConsumer(VertexConsumer delegate, int marker) implements VertexConsumer {
    public static final int REAL = 255;
    public static final int BOUNDARY = 254;

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red, green, blue, marker);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        delegate.setNormal(normalX, normalY, normalZ);
        return this;
    }
}
