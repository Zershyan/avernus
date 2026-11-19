package io.zershyan.avernus.registry.entity;

import io.zershyan.avernus.realm.MirrorChoreography;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** 领域镜子的世界实体：位置由服务端按领域编舞驱动，外观由客户端按同一编舞插值绘制；不存档。 */
public class MirrorEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> REALM = SynchedEntityData.defineId(MirrorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** 镜片扇形射出后超出碰撞箱的余量。 */
    private static final double CULL_MARGIN = 1.6;

    public MirrorEntity(EntityType<? extends MirrorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static MirrorEntity spawn(ServerLevel level, Realm realm) {
        MirrorEntity mirror = new MirrorEntity(AvernusEntities.MIRROR.get(), level);
        mirror.entityData.set(REALM, Optional.of(realm.id()));
        mirror.moveTo(realm.launchOrigin().subtract(0.0, mirror.getBbHeight() / 2, 0.0));
        level.addFreshEntity(mirror);
        return mirror;
    }

    public @Nullable Realm realm() {
        return entityData.get(REALM).map(id -> RealmSets.of(level()).get(id)).orElse(null);
    }

    @Override
    public void tick() {
        if (!(level() instanceof ServerLevel level)) return;
        Realm realm = realm();
        Entity owner = realm == null ? null : level.getEntity(realm.ownerId());
        MirrorChoreography.Pose pose = realm == null ? null : MirrorChoreography.at(realm,
                owner != null ? RealmManager.chestOf(owner) : realm.motion().chest(), realm.shatterPoint(),
                level.getGameTime() - realm.startTime());
        if (pose == null) {
            discard();
            return;
        }
        setPos(pose.center().subtract(0.0, getBbHeight() / 2, 0.0));
        setYRot(pose.yaw());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(REALM, Optional.empty());
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CULL_MARGIN);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void push(@NotNull Vec3 vector) {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }
}
