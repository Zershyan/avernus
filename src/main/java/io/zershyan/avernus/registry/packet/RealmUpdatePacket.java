package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.handler.client.RealmClientPackets;
import io.zershyan.avernus.realm.Realm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 玩家开始观察一个领域时收到一次完整状态。 */
public final class RealmUpdatePacket {
    private final long epoch;
    private final long order;
    private final Realm.Snapshot snapshot;

    public RealmUpdatePacket(long epoch, long order, Realm.Snapshot snapshot) {
        this.epoch = epoch;
        this.order = order;
        this.snapshot = snapshot;
    }

    public RealmUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readVarLong(), buf.readVarLong(), Realm.Snapshot.read(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(epoch);
        buf.writeVarLong(order);
        snapshot.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RealmClientPackets.update(epoch, order, snapshot)));
    }
}
