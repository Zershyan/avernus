package io.zershyan.avernus.realm;

import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmPhase;
import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 领域权威状态，服务端与客户端共用同一份纯计算：覆盖、阶段与结束时刻都只由 gameTime 推导。
 * 除永恒中心、关闭时刻与传送序号外均在开境时冻结。
 */
public final class Realm implements RealmView {
    public record Motion(long revision, long closeTime, double centerX, double centerZ, Vec3 chest, double headY, int teleportSeq) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Motion> STREAM_CODEC = StreamCodec.of((buf, value) -> {
            buf.writeVarLong(value.revision);
            buf.writeLong(value.closeTime);
            buf.writeDouble(value.centerX);
            buf.writeDouble(value.centerZ);
            buf.writeVec3(value.chest);
            buf.writeDouble(value.headY);
            buf.writeVarInt(value.teleportSeq);
        }, buf -> new Motion(buf.readVarLong(), buf.readLong(), buf.readDouble(), buf.readDouble(),
                buf.readVec3(), buf.readDouble(), buf.readVarInt()));

        public Vec3 head() {
            return new Vec3(chest.x, headY, chest.z);
        }
    }

    public record Snapshot(UUID id, UUID ownerId, int ownerEntityId, ResourceKey<Level> dimension, MirrorForm form,
                           int height, int radius, long sequence, long startTime, long seed, RealmTimeline timeline,
                           Vec3 launchOrigin, float launchYaw, double sourceOffsetX, double sourceOffsetZ,
                           Motion motion) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = StreamCodec.of(Realm::encode, Realm::decode);

        public @Nullable Realm instantiate() {
            return MirrorForms.resolve(form).map(resolved -> new Realm(this, resolved)).orElse(null);
        }
    }

    private final UUID id;
    private final UUID ownerId;
    private final int ownerEntityId;
    private final ResourceKey<Level> dimension;
    private final MirrorForm form;
    private final MirrorForms.Resolved resolved;
    private final int height;
    private final int radius;
    private final long sequence;
    private final long startTime;
    private final long seed;
    private final RealmTimeline timeline;
    /** 开镜时胸口位置与视线，用于镜子飞出轨迹。 */
    private final Vec3 launchOrigin;
    private final float launchYaw;
    /** 波源相对领域中心的水平偏移（永恒为零，随中心移动）。 */
    private final double sourceOffsetX;
    private final double sourceOffsetZ;
    private final boolean eternal;
    private final boolean isolated;
    private final double maxWaveRadius;

    private volatile Motion motion;
    private boolean shatterSoundPlayed;

    public Realm(Snapshot state, MirrorForms.Resolved resolved) {
        this.id = state.id;
        this.ownerId = state.ownerId;
        this.ownerEntityId = state.ownerEntityId;
        this.dimension = state.dimension;
        this.form = state.form;
        this.resolved = resolved;
        this.height = state.height;
        this.radius = state.radius;
        this.sequence = state.sequence;
        this.startTime = state.startTime;
        this.seed = state.seed;
        this.timeline = state.timeline;
        this.launchOrigin = state.launchOrigin;
        this.launchYaw = state.launchYaw;
        this.sourceOffsetX = state.sourceOffsetX;
        this.sourceOffsetZ = state.sourceOffsetZ;
        this.motion = state.motion;
        this.eternal = resolved.traits().contains(AvernusRealmTraits.ETERNITY.get());
        this.isolated = resolved.traits().contains(AvernusRealmTraits.ISOLATION.get());
        this.maxWaveRadius = radius + Math.hypot(sourceOffsetX, sourceOffsetZ) + 1.0;
    }

    // region 覆盖与阶段

    private double waveRate() {
        return maxWaveRadius / timeline.waveTicks();
    }

    private double openingWaveRadius(long gameTime) {
        long elapsed = gameTime - startTime - timeline.shatterDelay();
        return elapsed < 0 ? -1.0 : Math.min(maxWaveRadius, elapsed * waveRate());
    }

    /** 领域可能影响的水平外接范围（含整个圆柱），用于快速排除。 */
    public boolean mayContain(int x, int z) {
        return Math.abs(x + 0.5 - motion.centerX()) <= radius + 1 && Math.abs(z + 0.5 - motion.centerZ()) <= radius + 1;
    }

    /** 当前波面半径（按整 tick 离散），未碎裂或已收回时为负。 */
    public double waveRadius(long gameTime) {
        if (!isClosing(gameTime)) return openingWaveRadius(gameTime);
        double atClose = openingWaveRadius(motion.closeTime());
        if (atClose <= 0.0) return -1.0;
        double radiusNow = atClose - (gameTime - motion.closeTime()) * waveRate();
        return radiusNow > 0.0 ? radiusNow : -1.0;
    }

    public boolean isClosing(long gameTime) {
        return motion.closeTime() >= 0L && gameTime >= motion.closeTime();
    }

    /** 波纹收回所需 tick 数，自关闭时刻起算。 */
    public int contractTicks() {
        if (motion.closeTime() < 0L) return 0;
        double atClose = openingWaveRadius(motion.closeTime());
        return atClose <= 0.0 ? 0 : Mth.ceil(atClose / waveRate());
    }

    /** 关闭演出（镜片随波纹收回、裂纹愈合、飞回）完全结束的时刻；未关闭时为 Long.MAX_VALUE。 */
    public long endTime() {
        if (motion.closeTime() < 0L) return Long.MAX_VALUE;
        long elapsed = motion.closeTime() - startTime;
        if (elapsed < timeline.shatterDelay()) return motion.closeTime() + timeline.flyTicks();
        return motion.closeTime() + contractTicks() + timeline.crackTicks() + timeline.flyTicks();
    }

    @Override
    public RealmPhase phase(long gameTime) {
        if (isClosing(gameTime)) return RealmPhase.CLOSING;
        long elapsed = gameTime - startTime - timeline.shatterDelay();
        if (elapsed < 0) return RealmPhase.OPENING;
        return elapsed < timeline.waveTicks() ? RealmPhase.EXPANDING : RealmPhase.ACTIVE;
    }

    public boolean inCylinder(int x, int z) {
        double dx = x + 0.5 - motion.centerX();
        double dz = z + 0.5 - motion.centerZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    @Override
    public boolean covers(int x, int z, long gameTime) {
        if (!mayContain(x, z) || !inCylinder(x, z)) return false;
        double wave = waveRadius(gameTime);
        return wave > 0.0 && inDisc(x, z, sourceX(), sourceZ(), wave);
    }

    public Coverage coverage(long gameTime) {
        return new Coverage(exactCenterX(), exactCenterZ(), radius, sourceX(), sourceZ(), waveRadius(gameTime));
    }

    private static boolean inDisc(int x, int z, double cx, double cz, double radius) {
        double dx = x + 0.5 - cx;
        double dz = z + 0.5 - cz;
        return dx * dx + dz * dz <= radius * radius;
    }

    public record Coverage(double centerX, double centerZ, int radius, double sourceX, double sourceZ, double wave) {
        public boolean covers(int x, int z) {
            return wave > 0.0 && inDisc(x, z, centerX, centerZ, radius) && inDisc(x, z, sourceX, sourceZ, wave);
        }

        /** -1 为无交集，0 为局部覆盖，1 为全部覆盖。 */
        public int tile(int x, int z, int size) {
            if (wave <= 0.0) return -1;
            int disc = discTile(x, z, size, centerX, centerZ, radius);
            int front = discTile(x, z, size, sourceX, sourceZ, wave);
            return Math.min(disc, front);
        }

        private static int discTile(int x, int z, int size, double cx, double cz, double radius) {
            double x0 = x + 0.5 - cx, x1 = x0 + size - 1;
            double z0 = z + 0.5 - cz, z1 = z0 + size - 1;
            double nx = Math.max(0.0, Math.max(x0, -x1)), nz = Math.max(0.0, Math.max(z0, -z1));
            double r2 = radius * radius;
            if (nx * nx + nz * nz > r2) return -1;
            double fx = Math.max(Math.abs(x0), Math.abs(x1)), fz = Math.max(Math.abs(z0), Math.abs(z1));
            return fx * fx + fz * fz <= r2 ? 1 : 0;
        }
    }

    /**
     * 几何对该主体是否生效：脚底已落到境面层以下的主体留在真实世界（由平等类型负责抬升），
     * 其余按隔绝规则判定。
     */
    public boolean geometryAppliesTo(@Nullable Entity subject) {
        return (subject == null || !belowSurface(subject)) && reaches(subject);
    }

    /** 脚底低于境面层中线即视为在境面以下。 */
    public boolean belowSurface(Entity subject) {
        return subject.getY() < height - 1.5;
    }

    /** 隔绝只作用于所有者及其拥有的弹射物；无主体查询不受隔绝领域影响。 */
    public boolean reaches(@Nullable Entity subject) {
        if (!isolated) return true;
        if (subject == null) return false;
        if (ownerId.equals(subject.getUUID())) return true;
        return subject instanceof Projectile projectile && projectile.getOwner() != null && ownerId.equals(projectile.getOwner().getUUID());
    }

    // endregion

    // region 可变状态

    public Motion motion() {
        return motion;
    }

    public Snapshot snapshot() {
        return new Snapshot(id, ownerId, ownerEntityId, dimension, form, height, radius, sequence, startTime, seed,
                timeline, launchOrigin, launchYaw, sourceOffsetX, sourceOffsetZ, motion);
    }

    public void applyMotion(Motion next) {
        if (next.revision() > motion.revision()) motion = next;
    }

    public boolean moveAnchor(Vec3 chest, double headY, boolean snap) {
        Motion old = motion;
        if (!snap && old.chest().equals(chest) && old.headY() == headY) return false;
        motion = new Motion(old.revision() + 1, old.closeTime(), eternal ? chest.x : old.centerX(),
                eternal ? chest.z : old.centerZ(), chest, headY, old.teleportSeq() + (snap ? 1 : 0));
        return true;
    }

    /** 记录关闭请求；重复请求不改变首次关闭时刻。 */
    public boolean requestClose(long gameTime) {
        Motion old = motion;
        if (old.closeTime() >= 0L) return false;
        motion = new Motion(old.revision() + 1, gameTime, old.centerX(), old.centerZ(), old.chest(), old.headY(), old.teleportSeq());
        return true;
    }

    public boolean shouldPlayShatter(long gameTime) {
        if (shatterSoundPlayed || gameTime < shatterTime() || !shatters()) return false;
        shatterSoundPlayed = true;
        return true;
    }

    public long shatterTime() {
        return startTime + timeline.shatterDelay();
    }

    /** 碎裂前关闭的镜子沿原路飞回，不会碎裂。 */
    public boolean shatters() {
        return closeTime() < 0L || closeTime() > shatterTime();
    }

    /** 碎裂位置：永恒在头顶，否则从眼睛沿开镜朝向水平飞出。 */
    public Vec3 shatterPoint() {
        return eternal ? motion.head() : launchOrigin.add(Vec3.directionFromRotation(0.0F, launchYaw).scale(timeline.flyDistance()));
    }

    /** 波面覆盖整个圆柱所需的半径。 */
    public double maxWaveRadius() {
        return maxWaveRadius;
    }

    // endregion

    // region 访问器

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public UUID ownerId() {
        return ownerId;
    }

    public int ownerEntityId() {
        return ownerEntityId;
    }
    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public ResourceLocation aspectId() {
        return form.aspect();
    }

    @Override
    public List<ResourceLocation> traitIds() {
        return form.traits();
    }

    public RealmAspect aspect() {
        return resolved.aspect();
    }

    public Set<RealmTrait> traits() {
        return resolved.traits();
    }

    public boolean hasTrait(RealmTrait trait) {
        return resolved.traits().contains(trait);
    }

    public MirrorForm form() {
        return form;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int radius() {
        return radius;
    }

    @Override
    public int centerX() {
        return Mth.floor(motion.centerX());
    }

    @Override
    public int centerZ() {
        return Mth.floor(motion.centerZ());
    }

    public double exactCenterX() {
        return motion.centerX();
    }

    public double exactCenterZ() {
        return motion.centerZ();
    }

    public double sourceX() {
        return motion.centerX() + sourceOffsetX;
    }

    public double sourceZ() {
        return motion.centerZ() + sourceOffsetZ;
    }

    @Override
    public long sequence() {
        return sequence;
    }

    public long startTime() {
        return startTime;
    }

    public long closeTime() {
        return motion.closeTime();
    }

    public long seed() {
        return seed;
    }

    public RealmTimeline timeline() {
        return timeline;
    }

    public Vec3 launchOrigin() {
        return launchOrigin;
    }

    public float launchYaw() {
        return launchYaw;
    }

    public boolean eternal() {
        return eternal;
    }

    public boolean isolated() {
        return isolated;
    }

    public int teleportSeq() {
        return motion.teleportSeq();
    }

    // endregion

    // region 网络

    private static void encode(RegistryFriendlyByteBuf buf, Snapshot realm) {
        UUIDUtil.STREAM_CODEC.encode(buf, realm.id);
        UUIDUtil.STREAM_CODEC.encode(buf, realm.ownerId);
        buf.writeVarInt(realm.ownerEntityId);
        buf.writeResourceKey(realm.dimension);
        MirrorForms.STREAM_CODEC.encode(buf, realm.form);
        buf.writeVarInt(realm.height);
        buf.writeVarInt(realm.radius);
        buf.writeVarLong(realm.sequence);
        buf.writeVarLong(realm.startTime);
        buf.writeLong(realm.seed);
        RealmTimeline.STREAM_CODEC.encode(buf, realm.timeline);
        buf.writeVec3(realm.launchOrigin);
        buf.writeFloat(realm.launchYaw);
        buf.writeDouble(realm.sourceOffsetX);
        buf.writeDouble(realm.sourceOffsetZ);
        Motion.STREAM_CODEC.encode(buf, realm.motion);
    }

    private static Snapshot decode(RegistryFriendlyByteBuf buf) {
        return new Snapshot(UUIDUtil.STREAM_CODEC.decode(buf), UUIDUtil.STREAM_CODEC.decode(buf), buf.readVarInt(),
                buf.readResourceKey(net.minecraft.core.registries.Registries.DIMENSION), MirrorForms.STREAM_CODEC.decode(buf),
                buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readLong(),
                RealmTimeline.STREAM_CODEC.decode(buf), buf.readVec3(), buf.readFloat(),
                buf.readDouble(), buf.readDouble(), Motion.STREAM_CODEC.decode(buf));
    }

    // endregion
    }
