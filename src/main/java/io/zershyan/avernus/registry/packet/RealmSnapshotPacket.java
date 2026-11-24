package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.handler.client.RealmClientPackets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 开始新观察流；完整状态由随后的单领域包逐一补齐。 */
public final class RealmSnapshotPacket {
    private final ResourceKey<Level> dimension;
    private final long epoch;

    public RealmSnapshotPacket(ResourceKey<Level> dimension, long epoch) {
        this.dimension = dimension;
        this.epoch = epoch;
    }

    public RealmSnapshotPacket(FriendlyByteBuf buf) {
        this(buf.readResourceKey(Registries.DIMENSION), buf.readVarLong());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
        buf.writeVarLong(epoch);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RealmClientPackets.begin(dimension, epoch)));
    }
}
