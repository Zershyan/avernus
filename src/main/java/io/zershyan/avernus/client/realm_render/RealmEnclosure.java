package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.entity.MirrorEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 摄像机身处领域已覆盖的范围时，境外一概不绘制：覆盖范围（圆柱与波面圆盘的交集）之外的分区、实体与方块实体被剔除，
 * 边界分区内越界的地形由区块着色器按方块列裁掉，空出的部分保持天空深度，由后处理画成境相天空。
 * 覆盖判定与境面列表（{@link Realm#coverage}）逐列一致：按列中心、取整 tick 的波面半径，否则两者之间会露出一圈真实地形的剖面。
 */
public final class RealmEnclosure {
    private static @Nullable Realm enclosing;
    private static double wave;

    private RealmEnclosure() {
    }

    /** 每帧开始时按摄像机位置确定封闭的领域与波面半径。 */
    public static void update(ClientLevel level, Camera camera) {
        enclosing = null;
        Vec3 pos = camera.getPosition();
        long time = level.getGameTime();
        for (Realm realm : RealmSets.of(level).all()) {
            if (!realm.geometryAppliesTo(camera.getEntity()) || pos.y < realm.height() - 1) continue;
            double radius = realm.waveRadius(time);
            if (radius > 0.0 && inside(pos.x - realm.exactCenterX(), pos.z - realm.exactCenterZ(), realm.radius())
                    && inside(pos.x - realm.sourceX(), pos.z - realm.sourceZ(), radius)) {
                enclosing = realm;
                wave = radius;
                return;
            }
        }
    }

    public static void reset() {
        enclosing = null;
    }

    public static boolean active() {
        return enclosing != null;
    }

    /** 覆盖范围外或整片沉在底板层以下的分区都不可见，整片跳过。 */
    public static boolean keeps(ChunkRenderDispatcher.RenderChunk section) {
        Realm realm = enclosing;
        if (realm == null) return true;
        BlockPos origin = section.getOrigin();
        return origin.getY() + 16 > realm.height() - 2 && overlaps(realm, origin.getX(), origin.getZ(), origin.getX() + 16, origin.getZ() + 16);
    }

    /** 实体所在列被覆盖才绘制；领域的镜子不受限制。 */
    public static boolean keeps(Entity entity) {
        return entity instanceof MirrorEntity || keeps(Math.floor(entity.getX()) + 0.5, Math.floor(entity.getZ()) + 0.5);
    }

    public static boolean keeps(BlockPos pos) {
        return keeps(pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    private static boolean keeps(double x, double z) {
        Realm realm = enclosing;
        return realm == null || inside(x - realm.exactCenterX(), z - realm.exactCenterZ(), realm.radius())
                && inside(x - realm.sourceX(), z - realm.sourceZ(), wave);
    }

    private static boolean inside(double dx, double dz, double radius) {
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean overlaps(Realm realm, double x0, double z0, double x1, double z1) {
        return boxMeetsDisc(realm.exactCenterX(), realm.exactCenterZ(), realm.radius(), x0, z0, x1, z1)
                && boxMeetsDisc(realm.sourceX(), realm.sourceZ(), wave, x0, z0, x1, z1);
    }

    private static boolean boxMeetsDisc(double cx, double cz, double radius, double x0, double z0, double x1, double z1) {
        return inside(Math.max(0.0, Math.max(x0 - cx, cx - x1)), Math.max(0.0, Math.max(z0 - cz, cz - z1)), radius);
    }

    /** 区块着色器据此裁掉覆盖范围外的方块列。 */
    public static void setupShader(ShaderInstance shader, Vec3 camera) {
        Realm realm = enclosing;
        var cylinder = shader.safeGetUniform("RealmEncloseA");
        var disc = shader.safeGetUniform("RealmEncloseB");
        if (realm == null) {
            cylinder.set(0.0F, 0.0F, -1.0F, 0.0F);
            disc.set(0.0F, 0.0F, -1.0F, 0.0F);
            return;
        }
        cylinder.set((float) (realm.exactCenterX() - camera.x), (float) (realm.exactCenterZ() - camera.z), (float) realm.radius(), 0.0F);
        disc.set((float) (realm.sourceX() - camera.x), (float) (realm.sourceZ() - camera.z), (float) wave, 0.0F);
    }
}
