package io.zershyan.avernus.realm;

import io.zershyan.avernus.registry.config.ServerConfig;
import net.minecraft.network.FriendlyByteBuf;

/** 开境时冻结的演出时长快照，随领域同步，客户端不得自行改变节奏。 */
public record RealmTimeline(int flyTicks, int turnTicks, int crackTicks, int scatterTicks, int waveTicks, float flyDistance) {
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(flyTicks);
        buf.writeVarInt(turnTicks);
        buf.writeVarInt(crackTicks);
        buf.writeVarInt(scatterTicks);
        buf.writeVarInt(waveTicks);
        buf.writeFloat(flyDistance);
    }

    public static RealmTimeline read(FriendlyByteBuf buf) {
        return new RealmTimeline(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public static RealmTimeline fromConfig() {
        return new RealmTimeline(
                ServerConfig.MIRROR_FLY_TICKS.get(),
                ServerConfig.MIRROR_TURN_TICKS.get(),
                ServerConfig.MIRROR_CRACK_TICKS.get(),
                ServerConfig.SHARD_SCATTER_TICKS.get(),
                ServerConfig.WAVE_TICKS.get(),
                ServerConfig.MIRROR_FLY_DISTANCE.get().floatValue());
    }

    /** 从开启到镜子碎裂（同时波纹展开）的时长：飞出、转正、蓄势龟裂。 */
    public int shatterDelay() {
        return flyTicks + turnTicks + crackTicks;
    }
}
