package io.zershyan.avernus.datagen.provider;

import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Map;

public class ModItemModelProvider extends ItemModelProvider {
    /**
     * 镜子物品的左右对称像素轮廓，与物品贴图一一对应：F 镜框、C 顶饰、G 镜面、S 支柱、B 底座。
     * 各部件厚度不同，镜面凹进镜框，底座最厚。
     */
    private static final String[] MIRROR_SHAPE = {
            ".......CC.......",
            ".....FFFFFF.....",
            "....FGGGGGGF....",
            "...FGGGGGGGGF...",
            "..FGGGGGGGGGGF..",
            "..FGGGGGGGGGGF..",
            ".FFGGGGGGGGGGFF.",
            "..FGGGGGGGGGGF..",
            "..FGGGGGGGGGGF..",
            "..FGGGGGGGGGGF..",
            "...FGGGGGGGGF...",
            "....FGGGGGGF....",
            ".....FFFFFF.....",
            "......SSSS......",
            ".......SS.......",
            ".....BBBBBB....."};
    private static final Map<Character, float[]> DEPTH = Map.of(
            'F', new float[]{7.0F, 9.0F},
            'C', new float[]{7.0F, 9.0F},
            'S', new float[]{7.25F, 8.75F},
            'G', new float[]{7.5F, 8.5F},
            'B', new float[]{6.0F, 10.0F});
    private static final String TEXTURE = "#mirror";

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Avernus.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ResourceLocation base = AvernusItems.MIRROR.getId();
        mirror(base.toString(), base.withPrefix("item/"));
        AvernusRealmAspects.REGISTER.getEntries().forEach(aspect -> {
            ResourceLocation model = aspect.getId().withPrefix("item/mirror/");
            mirror(model.toString(), model);
        });
    }

    /** 按轮廓把同部件的像素合并成矩形块，拼出有厚度的立体镜子。 */
    private void mirror(String name, ResourceLocation texture) {
        ItemModelBuilder model = getBuilder(name).texture("mirror", texture).texture("particle", texture)
                .guiLight(BlockModel.GuiLight.FRONT);
        int size = MIRROR_SHAPE.length;
        boolean[][] used = new boolean[size][size];
        for (int row = 0; row < size; row++) {
            for (int x = 0; x < size; x++) {
                char part = MIRROR_SHAPE[row].charAt(x);
                if (part == '.' || used[row][x]) continue;
                int right = x;
                while (right + 1 < size && MIRROR_SHAPE[row].charAt(right + 1) == part) right++;
                int bottom = row;
                while (bottom + 1 < size && uniform(bottom + 1, x, right, part)) bottom++;
                for (int r = row; r <= bottom; r++) for (int c = x; c <= right; c++) used[r][c] = true;
                box(model, DEPTH.get(part), x, right + 1, row, bottom + 1);
            }
        }
        display(model);
    }

    private static boolean uniform(int row, int from, int to, char part) {
        for (int x = from; x <= to; x++) if (MIRROR_SHAPE[row].charAt(x) != part) return false;
        return true;
    }

    /** 正面取对应像素，背面镜像，侧面取边缘那一列/行像素。 */
    private static void box(ItemModelBuilder model, float[] depth, int x1, int x2, int y1, int y2) {
        model.element().from(x1, 16 - y2, depth[0]).to(x2, 16 - y1, depth[1]).allFaces((direction, face) -> {
            face.texture(TEXTURE);
            switch (direction) {
                case SOUTH -> face.uvs(x1, y1, x2, y2);
                case NORTH -> face.uvs(x2, y1, x1, y2);
                case WEST -> face.uvs(x1, y1, x1 + 1, y2);
                case EAST -> face.uvs(x2 - 1, y1, x2, y2);
                case UP -> face.uvs(x1, y1, x2, y1 + 1);
                case DOWN -> face.uvs(x1, y2 - 1, x2, y2);
            }
        });
    }

    /** 与原版 item/generated 相同的持握与展示变换。 */
    private static void display(ItemModelBuilder model) {
        var transforms = model.transforms();
        transforms.transform(ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.5F);
        transforms.transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7);
        transforms.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).translation(0, 3, 1).scale(0.55F);
        transforms.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, 25).translation(1.13F, 3.2F, 1.13F).scale(0.68F);
        transforms.transform(ItemDisplayContext.FIXED).rotation(0, 180, 0);
    }
}
