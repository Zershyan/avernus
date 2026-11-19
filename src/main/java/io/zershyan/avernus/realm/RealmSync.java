package io.zershyan.avernus.realm;

import io.zershyan.avernus.registry.packet.RealmMotionPacket;
import io.zershyan.avernus.registry.packet.RealmRemovePacket;
import io.zershyan.avernus.registry.packet.RealmSnapshotPacket;
import io.zershyan.avernus.registry.packet.RealmUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 每位玩家独立订阅附近领域；一个流内的全部包按发送顺序应用。 */
public final class RealmSync {
    private static final int OBSERVATION_MARGIN = 32;
    private static final Map<UUID, Observer> OBSERVERS = new HashMap<>();
    private static long nextEpoch;

    private static final class Observer {
        final ServerPlayer player;
        final ResourceKey<Level> dimension;
        final long epoch;
        long order;
        final Map<UUID, Long> revisions = new HashMap<>();

        Observer(ServerPlayer player) {
            this.player = player;
            this.dimension = player.level().dimension();
            this.epoch = ++nextEpoch;
        }
    }

    private RealmSync() {
    }

    public static void reset(ServerPlayer player) {
        if (player.connection == null) return;
        Observer observer = new Observer(player);
        OBSERVERS.put(player.getUUID(), observer);
        PacketDistributor.sendToPlayer(player, new RealmSnapshotPacket(observer.dimension, observer.epoch));
        sync(observer, RealmSets.of(player.serverLevel()));
    }

    public static void tick(ServerLevel level, RealmSet set) {
        for (ServerPlayer player : level.players()) {
            if (player.connection == null) continue;
            Observer observer = OBSERVERS.get(player.getUUID());
            if (observer == null || observer.player != player || !observer.dimension.equals(level.dimension())) reset(player);
            else sync(observer, set);
        }
    }

    public static void remove(ServerLevel level, Realm realm) {
        for (ServerPlayer player : level.players()) {
            Observer observer = OBSERVERS.get(player.getUUID());
            if (observer != null && observer.player == player && observer.dimension.equals(level.dimension())
                    && observer.revisions.remove(realm.id()) != null) {
                sendRemoval(observer, realm.id());
            }
        }
    }

    private static void sync(Observer observer, RealmSet set) {
        var entry = observer.revisions.entrySet().iterator();
        while (entry.hasNext()) {
            UUID id = entry.next().getKey();
            Realm realm = set.get(id);
            if (realm != null && observes(observer.player, realm)) continue;
            entry.remove();
            sendRemoval(observer, id);
        }
        for (Realm realm : set.all()) {
            if (!observes(observer.player, realm)) continue;
            Realm.Motion motion = realm.motion();
            Long known = observer.revisions.get(realm.id());
            if (known == null) {
                PacketDistributor.sendToPlayer(observer.player, new RealmUpdatePacket(observer.epoch, ++observer.order, realm.snapshot()));
            } else if (known < motion.revision()) {
                PacketDistributor.sendToPlayer(observer.player, new RealmMotionPacket(observer.dimension, observer.epoch, ++observer.order, realm.id(), motion));
            } else continue;
            observer.revisions.put(realm.id(), motion.revision());
        }
    }

    private static boolean observes(ServerPlayer player, Realm realm) {
        if (realm.ownerId().equals(player.getUUID())) return true;
        int distance;
        double x, z;
        if (player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view) {
            distance = view.viewDistance();
            x = view.center().getMiddleBlockX();
            z = view.center().getMiddleBlockZ();
        } else {
            distance = player.server.getPlayerList().getViewDistance();
            x = player.getX();
            z = player.getZ();
        }
        return intersectsView(realm.exactCenterX(), realm.exactCenterZ(), realm.radius(), x, z, distance);
    }

    static boolean intersectsView(double realmX, double realmZ, int radius, double viewX, double viewZ, int chunks) {
        double halfView = (chunks + 2) * 16.0;
        double dx = Math.max(0.0, Math.abs(realmX - viewX) - halfView);
        double dz = Math.max(0.0, Math.abs(realmZ - viewZ) - halfView);
        double reach = radius + OBSERVATION_MARGIN;
        return dx * dx + dz * dz <= reach * reach;
    }

    private static void sendRemoval(Observer observer, UUID id) {
        PacketDistributor.sendToPlayer(observer.player, new RealmRemovePacket(observer.dimension, observer.epoch, ++observer.order, id));
    }

    public static void forget(UUID player) {
        OBSERVERS.remove(player);
    }

    public static void clear() {
        OBSERVERS.clear();
    }
}
