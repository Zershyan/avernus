package io.zershyan.avernus.handler.common;

import io.zershyan.avernus.realm.RealmAttributeEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;

/** 实体卸载、下线或跨维度时撤销境相临时属性。 */
public final class RealmAttributeHandler {
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            RealmAttributeEffects.clear(living);
        }
    }
}
