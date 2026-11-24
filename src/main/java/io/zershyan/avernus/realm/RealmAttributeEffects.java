package io.zershyan.avernus.realm;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.api.event.RealmAttributeEvent;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.attachment.RealmAttributes;
import io.zershyan.avernus.registry.config.ServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 境相属性的服务端计算与差量应用；仅修改本模组的临时修饰，不改基础属性。 */
public final class RealmAttributeEffects {
    private static final ResourceLocation MODIFIER_ID = Avernus.id("realm_aspect");

    public static void tick(ServerLevel level) {
        RealmSet realms = RealmSets.of(level);
        if (realms.isEmpty()) return;
        Map<UUID, RealmAttributeEvent.Configure> configured = new HashMap<>();
        for (Realm realm : realms.all()) {
            if (!(level.getEntity(realm.ownerId()) instanceof LivingEntity owner) || !owner.isAlive()) continue;
            RealmAttributeEvent.Configure event = defaults(level, realm, owner);
            MinecraftForge.EVENT_BUS.post(event);
            if (!event.isCanceled() && realms.get(realm.id()) == realm) configured.put(realm.id(), event);
        }
        long time = level.getGameTime();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            Realm realm = realms.geometryAt(living, living.getBlockX(), living.getBlockZ(), time);
            RealmAttributeEvent.Configure config = realm == null ? null : configured.get(realm.id());
            if (realm == null || config == null || !living.isAlive() || living.isRemoved() || living.isSpectator()
                    || !canAffect(level, config.getOwner(), living)) {
                clear(living);
                continue;
            }
            boolean owner = realm.ownerId().equals(living.getUUID());
            var modifiers = owner ? config.getOwnerModifiers() : config.getOtherModifiers();
            var event = new RealmAttributeEvent.Apply(level, realm, config.getOwner(), living, modifiers.values());
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled() || !living.isAlive() || living.isRemoved() || living.isSpectator()
                    || !config.getOwner().isAlive() || !canAffect(level, config.getOwner(), living)
                    || realms.get(realm.id()) != realm
                    || realms.geometryAt(living, living.getBlockX(), living.getBlockZ(), time) != realm) {
                clear(living);
            } else apply(living, realm.id(), event.getModifiers().values());
        }
    }

    private static RealmAttributeEvent.Configure defaults(ServerLevel level, Realm realm, LivingEntity owner) {
        Map<Holder<Attribute>, Double> own = Map.of();
        Map<Holder<Attribute>, Double> other = Map.of();
        if (realm.aspect() == AvernusRealmAspects.UMBRAL.get()) {
            own = Map.of(Attributes.MOVEMENT_SPEED, ServerConfig.UMBRAL_OWNER_SPEED_BONUS.get(),
                    Attributes.JUMP_STRENGTH, ServerConfig.UMBRAL_OWNER_JUMP_BONUS.get());
            other = Map.of(Attributes.MOVEMENT_SPEED, -ServerConfig.UMBRAL_OTHER_SPEED_PENALTY.get());
        } else if (realm.aspect() == AvernusRealmAspects.INFERNAL.get()) {
            own = Map.of(Attributes.ATTACK_DAMAGE, ServerConfig.INFERNAL_OWNER_ATTACK_BONUS.get());
            other = Map.of(Attributes.ATTACK_DAMAGE, -ServerConfig.INFERNAL_OTHER_ATTACK_PENALTY.get());
        } else if (realm.aspect() == AvernusRealmAspects.SILENT.get()) {
            own = Map.of(Attributes.ARMOR, ServerConfig.SILENT_OWNER_ARMOR_BONUS.get(),
                    Attributes.ARMOR_TOUGHNESS, ServerConfig.SILENT_OWNER_TOUGHNESS_BONUS.get());
            other = Map.of(Attributes.ARMOR, -ServerConfig.SILENT_OTHER_ARMOR_PENALTY.get(),
                    Attributes.ARMOR_TOUGHNESS, -ServerConfig.SILENT_OTHER_TOUGHNESS_PENALTY.get());
        }
        return new RealmAttributeEvent.Configure(level, realm, owner, own, other);
    }

    private static boolean canAffect(ServerLevel level, LivingEntity owner, LivingEntity target) {
        if (owner.getUUID().equals(target.getUUID())) return true;
        if (owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        if (target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        return !(owner instanceof Player player && target instanceof Player other)
                || level.getServer().isPvpAllowed() && player.canHarmPlayer(other) && other.canHarmPlayer(player);
    }

    private static void apply(LivingEntity entity, UUID realm, Map<Holder<Attribute>, Double> modifiers) {
        RealmAttributes old = entity.getExistingDataOrNull(AvernusAttachments.REALM_ATTRIBUTES);
        Set<Holder<Attribute>> installed = new HashSet<>();
        for (var entry : modifiers.entrySet()) {
            double amount = entry.getValue();
            if (amount == 0.0) continue;
            AttributeInstance attribute = entity.getAttribute(entry.getKey());
            if (attribute == null) continue;
            var modifier = new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            if (!modifier.equals(attribute.getModifier(MODIFIER_ID))) attribute.addOrUpdateTransientModifier(modifier);
            installed.add(entry.getKey());
        }
        if (old != null) {
            for (Holder<Attribute> attribute : old.attributes()) {
                if (!installed.contains(attribute)) remove(entity, attribute);
            }
        }
        if (installed.isEmpty()) entity.removeData(AvernusAttachments.REALM_ATTRIBUTES);
        else {
            var state = new RealmAttributes(realm, installed);
            if (!state.equals(old)) entity.setData(AvernusAttachments.REALM_ATTRIBUTES, state);
        }
    }

    /** 境彻底移除时立即撤销，避免最后一个境移除后没有后续 tick 能清理。 */
    public static void release(ServerLevel level, Realm realm) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            RealmAttributes state = living.getExistingDataOrNull(AvernusAttachments.REALM_ATTRIBUTES);
            if (state != null && realm.id().equals(state.realm())) clear(living);
        }
    }

    /** 实体离境或卸载时撤销，保留其他模组、装备和药水的修饰。 */
    public static void clear(LivingEntity entity) {
        RealmAttributes state = entity.getExistingDataOrNull(AvernusAttachments.REALM_ATTRIBUTES);
        if (state == null) return;
        state.attributes().forEach(attribute -> remove(entity, attribute));
        entity.removeData(AvernusAttachments.REALM_ATTRIBUTES);
    }

    private static void remove(LivingEntity entity, Holder<Attribute> key) {
        AttributeInstance attribute = entity.getAttribute(key);
        if (attribute != null) attribute.removeModifier(MODIFIER_ID);
    }
}
