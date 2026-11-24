package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/** 碎裂与波前经过时的冲击量：闪屏、震屏与冲击壳，全部由附近领域时刻推导，不保存状态。 */
public final class RealmImpact {
    public static final float SHOCK_SECONDS = 1.5F;
    private static final double SHOCK_RANGE = 48.0;
    private static final float WAVE_IMPACT = 0.35F;
    private static final double WAVE_WIDTH = 2.5;
    private static final float FLASH_WHITE = 0.4F;
    private static final float YAW = 1.6F, PITCH = 1.2F, ROLL = 2.0F;
    private static final Vector4f NONE = new Vector4f(0.0F, 0.0F, 0.0F, -1.0F);

    private RealmImpact() {
    }

    /** 距相机最近、仍在冲击期内的碎裂点 (相对坐标, 已过秒数)。 */
    static Vector4f shock(List<RealmMoments.Moment> moments, Vec3 camera) {
        Vector4f result = NONE;
        double best = Double.MAX_VALUE;
        for (RealmMoments.Moment moment : moments) {
            if (!shocking(moment)) continue;
            Vec3 at = moment.shatter().subtract(camera);
            if (at.lengthSqr() < best) {
                best = at.lengthSqr();
                result = new Vector4f((float) at.x, (float) at.y, (float) at.z, moment.shatterAge());
            }
        }
        return result;
    }

    static Vector4f flash(List<RealmMoments.Moment> moments, Vec3 camera, RealmFxQuality quality) {
        if (quality == RealmFxQuality.OFF || !ClientConfig.SCREEN_FLASH.get()) return new Vector4f();
        float strongest = 0.0F;
        Vector3f color = new Vector3f(1.0F);
        for (RealmMoments.Moment moment : moments) {
            float m = magnitude(moment, camera);
            if (m > strongest) {
                strongest = m;
                color = RealmVisualProfile.rgb(moment.visual().glowColor()).lerp(new Vector3f(1.0F), FLASH_WHITE);
            }
        }
        return new Vector4f(color, Math.min(strongest, 1.0F));
    }

    public static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientConfig.quality() == RealmFxQuality.OFF || !ClientConfig.SCREEN_SHAKE.get()) return;
        Camera camera = event.getCamera();
        float partialTick = (float) event.getPartialTick();
        float m = 0.0F;
        for (RealmMoments.Moment moment : RealmMoments.collect(mc.level, camera.getEntity(), camera.getPosition(), partialTick)) {
            m = Math.max(m, magnitude(moment, camera.getPosition()));
        }
        if (m <= 0.0F) return;
        float t = (mc.levelRenderer.getTicks() + partialTick) / 20.0F;
        event.setYaw(event.getYaw() + YAW * m * wobble(t, 0.0F));
        event.setPitch(event.getPitch() + PITCH * m * wobble(t, 1.7F));
        event.setRoll(event.getRoll() + ROLL * m * wobble(t, 3.1F));
    }

    private static boolean shocking(RealmMoments.Moment moment) {
        return moment.shatterAge() >= 0.0F && moment.shatterAge() < SHOCK_SECONDS;
    }

    private static float magnitude(RealmMoments.Moment moment, Vec3 camera) {
        float m = 0.0F;
        if (shocking(moment)) {
            float near = (float) Mth.clamp(1.0 - moment.shatter().distanceTo(camera) / SHOCK_RANGE, 0.0, 1.0);
            m = near * (float) Math.exp(-4.0F * moment.shatterAge());
        }
        Realm realm = moment.realm();
        if (moment.moving() && moment.visible() && realm.inCylinder(Mth.floor(camera.x), Mth.floor(camera.z))) {
            double front = (moment.wave() - Math.hypot(camera.x - realm.sourceX(), camera.z - realm.sourceZ())) / WAVE_WIDTH;
            m = Math.max(m, WAVE_IMPACT * (float) Math.exp(-front * front));
        }
        return m;
    }

    /** 平滑的伪随机摆动，取值约在 [-1, 1]。 */
    private static float wobble(float t, float phase) {
        return 0.6F * Mth.sin(t * 23.0F + phase) + 0.4F * Mth.sin(t * 37.0F + phase * 2.3F);
    }
}
