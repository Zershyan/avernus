package io.zershyan.avernus.registry.attachment;

import com.mojang.serialization.Codec;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import io.zershyan.forgeattachment.network.codec.StreamCodec;

/** 使用者级关境冷却：以服务器主世界 gameTime 记录结束时刻。 */
public record RealmCooldown(long endTime) {
    public static final RealmCooldown NONE = new RealmCooldown(0L);

    public static final Codec<RealmCooldown> CODEC = Codec.LONG.xmap(RealmCooldown::new, RealmCooldown::endTime);
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmCooldown> STREAM_CODEC =
            StreamCodec.of((buf, value) -> buf.writeVarLong(value.endTime()), buf -> new RealmCooldown(buf.readVarLong()));

    public static AttachmentType<RealmCooldown> createType() {
        return AttachmentType.builder(() -> NONE)
                .serialize(CODEC, cooldown -> cooldown.endTime() > 0L)
                .copyOnDeath()
                .sync((holder, player) -> holder == player, STREAM_CODEC)
                .build();
    }

    public long remaining(long gameTime) {
        return Math.max(0L, endTime - gameTime);
    }
}
