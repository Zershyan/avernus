package io.zershyan.avernus.client.realm_render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_RGBA32I;
import static org.lwjgl.opengl.GL30.GL_RGBA_INTEGER;

/** 每列只上传胜出的表层；环形纹理在视距移动时保留没有变化的区块。 */
final class RealmColumns {
    static final int SOLID = 1;
    static final int WATER = 2;
    private static final int SIZE = 16;
    private static final int STRIDE = 4;
    private static final int TILE_INTS = SIZE * SIZE * STRIDE;
    private static final int NO_CUT = Integer.MAX_VALUE;
    private static final int[] EMPTY = new int[TILE_INTS];

    record Style(int id, RealmMeshKey key) { }
    private record Slice(Style style, @Nullable Realm.Coverage partial) { }
    private record Tile(List<Slice> slices, int[] data, Set<Style> styles, int minimumLayer) { }
    record Range(int minY, int maxY) {
        boolean intersects(int sectionY) {
            return sectionY <= maxY && sectionY + SIZE > minY;
        }
    }

    final WeakWaterAbyss abyss = new WeakWaterAbyss();
    private final Map<RealmMeshKey, Style> styles = new HashMap<>();
    private final Map<Long, Tile> tiles = new HashMap<>();
    private final Map<Long, Range> boundaries = new HashMap<>();
    private final Set<Long> dirty = new HashSet<>();
    private int nextStyle;
    private int texture;
    private int textureSize;
    private int minX, minZ, maxX, maxZ;

    private record Candidate(Style style, Realm.Coverage coverage) { }
    private record Stamp(int sx, int sz, int viewDistance, List<Candidate> candidates) { }
    private @Nullable Stamp stamp;

    void update(ClientLevel level, @Nullable Entity viewer, int sx, int sz, int viewDistance) {
        long time = level.getGameTime();
        List<Candidate> candidates = new ArrayList<>();
        for (Realm realm : RealmSets.of(level).all()) {
            if (!realm.geometryAppliesTo(viewer)) continue;
            Realm.Coverage coverage = realm.coverage(time);
            if (coverage.wave() <= 0.0) continue;
            RealmMeshKey key = RealmMeshKey.of(realm);
            candidates.add(new Candidate(styles.computeIfAbsent(key, k -> new Style(++nextStyle, k)), coverage));
        }
        if (candidates.isEmpty()) {
            if (texture != 0) close();
            return;
        }
        int margin = viewDistance + 2;
        Stamp next = new Stamp(sx, sz, viewDistance, candidates);
        if (next.equals(stamp) && dirty.isEmpty()) return;
        stamp = next;
        minX = (sx - margin) * SIZE;
        minZ = (sz - margin) * SIZE;
        maxX = (sx + margin + 1) * SIZE;
        maxZ = (sz + margin + 1) * SIZE;
        ensureTexture(maxX - minX);

        Map<Long, List<Slice>> slices = new HashMap<>();
        for (Candidate candidate : candidates) addSlices(slices, candidate);
        Set<Long> boundaryUpdates = new HashSet<>();
        int previous = GlStateManager._getInteger(GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTexture(texture);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pixels = stack.mallocInt(TILE_INTS);
            var iterator = tiles.entrySet().iterator();
            while (iterator.hasNext()) {
                long pos = iterator.next().getKey();
                if (slices.containsKey(pos)) continue;
                iterator.remove();
                abyss.remove(pos);
                upload(pos, EMPTY, pixels);
                addBoundaryUpdates(boundaryUpdates, pos);
            }
            for (var entry : slices.entrySet()) {
                long pos = entry.getKey();
                List<Slice> values = entry.getValue();
                Tile old = tiles.get(pos);
                if (old != null && !dirty.contains(pos) && old.slices.equals(values)) continue;
                Tile tile = resolve(level, pos, values);
                tiles.put(pos, tile);
                if (old == null || !Arrays.equals(old.data, tile.data)) {
                    upload(pos, tile.data, pixels);
                    addBoundaryUpdates(boundaryUpdates, pos);
                }
            }
        } finally {
            RenderSystem.bindTexture(previous);
        }
        dirty.clear();
        for (long chunk : boundaryUpdates) {
            updateBoundary(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), level);
        }
        Set<RealmMeshKey> current = new HashSet<>();
        candidates.forEach(candidate -> current.add(candidate.style.key));
        styles.keySet().retainAll(current);
    }

    private static void addBoundaryUpdates(Set<Long> chunks, long changed) {
        int x = ChunkPos.getX(changed), z = ChunkPos.getZ(changed);
        chunks.add(changed);
        chunks.add(ChunkPos.asLong(x - 1, z));
        chunks.add(ChunkPos.asLong(x + 1, z));
        chunks.add(ChunkPos.asLong(x, z - 1));
        chunks.add(ChunkPos.asLong(x, z + 1));
    }

    private void addSlices(Map<Long, List<Slice>> tilesByPosition, Candidate candidate) {
        Realm.Coverage c = candidate.coverage;
        int minChunkX = SectionPos.blockToSectionCoord(Math.max(minX, (int) Math.floor(Math.max(c.centerX() - c.radius(), c.sourceX() - c.wave()))));
        int minChunkZ = SectionPos.blockToSectionCoord(Math.max(minZ, (int) Math.floor(Math.max(c.centerZ() - c.radius(), c.sourceZ() - c.wave()))));
        int maxChunkX = SectionPos.blockToSectionCoord(Math.min(maxX - 1, (int) Math.floor(Math.min(c.centerX() + c.radius(), c.sourceX() + c.wave()))));
        int maxChunkZ = SectionPos.blockToSectionCoord(Math.min(maxZ - 1, (int) Math.floor(Math.min(c.centerZ() + c.radius(), c.sourceZ() + c.wave()))));
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                int relation = c.tile(x * SIZE, z * SIZE, SIZE);
                if (relation < 0) continue;
                var slices = tilesByPosition.computeIfAbsent(ChunkPos.asLong(x, z), p -> new ArrayList<>());
                if (slices.isEmpty() || slices.getLast().partial != null) {
                    slices.add(new Slice(candidate.style, relation == 1 ? null : c));
                }
            }
        }
    }

    private Tile resolve(ClientLevel level, long packed, List<Slice> slices) {
        int baseX = ChunkPos.getX(packed) * SIZE;
        int baseZ = ChunkPos.getZ(packed) * SIZE;
        Map<Integer, BitSetDiscreteVoxelShape> weakWater = null;
        int[] data = new int[TILE_INTS];
        int minLayer = NO_CUT;
        Set<Style> styles = new HashSet<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                for (Slice slice : slices) {
                    if (slice.partial != null && !slice.partial.covers(baseX + x, baseZ + z)) continue;
                    Style style = slice.style;
                    int y = style.key.layerY();
                    int i = index(x, z);
                    if (style.key.weakWater()) {
                        if (weakWater == null) weakWater = new HashMap<>();
                        weakWater.computeIfAbsent(y, h -> new BitSetDiscreteVoxelShape(SIZE, 1, SIZE)).fill(x, 0, z);
                        data[i + 3] = 1;
                    }
                    data[i] = y;
                    data[i + 1] = style.id;
                    data[i + 2] = switch (style.key.kind(level.getBlockState(pos.set(baseX + x, y, baseZ + z)))) {
                        case SOLID -> SOLID;
                        case WATER -> WATER;
                        default -> 0;
                    };
                    minLayer = Math.min(minLayer, y - 1);
                    styles.add(style);
                    break;
                }
            }
        }
        if (weakWater == null) abyss.remove(packed);
        else abyss.replace(packed, weakWater);
        return new Tile(slices, data, styles, minLayer);
    }

    private void updateBoundary(int chunkX, int chunkZ, ClientLevel level) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        int min = NO_CUT, max = Integer.MIN_VALUE;
        int bx = chunkX * SIZE, bz = chunkZ * SIZE;
        Tile own = tiles.get(key);
        Tile west = tiles.get(ChunkPos.asLong(chunkX - 1, chunkZ)), east = tiles.get(ChunkPos.asLong(chunkX + 1, chunkZ));
        Tile north = tiles.get(ChunkPos.asLong(chunkX, chunkZ - 1)), south = tiles.get(ChunkPos.asLong(chunkX, chunkZ + 1));
        if (own != null || west != null || east != null || north != null || south != null) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    int high = cut(own, x, z);
                    int low = Math.min(Math.min(cut(x == 0 ? west : own, x + SIZE - 1, z), cut(x == SIZE - 1 ? east : own, x + 1, z)),
                            Math.min(cut(z == 0 ? north : own, x, z + SIZE - 1), cut(z == SIZE - 1 ? south : own, x, z + 1)));
                    if (low < high) {
                        min = Math.min(min, low);
                        max = Math.max(max, high == NO_CUT ? level.getMaxBuildHeight() - 1 : high - 1);
                    }
                }
            }
        }
        if (bx < minX || bx >= maxX || bz < minZ || bz >= maxZ || min > max) boundaries.remove(key);
        else boundaries.put(key, new Range(min, max));
    }

    /** 该列真实方块开始隐藏的高度：境面层下方的底板层。 */
    private static int cut(@Nullable Tile tile, int x, int z) {
        int i = index(x, z);
        return tile == null || tile.data[i + 1] == 0 ? NO_CUT : tile.data[i] - 1;
    }

    private static int index(int x, int z) {
        return ((z & 15) * SIZE + (x & 15)) * STRIDE;
    }

    boolean hides(BlockPos pos) {
        Tile tile = tiles.get(ChunkPos.asLong(pos));
        if (tile == null) return false;
        int i = index(pos.getX(), pos.getZ());
        return tile.data[i + 1] != 0 && (tile.data[i + 3] != 0 || pos.getY() >= tile.data[i] - 1);
    }

    @Nullable Range boundary(long chunk) {
        return boundaries.get(chunk);
    }

    Set<Style> styles(long chunk) {
        Tile tile = tiles.get(chunk);
        return tile == null ? Set.of() : tile.styles;
    }

    int minimumLayer(long chunk) {
        Tile tile = tiles.get(chunk);
        return tile == null ? NO_CUT : tile.minimumLayer;
    }

    void forEachCandidate(java.util.function.LongConsumer action) {
        tiles.keySet().forEach(action::accept);
        boundaries.keySet().forEach(chunk -> { if (!tiles.containsKey(chunk)) action.accept(chunk); });
    }

    boolean active() { return !tiles.isEmpty(); }
    int texture() { return texture; }
    int size() { return textureSize; }
    int minX() { return minX; }
    int minZ() { return minZ; }
    int maxX() { return maxX; }
    int maxZ() { return maxZ; }

    void dirty(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        if (tiles.containsKey(key)) dirty.add(key);
    }

    private void ensureTexture(int size) {
        if (texture != 0 && textureSize == size) return;
        if (texture != 0) GlStateManager._deleteTexture(texture);
        texture = GlStateManager._genTexture();
        textureSize = size;
        int previous = GlStateManager._getInteger(GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTexture(texture);
        var empty = MemoryUtil.memCallocInt(size * size * STRIDE);
        try {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32I, size, size, 0, GL_RGBA_INTEGER, GL_INT, empty);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        } finally {
            MemoryUtil.memFree(empty);
            RenderSystem.bindTexture(previous);
        }
        tiles.clear();
        abyss.clear();
        boundaries.clear();
        dirty.clear();
    }

    private void upload(long pos, int[] data, IntBuffer pixels) {
        pixels.clear().put(data).flip();
        int x = Math.floorMod(ChunkPos.getX(pos) * SIZE, textureSize);
        int z = Math.floorMod(ChunkPos.getZ(pos) * SIZE, textureSize);
        glTexSubImage2D(GL_TEXTURE_2D, 0, x, z, SIZE, SIZE, GL_RGBA_INTEGER, GL_INT, pixels);
    }

    void close() {
        if (texture != 0) GlStateManager._deleteTexture(texture);
        texture = 0;
        nextStyle = 0;
        stamp = null;
        tiles.clear();
        abyss.clear();
        boundaries.clear();
        styles.clear();
        dirty.clear();
    }
}
