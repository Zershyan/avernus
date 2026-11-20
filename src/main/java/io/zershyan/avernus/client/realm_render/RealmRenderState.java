package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinViewArea;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 保留原版区块缓冲，只对受影响列做裁剪和补绘。 */
public final class RealmRenderState {
    private static final RealmColumns COLUMNS = new RealmColumns();
    private static final RealmSectionMeshes MESHES = new RealmSectionMeshes();
    private static final ObjectArrayList<RenderSection> DRAW = new ObjectArrayList<>();
    private static final List<RenderSection> SUPPLEMENT = new ArrayList<>();
    private static @Nullable ClientLevel world;
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static boolean warming;
    private static long warmTick = Long.MIN_VALUE;

    private RealmRenderState() {
    }

    public static boolean active() {
        return COLUMNS.active();
    }

    public static void reset() {
        world = null;
        warming = false;
        warmTick = Long.MIN_VALUE;
        COLUMNS.close();
        RealmEnclosure.reset();
        MESHES.reset();
        DRAW.clear();
        SUPPLEMENT.clear();
    }

    public static boolean hides(BlockPos pos) {
        return COLUMNS.hides(pos);
    }

    public static boolean inWeakWaterAbyss(BlockPos pos) {
        return COLUMNS.abyss.contains(pos);
    }

    public static void renderWeakWaterAbyss(RenderLevelStageEvent event) {
        COLUMNS.abyss.render(event);
    }

    public static void dirty(@Nullable ClientLevel level, int x, int y, int z) {
        if (level == null || level != world) return;
        COLUMNS.dirty(x, z);
        MESHES.dirty(x, y, z);
    }

    public static void prepare(ClientLevel level, ViewArea view, Camera camera, Frustum frustum, List<RenderSection> vanilla) {
        if (world != level) {
            reset();
            world = level;
        }
        var player = Minecraft.getInstance().player;
        BlockPos center = player != null ? player.blockPosition() : camera.getBlockPosition();
        cameraPosition = camera.getPosition();
        RealmEnclosure.update(level, camera);
        boolean wasActive = active();
        COLUMNS.update(level, camera.getEntity(), SectionPos.blockToSectionCoord(center.getX()),
                SectionPos.blockToSectionCoord(center.getZ()), view.getViewDistance());
        DRAW.clear();
        SUPPLEMENT.clear();
        long time = level.getGameTime();
        var realms = RealmSets.of(level).all();
        boolean wasWarming = warming;
        warming = false;
        if (!realms.isEmpty()) {
            MESHES.beginFrame();
            RenderRegionCache regions = null;
            if (active()) {
                regions = new RenderRegionCache();
                prepareActive(level, view, frustum, vanilla, regions);
            }
            for (var realm : realms) {
                if (!realm.geometryAppliesTo(camera.getEntity()) || realm.closeTime() >= 0 || time > realm.startTime() + realm.timeline().shatterDelay()) continue;
                warming = true;
                if (time == warmTick) break;
                if (regions == null) regions = new RenderRegionCache();
                RealmPrewarm.request(level, realm, view, frustum, center, cameraPosition, MESHES, regions);
            }
            if (warming) warmTick = time;
        }
        if (active() || warming) MESHES.prune();
        else if (wasActive || wasWarming) MESHES.reset();
    }

    private static void prepareActive(ClientLevel level, ViewArea view, Frustum frustum, List<RenderSection> vanilla, RenderRegionCache regions) {
        Set<RenderSection> existing = new HashSet<>(vanilla);
        List<RenderSection> relevant = new ArrayList<>();
        BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos();
        IMixinViewArea lookup = (IMixinViewArea) view;
        COLUMNS.forEachCandidate(chunk -> {
            int x = ChunkPos.getX(chunk), z = ChunkPos.getZ(chunk);
            if (level.getChunk(x, z, ChunkStatus.FULL, false) == null) return;
            int layer = COLUMNS.minimumLayer(chunk);
            var range = COLUMNS.boundary(chunk);
            int firstY = range == null ? layer : Math.min(layer, range.minY());
            int lastY = layer != Integer.MAX_VALUE ? level.getMaxSection() - 1
                    : range == null ? level.getMinSection() - 1 : SectionPos.blockToSectionCoord(range.maxY());
            for (int sy = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(firstY)); sy <= lastY; sy++) {
                origin.set(SectionPos.sectionToBlockCoord(x), SectionPos.sectionToBlockCoord(sy), SectionPos.sectionToBlockCoord(z));
                RenderSection section = lookup.avernus$sectionAt(origin);
                if (section == null || !origin.equals(section.getOrigin()) || !frustum.isVisible(section.getBoundingBox())) continue;
                relevant.add(section);
                if (existing.add(section)) SUPPLEMENT.add(section);
            }
        });
        DRAW.addAll(vanilla);
        DRAW.addAll(SUPPLEMENT);
        if (RealmEnclosure.active()) DRAW.removeIf(section -> !RealmEnclosure.keeps(section));
        if (!SUPPLEMENT.isEmpty()) DRAW.sort(Comparator.comparingDouble(RealmRenderState::distance));
        relevant.sort(Comparator.comparingDouble(RealmRenderState::distance));
        for (RenderSection section : relevant) requestSurface(level, regions, section);
        for (RenderSection section : relevant) requestBoundary(level, regions, section);
    }

    private static double distance(RenderSection section) {
        var origin = section.getOrigin();
        return cameraPosition.distanceToSqr(origin.getX() + 8.0, origin.getY() + 8.0, origin.getZ() + 8.0);
    }

    private static long chunk(BlockPos origin) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(origin.getX()), SectionPos.blockToSectionCoord(origin.getZ()));
    }

    private static void requestSurface(ClientLevel level, RenderRegionCache regions, RenderSection section) {
        BlockPos origin = section.getOrigin();
        long key = SectionPos.asLong(origin);
        for (var style : COLUMNS.styles(chunk(origin))) {
            if (sameLayer(style.key(), origin)) MESHES.request(level, regions, key, style.key(), cameraPosition);
        }
    }

    private static void requestBoundary(ClientLevel level, RenderRegionCache regions, RenderSection section) {
        BlockPos origin = section.getOrigin();
        var range = COLUMNS.boundary(chunk(origin));
        if (range != null && range.intersects(origin.getY())) MESHES.request(level, regions, SectionPos.asLong(origin), null, cameraPosition);
    }

    /** 分区是否含该形态的境面层或底板层。 */
    private static boolean sameLayer(RealmMeshKey style, BlockPos origin) {
        int section = SectionPos.blockToSectionCoord(origin.getY());
        return SectionPos.blockToSectionCoord(style.layerY()) == section || SectionPos.blockToSectionCoord(style.floorY()) == section;
    }

    public static List<RenderSection> supplement() {
        return SUPPLEMENT;
    }

    public static ObjectListIterator<RenderSection> iterator(RenderType layer) {
        return DRAW.listIterator(layer == RenderType.translucent() ? DRAW.size() : 0);
    }

    public static void setupShader(ShaderInstance shader) {
        int size = COLUMNS.size();
        shader.setSampler("RealmColumns", COLUMNS.texture());
        shader.safeGetUniform("RealmBounds").set(COLUMNS.minX(), COLUMNS.minZ(), COLUMNS.maxX(), COLUMNS.maxZ());
        shader.safeGetUniform("RealmTableOrigin").set(Math.floorMod(COLUMNS.minX(), size), Math.floorMod(COLUMNS.minZ(), size));
        shader.safeGetUniform("RealmTableSize").set(size);
    }

    public static boolean hasAdditional(RenderSection section, RenderType type) {
        BlockPos origin = section.getOrigin();
        long chunk = chunk(origin), pos = SectionPos.asLong(origin);
        var range = COLUMNS.boundary(chunk);
        if (range != null && range.intersects(origin.getY()) && MESHES.hasLayer(pos, null, type)) return true;
        for (var style : COLUMNS.styles(chunk)) {
            if (sameLayer(style.key(), origin) && MESHES.hasLayer(pos, style.key(), type)) return true;
        }
        return false;
    }

    public static void draw(RenderSection section, RenderType type, ShaderInstance shader, Runnable drawOriginal) {
        BlockPos origin = section.getOrigin();
        long chunk = chunk(origin), pos = SectionPos.asLong(origin);
        var uniform = shader.getUniform("RealmSectionOrigin");
        var mode = shader.getUniform("RealmMeshMode");
        var label = shader.getUniform("RealmStyle");
        if (uniform == null || mode == null || label == null) {
            if (!section.getCompiled().isEmpty(type)) drawOriginal.run();
            return;
        }
        uniform.set(origin.getX(), origin.getY(), origin.getZ());
        uniform.upload();
        if (!section.getCompiled().isEmpty(type)) {
            mode.set(COLUMNS.minimumLayer(chunk) < origin.getY() + 16 || COLUMNS.abyss.hasTile(chunk) ? 0 : -1);
            mode.upload();
            drawOriginal.run();
        }
        var range = COLUMNS.boundary(chunk);
        if (range != null && range.intersects(origin.getY()) && MESHES.hasLayer(pos, null, type)) {
            mode.set(1);
            mode.upload();
            MESHES.draw(pos, null, type, origin, cameraPosition);
        }
        for (var style : COLUMNS.styles(chunk)) {
            if (!sameLayer(style.key(), origin) || !MESHES.hasLayer(pos, style.key(), type)) continue;
            mode.set(2);
            mode.upload();
            label.set(style.id());
            label.upload();
            MESHES.draw(pos, style.key(), type, origin, cameraPosition);
        }
    }
}
