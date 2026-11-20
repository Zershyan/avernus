package io.zershyan.avernus.registry.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.zershyan.avernus.api.realm.MirrorForm;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** 镜子物品上附加的类型 ID：去重排序后保存稳定资源 ID，境相由物品本身决定。 */
public record MirrorTraits(List<ResourceLocation> ids) {
    public static final int MAX = MirrorForm.MAX_TRAITS;
    public static final MirrorTraits EMPTY = new MirrorTraits(List.of());

    public static final Codec<MirrorTraits> CODEC = ResourceLocation.CODEC.listOf()
            .validate(ids -> ids.size() > MAX ? DataResult.error(() -> "Too many realm traits: " + ids.size()) : DataResult.success(ids))
            .xmap(MirrorTraits::new, MirrorTraits::ids);

    public static final StreamCodec<ByteBuf, MirrorTraits> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)).map(MirrorTraits::new, MirrorTraits::ids);

    public MirrorTraits {
        ids = List.copyOf(new TreeSet<>(ids));
    }

    public boolean has(ResourceLocation trait) {
        return ids.contains(trait);
    }

    public MirrorTraits with(ResourceLocation trait) {
        List<ResourceLocation> list = new ArrayList<>(ids);
        list.add(trait);
        return new MirrorTraits(list);
    }

    public MirrorTraits without(ResourceLocation trait) {
        List<ResourceLocation> list = new ArrayList<>(ids);
        list.remove(trait);
        return new MirrorTraits(list);
    }
}
