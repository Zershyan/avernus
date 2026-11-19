package io.zershyan.avernus.realm;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 镜子演出编舞：从施放者身上边上抛边绕竖直轴翻转飞出（永恒为升至头顶），慢—快—慢停稳并面对施放者 → 龟裂透光 → 炸开，
 * 整面碎成镜片扇形射出并靠惯性滑行至悬停 → 关闭时与展开镜像倒放（镜片与波纹同时收尽）、裂纹愈合、加速飞回施放者。
 * 纯函数，服务端实体与客户端绘制共用。
 */
public final class MirrorChoreography {
    private static final float SPIN_DEGREES = 360.0F * 3;
    private static final double ARC_HEIGHT = 0.6;
    private static final float AFTERGLOW_TICKS = 8.0F;
    /** 抛出初速，以全程平均速度为 1；取 0 即对称的慢—快—慢。 */
    private static final float LAUNCH_SPEED = 0.75F;

    /**
     * @param spread 为负表示完整镜子，否则为镜片射出程度
     * @param crack  碎裂前的蓄势进度：前段裂纹蔓延，后段裂缝透光；炸开后随余光褪回 0
     */
    public record Pose(Vec3 center, float yaw, float spread, float crack) {
        public boolean whole() {
            return spread < 0.0F;
        }
    }

    private MirrorChoreography() {
    }

    /**
     * @param home   开境者胸口（飞回终点）
     * @param target 碎裂位置
     * @param t      自开境起的 tick（含插值）
     * @return 演出已结束时为空
     */
    public static @Nullable Pose at(Realm realm, Vec3 home, Vec3 target, float t) {
        RealmTimeline timeline = realm.timeline();
        int delay = timeline.shatterDelay();
        float closeAt = realm.closeTime() < 0L ? Float.MAX_VALUE : realm.closeTime() - realm.startTime();
        float facing = realm.launchYaw() + 180.0F;

        // 碎裂前关闭：从当前位置直接加速飞回
        if (closeAt < delay && t >= closeAt) {
            Pose from = outbound(realm, target, closeAt, facing);
            return returning(from.center(), home, from.yaw(), (t - closeAt) / Math.max(1, timeline.flyTicks()));
        }
        if (t < delay) return outbound(realm, target, t, facing);
        float scatter = burst((t - delay) / Math.max(1, timeline.scatterTicks()));
        if (t < closeAt) return new Pose(target, facing, scatter, afterglow(t - delay));

        // 收回与展开完全镜像：以波纹收尽的时刻为镜面，镜片沿原路倒放飞回，波纹收尽的同一刻拼回原状并闪光，
        // 随后裂纹倒放愈合，再加速飞回施放者
        float mirrored = closeAt + realm.contractTicks() - t;
        if (mirrored > 0.0F) return new Pose(target, facing, burst(mirrored / Math.max(1, timeline.scatterTicks())), afterglow(mirrored));
        float healed = -mirrored - timeline.crackTicks();
        if (healed < 0.0F) return new Pose(target, facing, -1.0F, 1.0F + mirrored / timeline.crackTicks());
        return returning(target, home, facing, healed / Math.max(1, timeline.flyTicks()));
    }

    /** 从施放者身上翻转飞出，短促加速后长距离刹停，转满整圈后停稳面对施放者，再龟裂蓄势。 */
    private static Pose outbound(Realm realm, Vec3 target, float t, float facing) {
        RealmTimeline timeline = realm.timeline();
        float raw = Mth.clamp(t / Math.max(1, timeline.flyTicks()), 0.0F, 1.0F);
        float travel = brake(raw);
        Vec3 center = realm.launchOrigin().lerp(target, travel).add(0.0, Mth.sin(travel * Mth.PI) * ARC_HEIGHT, 0.0);
        float yaw = realm.launchYaw() + travel * (Mth.wrapDegrees(facing - realm.launchYaw()) + SPIN_DEGREES);
        float crack = Mth.clamp((t - timeline.flyTicks() - timeline.turnTicks()) / timeline.crackTicks(), 0.0F, 1.0F);
        return new Pose(center, yaw, -1.0F, crack);
    }

    /** 完整镜子边转边朝施放者单纯加速飞回，到达即结束。 */
    private static @Nullable Pose returning(Vec3 from, Vec3 home, float yaw, float raw) {
        if (raw >= 1.0F) return null;
        float travel = Math.max(0.0F, raw) * Math.max(0.0F, raw);
        return new Pose(from.lerp(home, travel), yaw - travel * SPIN_DEGREES, -1.0F, 0.0F);
    }

    /** 加速度 6 − 4v₀ + (6v₀ − 12)t 线性递减：以 {@link #LAUNCH_SPEED} 抛出，先加速后刹停，终点速度为零。 */
    private static float brake(float t) {
        return t * (LAUNCH_SPEED + t * (3.0F - 2.0F * LAUNCH_SPEED + t * (LAUNCH_SPEED - 2.0F)));
    }

    /** 炸开瞬间的强光随后迅速褪去。 */
    private static float afterglow(float sinceShatter) {
        return Mth.clamp(1.0F - sinceShatter / AFTERGLOW_TICKS, 0.0F, 1.0F);
    }

    /** 炸开后靠惯性滑行：起步极快，随后长时间缓慢减速直到停住。 */
    private static float burst(float t) {
        float rest = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
        float rest2 = rest * rest;
        return 1.0F - rest2 * rest2 * rest2 * rest;
    }
}
