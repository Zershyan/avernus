package io.zershyan.avernus.client.realm_render;

import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/** 境面以上的空间本应敞开在天空下：被消隐的山体不该再把那里的实体、手与粒子压成全黑。 */
public final class RealmLighting {
    private static final int FULL_SKY = 15;

    private RealmLighting() {
    }

    /** 该位置在对摄像机生效的领域境面层或以上时，天空光取满。 */
    public static int skyLight(BlockAndTintGetter getter, BlockPos pos, int original) {
        if (original >= FULL_SKY || !(getter instanceof ClientLevel level)) return original;
        Realm realm = RealmSets.of(level).geometryAt(Minecraft.getInstance().gameRenderer.getMainCamera().getEntity(),
                pos.getX(), pos.getZ(), level.getGameTime());
        return realm != null && pos.getY() >= realm.height() - 1 ? FULL_SKY : original;
    }
}
