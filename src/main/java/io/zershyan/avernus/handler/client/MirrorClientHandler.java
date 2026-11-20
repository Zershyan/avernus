package io.zershyan.avernus.handler.client;

import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.realm.MirrorForms;
import io.zershyan.avernus.registry.AvernusKeyMappings;
import io.zershyan.avernus.registry.AvernusRegistries;
import io.zershyan.avernus.registry.item.MirrorItem;
import io.zershyan.avernus.registry.packet.ToggleWornMirrorPacket;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 胸饰镜快捷键与镜子提示：常显背景文案，Shift 境相、Alt 特殊效果、Ctrl 用法。 */
public final class MirrorClientHandler {
    private MirrorClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyMapping key = AvernusKeyMappings.toggleRealm();
        if (key == null || Minecraft.getInstance().player == null) return;
        boolean pressed = false;
        while (key.consumeClick()) pressed = true;
        if (pressed) PacketDistributor.sendToServer(new ToggleWornMirrorPacket());
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof MirrorItem mirror)) return;
        List<Component> tooltip = event.getToolTip();
        MirrorForm form = mirror.form(event.getItemStack());
        if (MirrorForms.resolve(form).isEmpty()) tooltip.add(AvernusKeyLang.TooltipInvalidForm.copy().withStyle(ChatFormatting.RED));
        if (Screen.hasShiftDown()) {
            tooltip.add(AvernusKeyLang.TooltipAspect.get(mirror.aspect().getName()).withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(mirror.aspect().getDescription().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else if (Screen.hasAltDown()) {
            effects(tooltip, form.traits());
        } else if (Screen.hasControlDown()) {
            usage(tooltip);
        } else {
            tooltip.add(AvernusKeyLang.TooltipLore1.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(AvernusKeyLang.TooltipLore2.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(AvernusKeyLang.TooltipHint.copy().withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void effects(List<Component> tooltip, List<ResourceLocation> ids) {
        if (ids.isEmpty()) {
            tooltip.add(AvernusKeyLang.TooltipNoEffects.copy().withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(AvernusKeyLang.TooltipEffects.copy().withStyle(ChatFormatting.DARK_PURPLE));
        for (ResourceLocation id : ids) {
            RealmTrait trait = AvernusRegistries.REALM_TRAIT.get(id);
            Component name = trait != null ? trait.getName() : Component.literal(id.toString());
            Component description = trait != null ? trait.getDescription() : Component.empty();
            tooltip.add(AvernusKeyLang.TooltipEffect.get(name.copy().withStyle(ChatFormatting.LIGHT_PURPLE), description)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void usage(List<Component> tooltip) {
        tooltip.add(AvernusKeyLang.TooltipUse.copy().withStyle(ChatFormatting.GRAY));
        KeyMapping key = AvernusKeyMappings.toggleRealm();
        if (key != null) {
            Component bound = key.isUnbound() ? AvernusKeyLang.TooltipUnbound.copy() : key.getTranslatedKeyMessage();
            tooltip.add(AvernusKeyLang.TooltipCurioKey.get(bound).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(AvernusKeyLang.TooltipAspectHint.copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(AvernusKeyLang.TooltipSmithingHint.copy().withStyle(ChatFormatting.GRAY));
    }
}
