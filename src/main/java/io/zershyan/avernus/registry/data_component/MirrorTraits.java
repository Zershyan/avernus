package io.zershyan.avernus.registry.data_component;

import io.zershyan.avernus.api.realm.MirrorForm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** 镜子物品上附加的境印 ID：去重排序后保存在物品 NBT 中，境相由物品本身决定。 */
public record MirrorTraits(List<ResourceLocation> ids) {
    public static final int MAX = MirrorForm.MAX_TRAITS;
    public static final MirrorTraits EMPTY = new MirrorTraits(List.of());
    private static final String TAG = "MirrorTraits";

    public MirrorTraits {
        ids = List.copyOf(new TreeSet<>(ids));
    }

    public static MirrorTraits read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_LIST)) return EMPTY;
        ListTag list = tag.getList(TAG, Tag.TAG_STRING);
        List<ResourceLocation> ids = new ArrayList<>(Math.min(list.size(), MAX));
        for (int i = 0; i < list.size() && ids.size() < MAX; i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) ids.add(id);
        }
        return new MirrorTraits(ids);
    }

    public static void write(ItemStack stack, MirrorTraits traits) {
        if (traits.ids.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag == null) return;
            tag.remove(TAG);
            if (tag.isEmpty()) stack.setTag(null);
            return;
        }
        ListTag list = new ListTag();
        for (ResourceLocation id : traits.ids) list.add(StringTag.valueOf(id.toString()));
        stack.getOrCreateTag().put(TAG, list);
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
