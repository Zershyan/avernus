package io.zershyan.avernus.realm;

import io.netty.buffer.ByteBuf;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.registry.AvernusRegistries;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** 镜子形态的内部编解码与注册解析；公开值对象不依赖注册实现。 */
public final class MirrorForms {
    public static final StreamCodec<ByteBuf, MirrorForm> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MirrorForm::aspect,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MirrorForm.MAX_TRAITS)), MirrorForm::traits,
            MirrorForm::new
    );

    private MirrorForms() {
    }

    /** 解析为注册对象；存在未注册 ID、超界或境印冲突时返回空。 */
    public static Optional<Resolved> resolve(MirrorForm form) {
        if (form.traits().size() > MirrorForm.MAX_TRAITS) return Optional.empty();
        RealmAspect resolvedAspect = AvernusRegistries.REALM_ASPECT.get(form.aspect());
        if (resolvedAspect == null) return Optional.empty();
        Set<RealmTrait> resolvedTraits = new LinkedHashSet<>();
        for (ResourceLocation id : form.traits()) {
            RealmTrait trait = AvernusRegistries.REALM_TRAIT.get(id);
            if (trait == null) return Optional.empty();
            resolvedTraits.add(trait);
        }
        return findConflict(resolvedTraits) == null ? Optional.of(new Resolved(resolvedAspect, Set.copyOf(resolvedTraits))) : Optional.empty();
    }

    /** 返回集合中首个与他者冲突的境印；双向检查。 */
    private static @Nullable RealmTrait findConflict(Set<RealmTrait> traits) {
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
