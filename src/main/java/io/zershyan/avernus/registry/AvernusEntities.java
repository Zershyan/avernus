package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.client.mirror.MirrorEntityRenderer;
import io.zershyan.avernus.registry.entity.MirrorEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class AvernusEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Avernus.MODID);

    public static final RegistryObject<EntityType<MirrorEntity>> MIRROR = mirror();

    @SuppressWarnings("unchecked")
    private static RegistryObject<EntityType<MirrorEntity>> mirror() {
        return (RegistryObject<EntityType<MirrorEntity>>) (RegistryObject<?>) REGISTER.register("mirror",
                () -> EntityType.Builder.<MirrorEntity>of(MirrorEntity::new, MobCategory.MISC)
                        .sized(1.5F, 1.5F).noSave().noSummon().fireImmune()
                        .clientTrackingRange(10).updateInterval(2).build("mirror"));
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.register(modBus));
    }

    private static class Client {
        static void register(IEventBus modBus) {
            modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                    event.registerEntityRenderer(MIRROR.get(), MirrorEntityRenderer::new));
        }
    }
}
