package io.zershyan.avernus.datagen.lang;

import io.zershyan.avernus.registry.AvernusEntities;
import io.zershyan.avernus.registry.AvernusItems;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.AvernusSounds;
import io.zershyan.avernus.registry.realm_aspect.builder.RealmAspect;
import io.zershyan.avernus.registry.realm_trait.builder.RealmTrait;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public class AvernusRegistryLang extends AvernusLang {

    /** 境相名、描述与对应镜子的名称；境相只在提示中显示，不进物品名。 */
    private void aspect(RegistryObject<RealmAspect> holder, String enUs, String zhCn, String descEn, String descZh) {
        addFinalEntry(RealmAspect.languageKey(holder.getId()), enUs, zhCn);
        addFinalEntry(RealmAspect.descriptionKey(holder.getId()), descEn, descZh);
        addFinalEntry(AvernusItems.MIRRORS_BY_ASPECT.get(holder).get(), "Mirror of Avernus", "阿维尔诺之镜");
    }

    /** 境印名称、作用说明与对应刻印材料的名称。 */
    private void trait(RegistryObject<RealmTrait> holder, String enUs, String zhCn, String descEn, String descZh) {
        addFinalEntry(RealmTrait.languageKey(holder.getId()), enUs + " Sigil", zhCn + "境印");
        addFinalEntry(RealmTrait.descriptionKey(holder.getId()), descEn, descZh);
        addFinalEntry(AvernusItems.SIGILS.get(holder).get(), enUs + " Sigil", zhCn + "境印");
    }

    private void subtitle(RegistryObject<SoundEvent> sound, String enUs, String zhCn) {
        addFinalEntry(sound.getId().toLanguageKey("subtitles"), enUs, zhCn);
    }

    @Override
    protected void init() {
        subtitle(AvernusSounds.MIRROR_THROW, "Mirror flies out", "镜子飞出");
        subtitle(AvernusSounds.SHATTER, "Mirror shatters", "镜子碎裂");
        subtitle(AvernusSounds.EXPAND, "Realm unfolds", "领域展开");
        subtitle(AvernusSounds.CONTRACT, "Realm recedes", "领域收缩");
        subtitle(AvernusSounds.MIRROR_RETURN, "Mirror returns", "镜子归来");

        addFinalEntry(AvernusEntities.MIRROR.get(), "Mirror of Avernus", "阿维尔诺之镜");

        aspect(AvernusRealmAspects.UMBRAL, "Umbral", "幽冥",
                "Grants the owner greater movement speed and jump strength; slows foes inside.", "提高开境者的移速与跳跃强度，降低境内敌方生物的移速。");
        aspect(AvernusRealmAspects.SILENT, "Silent", "寂静",
                "Strengthens the owner's armor and armor toughness; weakens those of foes inside.", "提高开境者的护甲值与盔甲韧性，降低境内敌方生物的护甲值与盔甲韧性。");
        aspect(AvernusRealmAspects.INFERNAL, "Infernal", "炼狱",
                "Increases the owner's attack damage; reduces that of foes inside.", "提高开境者的攻击伤害，降低境内敌方生物的攻击伤害。");

        trait(AvernusRealmTraits.ISOLATION, "Isolation", "隔绝",
                "The rewritten ground exists for the owner alone", "重写的地形只对开境者生效");
        trait(AvernusRealmTraits.ANNIHILATION, "Annihilation", "湮没",
                "Creatures slain inside drop nothing", "境内死去的生物不留下任何掉落物");
        trait(AvernusRealmTraits.ILLUSION, "Illusion", "幻化",
                "Hostile creatures ignore players inside until struck", "境内敌对生物不会主动攻击玩家，除非先被攻击");
        trait(AvernusRealmTraits.REBIRTH, "Rebirth", "往生",
                "Saves the owner from a fatal blow once, at the cost of the realm", "致命伤时救回开境者一次，领域随之破碎");
        trait(AvernusRealmTraits.WEAK_WATER, "Weak Water", "弱水",
                "The whole surface turns to weak water over a bottomless dark", "境面整片化为弱水，水下深不见底");
        trait(AvernusRealmTraits.BOUNDLESS, "Boundless", "无限",
                "The surface is sealed whole, without a single hole", "境面完整封合，不留一处空洞");
        trait(AvernusRealmTraits.UNREALITY, "Unreality", "虚妄",
                "The owner is immune to damage with no attacker", "开境者免疫没有攻击者的伤害");
        trait(AvernusRealmTraits.ETERNITY, "Eternity", "永恒",
                "The mirror hovers overhead and the realm follows its owner", "镜子悬于头顶，领域随开境者移动");
        trait(AvernusRealmTraits.EQUALITY, "Equality", "平等",
                "Creatures below the surface are raised onto it", "境面以下的生物会被带到境面之上");
        trait(AvernusRealmTraits.FREEDOM, "Freedom", "自由",
                "The owner may fly freely inside the realm", "开境者在境内可以自由飞行");
    }
}
