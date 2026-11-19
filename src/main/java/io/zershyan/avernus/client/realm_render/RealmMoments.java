package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.api.realm.RealmPhase;
import io.zershyan.avernus.client.mirror.MirrorAnchors;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** 相机附近领域在当前帧的演出时刻，后处理、冲击与粒子共用。 */
public final class RealmMoments {
    public static final int MAX = 4;
    private static final double MARGIN = 16.0;
    private static final float BOUNDARY_FADE_TICKS = 20.0F;

    /**
     * @param visible    几何对观察者生效（波幕、界壁、地表环、雾穹、波前粒子遵守）
     * @param wave       插值后的波面半径，无波面时为负
     * @param flow       波面运动方向：扩张 1，收缩 -1，静止 0
     * @param boundary   界壁可见度
     * @param shatterAge 距碎裂的秒数，未碎裂或不会碎裂时为负
     */
    public record Moment(Realm realm, RealmVisualProfile visual, boolean visible, double wave, float flow,
                         float boundary, Vec3 shatter, float shatterAge, double distance) {
        public boolean moving() {
            return flow != 0.0F;
        }
    }

    private RealmMoments() {
    }

    public static List<Moment> collect(ClientLevel level, @Nullable Entity viewer, Vec3 camera, float partialTick) {
        double limit = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0 + MARGIN;
        long gameTime = level.getGameTime();
        return RealmSets.of(level).all().stream()
                .map(realm -> moment(realm, viewer, camera, gameTime, partialTick))
                .filter(moment -> moment.distance <= limit)
                .sorted(Comparator.comparingDouble(Moment::distance))
                .limit(MAX)
                .toList();
    }

    private static Moment moment(Realm realm, @Nullable Entity viewer, Vec3 camera, long gameTime, float partialTick) {
        double now = realm.waveRadius(gameTime), next = realm.waveRadius(gameTime + 1);
        double wave = now > 0.0 && next > 0.0 ? Mth.lerp(partialTick, now, next) : now;
        RealmPhase phase = realm.phase(gameTime);
        float flow = wave <= 0.0 ? 0.0F : phase == RealmPhase.EXPANDING ? 1.0F : phase == RealmPhase.CLOSING ? -1.0F : 0.0F;

        double full = Math.hypot(realm.sourceX() - realm.exactCenterX(), realm.sourceZ() - realm.exactCenterZ()) + realm.radius();
        float boundary = (float) Mth.smoothstep(Mth.clamp((wave - full + 8.0) / 8.0, 0.0, 1.0));
        if (realm.isClosing(gameTime)) {
            boundary *= Mth.clamp(1.0F - (gameTime - realm.closeTime() + partialTick) / BOUNDARY_FADE_TICKS, 0.0F, 1.0F);
        }

        float age = (gameTime - realm.shatterTime() + partialTick) / 20.0F;
        float shatterAge = realm.shatters() && age >= 0.0F ? age : -1.0F;
        double edge = Math.hypot(camera.x - realm.exactCenterX(), camera.z - realm.exactCenterZ()) - realm.radius();
        return new Moment(realm, RealmVisuals.get(realm.aspect()), realm.geometryAppliesTo(viewer), wave, flow, boundary,
                MirrorAnchors.shatterPoint(realm, partialTick), shatterAge, Math.max(0.0, edge));
    }
}
