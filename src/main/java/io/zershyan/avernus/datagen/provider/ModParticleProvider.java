package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.registry.AvernusParticles;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ParticleDescriptionProvider;

public class ModParticleProvider extends ParticleDescriptionProvider {
    private static final int FRAMES = 4;

    public ModParticleProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        AvernusParticles.REGISTER.getEntries().forEach(type -> spriteSet(type.get(), type.getId(), FRAMES, false));
    }
}
