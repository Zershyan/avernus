package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.api.realm.RealmPhase;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端领域渲染状态（仅主线程访问）：为每个领域分配网格标签、在网格键变化时重建受影响 section，
 * 并按帧打包领域区块 Shader 的覆盖参数。覆盖判定与 {@link Realm#covers} 逐格一致。
 */
public final class RealmRenderState {
    public static final int MAX_REALMS = 8;
    private static final int STRIDE = 8;
    private static final int MAX_LABEL = 253;
    private static final ResourceLocation WATER_SPRITE = ResourceLocation.withDefaultNamespace("block/water_still");

    private static final Map<UUID, RealmMeshKey> KEYS = new HashMap<>();
    private static final float[] DATA = new float[MAX_REALMS * STRIDE];
    private static List<Realm> visible = List.of();
    private static long frameTime;

    private RealmRenderState() {
    }

    /** 是否需要领域区块 Shader：只要存在虚拟网格就必须裁剪，即使当前观察者看不到任何领域。 */
    public static boolean active() {
        return !KEYS.isEmpty();
    }

    public static boolean hasVisibleRealm() {
        return !visible.isEmpty();
    }

    // region 网格键与重建

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            reset();
            return;
        }
        long time = level.getGameTime();
        Set<UUID> seen = new HashSet<>();
        boolean changed = false;
        for (Realm realm : RealmSets.of(level).all()) {
            RealmMeshKey previous = KEYS.get(realm.id());
            int label = previous != null ? previous.label() : freeLabel();
            if (label < 0) continue;
            seen.add(realm.id());
            RealmMeshKey key = RealmMeshKey.of(realm, label, realm.phase(time) != RealmPhase.ACTIVE, previous);
            if (key.equals(previous)) continue;
            KEYS.put(realm.id(), key);
            if (previous != null) markDirty(level, previous);
            markDirty(level, key);
            changed = true;
        }
        var iterator = KEYS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (seen.contains(entry.getKey())) continue;
            markDirty(level, entry.getValue());
            iterator.remove();
            changed = true;
        }
        if (changed) mc.levelRenderer.needsUpdate();
    }

    public static void reset() {
        KEYS.clear();
        visible = List.of();
    }

    private static int freeLabel() {
        boolean[] used = new boolean[MAX_LABEL + 1];
        KEYS.values().forEach(key -> used[key.label()] = true);
        for (int i = 0; i <= MAX_LABEL; i++) if (!used[i]) return i;
        return -1;
    }

    private static void markDirty(ClientLevel level, RealmMeshKey key) {
        int minSy = Math.max(SectionPos.blockToSectionCoord(key.layerY()), level.getMinSection());
        int maxSy = level.getMaxSection() - 1;
        double reach = key.radius() + key.margin() + 3.0;
        int minSx = SectionPos.blockToSectionCoord(Mth.floor(key.centerX() - reach));
        int maxSx = SectionPos.blockToSectionCoord(Mth.floor(key.centerX() + reach));
        int minSz = SectionPos.blockToSectionCoord(Mth.floor(key.centerZ() - reach));
        int maxSz = SectionPos.blockToSectionCoord(Mth.floor(key.centerZ() + reach));
        var renderer = Minecraft.getInstance().levelRenderer;
        for (int sx = minSx; sx <= maxSx; sx++) {
            for (int sz = minSz; sz <= maxSz; sz++) {
                for (int sy = minSy; sy <= maxSy; sy++) {
                    if (key.affectsSection(sx, sy, sz)) renderer.setSectionDirty(sx, sy, sz);
                }
            }
        }
    }

    /** 主线程捕获构建所需数据，编译线程只读快照。 */
    public static void addGeometry(AddSectionGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (KEYS.isEmpty() || event.getLevel() != mc.level) return;
        BlockPos origin = event.getSectionOrigin();
        int sx = SectionPos.blockToSectionCoord(origin.getX());
        int sy = SectionPos.blockToSectionCoord(origin.getY());
        int sz = SectionPos.blockToSectionCoord(origin.getZ());
        TextureAtlas atlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        List<RealmSectionMesher.Job> jobs = new ArrayList<>();
        for (RealmMeshKey key : KEYS.values()) {
            if (!key.affectsSection(sx, sy, sz)) continue;
            RealmVisualProfile profile = RealmVisuals.get(key.aspect());
            jobs.add(new RealmSectionMesher.Job(key, atlas.getSprite(profile.solidTexture()), atlas.getSprite(WATER_SPRITE), profile.waterColor()));
        }
        if (!jobs.isEmpty()) event.addRenderer(context -> RealmSectionMesher.build(context, origin, jobs));
    }

    // endregion

    // region 帧参数

    /** 在相机就位后调用：筛选观察者可见的领域并写入 Shader 覆盖参数。 */
    public static void prepareFrame(ShaderInstance shader) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        int bx = Mth.floor(cam.x), by = Mth.floor(cam.y), bz = Mth.floor(cam.z);
        frameTime = level.getGameTime();
        List<Realm> list = new ArrayList<>(MAX_REALMS);
        for (Realm realm : RealmSets.of(level).all()) {
            if (list.size() == MAX_REALMS) break;
            RealmMeshKey key = KEYS.get(realm.id());
            if (key == null || !realm.geometryAppliesTo(camera.getEntity())) continue;
            int b = list.size() * STRIDE;
            DATA[b] = (float) (realm.sourceX() - bx);
            DATA[b + 1] = (float) (realm.sourceZ() - bz);
            DATA[b + 2] = (float) realm.waveRadius(frameTime);
            DATA[b + 3] = realm.height() - by;
            DATA[b + 4] = (float) (realm.exactCenterX() - bx);
            DATA[b + 5] = (float) (realm.exactCenterZ() - bz);
            DATA[b + 6] = realm.radius();
            DATA[b + 7] = key.label();
            list.add(realm);
        }
        visible = list;
        shader.safeGetUniform("RealmCount").set(list.size());
        shader.safeGetUniform("RealmData").set(DATA);
        shader.safeGetUniform("CameraFrac").set((float) (cam.x - bx), (float) (cam.y - by), (float) (cam.z - bz));
    }

    /** 该位置的真实方块是否被观察者所见的领域消隐（与 Shader 同一规则）。 */
    public static boolean hides(BlockPos pos) {
        for (Realm realm : visible) {
            if (realm.covers(pos.getX(), pos.getZ(), frameTime)) return pos.getY() >= realm.height() - 1;
        }
        return false;
    }

    // endregion
}
