package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 领域演出音效；具体声音由 sounds.json 组合，资源包可整体替换。碎裂、展开与收拢由客户端按领域范围播放，见 RealmSounds。 */
public final class AvernusSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, Avernus.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_THROW = register("realm.mirror_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHATTER = register("realm.shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPAND = register("realm.expand");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTRACT = register("realm.contract");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_RETURN = register("realm.mirror_return");

    private AvernusSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(Avernus.id(name)));
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
