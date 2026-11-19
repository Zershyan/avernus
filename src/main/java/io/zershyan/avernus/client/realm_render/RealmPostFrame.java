package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * 一帧后处理所需的全部输入，坐标均相对相机。
 *
 * @param realm  当前所处领域（渐出期间保留），为空时氛围部分不生效
 * @param wave   当前领域插值后的波面半径
 * @param shock  (碎裂点 xyz, 已过秒数)，无冲击时 w < 0
 * @param flash  (颜色 rgb, 强度)
 */
record RealmPostFrame(RealmFxQuality quality, float strength, @Nullable Realm realm, RealmVisualProfile visual, double wave,
                      Matrix4f viewProj, Matrix4f invViewProj, Vec3 camera, float time,
                      List<RealmMoments.Moment> moments, Vector4f shock, Vector4f flash) {
    private static final double NOISE_PERIOD = 4096.0;
    /** 弱水领域的雾比平常浓。 */
    private static final float WEAK_WATER_FOG = 1.6F;

    boolean idle() {
        return strength <= 0.0F && (quality == RealmFxQuality.OFF || moments.stream().noneMatch(RealmPostFrame::shows));
    }

    private static boolean shows(RealmMoments.Moment moment) {
        return moment.visible() && moment.wave() > 0.0 || moment.shatterAge() >= 0.0F && moment.shatterAge() < RealmImpact.SHOCK_SECONDS;
    }

    void apply(ShaderInstance shader) {
        shader.safeGetUniform("ViewProj").set(viewProj);
        shader.safeGetUniform("InvViewProj").set(invViewProj);
        shader.safeGetUniform("CameraPos").set(wrap(camera.x), wrap(camera.y), wrap(camera.z));
        shader.safeGetUniform("Time").set(time);
        shader.safeGetUniform("Quality").set(quality.ordinal());
        shader.safeGetUniform("FogSteps").set(quality.fogSteps);
        shader.safeGetUniform("Strength").set(strength);
        shader.safeGetUniform("Atmosphere").set(visual.atmosphere().ordinal());
        shader.safeGetUniform("Tint").set(RealmVisualProfile.rgb(visual.tint()));
        shader.safeGetUniform("Glow").set(RealmVisualProfile.rgb(visual.glowColor()));
        shader.safeGetUniform("FogTint").set(RealmVisualProfile.rgb(visual.fogColor()));
        shader.safeGetUniform("FogDistance").set(visual.fogDistance());
        if (realm != null) {
            shader.safeGetUniform("SurfaceY").set((float) (realm.height() - camera.y));
            shader.safeGetUniform("FogDensity").set(realm.hasTrait(AvernusRealmTraits.WEAK_WATER.get()) ? WEAK_WATER_FOG : 1.0F);
            shader.safeGetUniform("Bounds").set((float) (realm.exactCenterX() - camera.x), (float) (realm.exactCenterZ() - camera.z), realm.radius(), (float) wave);
            shader.safeGetUniform("Source").set((float) (realm.sourceX() - camera.x), (float) (realm.sourceZ() - camera.z));
        } else {
            shader.safeGetUniform("Bounds").set(0.0F, 0.0F, 0.0F, -1.0F);
        }

        Matrix4f shape = new Matrix4f().zero(), source = new Matrix4f().zero(), look = new Matrix4f().zero();
        for (int i = 0; i < moments.size(); i++) {
            RealmMoments.Moment moment = moments.get(i);
            Realm nearby = moment.realm();
            boolean visible = moment.visible();
            shape.setColumn(i, new Vector4f((float) (nearby.exactCenterX() - camera.x), (float) (nearby.exactCenterZ() - camera.z),
                    nearby.radius(), visible ? (float) moment.wave() : -1.0F));
            source.setColumn(i, new Vector4f((float) (nearby.sourceX() - camera.x), (float) (nearby.sourceZ() - camera.z),
                    (float) (nearby.height() - camera.y), visible ? moment.boundary() : 0.0F));
            Vector3f glow = RealmVisualProfile.rgb(moment.visual().glowColor());
            look.setColumn(i, new Vector4f(glow, moment.visual().atmosphere().ordinal() + 4.0F * moment.flow()));
        }
        shader.safeGetUniform("RealmShape").set(shape);
        shader.safeGetUniform("RealmSource").set(source);
        shader.safeGetUniform("RealmLook").set(look);
        shader.safeGetUniform("RealmCount").set(moments.size());
        shader.safeGetUniform("Shock").set(shock);
        shader.safeGetUniform("Flash").set(flash);
    }

    private static float wrap(double value) {
        return (float) (value - Math.floor(value / NOISE_PERIOD) * NOISE_PERIOD);
    }
}
