package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.handler.client.RealmClientPackets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class RealmRemovePacket {
    private final ResourceKey<Level> dimension;
    private final long epoch;
    private final long order;
    private final UUID id;

    public RealmRemovePacket(ResourceKey<Level> dimension, long epoch, long order, UUID id) {
        this.dimension = dimension;
        this.epoch = epoch;
        this.order = order;
        this.id = id;
    }

    public RealmRemovePacket(FriendlyByteBuf buf) {
        this(buf.readResourceKey(Registries.DIMENSION), buf.readVarLong(), buf.readVarLong(), buf.readUUID());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
        buf.writeVarLong(epoch);
        buf.writeVarLong(order);
        buf.writeUUID(id);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RealmClientPackets.remove(dimension, epoch, order, id)));
    }
}
