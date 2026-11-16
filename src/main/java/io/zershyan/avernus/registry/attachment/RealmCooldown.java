package io.zershyan.avernus.registry.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

/** 使用者级关境冷却：以服务器主世界 gameTime 记录结束时刻。 */
public record RealmCooldown(long endTime) {
    public static final RealmCooldown NONE = new RealmCooldown(0L);

    public static final Codec<RealmCooldown> CODEC = Codec.LONG.xmap(RealmCooldown::new, RealmCooldown::endTime);
    public static final StreamCodec<ByteBuf, RealmCooldown> STREAM_CODEC =
            ByteBufCodecs.VAR_LONG.map(RealmCooldown::new, RealmCooldown::endTime);

    public long remaining(long gameTime) {
        return Math.max(0L, endTime - gameTime);
    }
}
