package io.zershyan.avernus.handler.client;

import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.compat.curios.registry.CuriosKeyMappings;
import io.zershyan.avernus.compat.curios.registry.CuriosToggleMirrorPacket;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.realm.MirrorForms;
import io.zershyan.avernus.registry.AvernusRegistries;
import io.zershyan.avernus.registry.item.MirrorItem;
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

/** 胸饰镜快捷键与镜子提示：境相与效果名常显，Ctrl 效果详情、Shift 境相描述、Alt 用法。 */
public final class MirrorClientHandler {
    private MirrorClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyMapping key = CuriosKeyMappings.toggleRealm();
        if (key == null || Minecraft.getInstance().player == null) return;
        boolean pressed = false;
        while (key.consumeClick()) pressed = true;
        if (pressed) PacketDistributor.sendToServer(new CuriosToggleMirrorPacket());
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof MirrorItem mirror)) return;
        List<Component> tooltip = event.getToolTip();
        MirrorForm form = mirror.form(event.getItemStack());
        if (MirrorForms.resolve(form).isEmpty()) tooltip.add(AvernusKeyLang.TooltipInvalidForm.copy().withStyle(ChatFormatting.RED));
        tooltip.add(AvernusKeyLang.TooltipAspect.get(mirror.aspect().getName()).withStyle(ChatFormatting.DARK_PURPLE));
        if (Screen.hasShiftDown()) tooltip.add(mirror.aspect().getDescription().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        effects(tooltip, form.traits(), Screen.hasControlDown());
        if (Screen.hasAltDown()) {
            usage(tooltip);
        } else if (!Screen.hasShiftDown() && !Screen.hasControlDown()) {
            tooltip.add(AvernusKeyLang.TooltipLore1.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(AvernusKeyLang.TooltipLore2.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(AvernusKeyLang.TooltipHint.copy().withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** 效果名常显，detailed 时附上详细描述。 */
    private static void effects(List<Component> tooltip, List<ResourceLocation> ids, boolean detailed) {
        if (ids.isEmpty()) {
            tooltip.add(AvernusKeyLang.TooltipNoEffects.copy().withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(AvernusKeyLang.TooltipEffects.copy().withStyle(ChatFormatting.DARK_PURPLE));
        for (ResourceLocation id : ids) {
            RealmTrait trait = AvernusRegistries.REALM_TRAIT.get(id);
            Component name = (trait != null ? trait.getName() : Component.literal(id.toString())).copy().withStyle(ChatFormatting.LIGHT_PURPLE);
            tooltip.add(detailed && trait != null
                    ? AvernusKeyLang.TooltipEffect.get(name, trait.getDescription()).withStyle(ChatFormatting.GRAY)
                    : Component.literal(" ").append(name));
        }
    }

    private static void usage(List<Component> tooltip) {
        tooltip.add(AvernusKeyLang.TooltipUse.copy().withStyle(ChatFormatting.GRAY));
        KeyMapping key = CuriosKeyMappings.toggleRealm();
        if (key != null) {
            Component bound = key.isUnbound() ? AvernusKeyLang.TooltipUnbound.copy() : key.getTranslatedKeyMessage();
            tooltip.add(AvernusKeyLang.TooltipCurioKey.get(bound).withStyle(ChatFormatting.GRAY));
        }
    }
    }
