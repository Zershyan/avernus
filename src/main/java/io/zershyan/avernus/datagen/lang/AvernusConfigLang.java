package io.zershyan.avernus.datagen.lang;

import io.zershyan.avernus.Avernus;

/** 配置界面分组与条目名称。 */
public class AvernusConfigLang extends AvernusLang {
    private void entry(String path, String enUs, String zhCn) {
        addFinalEntry(Avernus.MODID + ".configuration." + path, enUs, zhCn);
    }

    @Override
    protected void init() {
        entry("realm", "Realm", "领域");
        entry("realm_radius", "Realm Radius", "领域半径");
        entry("close_cooldown_ticks", "Close Cooldown (ticks)", "关境冷却（tick）");
        entry("animation", "Animation", "演出");
        entry("mirror_fly_distance", "Mirror Fly Distance", "镜子飞出距离");
        entry("mirror_fly_ticks", "Mirror Fly Ticks", "镜子飞出时长");
        entry("mirror_turn_ticks", "Mirror Turn Ticks", "镜子转向时长");
        entry("shard_scatter_ticks", "Shard Scatter Ticks", "镜片飞射时长");
        entry("wave_ticks", "Wave Ticks", "波纹展开时长");
        entry("reassemble_ticks", "Reassemble Ticks", "镜片重组时长");
        entry("rebirth", "Rebirth", "往生");
        entry("regeneration_ticks", "Regeneration Ticks", "生命恢复时长");
        entry("regeneration_level", "Regeneration Level", "生命恢复等级");
        entry("absorption_ticks", "Absorption Ticks", "伤害吸收时长");
        entry("absorption_level", "Absorption Level", "伤害吸收等级");
        entry("fire_resistance_ticks", "Fire Resistance Ticks", "抗火时长");
    }
}
