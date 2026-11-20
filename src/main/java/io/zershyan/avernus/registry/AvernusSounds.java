package io.zershyan.avernus.registry;

import io.zershyan.avernus.Avernus;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 领域演出音效；具体声音由 sounds.json 组合，资源包可整体替换。 */
public final class AvernusSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, Avernus.MODID);
    /** 展开与收缩覆盖整个领域，可听距离与最大半径同量级。 */
    private static final float REALM_RANGE = 160.0F;

    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_THROW = variable("realm.mirror_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHATTER = variable("realm.shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPAND = fixed("realm.expand");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTRACT = fixed("realm.contract");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_RETURN = variable("realm.mirror_return");

    private AvernusSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> variable(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(Avernus.id(name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixed(String name) {
        return REGISTER.register(name, () -> SoundEvent.createFixedRangeEvent(Avernus.id(name), REALM_RANGE));
    }

    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
