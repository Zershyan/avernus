package io.zershyan.avernus.client.mirror;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.zershyan.avernus.client.realm_render.RealmVisualProfile;
import io.zershyan.avernus.client.realm_render.RealmVisuals;
import io.zershyan.avernus.realm.MirrorChoreography;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.registry.AvernusShaders;
import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import io.zershyan.avernus.registry.entity.MirrorEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 按领域编舞插值绘制镜子：完整时是由贴图轮廓挤出的立体镜子；碎裂时整面镜子（含镜框）裂成带薄厚度的镜片，以下边框中点为扇轴向外散开，
 * 离下边框越远飞得越远，底部镜片只稍微挪动。
 */
public class MirrorEntityRenderer extends EntityRenderer<MirrorEntity> {
    static final float SIZE = 1.5F;
    private static final float HALF = SIZE / 2;
    /** 受力点偏离镜面中心的最大范围（格）。 */
    private static final float IMPACT_OFFSET = 0.3F;
    /** 每条边额外长出一条主裂纹的概率。 */
    private static final float SIDE_SPOKE_CHANCE = 0.5F;
    /** 环形裂纹在主裂纹上的位置范围（受力点到边缘的比例）。 */
    private static final float RING_MIN = 0.35F;
    private static final float RING_MAX = 0.6F;
    /** UV1.y 的标志位，与镜子着色器一致。 */
    private static final int GHOST = 256, HIDE_Y = 512, HIDE_Z = 1024;
    /** 镜面四角，逆时针。 */
    private static final Vector3f FRONT = new Vector3f(0.0F, 0.0F, 1.0F), BACK = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector2f[] CORNERS = {
            new Vector2f(-HALF, -HALF), new Vector2f(HALF, -HALF), new Vector2f(HALF, HALF), new Vector2f(-HALF, HALF)};
    private static final float RANGE_MIN = 1.8F;
    private static final float RANGE_MAX = 2.4F;
    /** 紧贴下边框的镜片的位移比例。 */
    private static final float BASE_RANGE = 0.25F;
    /** 横向位移放大，让两侧镜片张开成扇面而不是收拢成蛋形。 */
    private static final float SIDE_BOOST = 1.5F;
    private static final Vector2f PIVOT = new Vector2f(0.0F, -HALF);
    /** 镜片随射出方向偏转成扇骨的比例。 */
    private static final float FAN_TILT = 0.3F;
    /** 相邻残影间隔的 tick 数。 */
    private static final float GHOST_LAG = 0.75F;
    /** 飞散镜片的半厚度（格）。 */
    private static final float SHARD_HALF_THICKNESS = 0.02F;
    /** 镜片内部点的重心坐标，离所有边都远，着色器不会在立体外壳上画线。 */
    private static final int INTERIOR = 85;
    /** 碎裂前颤动的最大幅度（格）。 */
    private static final float TREMBLE = 0.05F;

    public MirrorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull MirrorEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffers, int packedLight) {
        Realm realm = entity.realm();
        if (realm == null || !(entity.level() instanceof ClientLevel level)) return;
        float t = (float) (level.getGameTime() - realm.startTime()) + partialTick;
        Vec3 home = MirrorAnchors.home(level, realm, partialTick);
        Vec3 target = MirrorAnchors.shatterPoint(realm, partialTick);
        MirrorChoreography.Pose pose = MirrorChoreography.at(realm, home, target, t);
        if (pose == null) return;
        Vec3 origin = entity.getPosition(partialTick);
        ResourceLocation texture = getTextureLocation(entity);
        RealmFxQuality quality = ClientConfig.quality();
        if (quality == RealmFxQuality.OFF || AvernusShaders.Effect.MIRROR.get() == null) {
            draw(buffers.getBuffer(RenderType.entityTranslucent(texture)), poseStack, texture, realm.seed(), pose, origin, t, null);
            return;
        }
        int glow = RealmVisuals.get(realm.aspect()).glowColor();
        boolean detailed = quality.atLeast(RealmFxQuality.MEDIUM);
        draw(buffers.getBuffer(MirrorRenderTypes.body(texture)), poseStack, texture, realm.seed(), pose, origin, t,
                new Style(glow, 255, pose.crack(), detailed && !pose.whole(), false));
        VertexConsumer ghosts = buffers.getBuffer(MirrorRenderTypes.ghost(texture));
        for (int k = 1; k <= quality.mirrorGhosts; k++) {
            MirrorChoreography.Pose ghost = MirrorChoreography.at(realm, home, target, t - k * GHOST_LAG);
            // 悬停冻结时姿态不变，不画残影
            if (ghost == null || ghost.equals(pose)) break;
            int alpha = (int) (127.5F * (1.0F - k / (quality.mirrorGhosts + 1.0F)));
            draw(ghosts, poseStack, texture, realm.seed(), ghost, origin, t - k * GHOST_LAG, new Style(glow, alpha, 0.0F, !ghost.whole(), true));
        }
    }

    /**
     * 顶点附带的着色参数：颜色为境相辉光，UV1 为（裂纹量，描边量 | 残影与隐藏边标志位），UV2 为三角形重心坐标。
     * 为空时按原版实体着色器的格式写入。
     */
    private record Style(int glow, int alpha, float crack, boolean outline, boolean ghost) {
    }

    /** 完整镜子画立体外壳加凹进的镜面；碎开后每块镜片是带薄厚度的板。 */
    private static void draw(VertexConsumer consumer, PoseStack poseStack, ResourceLocation texture, long seed, MirrorChoreography.Pose pose,
                             Vec3 origin, float time, @Nullable Style style) {
        Vec3 offset = pose.center().subtract(origin);
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-pose.yaw()));
        if (pose.whole() && pose.crack() > 0.0F) tremble(poseStack, pose.crack(), time);
        if (pose.whole()) {
            PoseStack.Pose last = poseStack.last();
            for (MirrorSolid.Face face : MirrorSolid.of(texture).faces()) {
                float[] xyz = face.xyz(), uv = face.uv();
                for (int i = 0; i < 4; i++) {
                    vertex(consumer, last, xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2], uv[i * 2], uv[i * 2 + 1], face.normal(),
                            style, 0, INTERIOR, INTERIOR);
                }
            }
        }
        shards(consumer, poseStack, seed, Math.max(0.0F, pose.spread()), pose.whole(), style);
        poseStack.popPose();
    }

    /** 碎裂前蓄势：越临近碎裂颤得越凶，镜面微微内缩。 */
    private static void tremble(PoseStack poseStack, float crack, float time) {
        float tension = crack * crack * crack;
        float amplitude = TREMBLE * tension;
        poseStack.translate(Mth.sin(time * 9.1F) * amplitude, Mth.sin(time * 7.3F + 1.0F) * amplitude, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(Mth.sin(time * 11.7F) * amplitude * 0.6F));
        float squeeze = 1.0F - 0.04F * tension;
        poseStack.scale(squeeze, squeeze, squeeze);
    }

    /**
     * 按真实玻璃的碎法切分：偏离中心的受力点向四角和若干随机边点放射出主裂纹，
     * 每条主裂纹在随机位置被一圈环形裂纹截断，内圈是尖楔、外圈是梯形块。
     */
    private static void shards(VertexConsumer consumer, PoseStack poseStack, long seed, float spread, boolean whole, @Nullable Style style) {
        RandomSource random = RandomSource.create(seed);
        Vector2f impact = new Vector2f((random.nextFloat() - 0.5F) * IMPACT_OFFSET, (random.nextFloat() - 0.5F) * IMPACT_OFFSET);
        List<Vector2f> rim = new ArrayList<>();
        for (int side = 0; side < 4; side++) {
            Vector2f from = CORNERS[side], to = CORNERS[(side + 1) % 4];
            rim.add(from);
            if (random.nextFloat() < SIDE_SPOKE_CHANCE) rim.add(new Vector2f(from).lerp(to, Mth.lerp(random.nextFloat(), 0.3F, 0.7F)));
        }
        int spokes = rim.size();
        Vector2f[] ring = new Vector2f[spokes];
        for (int i = 0; i < spokes; i++) ring[i] = new Vector2f(impact).lerp(rim.get(i), Mth.lerp(random.nextFloat(), RING_MIN, RING_MAX));
        for (int i = 0; i < spokes; i++) {
            int next = (i + 1) % spokes;
            shard(consumer, poseStack, random, spread, whole, style, impact, ring[i], ring[next]);
            shard(consumer, poseStack, random, spread, whole, style, ring[i], rim.get(i), rim.get(next), ring[next]);
        }
    }

    /**
     * 凸多边形镜片绕自身重心偏转，沿扇轴到重心的方向射出，射程随离下边框的高度增长；spread 为 0 时拼回整面镜子。
     * 完整镜子时只画凹进的镜面，外壳由 {@link MirrorSolid} 提供；碎开后画成带薄厚度的板。
     */
    private static void shard(VertexConsumer consumer, PoseStack poseStack, RandomSource random, float spread, boolean whole,
                              @Nullable Style style, Vector2f... polygon) {
        Vector2f center = new Vector2f();
        for (Vector2f point : polygon) center.add(point);
        center.div(polygon.length);
        Vector2f arm = new Vector2f(center).sub(PIVOT);
        float reach = spread * Mth.lerp((float) Math.pow(arm.y / SIZE, 0.6), BASE_RANGE, 1.0F);
        Vector2f out = new Vector2f(arm).normalize().mul(Mth.lerp(random.nextFloat(), RANGE_MIN, RANGE_MAX) * reach);
        out.x *= SIDE_BOOST;
        float depth = (random.nextFloat() - 0.5F) * 0.3F * reach;
        float flutter = (random.nextFloat() - 0.5F) * 1.2F * reach;
        float twist = (random.nextFloat() - 0.5F) * 0.8F * reach;
        poseStack.pushPose();
        poseStack.translate(center.x + out.x, center.y + out.y, depth);
        poseStack.mulPose(new Quaternionf().rotationZ(-(float) Math.atan2(arm.x, arm.y) * FAN_TILT * reach + twist).rotateY(flutter));
        PoseStack.Pose last = poseStack.last();
        if (whole) {
            face(consumer, last, polygon, center, MirrorSolid.GLASS, FRONT, style);
        } else {
            face(consumer, last, polygon, center, SHARD_HALF_THICKNESS, FRONT, style);
            face(consumer, last, polygon, center, -SHARD_HALF_THICKNESS, BACK, style);
            rim(consumer, last, polygon, center, style);
        }
        poseStack.popPose();
    }

    /** 镜片一面：以首点扇形三角化，三角形内部的对角线标记为非边缘，着色器不在那里画线。 */
    private static void face(VertexConsumer consumer, PoseStack.Pose pose, Vector2f[] polygon, Vector2f center, float z, Vector3f normal,
                             @Nullable Style style) {
        int last = polygon.length - 1;
        for (int i = 1; i < last; i++) {
            // 边 0-i 与 0-(i+1) 是否为内部对角线，分别对应重心坐标第三、第二分量
            int hidden = (i > 1 ? HIDE_Z : 0) | (i + 1 < last ? HIDE_Y : 0);
            corner(consumer, pose, polygon[0], center, z, normal, style, hidden, 255, 0);
            corner(consumer, pose, polygon[i], center, z, normal, style, hidden, 0, 255);
            corner(consumer, pose, polygon[i + 1], center, z, normal, style, hidden, 0, 0);
            corner(consumer, pose, polygon[i + 1], center, z, normal, style, hidden, 0, 0);
        }
    }

    /** 镜片侧边：每条边一个窄面，取边中点的贴图颜色。 */
    private static void rim(VertexConsumer consumer, PoseStack.Pose pose, Vector2f[] polygon, Vector2f center, @Nullable Style style) {
        float t = SHARD_HALF_THICKNESS;
        for (int i = 0; i < polygon.length; i++) {
            Vector2f a = polygon[i], b = polygon[(i + 1) % polygon.length];
            if (a.equals(b)) continue;
            Vector3f normal = new Vector3f(b.y - a.y, a.x - b.x, 0.0F).normalize();
            float u = ((a.x + b.x) / 2 + HALF) / SIZE, v = (HALF - (a.y + b.y) / 2) / SIZE;
            vertex(consumer, pose, a.x - center.x, a.y - center.y, -t, u, v, normal, style, 0, INTERIOR, INTERIOR);
            vertex(consumer, pose, b.x - center.x, b.y - center.y, -t, u, v, normal, style, 0, INTERIOR, INTERIOR);
            vertex(consumer, pose, b.x - center.x, b.y - center.y, t, u, v, normal, style, 0, INTERIOR, INTERIOR);
            vertex(consumer, pose, a.x - center.x, a.y - center.y, t, u, v, normal, style, 0, INTERIOR, INTERIOR);
        }
    }

    /** 镜面上的点：贴图按镜面位置取样，顶点相对镜片中心绘制。 */
    private static void corner(VertexConsumer consumer, PoseStack.Pose pose, Vector2f point, Vector2f center, float z, Vector3f normal,
                               @Nullable Style style, int hidden, int baryX, int baryY) {
        vertex(consumer, pose, point.x - center.x, point.y - center.y, z, (point.x + HALF) / SIZE, (HALF - point.y) / SIZE, normal,
                style, hidden, baryX, baryY);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, Vector3f normal,
                               @Nullable Style style, int hidden, int baryX, int baryY) {
        consumer.addVertex(pose, x, y, z);
        if (style == null) {
            consumer.setColor(255, 255, 255, 255).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
        } else {
            consumer.setColor(style.glow >> 16 & 255, style.glow >> 8 & 255, style.glow & 255, style.alpha)
                    .setUv1((int) (style.crack * 255.0F), (style.outline ? 255 : 0) + (style.ghost ? GHOST : 0) + hidden)
                    .setUv2(baryX, baryY);
        }
        consumer.setUv(u, v).setNormal(pose, normal.x, normal.y, normal.z);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MirrorEntity entity) {
        Realm realm = entity.realm();
        return (realm == null ? RealmVisualProfile.FALLBACK : RealmVisuals.get(realm.aspect())).mirrorTexture();
    }
}
