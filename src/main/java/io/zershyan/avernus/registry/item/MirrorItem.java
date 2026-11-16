package io.zershyan.avernus.registry.item;

import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.realm.RealmManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import io.zershyan.avernus.registry.AvernusDataComponents;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.data_component.MirrorForm;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 阿维尔诺之镜：形态保存在 {@link MirrorForm} 组件中，单一物品覆盖全部组合。 */
public class MirrorItem extends Item {
    public static final MirrorForm DEFAULT_FORM = new MirrorForm(AvernusRealmAspects.UMBRAL.getId(), List.of());

    public MirrorItem() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .component(AvernusDataComponents.MIRROR_FORM.get(), DEFAULT_FORM));
    }

    public static MirrorForm getForm(ItemStack stack) {
        return stack.getOrDefault(AvernusDataComponents.MIRROR_FORM.get(), DEFAULT_FORM);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) RealmManager.toggle(player, getForm(stack));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MirrorForm form = getForm(stack);
        tooltip.add(AvernusKeyLang.TooltipAspect.get(aspectName(form.aspect())).withStyle(ChatFormatting.DARK_PURPLE));
        if (form.traits().isEmpty()) {
            tooltip.add(AvernusKeyLang.TooltipNoTraits.copy().withStyle(ChatFormatting.GRAY));
        } else {
            form.traits().forEach(id -> tooltip.add(AvernusKeyLang.TooltipTrait.get(traitName(id)).withStyle(ChatFormatting.GRAY)));
        }
        if (form.resolve().isEmpty()) tooltip.add(AvernusKeyLang.TooltipInvalidForm.copy().withStyle(ChatFormatting.RED));
        tooltip.add(AvernusKeyLang.TooltipUse.copy().withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component aspectName(ResourceLocation id) {
        return Component.translatable(RealmAspect.languageKey(id));
    }

    private static Component traitName(ResourceLocation id) {
        return Component.translatable(RealmTrait.languageKey(id));
    }
}
