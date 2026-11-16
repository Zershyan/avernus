package io.zershyan.avernus.datagen.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.zershyan.avernus.Avernus;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/** 为玩家声明 Curios 胸饰槽；Curios 未安装时该数据不被读取。 */
public class CuriosSlotProvider implements DataProvider {
    private final PackOutput.PathProvider paths;

    public CuriosSlotProvider(PackOutput output) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "curios/entities");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        JsonObject json = new JsonObject();
        JsonArray entities = new JsonArray();
        entities.add("player");
        JsonArray slots = new JsonArray();
        slots.add("body");
        json.add("entities", entities);
        json.add("slots", slots);
        return DataProvider.saveStable(output, json, paths.json(Avernus.id("player")));
    }

    @Override
    public @NotNull String getName() {
        return "Avernus Curios Slots";
    }
}
