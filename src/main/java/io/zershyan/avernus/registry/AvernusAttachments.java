package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.attachment.IllusionPermits;
import io.zershyan.avernus.registry.attachment.RealmAttributes;
import io.zershyan.avernus.registry.attachment.RealmCooldown;
import io.zershyan.avernus.registry.attachment.RealmPresence;
import io.zershyan.forgeattachment.api.ForgeAttachmentApi;
import io.zershyan.forgeattachment.attachment.AttachmentSync;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import io.zershyan.forgeattachment.network.codec.ByteBufCodecs;
import io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** 实体附件：Forge 1.20.1 没有 AttachmentType，使用固定正式版的 forgeattachment。 */
public class AvernusAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(ForgeAttachmentRegistries.ATTACHMENT_TYPES_KEY, Avernus.MODID);

    /** 冷却只同步给持有者本人。 */
    public static final RegistryObject<AttachmentType<RealmCooldown>> REALM_COOLDOWN = register("realm_cooldown", RealmCooldown::createType);

    /** 实体曾受虚拟几何影响，加载后须先做一次脱困检查。 */
    public static final RegistryObject<AttachmentType<Boolean>> PENDING_RECOVERY = register("pending_recovery", () -> AttachmentType
            .builder(() -> Boolean.FALSE)
            .serialize(com.mojang.serialization.Codec.BOOL, Boolean::booleanValue)
            .build());

    /** 实体当前所处的有效领域与待消耗的首次落地保护，仅存在于内存。 */
    public static final RegistryObject<AttachmentType<RealmPresence>> REALM_PRESENCE = register("realm_presence", () -> AttachmentType
            .builder(() -> RealmPresence.NONE)
            .build());

    /** 境相临时属性的来源与清理范围，不存档、不复制给重生实体。 */
    public static final RegistryObject<AttachmentType<RealmAttributes>> REALM_ATTRIBUTES = register("realm_attributes", () -> AttachmentType
            .builder(() -> RealmAttributes.NONE)
            .build());

    /** 起跳倍率需要两端一致，仅同步实际数值，不同步服务端的属性来源。 */
    public static final RegistryObject<AttachmentType<Double>> REALM_JUMP_BONUS = register("realm_jump_bonus", () -> AttachmentType
            .builder(() -> 0.0)
            .sync(ByteBufCodecs.DOUBLE)
            .build());

    /** 当前开启的领域尚未结算冷却；结算时即清除，不等待收束动画结束。 */
    public static final RegistryObject<AttachmentType<Boolean>> OWNS_OPEN_REALM = register("owns_open_realm", () -> AttachmentType
            .builder(() -> Boolean.FALSE)
            .serialize(com.mojang.serialization.Codec.BOOL, Boolean::booleanValue)
            .build());

    /** 幻化反击许可，仅存在于内存。 */
    public static final RegistryObject<AttachmentType<IllusionPermits>> ILLUSION_PERMITS = register("illusion_permits", () -> AttachmentType
            .<IllusionPermits>builder(() -> new IllusionPermits(new UUID(0L, 0L), java.util.Set.of()))
            .build());

    private AvernusAttachments() {
    }

    @SuppressWarnings("unchecked")
    private static <T> RegistryObject<AttachmentType<T>> register(String name, Supplier<AttachmentType<T>> supplier) {
        return (RegistryObject<AttachmentType<T>>) (RegistryObject<?>) REGISTER.register(name, supplier);
    }

    public static <T> @Nullable T get(Entity entity, RegistryObject<AttachmentType<T>> type) {
        return ForgeAttachmentApi.of(entity).map(holder -> holder.getData(type.get())).orElse(null);
    }

    public static <T> @Nullable T getExisting(Entity entity, RegistryObject<AttachmentType<T>> type) {
        return ForgeAttachmentApi.of(entity).map(holder -> holder.getExistingDataOrNull(type.get())).orElse(null);
    }

    /** 是否仍有境相属性来源。 */
    public static boolean hasRealmAttributes(Entity entity) {
        return has(entity, REALM_ATTRIBUTES);
    }

    public static void resetCooldown(Entity entity) {
        set(entity, REALM_COOLDOWN, RealmCooldown.NONE);
    }

    public static <T> void set(Entity entity, RegistryObject<AttachmentType<T>> type, T value) {
        ForgeAttachmentApi.of(entity).ifPresent(holder -> {
            AttachmentType<T> attachment = type.get();
            if (!Objects.equals(holder.setData(attachment, value), value)) sync(entity, holder, attachment);
        });
    }

    public static void remove(Entity entity, RegistryObject<? extends AttachmentType<?>> type) {
        ForgeAttachmentApi.of(entity).ifPresent(holder -> {
            AttachmentType<?> attachment = type.get();
            if (holder.removeData(attachment) != null) sync(entity, holder, attachment);
        });
    }

    public static boolean has(Entity entity, RegistryObject<? extends AttachmentType<?>> type) {
        return ForgeAttachmentApi.of(entity).map(holder -> holder.hasData(type.get())).orElse(false);
    }

    /** ForgeAttachment 0.0.1 的内部 holder 未转发变更同步，须由实体显式同步。 */
    private static void sync(Entity entity, IAttachmentHolder holder, AttachmentType<?> type) {
        if (type.syncHandler() == null || entity.level().isClientSide) return;
        if (entity instanceof ServerPlayer player && player.connection == null) return;
        if (type == REALM_COOLDOWN.get()) {
            if (entity instanceof ServerPlayer player) AttachmentSync.syncToPlayer(player, type, entity);
        } else holder.syncData(type);
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
