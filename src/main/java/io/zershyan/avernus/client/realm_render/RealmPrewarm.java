package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinViewArea;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/** 预测范围只用于准备网格，绝不写入实际裁剪表。 */
final class RealmPrewarm {
    static void request(ClientLevel level, Realm realm, ViewArea view, Frustum frustum, BlockPos viewer, Vec3 eye,
                        RealmSectionMeshes meshes, RenderRegionCache regions) {
        if (!meshes.canPrewarm()) return;
        int distance = view.getViewDistance();
        int vx = SectionPos.blockToSectionCoord(viewer.getX()), vz = SectionPos.blockToSectionCoord(viewer.getZ());
        int radius = realm.radius() + 1;
        int x0 = Math.max(vx - distance, SectionPos.blockToSectionCoord(realm.centerX() - radius));
        int z0 = Math.max(vz - distance, SectionPos.blockToSectionCoord(realm.centerZ() - radius));
        int x1 = Math.min(vx + distance, SectionPos.blockToSectionCoord(realm.centerX() + radius));
        int z1 = Math.min(vz + distance, SectionPos.blockToSectionCoord(realm.centerZ() + radius));
        if (x0 > x1 || z0 > z1) return;
        int x = Math.clamp(SectionPos.blockToSectionCoord(Mth.floor(realm.sourceX())), x0, x1);
        int z = Math.clamp(SectionPos.blockToSectionCoord(Mth.floor(realm.sourceZ())), z0, z1);
        int rings = Math.max(Math.max(x - x0, x1 - x), Math.max(z - z0, z1 - z));
        var footprint = realm.coverage(realm.startTime() + realm.timeline().shatterDelay() + realm.timeline().waveTicks());
        RealmMeshKey style = RealmMeshKey.of(realm);
        int surface = SectionPos.blockToSectionCoord(style.layerY());
        var lookup = (IMixinViewArea) view;
        for (BlockPos column : BlockPos.spiralAround(new BlockPos(x, 0, z), rings, Direction.EAST, Direction.SOUTH)) {
            int cx = column.getX(), cz = column.getZ();
            if (cx < x0 || cx > x1 || cz < z0 || cz > z1 || footprint.tile(cx * 16 - 1, cz * 16 - 1, 18) < 0) continue;
            var chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            int first = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(style.layerY() - 1));
            int last = Math.max(surface, level.getMinSection() + chunk.getHighestFilledSectionIndex());
            for (int sy = first; sy <= last && sy < level.getMaxSection(); sy++) {
                boolean isSurface = sy == surface;
                if (!isSurface && chunk.isSectionEmpty(sy)) continue;
                var origin = new BlockPos(cx * 16, sy * 16, cz * 16);
                var section = lookup.avernus$sectionAt(origin);
                if (section == null || !origin.equals(section.getOrigin()) || !frustum.isVisible(section.getBoundingBox())) continue;
                long key = SectionPos.asLong(origin);
                if (isSurface) meshes.request(level, regions, key, style, eye);
                if (!meshes.canPrewarm()) return;
                if (!chunk.isSectionEmpty(sy)) meshes.request(level, regions, key, null, eye);
                if (!meshes.canPrewarm()) return;
            }
        }
    }
}
