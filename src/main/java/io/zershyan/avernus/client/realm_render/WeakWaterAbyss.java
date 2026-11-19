package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;

/** 胜出弱水列从水层底面起遮挡世界，只合并和绘制边界，不向深处堆叠方块。 */
final class WeakWaterAbyss {
    private final Map<Long, List<Patch>> tiles = new HashMap<>();

    void replace(long chunk, Map<Integer, BitSetDiscreteVoxelShape> levels) {
        if (levels.isEmpty()) {
            remove(chunk);
            return;
        }
        int x = ChunkPos.getX(chunk) << 4, z = ChunkPos.getZ(chunk) << 4;
        List<Patch> patches = new ArrayList<>();
        levels.forEach((height, shape) -> shape.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                patches.add(new Patch(x + x0, z + z0, x + x1, z + z1, height)), true));
        tiles.put(chunk, patches);
    }

    void remove(long chunk) { tiles.remove(chunk); }
    void clear() { tiles.clear(); }

    boolean hasTile(long chunk) {
        return tiles.containsKey(chunk);
    }
    boolean contains(BlockPos pos) {
        var patches = tiles.get(ChunkPos.asLong(pos));
        if (patches == null) return false;
        for (var patch : patches) {
            if (pos.getY() < patch.top && pos.getX() >= patch.x0 && pos.getX() < patch.x1
                    && pos.getZ() >= patch.z0 && pos.getZ() < patch.z1) return true;
        }
        return false;
    }

    void render(RenderLevelStageEvent event) {
        if (tiles.isEmpty() || contains(event.getCamera().getBlockPosition())) return;
        var shader = GameRenderer.getPositionColorShader();
        if (shader == null) return;
        var mc = Minecraft.getInstance();
        var projection = event.getProjectionMatrix();
        float bottom = -mc.gameRenderer.getDepthFar() * (1 + 1 / Math.abs(projection.m00()) + 1 / Math.abs(projection.m11()));
        Vec3 eye = event.getCamera().getPosition();
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (var patches : tiles.values()) for (var patch : patches) patch.emit(buffer, eye, bottom);
        var mesh = buffer.build();
        if (mesh == null) return;
        // 贴近边界时入口可能在近平面之前，仍须写入深度，不能露出背后的地形。
        boolean clamped = glIsEnabled(GL_DEPTH_CLAMP);
        if (!clamped) glEnable(GL_DEPTH_CLAMP);
        try {
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, event.getModelViewMatrix(), projection, mc.getWindow());
            shader.apply();
            BufferUploader.draw(mesh);
        } finally {
            shader.clear();
            VertexBuffer.unbind();
            if (!clamped) glDisable(GL_DEPTH_CLAMP);
        }
    }

    private record Patch(int x0, int z0, int x1, int z1, int top) {
        void emit(VertexConsumer out, Vec3 eye, float bottom) {
            float y = (float) (top - eye.y);
            if (y == 0) y = -1.0E-4F;
            if (y <= bottom) return;
            float west = (float) (x0 - eye.x), east = (float) (x1 - eye.x);
            float north = (float) (z0 - eye.z), south = (float) (z1 - eye.z);
            if (y <= 0) {
                vertex(out, west, y, north);
                vertex(out, west, y, south);
                vertex(out, east, y, south);
                vertex(out, east, y, north);
            }
            if (west >= 0) wall(out, west, north, west, south, y, bottom);
            if (east <= 0) wall(out, east, south, east, north, y, bottom);
            if (north >= 0) wall(out, east, north, west, north, y, bottom);
            if (south <= 0) wall(out, west, south, east, south, y, bottom);
        }

        private static void wall(VertexConsumer out, float x0, float z0, float x1, float z1, float top, float bottom) {
            vertex(out, x0, top, z0);
            vertex(out, x0, bottom, z0);
            vertex(out, x1, bottom, z1);
            vertex(out, x1, top, z1);
        }

        private static void vertex(VertexConsumer out, float x, float y, float z) {
            out.addVertex(x, y, z).setColor(0xFF000000);
        }
    }
}
