package io.zershyan.avernus.client.mirror;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.zershyan.avernus.client.realm_render.RealmVisuals;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.realm.RealmTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 镜子演出：飞出（永恒为升至头顶）→ 转向 → 碎裂镜片飞射并冻结悬停 → 关闭时随波纹收回、重组、飞回。
 * 全部由领域冻结的时间线与 gameTime 推导，加入中途的观察者直接看到当前阶段。
 */
public final class RealmMirrorRenderer {
    private static final int COLS = 4;
    private static final int ROWS = 4;
    private static final float SIZE = 0.9F;
    private static final double HEAD_CLEARANCE = 0.8;
    private static final double SNAP_DISTANCE = 32.0;

    /** 永恒镜片云的阻尼锚点：离目标越远追赶越快，无速度项因此不会振荡。 */
    private static final class Anchor {
        Vec3 previous;
        Vec3 current;

        Anchor(Vec3 at) {
            previous = current = at;
        }

        void follow(Vec3 target) {
            previous = current;
            Vec3 delta = target.subtract(current);
            double distance = delta.length();
            current = distance > SNAP_DISTANCE ? target : current.add(delta.scale(Math.min(1.0, 0.2 + distance * 0.08)));
        }

        Vec3 at(float partialTick) {
            return previous.lerp(current, partialTick);
        }
    }

    /** 某一帧的演出状态；spread 为负表示完整镜子，否则为镜片散开程度。 */
    private record Frame(Vec3 center, float yaw, float spread) {
    }

    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();
    private static final Set<UUID> SHATTERED = new HashSet<>();

    private RealmMirrorRenderer() {
    }

    public static void reset() {
        ANCHORS.clear();
        SHATTERED.clear();
    }

    // region tick

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            reset();
            return;
        }
        long time = level.getGameTime();
        Set<UUID> alive = new HashSet<>();
        for (Realm realm : RealmSets.of(level).all()) {
            alive.add(realm.id());
            Entity owner = level.getPlayerByUUID(realm.ownerId());
            if (realm.eternal() && owner != null) {
                Vec3 head = headOf(owner, 1.0F);
                ANCHORS.computeIfAbsent(realm.id(), id -> new Anchor(head)).follow(head);
            }
            long elapsed = time - realm.startTime() - realm.timeline().shatterDelay();
            // 只在碎裂时刻附近播放，避免中途加入时补播
            if (elapsed >= 0 && elapsed < 2 && realm.closeTime() < 0 && SHATTERED.add(realm.id())) {
                Vec3 at = shatterPoint(realm, 1.0F);
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F, false);
            }
        }
        ANCHORS.keySet().retainAll(alive);
        SHATTERED.retainAll(alive);
    }

    // endregion

    // region 时间线

    private static Vec3 chestOf(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.7, 0.0);
    }

    private static Vec3 headOf(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() + HEAD_CLEARANCE, 0.0);
    }

    private static Vec3 shatterPoint(Realm realm, float partialTick) {
        if (realm.eternal()) {
            Anchor anchor = ANCHORS.get(realm.id());
            return anchor != null ? anchor.at(partialTick) : realm.launchOrigin().add(0.0, HEAD_CLEARANCE, 0.0);
        }
        return realm.launchOrigin().add(Vec3.directionFromRotation(realm.launchPitch(), realm.launchYaw())
                .scale(realm.timeline().flyDistance()));
    }

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - (1.0F - c) * (1.0F - c);
    }

    private static @Nullable Frame frame(Realm realm, @Nullable Entity owner, float t, float partialTick) {
        RealmTimeline timeline = realm.timeline();
        int delay = timeline.shatterDelay();
        float closeAt = realm.closeTime() < 0L ? Float.MAX_VALUE : realm.closeTime() - realm.startTime();
        Vec3 home = owner != null ? chestOf(owner, partialTick) : realm.launchOrigin();
        Vec3 target = shatterPoint(realm, partialTick);
        float facing = realm.launchYaw() + 180.0F;

        // 碎裂前关闭：沿原路倒放飞回
        if (closeAt < delay && t >= closeAt) {
            t = closeAt - (t - closeAt);
            if (t < 0.0F) return null;
        }
        if (t < delay) {
            Vec3 center = home.lerp(target, ease(t / Math.max(1, timeline.flyTicks())));
            float turn = ease((t - timeline.flyTicks()) / Math.max(1, timeline.turnTicks()));
            return new Frame(center, Mth.rotLerp(turn, realm.launchYaw(), facing), -1.0F);
        }
        float scatter = ease((t - delay) / Math.max(1, timeline.scatterTicks()));
        if (t < closeAt) return new Frame(target, facing, scatter);

        float frozen = ease((closeAt - delay) / Math.max(1, timeline.scatterTicks()));
        float reassembleAt = closeAt + realm.contractTicks();
        if (t < reassembleAt) return new Frame(target, facing, frozen);
        float back = (t - reassembleAt) / Math.max(1, timeline.reassembleTicks());
        if (back < 1.0F) return new Frame(target, facing, frozen * (1.0F - ease(back)));
        float fly = (t - reassembleAt - timeline.reassembleTicks()) / Math.max(1, timeline.flyTicks());
        if (fly >= 1.0F) return null;
        return new Frame(target.lerp(home, ease(fly)), facing, -1.0F);
    }

    // endregion

    // region 绘制

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        PoseStack poseStack = event.getPoseStack();
        if (level == null || poseStack == null) return;
        var realms = RealmSets.of(level).all();
        if (realms.isEmpty()) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partialTick;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Realm realm : realms) {
            Frame frame = frame(realm, level.getPlayerByUUID(realm.ownerId()), now - realm.startTime(), partialTick);
            if (frame == null) continue;
            RenderType type = RenderType.entityTranslucent(RealmVisuals.get(realm.aspect()).mirrorTexture());
            VertexConsumer consumer = buffers.getBuffer(type);
            poseStack.pushPose();
            poseStack.translate(frame.center.x - cam.x, frame.center.y - cam.y, frame.center.z - cam.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-frame.yaw));
            if (frame.spread < 0.0F) quad(consumer, poseStack.last(), SIZE / 2, SIZE / 2, 0.0F, 0.0F, 1.0F, 1.0F);
            else shards(consumer, poseStack, realm.seed(), frame.spread, now);
            poseStack.popPose();
            buffers.endBatch(type);
        }
    }

    private static void shards(VertexConsumer consumer, PoseStack poseStack, long seed, float spread, float now) {
        RandomSource random = RandomSource.create(seed);
        float cell = SIZE / COLS;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float lx = (col + 0.5F) * cell - SIZE / 2;
                float ly = SIZE / 2 - (row + 0.5F) * cell;
                Vector3f out = new Vector3f(lx, ly, (random.nextFloat() - 0.5F) * 0.6F)
                        .add(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).normalize()
                        .mul(1.5F + random.nextFloat() * 3.0F);
                Vector3f axis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).normalize();
                float spin = (random.nextFloat() * 2.0F - 1.0F) * Mth.TWO_PI;
                float bob = Mth.sin(now * 0.08F + row * 1.7F + col * 0.9F) * 0.05F * spread;
                poseStack.pushPose();
                poseStack.translate(lx + out.x * spread, ly + out.y * spread + bob, out.z * spread);
                poseStack.mulPose(new Quaternionf().rotationAxis(spin * spread, axis));
                float u0 = (float) col / COLS, v0 = (float) row / ROWS;
                quad(consumer, poseStack.last(), cell / 2, cell / 2, u0, v0, u0 + 1.0F / COLS, v0 + 1.0F / ROWS);
                poseStack.popPose();
            }
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float hw, float hh, float u0, float v0, float u1, float v1) {
        vertex(consumer, pose, -hw, -hh, u0, v1);
        vertex(consumer, pose, hw, -hh, u1, v1);
        vertex(consumer, pose, hw, hh, u1, v0);
        vertex(consumer, pose, -hw, hh, u0, v0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    // endregion
}
