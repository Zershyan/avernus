package io.zershyan.avernus.mixin.avernus.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public interface MixinBufferBuilder extends IMixinBufferBuilder {
    @Override
    @Accessor("buffer")
    ByteBuffer avernus$buffer();
}
