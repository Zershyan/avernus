package io.zershyan.avernus.handler.client;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** 客户端接收领域同步包；仅在客户端加载。 */
public final class RealmClientPackets {
    private RealmClientPackets() {
    }

    private static Level level(ResourceKey<Level> dimension) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return null;
        Level level = player.level();
        return level.dimension().equals(dimension) ? level : null;
    }

    public static void update(long epoch, long order, Realm.Snapshot snapshot) {
        Level level = level(snapshot.dimension());
        if (level == null) return;
        var set = RealmSets.of(level);
        if (!set.acceptUpdate(epoch, order)) return;
        Realm realm = snapshot.instantiate();
        if (realm != null) set.put(realm);
    }

    public static void remove(ResourceKey<Level> dimension, long epoch, long order, UUID id) {
        Level level = level(dimension);
        if (level == null) return;
        var set = RealmSets.of(level);
        if (set.acceptUpdate(epoch, order)) set.remove(id);
    }

    public static void motion(ResourceKey<Level> dimension, long epoch, long order, UUID id, Realm.Motion motion) {
        Level level = level(dimension);
        if (level == null) return;
        var set = RealmSets.of(level);
        if (!set.acceptUpdate(epoch, order)) return;
        Realm realm = set.get(id);
        if (realm != null) realm.applyMotion(motion);
    }

    public static void begin(ResourceKey<Level> dimension, long epoch) {
        Level level = level(dimension);
        if (level != null) RealmSets.of(level).beginUpdates(epoch);
    }
}
