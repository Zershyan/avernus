package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.attachment.IllusionPermits;
import io.zershyan.avernus.registry.attachment.RealmAttributes;
import io.zershyan.avernus.registry.attachment.RealmCooldown;
import io.zershyan.avernus.registry.attachment.RealmPresence;
import net.minecraftforge.eventbus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class AvernusAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Avernus.MODID);

    /** 冷却只同步给持有者本人。 */
    public static final Supplier<AttachmentType<RealmCooldown>> REALM_COOLDOWN = REGISTER.register("realm_cooldown", () -> AttachmentType
            .builder(() -> RealmCooldown.NONE)
            .serialize(RealmCooldown.CODEC, cooldown -> cooldown.endTime() > 0L)
            .sync((holder, player) -> holder == player, RealmCooldown.STREAM_CODEC)
            .copyOnDeath()
            .build());

    /** 实体曾受虚拟几何影响，加载后须先做一次脱困检查。 */
    public static final Supplier<AttachmentType<Boolean>> PENDING_RECOVERY = REGISTER.register("pending_recovery", () -> AttachmentType
            .builder(() -> Boolean.FALSE)
            .serialize(com.mojang.serialization.Codec.BOOL, Boolean::booleanValue)
            .build());

    /** 实体当前所处的有效领域与待消耗的首次落地保护，仅存在于内存。 */
    public static final Supplier<AttachmentType<RealmPresence>> REALM_PRESENCE = REGISTER.register("realm_presence", () -> AttachmentType
            .builder(() -> RealmPresence.NONE)
            .build());

    /** 境相临时属性的来源与清理范围，不存档、不复制给重生实体。 */
    public static final Supplier<AttachmentType<RealmAttributes>> REALM_ATTRIBUTES = REGISTER.register("realm_attributes", () -> AttachmentType
            .builder(() -> RealmAttributes.NONE)
            .build());

    /** 当前开启的领域尚未结算冷却；结算时即清除，不等待收束动画结束。 */
    public static final Supplier<AttachmentType<Boolean>> OWNS_OPEN_REALM = REGISTER.register("owns_open_realm", () -> AttachmentType
            .builder(() -> Boolean.FALSE)
            .serialize(com.mojang.serialization.Codec.BOOL, Boolean::booleanValue)
            .build());

    /** 幻化反击许可，仅存在于内存。 */
    public static final Supplier<AttachmentType<IllusionPermits>> ILLUSION_PERMITS = REGISTER.register("illusion_permits", () -> AttachmentType
            .<IllusionPermits>builder(() -> new IllusionPermits(new java.util.UUID(0L, 0L), java.util.Set.of()))
            .build());

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
