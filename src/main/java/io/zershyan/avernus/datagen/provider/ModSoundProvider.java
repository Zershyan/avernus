package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import java.util.function.Supplier;

/** 演出音效：每个事件两段自制变体，文件位于 sounds/realm/。 */
public class ModSoundProvider extends SoundDefinitionsProvider {
    private static final int VARIANTS = 2;

    public ModSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Avernus.MODID, helper);
    }

    @Override
    public void registerSounds() {
        add(AvernusSounds.MIRROR_THROW);
        add(AvernusSounds.SHATTER);
        add(AvernusSounds.EXPAND);
        add(AvernusSounds.CONTRACT);
        add(AvernusSounds.MIRROR_RETURN);
    }

    private void add(Supplier<SoundEvent> sound) {
        var id = sound.get().getLocation();
        var definition = SoundDefinition.definition().subtitle(id.toLanguageKey("subtitles"));
        for (int i = 1; i <= VARIANTS; i++) {
            definition.with(SoundDefinition.Sound.sound(id.withPath(id.getPath().replace('.', '/') + "_" + i), SoundDefinition.SoundType.SOUND));
        }
        add(sound, definition);
    }
}
