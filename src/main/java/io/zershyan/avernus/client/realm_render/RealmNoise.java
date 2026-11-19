package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.GL11.*;

/**
 * 256² 平铺噪声纹理：G 为逐格随机值，R 为 G 偏移 (37, 17) 后的值，单次线性采样即可插值 3D 值噪声；B、A 为独立白噪声。
 */
final class RealmNoise {
    static final int SIZE = 256;

    private static @Nullable DynamicTexture texture;

    private RealmNoise() {
    }

    static int id() {
        if (texture == null) texture = create();
        return texture.getId();
    }

    static void close() {
        if (texture != null) texture.close();
        texture = null;
    }

    private static DynamicTexture create() {
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int r = random(x + 37, y + 17, 0), g = random(x, y, 0), b = random(x, y, 1), a = random(x, y, 2);
                image.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
            }
        }
        DynamicTexture result = new DynamicTexture(image);
        result.setFilter(true, false);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GlStateManager._bindTexture(0);
        return result;
    }

    private static int random(int x, int y, int channel) {
        int h = (x & SIZE - 1) * 0x27D4EB2D ^ (y & SIZE - 1) * 0x165667B1 ^ channel * 0x9E3779B9;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h & 0xFF;
    }
}
