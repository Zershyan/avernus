package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.entity.MirrorEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;

/**
 * 摄像机身处领域已覆盖的范围时，境外一概不绘制：覆盖范围（圆柱与波面圆盘的交集）之外的分区、实体与方块实体被剔除，
 * 沿覆盖边界立一圈朝内的纯黑墙，头顶仍是境相天空。展开与收缩时边界随波面移动。
 */
public final class RealmEnclosure {
    /** 墙面每段弧长（格）。 */
    private static final double SEGMENT = 2.0;
    /** 墙比覆盖边界略向外，避免与边缘列的境面重叠闪烁。 */
    private static final double WALL_MARGIN = 0.5;

    private static @Nullable Realm enclosing;
    private static double wave;

    private RealmEnclosure() {
    }

    /** 每帧开始时按摄像机位置确定封闭的领域与插值后的波面半径。 */
    public static void update(ClientLevel level, Camera camera) {
        enclosing = null;
        Vec3 pos = camera.getPosition();
        long time = level.getGameTime();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        for (Realm realm : RealmSets.of(level).all()) {
            if (!realm.geometryAppliesTo(camera.getEntity()) || pos.y < realm.height() - 1) continue;
            double now = realm.waveRadius(time), next = realm.waveRadius(time + 1);
            double radius = now > 0.0 && next > 0.0 ? Mth.lerp(partialTick, now, next) : now;
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
    public static boolean keeps(RenderSection section) {
        Realm realm = enclosing;
        if (realm == null) return true;
        BlockPos origin = section.getOrigin();
        return origin.getY() + 16 > realm.height() - 2 && overlaps(realm, origin.getX(), origin.getZ(), origin.getX() + 16, origin.getZ() + 16);
    }

    /** 实体所在列被覆盖才绘制；领域的镜子不受限制。 */
    public static boolean keeps(Entity entity) {
        return entity instanceof MirrorEntity || keeps(entity.getX(), entity.getZ());
    }

    public static boolean keeps(BlockPos pos) {
        return keeps(pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    private static boolean keeps(double x, double z) {
        Realm realm = enclosing;
        return realm == null || inside(x - realm.exactCenterX(), z - realm.exactCenterZ(), realm.radius() + 1.0)
                && inside(x - realm.sourceX(), z - realm.sourceZ(), wave + 1.0);
    }

    private static boolean inside(double dx, double dz, double radius) {
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean overlaps(Realm realm, double x0, double z0, double x1, double z1) {
        return boxMeetsDisc(realm.exactCenterX(), realm.exactCenterZ(), realm.radius() + 1.0, x0, z0, x1, z1)
                && boxMeetsDisc(realm.sourceX(), realm.sourceZ(), wave + 1.0, x0, z0, x1, z1);
    }

    private static boolean boxMeetsDisc(double cx, double cz, double radius, double x0, double z0, double x1, double z1) {
        return inside(Math.max(0.0, Math.max(x0 - cx, cx - x1)), Math.max(0.0, Math.max(z0 - cz, cz - z1)), radius);
    }

    /** 从摄像机沿水平方向到圆的出射距离；摄像机在圆内，所以恒有正解。 */
    private static double exit(double ox, double oz, double dx, double dz, double radius) {
        double b = ox * dx + oz * dz;
        return -b + Math.sqrt(Math.max(0.0, b * b - (ox * ox + oz * oz - radius * radius)));
    }

    /** 在实心方块之后绘制：写入深度，挡住其后的一切。 */
    public static void render(RenderLevelStageEvent event) {
        Realm realm = enclosing;
        var shader = GameRenderer.getPositionColorShader();
        if (realm == null || shader == null) return;
        var mc = Minecraft.getInstance();
        Vec3 eye = event.getCamera().getPosition();
        float bottom = (float) (realm.height() - 2 - eye.y);
        float top = (float) (mc.level.getMaxBuildHeight() + 64 - eye.y);
        double cylinderX = eye.x - realm.exactCenterX(), cylinderZ = eye.z - realm.exactCenterZ();
        double waveX = eye.x - realm.sourceX(), waveZ = eye.z - realm.sourceZ();
        double cylinder = realm.radius() + WALL_MARGIN, disc = wave + WALL_MARGIN;
        int segments = Math.max(16, Mth.ceil(Mth.TWO_PI * Math.min(cylinder, disc) / SEGMENT));
        // 覆盖范围是两圆之交，为凸集，从内部的摄像机按角度投射即得其边界
        float[] xs = new float[segments + 1], zs = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = Mth.TWO_PI * i / segments, dx = Math.cos(angle), dz = Math.sin(angle);
            double reach = Math.min(exit(cylinderX, cylinderZ, dx, dz, cylinder), exit(waveX, waveZ, dx, dz, disc));
            xs[i] = (float) (dx * reach);
            zs[i] = (float) (dz * reach);
        }
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            buffer.addVertex(xs[i], bottom, zs[i]).setColor(0xFF000000);
            buffer.addVertex(xs[i], top, zs[i]).setColor(0xFF000000);
            buffer.addVertex(xs[i + 1], top, zs[i + 1]).setColor(0xFF000000);
            buffer.addVertex(xs[i + 1], bottom, zs[i + 1]).setColor(0xFF000000);
        }
        var mesh = buffer.build();
        if (mesh == null) return;
        boolean clamped = glIsEnabled(GL_DEPTH_CLAMP);
        if (!clamped) glEnable(GL_DEPTH_CLAMP);
        RenderSystem.disableCull();
        try {
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, event.getModelViewMatrix(), event.getProjectionMatrix(), mc.getWindow());
            shader.apply();
            BufferUploader.draw(mesh);
        } finally {
            shader.clear();
            VertexBuffer.unbind();
            RenderSystem.enableCull();
            if (!clamped) glDisable(GL_DEPTH_CLAMP);
        }
    }
}
