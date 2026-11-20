package io.zershyan.avernus.api.realm;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** 镜子形态的不可变值；只持有资源 ID，注册有效性由开境或创建镜子时校验。 */
public record MirrorForm(ResourceLocation aspect, List<ResourceLocation> traits) {
    public static final int MAX_TRAITS = 32;

    public MirrorForm {
        Objects.requireNonNull(aspect, "aspect");
        traits = List.copyOf(new TreeSet<>(traits));
    }

    public boolean hasTrait(ResourceLocation trait) {
        return traits.contains(trait);
    }
}
