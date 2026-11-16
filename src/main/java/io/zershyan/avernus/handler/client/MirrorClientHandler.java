package io.zershyan.avernus.handler.client;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.datagen.lang.AvernusKeyLang;
import io.zershyan.avernus.registry.AvernusKeyMappings;
import io.zershyan.avernus.registry.packet.ToggleWornMirrorPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 胸饰镜快捷键与镜子提示中的按键/锻造说明。 */
@EventBusSubscriber(modid = Avernus.MODID, value = Dist.CLIENT)
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
        if (!AvernusApi.isMirror(event.getItemStack())) return;
        KeyMapping key = AvernusKeyMappings.toggleRealm();
        if (key != null) {
            Component bound = key.isUnbound() ? AvernusKeyLang.TooltipUnbound.copy() : key.getTranslatedKeyMessage();
            event.getToolTip().add(AvernusKeyLang.TooltipCurioKey.get(bound).withStyle(ChatFormatting.DARK_GRAY));
        }
        event.getToolTip().add(AvernusKeyLang.TooltipSmithingHint.copy().withStyle(ChatFormatting.DARK_GRAY));
    }
}
