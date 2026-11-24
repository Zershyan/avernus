package io.zershyan.avernus.registry.packet;

import io.zershyan.avernus.handler.client.RealmClientPackets;
import io.zershyan.avernus.realm.Realm;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class RealmMotionPacket {
    private final ResourceKey<Level> dimension;
    private final long epoch;
    private final long order;
    private final UUID id;
    private final Realm.Motion motion;

    public RealmMotionPacket(ResourceKey<Level> dimension, long epoch, long order, UUID id, Realm.Motion motion) {
        this.dimension = dimension;
        this.epoch = epoch;
        this.order = order;
        this.id = id;
        this.motion = motion;
    }

    public RealmMotionPacket(FriendlyByteBuf buf) {
        this(buf.readResourceKey(Registries.DIMENSION), buf.readVarLong(), buf.readVarLong(), buf.readUUID(), Realm.Motion.read(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
        buf.writeVarLong(epoch);
        buf.writeVarLong(order);
        buf.writeUUID(id);
        motion.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RealmClientPackets.motion(dimension, epoch, order, id, motion)));
    }
}
