package io.zershyan.avernus.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import io.zershyan.avernus.registry.particle.RealmParticleOptions;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** 领域粒子：一个类按 {@link Kind} 预设区分运动、尺寸与透明度曲线；全亮绘制，交给泛光提供发光感。 */
public class RealmParticle extends TextureSheetParticle {
    public enum Kind {
        /** 游魂光球：缓升，水平正弦摇摆，渐入渐出。 */
        WISP(60, 100, 0.0F, 0.96F, 0.12F),
        /** 余烬：上升湍流，由亮变暗并缩小、闪烁。 */
        EMBER(30, 60, -0.004F, 0.94F, 0.08F),
        /** 冰晶：缓落旋转，透明度闪烁。 */
        FROST(60, 120, 0.004F, 0.97F, 0.09F),
        /** 碎裂火花：高速射出，强阻尼并受重力。 */
        SPARK(12, 24, 0.03F, 0.82F, 0.06F),
        /** 絮团：大而柔软，几乎不动，缓慢漂移后渐隐。 */
        FLOCK(80, 140, 0.0F, 0.95F, 0.45F);

        final int minLife, maxLife;
        final float gravity, friction, size;

        Kind(int minLife, int maxLife, float gravity, float friction, float size) {
            this.minLife = minLife;
            this.maxLife = maxLife;
            this.gravity = gravity;
            this.friction = friction;
            this.size = size;
        }
    }

    private final Kind kind;
    private final float baseSize, baseAlpha, phase, spin;

    private RealmParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, Kind kind,
                          RealmParticleOptions color, SpriteSet sprites) {
        super(level, x, y, z);
        this.kind = kind;
        xd = dx;
        yd = dy;
        zd = dz;
        gravity = kind.gravity;
        friction = kind.friction;
        hasPhysics = false;
        lifetime = Mth.nextInt(random, kind.minLife, kind.maxLife);
        baseSize = quadSize = kind.size * (0.7F + random.nextFloat() * 0.6F);
        baseAlpha = color.alpha();
        int rgb = color.rgb();
        setColor(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
        phase = random.nextFloat() * Mth.TWO_PI;
        spin = kind == Kind.FROST ? (random.nextFloat() - 0.5F) * 0.2F : 0.0F;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        pickSprite(sprites);
        alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float life = (float) age / lifetime;
        float fade = Math.min(1.0F, Math.min(life * 6.0F, (1.0F - life) * 3.0F));
        oRoll = roll;
        roll += spin;
        switch (kind) {
            case WISP -> {
                xd += Mth.sin(age * 0.15F + phase) * 0.002F;
                zd += Mth.cos(age * 0.13F + phase) * 0.002F;
                yd += 0.0015F;
            }
            case EMBER -> {
                xd += (random.nextFloat() - 0.5F) * 0.01F;
                zd += (random.nextFloat() - 0.5F) * 0.01F;
                quadSize = baseSize * (1.0F - life * 0.7F);
                fade *= 0.75F + 0.25F * Mth.sin(age * 1.3F + phase);
                rCol *= 0.985F;
                gCol *= 0.97F;
                bCol *= 0.97F;
            }
            case FROST -> fade *= 0.7F + 0.3F * Mth.sin(age * 0.6F + phase);
            case SPARK -> quadSize = baseSize * (1.0F - life);
            case FLOCK -> {
                xd += Mth.sin(age * 0.05F + phase) * 0.0006F;
                zd += Mth.cos(age * 0.04F + phase) * 0.0006F;
                quadSize = baseSize * (0.8F + 0.4F * life);
            }
        }
        alpha = baseAlpha * Math.max(0.0F, fade);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public record Provider(Kind kind, SpriteSet sprites) implements ParticleProvider<RealmParticleOptions> {
        @Override
        public Particle createParticle(@NotNull RealmParticleOptions color, @NotNull ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new RealmParticle(level, x, y, z, dx, dy, dz, kind, color, sprites);
        }
    }
}
