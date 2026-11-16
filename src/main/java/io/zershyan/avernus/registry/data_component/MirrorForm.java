package io.zershyan.avernus.registry.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.zershyan.avernus.registry.AvernusRegistries;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * 镜子形态：一个境相 ID 与去重排序后的类型 ID 集合。保存稳定资源 ID，使用前经 {@link #resolve()} 校验。
 */
public record MirrorForm(ResourceLocation aspect, List<ResourceLocation> traits) {
    public static final int MAX_TRAITS = 32;

    public static final Codec<MirrorForm> CODEC = RecordCodecBuilder.<MirrorForm>create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("aspect").forGetter(MirrorForm::aspect),
            ResourceLocation.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(MirrorForm::traits)
    ).apply(instance, MirrorForm::new)).validate(form -> form.traits.size() > MAX_TRAITS
            ? DataResult.error(() -> "Too many realm traits: " + form.traits.size())
            : DataResult.success(form));

    public static final StreamCodec<ByteBuf, MirrorForm> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MirrorForm::aspect,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TRAITS)), MirrorForm::traits,
            MirrorForm::new
    );

    public MirrorForm {
        traits = List.copyOf(new TreeSet<>(traits));
    }

    public boolean hasTrait(ResourceLocation trait) {
        return traits.contains(trait);
    }

    public MirrorForm withAspect(ResourceLocation newAspect) {
        return new MirrorForm(newAspect, traits);
    }

    public MirrorForm withTrait(ResourceLocation trait) {
        List<ResourceLocation> list = new ArrayList<>(traits);
        list.add(trait);
        return new MirrorForm(aspect, list);
    }

    public MirrorForm withoutTrait(ResourceLocation trait) {
        List<ResourceLocation> list = new ArrayList<>(traits);
        list.remove(trait);
        return new MirrorForm(aspect, list);
    }

    /** 解析为注册对象；存在未注册 ID、超界或类型冲突时返回空。 */
    public Optional<Resolved> resolve() {
        if (traits.size() > MAX_TRAITS) return Optional.empty();
        RealmAspect resolvedAspect = AvernusRegistries.REALM_ASPECT.get(aspect);
        if (resolvedAspect == null) return Optional.empty();
        Set<RealmTrait> resolvedTraits = new LinkedHashSet<>();
        for (ResourceLocation id : traits) {
            RealmTrait trait = AvernusRegistries.REALM_TRAIT.get(id);
            if (trait == null) return Optional.empty();
            resolvedTraits.add(trait);
        }
        return findConflict(resolvedTraits) == null ? Optional.of(new Resolved(resolvedAspect, Set.copyOf(resolvedTraits))) : Optional.empty();
    }

    /** 返回集合中首个与他者冲突的类型；双向检查。 */
    public static @Nullable RealmTrait findConflict(Set<RealmTrait> traits) {
        for (RealmTrait a : traits) {
            for (RealmTrait b : traits) {
                if (a != b && (a.conflictsWith(b) || b.conflictsWith(a))) return a;
            }
        }
        return null;
    }

    public record Resolved(RealmAspect aspect, Set<RealmTrait> traits) {
    }
}
