package io.zershyan.avernus.registry.attachment;

import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/** 本次境相实际安装的属性修饰，仅存在于服务端内存。 */
public record RealmAttributes(@Nullable UUID realm, Set<Attribute> attributes, double jumpBonus) {
    public static final RealmAttributes NONE = new RealmAttributes(null, Set.of(), 0.0);

    public RealmAttributes {
        attributes = Set.copyOf(attributes);
    }
}
