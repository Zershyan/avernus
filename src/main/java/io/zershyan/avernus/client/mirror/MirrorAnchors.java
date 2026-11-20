package io.zershyan.avernus.client.mirror;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 客户端镜子位置：开境者胸口与永恒镜片云的平滑锚点。 */
public final class MirrorAnchors {
    /** 每 tick 追赶剩余距离的比例：近处只有底数，远处按距离平方迅速增大，像一根超长的弹性绳。 */
    private static final double FOLLOW_BASE = 0.04;
    private static final double FOLLOW_STRETCH = 0.006;

    /** 永恒镜片云的阻尼锚点：离目标越远追赶越快，无速度项因此不会振荡。 */
    private static final class Anchor {
        Vec3 previous;
        Vec3 current;
        int teleportSeq;

        Anchor(Vec3 at, int sequence) {
            previous = current = at;
            teleportSeq = sequence;
        }

        void follow(Vec3 target, Realm.Motion motion) {
            if (teleportSeq != motion.teleportSeq()) {
                previous = current = motion.head();
                teleportSeq = motion.teleportSeq();
                return;
            }
            previous = current;
            Vec3 delta = target.subtract(current);
            current = current.add(delta.scale(Math.min(1.0, FOLLOW_BASE + FOLLOW_STRETCH * delta.lengthSqr())));
        }

        Vec3 at(float partialTick) {
            return previous.lerp(current, partialTick);
        }
    }

    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();

    private MirrorAnchors() {
    }

    public static void reset() {
        ANCHORS.clear();
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            reset();
            return;
        }
        Set<UUID> alive = new HashSet<>();
        for (Realm realm : RealmSets.of(level).all()) {
            alive.add(realm.id());
            if (!realm.eternal()) continue;
            Realm.Motion motion = realm.motion();
            Entity owner = ownerOf(level, realm);
            Vec3 head = owner != null ? headOf(owner, 1.0F) : motion.head();
            ANCHORS.computeIfAbsent(realm.id(), id -> new Anchor(head, motion.teleportSeq())).follow(head, motion);
        }
        ANCHORS.keySet().retainAll(alive);
    }

    private static @Nullable Entity ownerOf(ClientLevel level, Realm realm) {
        Entity entity = level.getEntity(realm.ownerEntityId());
        return entity != null && entity.getUUID().equals(realm.ownerId()) ? entity : null;
    }

    /** 飞回终点：开境者插值后的胸口，不在视野内时退回同步的位置。 */
    static Vec3 home(ClientLevel level, Realm realm, float partialTick) {
        Entity owner = ownerOf(level, realm);
        return owner != null ? owner.getPosition(partialTick).add(0.0, owner.getBbHeight() * 0.7, 0.0) : realm.motion().chest();
    }

    private static Vec3 headOf(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() + RealmManager.HEAD_CLEARANCE, 0.0);
    }

    /** 永恒镜片云跟随平滑锚点，其余与服务端碎裂位置一致。 */
    public static Vec3 shatterPoint(Realm realm, float partialTick) {
        Anchor anchor = realm.eternal() ? ANCHORS.get(realm.id()) : null;
        return anchor != null && anchor.teleportSeq == realm.teleportSeq() ? anchor.at(partialTick) : realm.shatterPoint();
    }
}
