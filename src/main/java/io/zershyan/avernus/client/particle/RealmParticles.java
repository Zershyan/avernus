package io.zershyan.avernus.client.particle;

import io.zershyan.avernus.client.mirror.MirrorAnchors;
import io.zershyan.avernus.client.realm_render.RealmMoments;
import io.zershyan.avernus.client.realm_render.RealmVisualProfile;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.registry.AvernusParticles;
import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 领域粒子的注册与每 tick 生成：境内环境粒子与絮团、移动波前上的粒子、镜子周围的絮团与碎裂喷发。
 * 数量随档位倍率缩放，关档不生成；一律走 {@code level.addParticle}，遵守原版粒子设置。
 */
public final class RealmParticles {
    private static final double SHATTER_RANGE = 64.0;
    private static final double WAVE_RANGE = 48.0;
    private static final float SHATTER_WINDOW = 0.5F;
    private static final Set<UUID> BURST = new HashSet<>();

    private RealmParticles() {
    }

    public static void registerProviders(RegisterParticleProvidersEvent event) {
        register(event, AvernusParticles.WISP.get(), RealmParticle.Kind.WISP);
        register(event, AvernusParticles.EMBER.get(), RealmParticle.Kind.EMBER);
        register(event, AvernusParticles.FROST.get(), RealmParticle.Kind.FROST);
        register(event, AvernusParticles.SPARK.get(), RealmParticle.Kind.SPARK);
        register(event, AvernusParticles.FLOCK.get(), RealmParticle.Kind.FLOCK);
    }

    private static void register(RegisterParticleProvidersEvent event, ParticleType<ColorParticleOption> type, RealmParticle.Kind kind) {
        event.registerSpriteSet(type, sprites -> new RealmParticle.Provider(kind, sprites));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        RealmFxQuality quality = ClientConfig.quality();
        if (level == null || mc.isPaused() || quality == RealmFxQuality.OFF) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return;
        Vec3 cam = camera.getPosition();
        RandomSource random = level.getRandom();
        Set<UUID> live = new HashSet<>();
        for (RealmMoments.Moment moment : RealmMoments.collect(level, camera.getEntity(), cam, 1.0F)) {
            Realm realm = moment.realm();
            live.add(realm.id());
            float rate = quality.particleScale;
            if (moment.visible() && realm.covers(Mth.floor(cam.x), Mth.floor(cam.z), level.getGameTime())) {
                ambient(level, random, moment, cam, rate);
            }
            if (moment.visible() && moment.moving()) wave(level, random, moment, cam, rate);
            mirror(level, random, moment, cam, rate);
        }
        BURST.retainAll(live);
    }

    public static void reset() {
        BURST.clear();
    }

    /** 相机周围 2–24 格、领域覆盖范围内：境相环境粒子与贴地絮团。 */
    private static void ambient(ClientLevel level, RandomSource random, RealmMoments.Moment moment, Vec3 cam, float rate) {
        Realm realm = moment.realm();
        RealmVisualProfile visual = moment.visual();
        double ground = realm.height() + 1.0;
        long time = level.getGameTime();
        for (int i = count(random, 4.0F * rate); i > 0; i--) {
            double angle = random.nextDouble() * Math.PI * 2, distance = Mth.lerp(random.nextDouble(), 2.0, 24.0);
            double x = cam.x + Math.cos(angle) * distance, z = cam.z + Math.sin(angle) * distance;
            if (!realm.covers(Mth.floor(x), Mth.floor(z), time)) continue;
            switch (visual.atmosphere()) {
                case WISP -> add(level, AvernusParticles.WISP.get(), visual.glowColor(), 0.8F, x, ground + random.nextDouble() * 10, z,
                        0.0, 0.01, 0.0);
                case EMBER -> add(level, AvernusParticles.EMBER.get(), visual.glowColor(), 1.0F, x, ground + random.nextDouble() * 2, z,
                        0.0, 0.04 + random.nextDouble() * 0.04, 0.0);
                case FROST -> add(level, AvernusParticles.FROST.get(), visual.glowColor(), 0.9F, x, cam.y + 4 + random.nextDouble() * 8, z,
                        0.0, -0.02, 0.0);
            }
        }
        for (int i = count(random, 0.6F * rate); i > 0; i--) {
            double angle = random.nextDouble() * Math.PI * 2, distance = Mth.lerp(random.nextDouble(), 4.0, 24.0);
            double x = cam.x + Math.cos(angle) * distance, z = cam.z + Math.sin(angle) * distance;
            if (realm.covers(Mth.floor(x), Mth.floor(z), time)) {
                flock(level, random, visual, x, ground + random.nextDouble() * 3, z);
            }
        }
    }

    /** 距相机 48 格内的波前弧段上取点，向上扬起本境相粒子。 */
    private static void wave(ClientLevel level, RandomSource random, RealmMoments.Moment moment, Vec3 cam, float rate) {
        Realm realm = moment.realm();
        double radius = moment.wave();
        double toCam = Math.atan2(cam.z - realm.sourceZ(), cam.x - realm.sourceX());
        double arc = Math.min(Math.PI, WAVE_RANGE / Math.max(1.0, radius));
        RealmVisualProfile visual = moment.visual();
        for (int i = count(random, 6.0F * rate); i > 0; i--) {
            double angle = toCam + (random.nextDouble() * 2 - 1) * arc;
            double x = realm.sourceX() + Math.cos(angle) * radius, z = realm.sourceZ() + Math.sin(angle) * radius;
            if (!realm.inCylinder(Mth.floor(x), Mth.floor(z))) continue;
            add(level, type(visual), visual.glowColor(), 1.0F, x, realm.height() + 1.0 + random.nextDouble(), z, 0.0, 0.06, 0.0);
        }
    }

    /** 镜子周围飘浮的絮团，以及碎裂刚发生时只喷发一次的火花。 */
    private static void mirror(ClientLevel level, RandomSource random, RealmMoments.Moment moment, Vec3 cam, float rate) {
        Vec3 at = MirrorAnchors.shatterPoint(moment.realm(), 1.0F);
        if (at.distanceTo(cam) > SHATTER_RANGE) return;
        RealmVisualProfile visual = moment.visual();
        for (int i = count(random, 0.5F * rate); i > 0; i--) {
            flock(level, random, visual, at.x + random.nextGaussian() * 1.2, at.y + random.nextGaussian() * 0.8, at.z + random.nextGaussian() * 1.2);
        }
        float age = moment.shatterAge();
        if (age < 0.0F || age > SHATTER_WINDOW || !BURST.add(moment.realm().id())) return;
        for (int i = Math.round(48 * rate); i > 0; i--) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6 + 0.3, random.nextGaussian()).normalize()
                    .scale(0.3 + random.nextDouble() * 0.5);
            add(level, AvernusParticles.SPARK.get(), visual.glowColor(), 1.0F, at.x, at.y, at.z, dir.x, dir.y, dir.z);
        }
        for (int i = Math.round(12 * rate); i > 0; i--) {
            add(level, type(visual), visual.glowColor(), 1.0F, at.x + random.nextGaussian() * 0.5, at.y + random.nextGaussian() * 0.5,
                    at.z + random.nextGaussian() * 0.5, random.nextGaussian() * 0.05, random.nextDouble() * 0.08, random.nextGaussian() * 0.05);
        }
    }

    private static void flock(ClientLevel level, RandomSource random, RealmVisualProfile visual, double x, double y, double z) {
        int color = FastColor.ARGB32.lerp(0.35F, visual.fogColor(), visual.glowColor());
        add(level, AvernusParticles.FLOCK.get(), color, 0.22F, x, y, z,
                (random.nextDouble() - 0.5) * 0.01, (random.nextDouble() - 0.3) * 0.005, (random.nextDouble() - 0.5) * 0.01);
    }

    private static ParticleType<ColorParticleOption> type(RealmVisualProfile visual) {
        return switch (visual.atmosphere()) {
            case WISP -> AvernusParticles.WISP.get();
            case EMBER -> AvernusParticles.EMBER.get();
            case FROST -> AvernusParticles.FROST.get();
        };
    }

    private static void add(ClientLevel level, ParticleType<ColorParticleOption> type, int rgb, float alpha,
                            double x, double y, double z, double dx, double dy, double dz) {
        level.addParticle(ColorParticleOption.create(type, FastColor.ARGB32.color(Math.round(alpha * 255), rgb)), x, y, z, dx, dy, dz);
    }

    /** 期望值为 expected 的整数：整数部分必出，小数部分按概率补一个。 */
    private static int count(RandomSource random, float expected) {
        int whole = (int) expected;
        return whole + (random.nextFloat() < expected - whole ? 1 : 0);
    }
}
