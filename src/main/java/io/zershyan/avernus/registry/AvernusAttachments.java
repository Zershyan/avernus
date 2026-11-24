package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.attachment.IllusionPermits;
import io.zershyan.avernus.registry.attachment.RealmAttributes;
import io.zershyan.avernus.registry.attachment.RealmCooldown;
import io.zershyan.avernus.registry.attachment.RealmPresence;
import io.zershyan.forgeattachment.api.ForgeAttachmentApi;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

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

    public static <T> void set(Entity entity, RegistryObject<AttachmentType<T>> type, T value) {
        ForgeAttachmentApi.of(entity).ifPresent(holder -> holder.setData(type.get(), value));
    }

    public static void remove(Entity entity, RegistryObject<? extends AttachmentType<?>> type) {
        ForgeAttachmentApi.of(entity).ifPresent(holder -> remove(holder, type.get()));
    }

    public static boolean has(Entity entity, RegistryObject<? extends AttachmentType<?>> type) {
        return ForgeAttachmentApi.of(entity).map(holder -> has(holder, type.get())).orElse(false);
    }

    private static <T> void remove(io.zershyan.forgeattachment.attachment.IAttachmentHolder holder, AttachmentType<T> type) {
        holder.removeData(type);
    }

    private static <T> boolean has(io.zershyan.forgeattachment.attachment.IAttachmentHolder holder, AttachmentType<T> type) {
        return holder.hasData(type);
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
