package io.zershyan.avernus.client.mirror;

import com.mojang.blaze3d.platform.NativeImage;
import io.zershyan.avernus.Avernus;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 由镜子贴图轮廓挤出的立体外壳：镜框正面凸起、外沿侧壁、镜框与凹进镜面之间的内壁和背板。
 * 镜框取轮廓向内 {@link #FRAME_PIXELS} 像素以内与底座区域，其余为镜面。坐标为镜面局部坐标（格）。
 */
public record MirrorSolid(List<Face> faces) {
    /** 镜框正面与背板离镜面中线的距离（格）。 */
    static final float FRAME = 0.08F;
    /** 凹进的镜面离中线的距离（格）。 */
    static final float GLASS = 0.04F;
    private static final int FRAME_PIXELS = 2;
    /** 贴图下方这一比例以下全部属于底座。 */
    private static final float BASE_FROM = 0.8F;
    private static final MirrorSolid EMPTY = new MirrorSolid(List.of());
    private static final Map<ResourceLocation, MirrorSolid> CACHE = new ConcurrentHashMap<>();

    /** 四个角按逆时针给出，每个角带贴图坐标。 */
    record Face(float[] xyz, float[] uv, Vector3f normal) {
    }

    static MirrorSolid of(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, MirrorSolid::load);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static MirrorSolid load(ResourceLocation texture) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture); NativeImage image = NativeImage.read(in)) {
            return new Builder(image).build();
        } catch (IOException e) {
            Avernus.LOGGER.warn("镜子贴图 {} 读取失败，镜子退回平面", texture, e);
            return EMPTY;
        }
    }

    private static final class Builder {
        private final int width, height;
        private final boolean[][] opaque, frame;
        private final List<Face> faces = new ArrayList<>();

        Builder(NativeImage image) {
            width = image.getWidth();
            height = image.getHeight();
            opaque = new boolean[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) opaque[x][y] = image.getPixelRGBA(x, y) >>> 24 != 0;
            }
            frame = new boolean[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) frame[x][y] = opaque[x][y] && (y >= height * BASE_FROM || nearEdge(x, y));
            }
        }

        private boolean nearEdge(int x, int y) {
            for (int dx = -FRAME_PIXELS; dx <= FRAME_PIXELS; dx++) {
                for (int dy = -FRAME_PIXELS; dy <= FRAME_PIXELS; dy++) if (!opaqueAt(x + dx, y + dy)) return true;
            }
            return false;
        }

        private boolean opaqueAt(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && opaque[x][y];
        }

        private float front(int x, int y) {
            return frame[x][y] ? FRAME : GLASS;
        }

        MirrorSolid build() {
            float half = MirrorEntityRenderer.SIZE / 2;
            face(new Vector3f(0, 0, -1), new float[]{-half, -half, -FRAME, -half, half, -FRAME, half, half, -FRAME, half, -half, -FRAME},
                    new float[]{0, 1, 0, 0, 1, 0, 1, 1});
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!opaque[x][y]) continue;
                    if (frame[x][y] && (x == 0 || !frame[x - 1][y])) frameCap(x, y);
                    float top = front(x, y);
                    wall(x, y, -1, 0, top);
                    wall(x, y, 1, 0, top);
                    wall(x, y, 0, -1, top);
                    wall(x, y, 0, 1, top);
                }
            }
            return new MirrorSolid(List.copyOf(faces));
        }

        /** 一行里连续的镜框像素合成一块凸起的正面。 */
        private void frameCap(int start, int y) {
            int end = start;
            while (end + 1 < width && frame[end + 1][y]) end++;
            float x0 = px(start), x1 = px(end + 1), y0 = py(y + 1), y1 = py(y);
            float u0 = (float) start / width, u1 = (float) (end + 1) / width, v0 = (float) y / height, v1 = (float) (y + 1) / height;
            face(new Vector3f(0, 0, 1), new float[]{x0, y0, FRAME, x1, y0, FRAME, x1, y1, FRAME, x0, y1, FRAME},
                    new float[]{u0, v1, u1, v1, u1, v0, u0, v0});
        }

        /** 像素朝 (dx, dy) 一侧的侧壁：外侧为空则从背板到正面，邻像素更低则只补高差。 */
        private void wall(int x, int y, int dx, int dy, float top) {
            float bottom = opaqueAt(x + dx, y + dy) ? front(x + dx, y + dy) : -FRAME;
            if (bottom >= top) return;
            float u = (x + 0.5F) / width, v = (y + 0.5F) / height;
            float[] uv = {u, v, u, v, u, v, u, v};
            Vector3f normal = new Vector3f(dx, -dy, 0);
            if (dx != 0) {
                float edge = px(dx < 0 ? x : x + 1);
                face(normal, new float[]{edge, py(y + 1), bottom, edge, py(y + 1), top, edge, py(y), top, edge, py(y), bottom}, uv);
            } else {
                float edge = py(dy < 0 ? y : y + 1);
                face(normal, new float[]{px(x), edge, bottom, px(x + 1), edge, bottom, px(x + 1), edge, top, px(x), edge, top}, uv);
            }
        }

        private float px(int x) {
            return (float) x / width * MirrorEntityRenderer.SIZE - MirrorEntityRenderer.SIZE / 2;
        }

        private float py(int y) {
            return MirrorEntityRenderer.SIZE / 2 - (float) y / height * MirrorEntityRenderer.SIZE;
        }

        private void face(Vector3f normal, float[] xyz, float[] uv) {
            faces.add(new Face(xyz, uv, normal));
        }
    }
}
