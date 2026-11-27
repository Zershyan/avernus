package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinBufferBuilder;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinRenderRegionCache;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 领域独立 VBO；原版 section 不因波纹或领域中心改变而重编。 */
final class RealmSectionMeshes {
    private static final int MAX_IN_FLIGHT = 6;
    private static final int MAX_SECTIONS = 2048;
    private static final long MAX_BYTES = 128L * 1024 * 1024;
    private static final int KEEP_FRAMES = 120;
    private static final Mesh EMPTY = new Mesh(Map.of(), 0);

    private final Map<Long, SectionMeshes> sections = new LinkedHashMap<>(128, 0.75F, true);
    private int inFlight;
    private long bytes;
    private long frame;
    private int sortBudget;
    private @Nullable BufferBuilder sortBuffer;

    private static final class Slot {
        @Nullable Mesh mesh;
    }

    private static final class SectionMeshes {
        final Map<RealmMeshKey, Slot> slots = new HashMap<>();
        long lastUsed;
    }

    private record Mesh(Map<RenderType, Layer> layers, long size) {
        void close() {
            layers.values().forEach(layer -> layer.vertices.close());
        }
    }

    private static final class Layer {
        final VertexBuffer vertices;
        final @Nullable BufferBuilder.SortState sort;
        final int vertexBytes;
        Vec3 sortedAt;

        Layer(VertexBuffer vertices, @Nullable BufferBuilder.SortState sort, int vertexBytes, Vec3 sortedAt) {
            this.vertices = vertices;
            this.sort = sort;
            this.vertexBytes = vertexBytes;
            this.sortedAt = sortedAt;
        }
    }

    private static final class CpuLayer implements AutoCloseable {
        final BufferBuilder builder;
        final BufferBuilder.RenderedBuffer data;
        final @Nullable BufferBuilder.SortState sort;
        private boolean uploaded;

        CpuLayer(BufferBuilder builder, BufferBuilder.RenderedBuffer data, @Nullable BufferBuilder.SortState sort) {
            this.builder = builder;
            this.data = data;
            this.sort = sort;
        }

        void uploadTo(VertexBuffer vertices) {
            uploaded = true;
            vertices.upload(data);
        }

        @Override
        public void close() {
            try {
                if (!uploaded) data.release();
            } finally {
                RealmGeometryContext.free(builder);
            }
        }
    }

    void beginFrame() {
        frame++;
        sortBudget = 12;
    }

    private boolean fits(long size) {
        return size <= MAX_BYTES - bytes;
    }

    boolean canPrewarm() {
        return inFlight < MAX_IN_FLIGHT - 2 && sections.size() < MAX_SECTIONS && bytes < MAX_BYTES;
    }

    void request(ClientLevel level, RenderRegionCache regions, long pos, @Nullable RealmMeshKey style, Vec3 camera) {
        SectionMeshes existing = sections.get(pos);
        if (existing != null) {
            existing.lastUsed = frame;
            if (existing.slots.containsKey(style)) return;
        }
        if (inFlight >= MAX_IN_FLIGHT) return;
        if ((existing == null || bytes >= MAX_BYTES) && !makeRoom(existing == null)) return;
        SectionMeshes entry = existing != null ? existing : sections.computeIfAbsent(pos, ignored -> new SectionMeshes());
        entry.lastUsed = frame;
        Slot slot = new Slot();
        entry.slots.put(style, slot);
        SectionPos section = SectionPos.of(pos);
        BlockPos origin = section.origin();
        BlockPos end = origin.offset(15, 15, 15);
        RenderChunkRegion region = style == null ? regions.createRegion(level, origin, end, 1)
                : ((IMixinRenderRegionCache) regions).avernus$createIncludingEmpty(level, origin, end, 1);
        if (region == null) {
            slot.mesh = EMPTY;
            return;
        }
        RealmSectionMesher.Material material = style == null ? null : material(style);
        Map<BlockPos, ModelData> modelData = style == null
                ? Map.copyOf(level.getModelDataManager().getAt(new ChunkPos(origin))) : Map.of();
        Minecraft minecraft = Minecraft.getInstance();
        inFlight++;
        CompletableFuture.supplyAsync(() -> build(region, origin, style, material, modelData, camera), Util.backgroundExecutor())
                .whenComplete((layers, error) -> minecraft.execute(() -> complete(pos, style, entry, slot, origin, camera, layers, error)));
    }

    private static RealmSectionMesher.Material material(RealmMeshKey style) {
        var profile = RealmVisuals.get(style.aspect());
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        return new RealmSectionMesher.Material(atlas.getSprite(profile.solidTexture()), atlas.getSprite(profile.stillTexture()),
                atlas.getSprite(profile.flowTexture()), profile.waterColor());
    }

    private static Map<RenderType, CpuLayer> build(RenderChunkRegion region, BlockPos origin, @Nullable RealmMeshKey style,
                                                   @Nullable RealmSectionMesher.Material material, Map<BlockPos, ModelData> modelData, Vec3 camera) {
        RealmGeometryContext context = new RealmGeometryContext(region);
        Map<RenderType, CpuLayer> layers = new HashMap<>();
        try {
            if (style == null) RealmSectionMesher.buildBoundary(context, origin, modelData);
            else if (material != null) RealmSectionMesher.buildSurface(context, origin, style, material);
            var iterator = context.buffers().entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var builder = entry.getValue();
                BufferBuilder.SortState sort = null;
                if (!builder.isCurrentBatchEmpty() && (entry.getKey() == RenderType.translucent() || entry.getKey() == RenderType.tripwire())) {
                    builder.setQuadSorting(sorting(origin, camera));
                    sort = builder.getSortState();
                }
                var data = builder.endOrDiscardIfEmpty();
                if (data == null) RealmGeometryContext.free(builder);
                else layers.put(entry.getKey(), new CpuLayer(builder, data, sort));
                iterator.remove();
            }
            return layers;
        } catch (RuntimeException | Error failure) {
            layers.values().forEach(CpuLayer::close);
            throw failure;
        } finally {
            context.buffers().values().forEach(RealmGeometryContext::free);
        }
    }

    private void complete(long pos, @Nullable RealmMeshKey style, SectionMeshes entry, Slot slot, BlockPos origin,
                          Vec3 camera, @Nullable Map<RenderType, CpuLayer> layers, @Nullable Throwable error) {
        inFlight--;
        if (error != null) {
            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(error, "领域 section 网格构建失败：" + origin));
        }
        if (sections.get(pos) != entry || entry.slots.get(style) != slot) {
            if (layers != null) layers.values().forEach(CpuLayer::close);
            return;
        }
        if (layers == null) {
            slot.mesh = EMPTY;
            return;
        }
        long estimate = estimateBytes(layers);
        if (!fits(estimate)) {
            var iterator = sections.entrySet().iterator();
            while (iterator.hasNext() && !fits(estimate)) {
                var candidate = iterator.next();
                if (candidate.getValue() == entry || candidate.getValue().lastUsed == frame) continue;
                iterator.remove();
                close(candidate.getValue());
            }
            if (!fits(estimate)) {
                layers.values().forEach(CpuLayer::close);
                entry.slots.remove(style);
                return;
            }
        }
        slot.mesh = upload(layers, camera);
        bytes += slot.mesh.size;
    }

    private static long estimateBytes(Map<RenderType, CpuLayer> layers) {
        long size = 0L;
        for (var cpu : layers.values()) size += cpu.data.drawState().bufferSize();
        return size;
    }

    private Mesh upload(Map<RenderType, CpuLayer> source, Vec3 camera) {
        Map<RenderType, Layer> layers = new HashMap<>();
        long size = estimateBytes(source);
        try {
            var iterator = source.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                RenderType type = entry.getKey();
                CpuLayer cpu = entry.getValue();
                iterator.remove();
                try (cpu) {
                    VertexBuffer vertices = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    try {
                        vertices.bind();
                        cpu.uploadTo(vertices);
                        layers.put(type, new Layer(vertices, cpu.sort, cpu.data.drawState().vertexBufferSize(), camera));
                    } catch (RuntimeException | Error failure) {
                        vertices.close();
                        throw failure;
                    }
                }
            }
            return new Mesh(layers, size);
        } catch (RuntimeException | Error failure) {
            layers.values().forEach(layer -> layer.vertices.close());
            source.values().forEach(CpuLayer::close);
            throw failure;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static VertexSorting sorting(BlockPos origin, Vec3 camera) {
        return VertexSorting.byDistance((float) (camera.x - origin.getX()), (float) (camera.y - origin.getY()),
                (float) (camera.z - origin.getZ()));
    }

    boolean hasLayer(long section, @Nullable RealmMeshKey style, RenderType type) {
        return layer(section, style, type) != null;
    }

    void draw(long section, @Nullable RealmMeshKey style, RenderType type, BlockPos origin, Vec3 camera) {
        Layer layer = layer(section, style, type);
        if (layer == null) return;
        layer.vertices.bind();
        if (layer.sort != null && sortBudget > 0 && camera.distanceToSqr(layer.sortedAt) > 1.0) {
            BufferBuilder builder = sortBuffer(layer.vertexBytes);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            builder.restoreSortState(layer.sort);
            builder.setQuadSorting(sorting(origin, camera));
            layer.vertices.upload(builder.end());
            layer.sortedAt = camera;
            sortBudget--;
        }
        layer.vertices.draw();
    }

    private BufferBuilder sortBuffer(int vertexBytes) {
        // 即使仅上传索引，1.20.1 的 VertexBuffer.upload 仍会访问完整顶点长度的切片。
        if (sortBuffer == null || ((IMixinBufferBuilder) sortBuffer).avernus$buffer().capacity() < vertexBytes) {
            if (sortBuffer != null) RealmGeometryContext.free(sortBuffer);
            sortBuffer = new BufferBuilder(Math.max(4096, (vertexBytes + 5) / 6));
        }
        return sortBuffer;
    }

    private @Nullable Layer layer(long section, @Nullable RealmMeshKey style, RenderType type) {
        SectionMeshes entry = sections.get(section);
        Slot slot = entry == null ? null : entry.slots.get(style);
        return slot == null || slot.mesh == null ? null : slot.mesh.layers.get(type);
    }

    void dirty(int x, int y, int z) {
        SectionMeshes entry = sections.remove(SectionPos.asLong(x, y, z));
        if (entry != null) close(entry);
    }

    private boolean makeRoom(boolean anotherSection) {
        if ((!anotherSection || sections.size() < MAX_SECTIONS) && bytes < MAX_BYTES) return true;
        var iterator = sections.values().iterator();
        while (iterator.hasNext()) {
            SectionMeshes entry = iterator.next();
            if (entry.lastUsed == frame) continue;
            iterator.remove();
            close(entry);
            if ((!anotherSection || sections.size() < MAX_SECTIONS) && bytes < MAX_BYTES) return true;
        }
        return false;
    }

    void prune() {
        var iterator = sections.values().iterator();
        while (iterator.hasNext()) {
            SectionMeshes entry = iterator.next();
            if (frame - entry.lastUsed <= KEEP_FRAMES) continue;
            iterator.remove();
            close(entry);
        }
    }

    private void close(SectionMeshes entry) {
        for (Slot slot : entry.slots.values()) {
            if (slot.mesh == null) continue;
            slot.mesh.close();
            bytes -= slot.mesh.size;
        }
    }

    void reset() {
        sections.values().forEach(this::close);
        sections.clear();
        if (sortBuffer != null) {
            RealmGeometryContext.free(sortBuffer);
            sortBuffer = null;
        }
    }
}
