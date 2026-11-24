package io.zershyan.avernus.registry.item;

import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.data_component.MirrorTraits;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.function.Supplier;

/** 阿维尔诺之镜：境相由物品本身决定，附加境印保存在 {@link MirrorTraits} 组件中。 */
public class MirrorItem extends Item {
    private final Supplier<? extends RealmAspect> aspect;

    public MirrorItem(Supplier<? extends RealmAspect> aspect) {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .component(AvernusDataComponents.MIRROR_TRAITS.get(), MirrorTraits.EMPTY));
        this.aspect = aspect;
    }

    public RealmAspect aspect() {
        return aspect.get();
    }

    public static MirrorTraits getTraits(ItemStack stack) {
        return stack.getOrDefault(AvernusDataComponents.MIRROR_TRAITS.get(), MirrorTraits.EMPTY);
    }

    public MirrorForm form(ItemStack stack) {
        return new MirrorForm(Objects.requireNonNull(aspect().getId()), getTraits(stack).ids());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) RealmManager.toggle(player, form(stack));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
