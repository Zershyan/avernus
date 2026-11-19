package io.zershyan.avernus.realm;

import io.netty.buffer.ByteBuf;
import io.zershyan.avernus.registry.config.ServerConfig;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 开境时冻结的演出时长快照，随领域同步，客户端不得自行改变节奏。 */
public record RealmTimeline(int flyTicks, int turnTicks, int crackTicks, int scatterTicks, int waveTicks, float flyDistance) {
    public static final StreamCodec<ByteBuf, RealmTimeline> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RealmTimeline::flyTicks,
            ByteBufCodecs.VAR_INT, RealmTimeline::turnTicks,
            ByteBufCodecs.VAR_INT, RealmTimeline::crackTicks,
            ByteBufCodecs.VAR_INT, RealmTimeline::scatterTicks,
            ByteBufCodecs.VAR_INT, RealmTimeline::waveTicks,
            ByteBufCodecs.FLOAT, RealmTimeline::flyDistance,
            RealmTimeline::new
    );

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
