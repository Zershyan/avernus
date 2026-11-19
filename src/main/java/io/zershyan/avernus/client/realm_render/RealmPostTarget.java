package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.zershyan.avernus.Avernus;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;

/** 后处理用的最小帧缓冲：单张线性过滤的 HDR 颜色纹理，无深度。 */
final class RealmPostTarget {
    private static boolean hdrUnsupported;

    private int framebuffer = -1;
    private int texture = -1;
    private int width;
    private int height;

    int texture() {
        return texture;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    void resize(int width, int height) {
        if (texture != -1 && this.width == width && this.height == height) return;
        close();
        this.width = width;
        this.height = height;
        texture = GlStateManager._genTexture();
        framebuffer = GlStateManager.glGenFramebuffers();
        if (hdrUnsupported || !allocate(GL_RGBA16F, GL_FLOAT)) {
            if (!hdrUnsupported) Avernus.LOGGER.warn("显卡不支持 RGBA16F 帧缓冲，领域后处理改用 RGBA8");
            hdrUnsupported = true;
            allocate(GL_RGBA8, GL_UNSIGNED_BYTE);
        }
        GlStateManager._bindTexture(0);
    }

    private boolean allocate(int format, int type) {
        GlStateManager._bindTexture(texture);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_RGBA, type, null);
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        return GlStateManager.glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
    }

    void bindWrite() {
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        RenderSystem.viewport(0, 0, width, height);
    }

    void close() {
        if (framebuffer != -1) GlStateManager._glDeleteFramebuffers(framebuffer);
        if (texture != -1) GlStateManager._deleteTexture(texture);
        framebuffer = texture = -1;
    }
}
