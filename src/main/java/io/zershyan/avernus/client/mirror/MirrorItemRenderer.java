package io.zershyan.avernus.client.mirror;

import com.mojang.blaze3d.vertex.PoseStack;
import io.zershyan.avernus.client.realm_render.RealmVisuals;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.item.MirrorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

/** 镜子物品直接画演出时的立体镜子，贴图取所属境相的实体贴图。 */
public final class MirrorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MirrorItemRenderer instance;

    private MirrorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext context, @NotNull PoseStack poseStack,
                             @NotNull MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof MirrorItem mirror)) return;
        ResourceLocation texture = RealmVisuals.get(mirror.aspect()).mirrorTexture();
        MirrorEntityRenderer.drawItem(buffers.getBuffer(RenderType.entityCutout(texture)), poseStack, texture, light);
    }

    public static void register(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (instance == null) instance = new MirrorItemRenderer();
                return instance;
            }
        };
        AvernusItems.MIRRORS_BY_ASPECT.values().forEach(mirror -> event.registerItem(extensions, mirror.get()));
    }
}
