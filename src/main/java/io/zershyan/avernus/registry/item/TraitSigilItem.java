package io.zershyan.avernus.registry.item;

import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** 境印：锻造台中把对应规则刻入镜子的材料。 */
public class TraitSigilItem extends Item {
    private final Supplier<? extends RealmTrait> trait;

    public TraitSigilItem(Supplier<? extends RealmTrait> trait) {
        super(new Properties().rarity(Rarity.UNCOMMON));
        this.trait = trait;
    }

    public RealmTrait trait() {
        return trait.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(trait().getDescription().withStyle(ChatFormatting.GRAY));
        tooltip.add(AvernusKeyLang.TooltipSigilUsage.copy().withStyle(ChatFormatting.DARK_GRAY));
    }
}
