package io.zershyan.avernus.realm;

import io.zershyan.avernus.api.realm.RealmPhase;
import io.zershyan.avernus.api.realm.RealmView;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * 领域权威状态，服务端与客户端共用同一份纯计算：覆盖、阶段与结束时刻都只由 gameTime 推导。
 * 除永恒中心、关闭时刻与传送序号外均在开境时冻结。
 */
public final class Realm implements RealmView {
    public static final StreamCodec<RegistryFriendlyByteBuf, Realm> STREAM_CODEC = StreamCodec.of(Realm::encode, Realm::decode);

    private final UUID id;
    private final UUID ownerId;
    private final ResourceKey<Level> dimension;
    private final MirrorForm form;
    private final MirrorForm.Resolved resolved;
    private final int height;
    private final int radius;
    private final long sequence;
    private final long startTime;
    private final long seed;
    private final RealmTimeline timeline;
    /** 开镜时胸口位置与视线，用于镜子飞出轨迹。 */
    private final Vec3 launchOrigin;
    private final float launchYaw;
    private final float launchPitch;
    /** 波源相对领域中心的水平偏移（永恒为零，随中心移动）。 */
    private final double sourceOffsetX;
    private final double sourceOffsetZ;
    private final boolean eternal;
    private final boolean isolated;
    private final double maxWaveRadius;

    private double centerX;
    private double centerZ;
    private long closeTime = -1L;
    private int teleportSeq;

    public Realm(UUID id, UUID ownerId, ResourceKey<Level> dimension, MirrorForm form, MirrorForm.Resolved resolved,
                 int height, int radius, long sequence, long startTime, long seed, RealmTimeline timeline,
                 Vec3 launchOrigin, float launchYaw, float launchPitch, double centerX, double centerZ,
                 double sourceOffsetX, double sourceOffsetZ) {
        this.id = id;
        this.ownerId = ownerId;
        this.dimension = dimension;
        this.form = form;
        this.resolved = resolved;
        this.height = height;
        this.radius = radius;
        this.sequence = sequence;
        this.startTime = startTime;
        this.seed = seed;
        this.timeline = timeline;
        this.launchOrigin = launchOrigin;
        this.launchYaw = launchYaw;
        this.launchPitch = launchPitch;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.sourceOffsetX = sourceOffsetX;
        this.sourceOffsetZ = sourceOffsetZ;
        this.eternal = resolved.traits().contains(AvernusRealmTraits.ETERNITY.get());
        this.isolated = resolved.traits().contains(AvernusRealmTraits.ISOLATION.get());
        this.maxWaveRadius = radius + Math.sqrt(sourceOffsetX * sourceOffsetX + sourceOffsetZ * sourceOffsetZ) + 1.0;
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
        return Math.abs(x + 0.5 - centerX) <= radius + 1 && Math.abs(z + 0.5 - centerZ) <= radius + 1;
    }

    /** 当前波面半径（按整 tick 离散），未碎裂或已收回时为负。 */
    public double waveRadius(long gameTime) {
        if (!isClosing(gameTime)) return openingWaveRadius(gameTime);
        double atClose = openingWaveRadius(closeTime);
        if (atClose <= 0.0) return -1.0;
        double radiusNow = atClose - (gameTime - closeTime) * waveRate();
        return radiusNow > 0.0 ? radiusNow : -1.0;
    }

    public boolean isClosing(long gameTime) {
        return closeTime >= 0L && gameTime >= closeTime;
    }

    /** 波纹收回所需 tick 数，自关闭时刻起算。 */
    public int contractTicks() {
        if (closeTime < 0L) return 0;
        double atClose = openingWaveRadius(closeTime);
        return atClose <= 0.0 ? 0 : Mth.ceil(atClose / waveRate());
    }

    /** 关闭演出（收束、重组、飞回）完全结束的时刻；未关闭时为 Long.MAX_VALUE。 */
    public long endTime() {
        if (closeTime < 0L) return Long.MAX_VALUE;
        long elapsed = closeTime - startTime;
        if (elapsed < timeline.shatterDelay()) return closeTime + elapsed;
        return closeTime + contractTicks() + timeline.reassembleTicks() + timeline.flyTicks();
    }

    @Override
    public RealmPhase phase(long gameTime) {
        if (isClosing(gameTime)) return RealmPhase.CLOSING;
        long elapsed = gameTime - startTime - timeline.shatterDelay();
        if (elapsed < 0) return RealmPhase.OPENING;
        return elapsed < timeline.waveTicks() ? RealmPhase.EXPANDING : RealmPhase.ACTIVE;
    }

    public boolean inCylinder(int x, int z) {
        double dx = x + 0.5 - centerX;
        double dz = z + 0.5 - centerZ;
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    @Override
    public boolean covers(int x, int z, long gameTime) {
        if (!mayContain(x, z) || !inCylinder(x, z)) return false;
        double wave = waveRadius(gameTime);
        if (wave <= 0.0) return false;
        double dx = x + 0.5 - sourceX();
        double dz = z + 0.5 - sourceZ();
        return dx * dx + dz * dz <= wave * wave;
    }

    /** 几何对该主体是否生效：隔绝只作用于所有者及其拥有的弹射物；无主体查询不受隔绝领域影响。 */
    public boolean geometryAppliesTo(@Nullable Entity subject) {
        if (!isolated) return true;
        if (subject == null) return false;
        if (ownerId.equals(subject.getUUID())) return true;
        return subject instanceof Projectile projectile && projectile.getOwner() != null && ownerId.equals(projectile.getOwner().getUUID());
    }

    // endregion

    // region 可变状态

    public void moveCenter(double x, double z) {
        centerX = x;
        centerZ = z;
    }

    /** 记录关闭请求；重复请求不改变首次关闭时刻。 */
    public boolean requestClose(long gameTime) {
        if (closeTime >= 0L) return false;
        closeTime = gameTime;
        return true;
    }

    public void markTeleported() {
        teleportSeq++;
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

    @Override
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public RealmAspect aspect() {
        return resolved.aspect();
    }

    @Override
    public Set<RealmTrait> traits() {
        return resolved.traits();
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
        return Mth.floor(centerX);
    }

    @Override
    public int centerZ() {
        return Mth.floor(centerZ);
    }

    public double exactCenterX() {
        return centerX;
    }

    public double exactCenterZ() {
        return centerZ;
    }

    public double sourceX() {
        return centerX + sourceOffsetX;
    }

    public double sourceZ() {
        return centerZ + sourceOffsetZ;
    }

    @Override
    public long sequence() {
        return sequence;
    }

    public long startTime() {
        return startTime;
    }

    public long closeTime() {
        return closeTime;
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

    public float launchPitch() {
        return launchPitch;
    }

    public boolean eternal() {
        return eternal;
    }

    public boolean isolated() {
        return isolated;
    }

    public int teleportSeq() {
        return teleportSeq;
    }

    // endregion

    // region 网络

    private static void encode(RegistryFriendlyByteBuf buf, Realm realm) {
        UUIDUtil.STREAM_CODEC.encode(buf, realm.id);
        UUIDUtil.STREAM_CODEC.encode(buf, realm.ownerId);
        buf.writeResourceKey(realm.dimension);
        MirrorForm.STREAM_CODEC.encode(buf, realm.form);
        buf.writeVarInt(realm.height);
        buf.writeVarInt(realm.radius);
        buf.writeVarLong(realm.sequence);
        buf.writeVarLong(realm.startTime);
        buf.writeLong(realm.seed);
        RealmTimeline.STREAM_CODEC.encode(buf, realm.timeline);
        buf.writeVec3(realm.launchOrigin);
        buf.writeFloat(realm.launchYaw);
        buf.writeFloat(realm.launchPitch);
        buf.writeDouble(realm.centerX);
        buf.writeDouble(realm.centerZ);
        buf.writeDouble(realm.sourceOffsetX);
        buf.writeDouble(realm.sourceOffsetZ);
        buf.writeLong(realm.closeTime);
        ByteBufCodecs.VAR_INT.encode(buf, realm.teleportSeq);
    }

    private static Realm decode(RegistryFriendlyByteBuf buf) {
        UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
        UUID owner = UUIDUtil.STREAM_CODEC.decode(buf);
        ResourceKey<Level> dimension = buf.readResourceKey(net.minecraft.core.registries.Registries.DIMENSION);
        MirrorForm form = MirrorForm.STREAM_CODEC.decode(buf);
        MirrorForm.Resolved resolved = form.resolve().orElseThrow(() -> new IllegalStateException("Unresolvable realm form " + form));
        Realm realm = new Realm(id, owner, dimension, form, resolved, buf.readVarInt(), buf.readVarInt(), buf.readVarLong(),
                buf.readVarLong(), buf.readLong(), RealmTimeline.STREAM_CODEC.decode(buf), buf.readVec3(), buf.readFloat(),
                buf.readFloat(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        realm.closeTime = buf.readLong();
        realm.teleportSeq = ByteBufCodecs.VAR_INT.decode(buf);
        return realm;
    }

    // endregion
}
