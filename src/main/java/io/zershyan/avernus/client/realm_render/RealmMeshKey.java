package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

/**
 * 一个领域虚拟网格的构建参数。波纹推进只改 GPU 覆盖参数，不改此键；
 * 永恒领域的网格中心带余量，中心漂移超出余量才重建。
 * front 表示波面正在推进或收缩：此时整个圆盘都需要波面边界补面，静止后只保留边缘环带。
 */
public record RealmMeshKey(int label, double centerX, double centerZ, int radius, int margin, int height,
                           boolean weakWater, boolean boundless, boolean front, RealmAspect aspect) {
    public static final int ETERNAL_MARGIN = 8;
    /** 边界补面环带相对圆柱边缘的半宽。 */
    private static final double RIM_WIDTH = 1.5;

    public static RealmMeshKey of(Realm realm, int label, boolean front, @Nullable RealmMeshKey previous) {
        int margin = realm.eternal() ? ETERNAL_MARGIN : 0;
        double cx = realm.exactCenterX();
        double cz = realm.exactCenterZ();
        if (previous != null) {
            double dx = cx - previous.centerX;
            double dz = cz - previous.centerZ;
            if (dx * dx + dz * dz <= (double) margin * margin) {
                cx = previous.centerX;
                cz = previous.centerZ;
            }
        }
        return new RealmMeshKey(label, cx, cz, realm.radius(), margin, realm.height(),
                realm.hasTrait(AvernusRealmTraits.WEAK_WATER.get()), realm.hasTrait(AvernusRealmTraits.BOUNDLESS.get()),
                front, realm.aspect());
    }

    public int layerY() {
        return height - 1;
    }

    private double distSqr(int x, int z) {
        double dx = x + 0.5 - centerX;
        double dz = z + 0.5 - centerZ;
        return dx * dx + dz * dz;
    }

    /** 该列是否需要虚拟层（含永恒余量）。 */
    public boolean inDisc(int x, int z) {
        double outer = radius + margin;
        return distSqr(x, z) <= outer * outer;
    }

    /** 该列是否需要边界补面。 */
    public boolean needsBoundary(int x, int z) {
        return front ? inDisc(x, z) : inRim(x, z);
    }

    private boolean inRim(int x, int z) {
        double d = Math.sqrt(distSqr(x, z));
        return d >= radius - margin - RIM_WIDTH && d <= radius + margin + RIM_WIDTH;
    }

    /** 该 section 是否承载本领域的附加几何：虚拟层所在 section，或环带上虚拟层及以上的 section。 */
    public boolean affectsSection(int sx, int sy, int sz) {
        int layerSection = SectionPos.blockToSectionCoord(layerY());
        if (sy < layerSection) return false;
        double minX = SectionPos.sectionToBlockCoord(sx), minZ = SectionPos.sectionToBlockCoord(sz);
        double nearX = Math.max(Math.max(minX - centerX, 0.0), centerX - minX - 16.0);
        double nearZ = Math.max(Math.max(minZ - centerZ, 0.0), centerZ - minZ - 16.0);
        double near = Math.sqrt(nearX * nearX + nearZ * nearZ);
        double farX = Math.max(Math.abs(minX - centerX), Math.abs(minX + 16.0 - centerX));
        double farZ = Math.max(Math.abs(minZ - centerZ), Math.abs(minZ + 16.0 - centerZ));
        double far = Math.sqrt(farX * farX + farZ * farZ);
        double rimOuter = radius + margin + RIM_WIDTH + 1.0;
        boolean rim = near <= rimOuter && (front || far >= radius - margin - RIM_WIDTH - 1.0);
        return rim || sy == layerSection && near <= radius + margin + 1.0;
    }
}
