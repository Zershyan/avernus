package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmResult;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmAttributeEffects;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.AvernusShaders;
import io.zershyan.avernus.registry.config.ClientConfig;
import io.zershyan.avernus.registry.config.RealmFxQuality;
import io.zershyan.avernus.util.mixin.avernus.client.IMixinRenderRegionCache;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

/** 独立客户端回归：真实开境、虚拟网格、重叠境相、永恒移动、境边光幕及收境清理。 */
@Mod.EventBusSubscriber(modid = Avernus.MODID, value = Dist.CLIENT)
public final class ClientMigrationScenarios {
    private enum Stage { START, WATER, MOVED, OVERLAP, CLOSED, BOUNDARY_OPEN, BOUNDARY_OUTSIDE, DONE }

    private static final BlockPos EMPTY_SECTION = new BlockPos(0, 48, 16);
    private static final long TEST_SECTION = SectionPos.asLong(EMPTY_SECTION);
    private static final BlockPos PROBE = new BlockPos(8, 63, 24);
    private static Stage stage = Stage.START;
    private static boolean created;
    private static int ticks;
    private static volatile @org.jetbrains.annotations.Nullable Throwable serverFailure;
    private static RealmSectionMeshes meshes;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("avernus.clientRegression") || event.phase != TickEvent.Phase.END || stage == Stage.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!created && mc.getOverlay() == null && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                created = true;
                mc.options.onboardAccessibility = false;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(6);
                mc.createWorldOpenFlows().createFreshLevel("regression-" + System.currentTimeMillis(),
                        new LevelSettings("迁移回归", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(1487121L, false, false),
                        regs -> regs.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.getSingleplayerServer() == null || mc.level == null || mc.player == null || mc.getOverlay() != null) return;
            if (serverFailure != null) throw new IllegalStateException("服务端测试操作失败", serverFailure);
            if (++ticks > 500) throw new AssertionError("客户端阶段超时：" + stage);
            advance(mc);
        } catch (Throwable failure) {
            stage = Stage.DONE;
            mc.delayCrash(CrashReport.forThrowable(failure, "1.20.1 客户端迁移回归"));
        }
    }

    private static void advance(Minecraft mc) throws ReflectiveOperationException {
        var realms = RealmSets.of(mc.level);
        Realm own = realms.all().stream().filter(realm -> realm.ownerId().equals(mc.player.getUUID())).findFirst().orElse(null);
        switch (stage) {
            case START -> {
                for (var effect : AvernusShaders.Effect.values()) require(effect.get() != null, "着色器未加载：" + effect);
                verifyAttributes(AvernusShaders.realmChunk(RenderType.solid()), DefaultVertexFormat.BLOCK);
                verifyAttributes(AvernusShaders.Effect.MIRROR.get(), DefaultVertexFormat.NEW_ENTITY);
                var field = RealmRenderState.class.getDeclaredField("MESHES");
                field.setAccessible(true);
                meshes = (RealmSectionMeshes) field.get(null);
                onServer(mc, player -> {
                    var level = player.serverLevel();
                    for (BlockPos pos : BlockPos.betweenClosed(7, 63, 7, 9, 63, 9)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(new BlockPos(9, 64, 12), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(new BlockPos(10, 64, 12), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, 8.5, 64, 8.5, Set.of(), 0, 30);
                    player.getInventory().setItem(0, new ItemStack(AvernusItems.UMBRAL_MIRROR.get()));
                    player.getInventory().selected = 0;
                    require(RealmManager.open(player, new MirrorForm(AvernusRealmAspects.UMBRAL.getId(),
                            List.of(AvernusRealmTraits.ETERNITY.getId(), AvernusRealmTraits.WEAK_WATER.getId()))) == RealmResult.OPENED, "开弱水境失败");
                });
                next(Stage.WATER);
            }
            case WATER -> {
                if (own == null || !own.covers(PROBE.getX(), PROBE.getZ(), mc.level.getGameTime()) || !RealmRenderState.active()
                        || !meshes.hasLayer(TEST_SECTION, RealmMeshKey.of(own), RenderType.translucent()) || ticks < 120) return;
                require(Math.abs(RealmAttributeEffects.jumpBonus(mc.player) - 0.2) < 1.0E-9, "跳跃倍率未同步到客户端");
                float jump = (float) ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_6118_").invoke(mc.player);
                require(Math.abs(jump - 0.42F * 1.2F) < 1.0E-5, "客户端实际起跳速度未应用倍率");
                verifyGeometry(mc, own);
                capture(mc, "water");
                onServer(mc, player -> player.teleportTo(player.serverLevel(), 14.5, 64, 8.5, Set.of(), 0, 30));
                next(Stage.MOVED);
            }
            case MOVED -> {
                if (own == null || own.teleportSeq() == 0 || own.exactCenterX() < 14.0 || ticks < 20) return;
                capture(mc, "eternity-moved");
                onServer(mc, player -> {
                    var level = player.serverLevel();
                    var owner = EntityType.ARMOR_STAND.create(level);
                    require(owner != null, "无法创建重叠境相所有者");
                    owner.setNoGravity(true);
                    owner.moveTo(30.5, 64, 12.5, 0, 0);
                    level.addFreshEntity(owner);
                    require(RealmManager.open(owner, new MirrorForm(AvernusRealmAspects.INFERNAL.getId(),
                            List.of(AvernusRealmTraits.BOUNDLESS.getId()))) == RealmResult.OPENED, "开无垠境失败");
                });
                next(Stage.OVERLAP);
            }
            case OVERLAP -> {
                if (realms.all().size() != 2 || ticks < 120) return;
                Realm winner = realms.geometryAt(mc.player, PROBE.getX(), PROBE.getZ(), mc.level.getGameTime());
                if (winner == null || !winner.hasTrait(AvernusRealmTraits.BOUNDLESS.get())
                        || !meshes.hasLayer(TEST_SECTION, RealmMeshKey.of(winner), RenderType.solid())) return;
                capture(mc, "overlap");
                onServer(mc, player -> {
                    var level = player.serverLevel();
                    for (Realm realm : RealmSets.of(level).all()) RealmManager.requestClose(level, realm);
                });
                next(Stage.CLOSED);
            }
            case CLOSED -> {
                if (!realms.isEmpty() || RealmRenderState.active()) return;
                require(RealmAttributeEffects.jumpBonus(mc.player) == 0.0, "收境后仍残留客户端跳跃倍率");
                ClientConfig.QUALITY.set(RealmFxQuality.HIGH);
                onServer(mc, player -> {
                    var level = player.serverLevel();
                    player.teleportTo(level, 8.5, level.getMinBuildHeight() + 4, 8.5, Set.of(), 0, 5);
                    AvernusAttachments.resetCooldown(player);
                    require(RealmManager.open(player, new MirrorForm(AvernusRealmAspects.UMBRAL.getId(),
                            List.of(AvernusRealmTraits.BOUNDLESS.getId()))) == RealmResult.OPENED, "开境边光效测试失败");
                });
                next(Stage.BOUNDARY_OPEN);
            }
            case BOUNDARY_OPEN -> {
                if (own == null || own.waveRadius(mc.level.getGameTime()) < own.maxWaveRadius() || ticks < 120) return;
                double outsideZ = own.exactCenterZ() + own.radius() + 14;
                onServer(mc, player -> player.teleportTo(player.serverLevel(), 8.5, player.getY(), outsideZ, Set.of(), 180, 5));
                next(Stage.BOUNDARY_OUTSIDE);
            }
            case BOUNDARY_OUTSIDE -> {
                if (own == null || ticks < 25 || own.covers(mc.player.getBlockX(), mc.player.getBlockZ(), mc.level.getGameTime())) return;
                verifyBoundaryLight();
                capture(mc, "boundary-outside");
                Avernus.LOGGER.info("CLIENT_REGRESSION PASSED");
                next(Stage.DONE);
                mc.stop();
            }
            case DONE -> { }
        }
    }

    private static void verifyAttributes(ShaderInstance shader, VertexFormat format) {
        var names = format.getElementAttributeNames();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name.equals("Padding")) continue;
            require(GL20.glGetAttribLocation(shader.getId(), name) == i, "着色器顶点位置不匹配：" + name);
        }
    }

    private static void verifyBoundaryLight() throws ReflectiveOperationException {
        var field = RealmPostChain.class.getDeclaredField("FX");
        field.setAccessible(true);
        var fx = (RealmPostTarget) field.get(null);
        require(fx.texture() > 0, "界壁光幕通道未建立");
        int previous = glGetInteger(GL_TEXTURE_BINDING_2D);
        var pixels = MemoryUtil.memAllocFloat(fx.width() * fx.height() * 4);
        int lit = 0;
        try {
            RenderSystem.bindTexture(fx.texture());
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_FLOAT, pixels);
            for (int i = 0; i < pixels.capacity(); i += 4) {
                if (Math.max(pixels.get(i), Math.max(pixels.get(i + 1), pixels.get(i + 2))) > 0.01F) lit++;
            }
        } finally {
            RenderSystem.bindTexture(previous);
            MemoryUtil.memFree(pixels);
        }
        require(lit > 100, "从境外观察时界壁光幕必须产生有效像素");
        Avernus.LOGGER.info("CLIENT_REGRESSION boundary PASSED litPixels={}", lit);
    }

    private static void verifyGeometry(Minecraft mc, Realm realm) {
        var cache = new RenderRegionCache();
        var end = EMPTY_SECTION.offset(15, 15, 15);
        require(cache.createRegion(mc.level, EMPTY_SECTION, end, 1) == null, "测试场地必须是真实空分区");
        var region = ((IMixinRenderRegionCache) cache).avernus$createIncludingEmpty(mc.level, EMPTY_SECTION, end, 1);
        var style = RealmMeshKey.of(realm);
        var visual = RealmVisuals.get(style.aspect());
        var atlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        var still = atlas.getSprite(visual.stillTexture());
        var surface = new RealmGeometryContext(region);
        RealmSectionMesher.buildSurface(surface, EMPTY_SECTION, style, new RealmSectionMesher.Material(
                atlas.getSprite(visual.solidTexture()), still, atlas.getSprite(visual.flowTexture()), visual.waterColor()));
        BufferBuilder water = surface.buffers().get(RenderType.translucent());
        var vertices = water.end();
        try {
            var bytes = vertices.vertexBuffer().order(ByteOrder.nativeOrder());
            int stride = vertices.drawState().format().getVertexSize();
            int uv = vertices.drawState().format().getOffset(2);
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                float u = bytes.getFloat(i * stride + uv);
                min = Math.min(min, u);
                max = Math.max(max, u);
            }
            require(Math.abs(min - still.getU0()) < 1.0E-5 && Math.abs(max - still.getU1()) < 1.0E-5, "水面 UV 未覆盖完整贴图");
        } finally {
            vertices.release();
            surface.buffers().values().forEach(RealmGeometryContext::free);
        }
        BlockPos origin = new BlockPos(0, 64, 0);
        var boundary = new RealmGeometryContext(cache.createRegion(mc.level, origin, origin.offset(15, 15, 15), 1));
        RealmSectionMesher.buildBoundary(boundary, origin, Map.of());
        var faces = boundary.buffers().get(RenderType.solid()).end();
        try {
            require(faces.drawState().vertexCount() == 8, "两个相邻石块应只补绘两张隐藏面");
        } finally {
            faces.release();
            boundary.buffers().values().forEach(RealmGeometryContext::free);
        }
        Avernus.LOGGER.info("CLIENT_REGRESSION geometry PASSED");
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(uuid);
                require(player != null, "缺少服务端玩家");
                action.accept(player);
            } catch (Throwable failure) {
                serverFailure = failure;
            }
        });
    }

    private static void next(Stage next) {
        stage = next;
        ticks = 0;
    }

    private static void capture(Minecraft mc, String stage) {
        Screenshot.grab(mc.gameDirectory, stage + ".png", mc.getMainRenderTarget(), message -> Avernus.LOGGER.info("{}", message.getString()));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
