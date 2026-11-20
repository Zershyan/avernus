package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import java.util.function.Supplier;

/** 演出音效由原版事件叠加而成，不附带音频文件。 */
public class ModSoundProvider extends SoundDefinitionsProvider {
    public ModSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Avernus.MODID, helper);
    }

    @Override
    public void registerSounds() {
        add(AvernusSounds.MIRROR_THROW, "item.trident.throw", 1.2F);
        add(AvernusSounds.SHATTER, "block.glass.break", 0.6F);
        add(AvernusSounds.EXPAND, "block.beacon.activate", 0.6F);
        add(AvernusSounds.CONTRACT, "block.beacon.deactivate", 0.6F);
        add(AvernusSounds.MIRROR_RETURN, "item.trident.return", 1.0F);
    }

    private void add(Supplier<SoundEvent> sound, String vanilla, float pitch) {
        add(sound, SoundDefinition.definition()
                .subtitle(sound.get().getLocation().toLanguageKey("subtitles"))
                .with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace(vanilla), SoundDefinition.SoundType.EVENT).pitch(pitch)));
    }
}
