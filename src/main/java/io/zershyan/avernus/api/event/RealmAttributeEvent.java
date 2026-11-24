package io.zershyan.avernus.api.event;

import io.zershyan.avernus.api.realm.RealmView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 境相属性事件；仅在服务端主线程的 Forge 游戏总线发布，取消时撤销本次影响的旧修饰。 */
public abstract class RealmAttributeEvent extends RealmEvent {
    private final LivingEntity owner;

    protected RealmAttributeEvent(ServerLevel level, RealmView realm, LivingEntity owner) {
        super(level, realm);
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public ResourceLocation getAspectId() {
        return getRealm().aspectId();
    }

    public List<ResourceLocation> getTraitIds() {
        return getRealm().traitIds();
    }

    /** 总事件：每境每次效果更新发布一次，初始值来自当前配置；取消则跳过该境全部属性。 */
    @Cancelable
    public static final class Configure extends RealmAttributeEvent {
        private final Modifiers ownerModifiers;
        private final Modifiers otherModifiers;

        public Configure(ServerLevel level, RealmView realm, LivingEntity owner,
                         Map<Attribute, Double> ownerModifiers, Map<Attribute, Double> otherModifiers) {
            super(level, realm, owner);
            this.ownerModifiers = new Modifiers(ownerModifiers);
            this.otherModifiers = new Modifiers(otherModifiers);
        }

        public Modifiers getOwnerModifiers() {
            return ownerModifiers;
        }

        public Modifiers getOtherModifiers() {
            return otherModifiers;
        }
    }

    /** 副事件：每个符合条件的生物独立发布，初始值为总事件的最终结果；取消只跳过此生物。 */
    @Cancelable
    public static final class Apply extends RealmAttributeEvent {
        private final LivingEntity entity;
        private final Modifiers modifiers;

        public Apply(ServerLevel level, RealmView realm, LivingEntity owner, LivingEntity entity,
                     Map<Attribute, Double> modifiers) {
            super(level, realm, owner);
            this.entity = Objects.requireNonNull(entity, "entity");
            this.modifiers = new Modifiers(modifiers);
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public boolean isOwner() {
            return getOwner().getUUID().equals(entity.getUUID());
        }

        public Modifiers getModifiers() {
            return modifiers;
        }
    }

    /** 独立总乘区的比例表：0.2 表示乘 1.2，-0.2 表示乘 0.8；0 不安装修饰。 */
    public static final class Modifiers {
        private final Map<Attribute, Double> original;
        private final Map<Attribute, Double> values = new LinkedHashMap<>();
        private final Map<Attribute, Double> view = Collections.unmodifiableMap(values);

        public Modifiers(Map<Attribute, Double> initial) {
            initial.forEach(this::set);
            original = Map.copyOf(values);
        }

        public Map<Attribute, Double> originalValues() {
            return original;
        }

        /** 当前结果的只读视图；不会把修改权限暴露给集合调用方。 */
        public Map<Attribute, Double> values() {
            return view;
        }

        public void set(Attribute attribute, double amount) {
            Objects.requireNonNull(attribute, "attribute");
            if (!Double.isFinite(amount) || amount < -1.0) {
                throw new IllegalArgumentException("属性比例必须是大于等于 -1 的有限数值：" + amount);
            }
            values.put(attribute, amount);
        }

        public void remove(Attribute attribute) {
            values.remove(attribute);
        }

        public void clear() {
            values.clear();
        }
    }
}
