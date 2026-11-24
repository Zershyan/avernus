package io.zershyan.avernus.gametest;

import com.mojang.authlib.GameProfile;
import io.zershyan.avernus.Avernus;
import io.zershyan.avernus.api.AvernusApi;
import io.zershyan.avernus.api.event.RealmAttributeEvent;
import io.zershyan.avernus.api.realm.MirrorForm;
import io.zershyan.avernus.api.realm.RealmResult;
import io.zershyan.avernus.realm.Realm;
import io.zershyan.avernus.realm.RealmAttributeEffects;
import io.zershyan.avernus.realm.RealmManager;
import io.zershyan.avernus.realm.RealmSets;
import io.zershyan.avernus.registry.AvernusAttachments;
import io.zershyan.avernus.registry.AvernusRealmAspects;
import io.zershyan.avernus.registry.AvernusRealmTraits;
import io.zershyan.avernus.registry.config.ServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** 境相属性与两级事件的服务端回归；各场景独占批次，监听器和配置均在同一 tick 内还原。 */
@GameTestHolder(Avernus.MODID)
@PrefixGameTestTemplate(false)
public final class RealmAttributeScenarios {
    private static final ResourceLocation MODIFIER = Avernus.id("realm_aspect");

    @GameTest(template = "empty", batch = "aspect_umbral", timeoutTicks = 260)
    public static void umbralUsesIndependentTransientMultipliers(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            scene.update();
            assertAmount(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.2);
            assertAmount(helper, scene.owner(), Attributes.JUMP_STRENGTH, 0.2);
            assertAmount(helper, scene.target(), Attributes.MOVEMENT_SPEED, -0.2);
            assertValue(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.24);
            assertValue(helper, scene.owner(), Attributes.JUMP_STRENGTH, 0.504);
            assertValue(helper, scene.target(), Attributes.MOVEMENT_SPEED, 0.16);
            helper.assertTrue(AvernusApi.realmAt(scene.target()) == scene.realm(), "应查询到其他生物开启的境");
            helper.assertTrue(AvernusRealmAspects.UMBRAL.getId().equals(AvernusApi.aspectAt(scene.target())), "应返回幽冥境相 ID");
            scene.update();
            assertValue(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.24);
            helper.assertFalse(scene.owner().getAttributes().save().toString().contains(MODIFIER.toString()), "境相修饰不得写入存档");
            scene.close();
            assertValue(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.2);
            assertClear(helper, scene.target());
            helper.assertTrue(AvernusApi.realmAt(scene.target()) == null && AvernusApi.aspectAt(scene.target()) == null, "移除境后查询应返回 null");
        });
    }

    @GameTest(template = "empty", batch = "aspect_infernal", timeoutTicks = 260)
    public static void infernalMultipliesAfterForeignModifiers(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.INFERNAL.getId(), scene -> {
            AttributeInstance attack = attribute(scene.owner(), Attributes.ATTACK_DAMAGE);
            var add = new AttributeModifier(Avernus.id("test_add"), 2.0, AttributeModifier.Operation.ADDITION);
            var base = new AttributeModifier(Avernus.id("test_base"), 0.5, AttributeModifier.Operation.MULTIPLY_BASE);
            var total = new AttributeModifier(Avernus.id("test_total"), 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
            attack.addTransientModifier(add);
            attack.addTransientModifier(base);
            attack.addTransientModifier(total);
            scene.update();
            assertAmount(helper, scene.owner(), Attributes.ATTACK_DAMAGE, 0.2);
            assertAmount(helper, scene.target(), Attributes.ATTACK_DAMAGE, -0.2);
            assertValue(helper, scene.owner(), Attributes.ATTACK_DAMAGE, 32.4);
            assertValue(helper, scene.target(), Attributes.ATTACK_DAMAGE, 8.0);
            scene.close();
            assertValue(helper, scene.owner(), Attributes.ATTACK_DAMAGE, 27.0);
            helper.assertTrue(attack.hasModifier(add.id()) && attack.hasModifier(base.id()) && attack.hasModifier(total.id()), "不得删除其他来源修饰");
        });
    }

    @GameTest(template = "empty", batch = "aspect_silent", timeoutTicks = 260)
    public static void silentScalesArmorAndToughness(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.SILENT.getId(), scene -> {
            scene.update();
            for (Holder<Attribute> key : List.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS)) {
                assertAmount(helper, scene.owner(), key, 0.25);
                assertAmount(helper, scene.target(), key, -0.25);
            }
            assertValue(helper, scene.owner(), Attributes.ARMOR, 10.0);
            assertValue(helper, scene.owner(), Attributes.ARMOR_TOUGHNESS, 5.0);
            assertValue(helper, scene.target(), Attributes.ARMOR, 6.0);
            assertValue(helper, scene.target(), Attributes.ARMOR_TOUGHNESS, 3.0);
            attribute(scene.owner(), Attributes.ARMOR).setBaseValue(0.0);
            scene.update();
            assertValue(helper, scene.owner(), Attributes.ARMOR, 0.0);
        });
    }

    @GameTest(template = "empty", batch = "aspect_event_chain", timeoutTicks = 260)
    public static void configurationFlowsThroughBothEventsWithoutLeakingTargets(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            double old = ServerConfig.UMBRAL_OWNER_SPEED_BONUS.get();
            AtomicInteger totals = new AtomicInteger();
            AtomicInteger units = new AtomicInteger();
            Consumer<RealmAttributeEvent.Configure> configure = event -> {
                if (!event.getRealm().id().equals(scene.realm().id())) return;
                totals.incrementAndGet();
                helper.assertTrue(event.getOwner() == scene.owner() && event.getAspectId().equals(AvernusRealmAspects.UMBRAL.getId()), "总事件须携带开境者和境相");
                helper.assertTrue(event.getTraitIds().equals(scene.realm().traitIds()), "总事件须携带境印");
                helper.assertTrue(event.getOwnerModifiers().originalValues().get(Attributes.MOVEMENT_SPEED) == 0.4, "总事件初值须来自配置");
                event.getOwnerModifiers().set(Attributes.MOVEMENT_SPEED, 0.5);
                event.getOtherModifiers().set(Attributes.MOVEMENT_SPEED, -0.3);
                event.getOtherModifiers().set(Attributes.MAX_HEALTH, 0.25);
                event.getOtherModifiers().set(Attributes.LUCK, 0.5);
            };
            Consumer<RealmAttributeEvent.Apply> apply = event -> {
                if (!event.getRealm().id().equals(scene.realm().id())) return;
                if (event.getEntity() == scene.owner() || event.getEntity() == scene.target() || event.getEntity() == scene.other()) units.incrementAndGet();
                if (event.isOwner()) {
                    helper.assertTrue(event.getModifiers().originalValues().get(Attributes.MOVEMENT_SPEED) == 0.5, "副事件须读取总事件最终值");
                    event.getModifiers().set(Attributes.MOVEMENT_SPEED, 0.6);
                } else if (event.getEntity() == scene.target()) {
                    event.getModifiers().set(Attributes.MOVEMENT_SPEED, -0.1);
                    event.getModifiers().remove(Attributes.MAX_HEALTH);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(configure);
            MinecraftForge.EVENT_BUS.addListener(apply);
            try {
                ServerConfig.UMBRAL_OWNER_SPEED_BONUS.set(0.4);
                scene.update();
                helper.assertTrue(totals.get() == 1 && units.get() == 3, "总事件每境一次，副事件每目标独立发布");
                assertValue(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.32);
                assertValue(helper, scene.target(), Attributes.MOVEMENT_SPEED, 0.18);
                assertValue(helper, scene.other(), Attributes.MOVEMENT_SPEED, 0.14);
                helper.assertFalse(attribute(scene.target(), Attributes.MAX_HEALTH).hasModifier(MODIFIER), "目标删除条目后不应施加");
                assertAmount(helper, scene.other(), Attributes.MAX_HEALTH, 0.25);
                helper.assertTrue(scene.other().getAttribute(Attributes.LUCK) == null, "缺少的属性不得强行创建");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(configure);
                MinecraftForge.EVENT_BUS.unregister(apply);
                ServerConfig.UMBRAL_OWNER_SPEED_BONUS.set(old);
            }
        });
    }

    @GameTest(template = "empty", batch = "aspect_cancel", timeoutTicks = 260)
    public static void cancelingEitherEventRevokesPreviousModifiers(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            scene.update();
            Consumer<RealmAttributeEvent.Apply> apply = event -> {
                if (event.getEntity() == scene.target()) event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(apply);
            try {
                scene.update();
                assertClear(helper, scene.target());
                assertAmount(helper, scene.other(), Attributes.MOVEMENT_SPEED, -0.2);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(apply);
            }
            Consumer<RealmAttributeEvent.Configure> configure = event -> {
                if (event.getRealm().id().equals(scene.realm().id())) event.setCanceled(true);
            };
            MinecraftForge.EVENT_BUS.addListener(configure);
            try {
                scene.update();
                assertClear(helper, scene.owner());
                assertClear(helper, scene.target());
                assertClear(helper, scene.other());
            } finally {
                MinecraftForge.EVENT_BUS.unregister(configure);
            }
            scene.update();
            assertAmount(helper, scene.target(), Attributes.MOVEMENT_SPEED, -0.2);
        });
    }

    @GameTest(template = "empty", batch = "aspect_friends", timeoutTicks = 260)
    public static void teamsAndPetsAreExcludedEvenWithFriendlyFire(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            var scoreboard = helper.getLevel().getScoreboard();
            var team = scoreboard.addPlayerTeam("aspect_" + UUID.randomUUID().toString().substring(0, 8));
            team.setAllowFriendlyFire(true);
            var pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(6.5, 2.0, 8.5));
            pet.setNoGravity(true);
            pet.setTame(true, false);
            pet.setOwnerUUID(scene.owner().getUUID());
            try {
                scene.update();
                assertAmount(helper, scene.target(), Attributes.MOVEMENT_SPEED, -0.2);
                scoreboard.addPlayerToTeam(scene.owner().getScoreboardName(), team);
                scoreboard.addPlayerToTeam(scene.target().getScoreboardName(), team);
                scene.update();
                assertClear(helper, scene.target());
                assertClear(helper, pet);
                assertAmount(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.2);
                assertAmount(helper, scene.other(), Attributes.MOVEMENT_SPEED, -0.2);
                helper.assertTrue(AvernusApi.realmAt(scene.target()) == scene.realm(), "友军免疫属性，但仍身处境中");
                scoreboard.removePlayerFromTeam(scene.target().getScoreboardName(), team);
                pet.setOwnerUUID(UUID.randomUUID());
                scene.update();
                assertAmount(helper, scene.target(), Attributes.MOVEMENT_SPEED, -0.2);
                assertAmount(helper, pet, Attributes.MOVEMENT_SPEED, -0.2);
            } finally {
                scoreboard.removePlayerTeam(team);
                pet.discard();
            }
        });
    }

    @GameTest(template = "empty", batch = "aspect_pvp", timeoutTicks = 260)
    public static void disabledPvpProtectsPlayersButNotOrdinaryMobs(GameTestHelper helper) {
        var owner = player(helper, 5.5);
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), owner, List.of(), scene -> {
            var target = player(helper, 6.5);
            var protectedPlayer = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "no-pvp-test"));
            protectedPlayer.setPos(target.position());
            configure(protectedPlayer);
            helper.getLevel().addFreshEntity(protectedPlayer);
            var server = helper.getLevel().getServer();
            boolean old = server.isPvpAllowed();
            try {
                server.setPvpAllowed(true);
                scene.update();
                assertAmount(helper, target, Attributes.MOVEMENT_SPEED, -0.2);
                assertClear(helper, protectedPlayer);
                server.setPvpAllowed(false);
                scene.update();
                assertClear(helper, target);
                assertAmount(helper, owner, Attributes.MOVEMENT_SPEED, 0.2);
                assertAmount(helper, scene.target(), Attributes.MOVEMENT_SPEED, -0.2);
            } finally {
                server.setPvpAllowed(old);
                target.discard();
                protectedPlayer.discard();
            }
        });
    }

    @GameTest(template = "empty", batch = "aspect_scope", timeoutTicks = 260)
    public static void effectsAndQueriesFollowActualEntityCoverage(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            scene.update();
            Vec3 inside = scene.target().position();
            scene.target().setPos(inside.add(1000.0, 0.0, 0.0));
            scene.update();
            assertClear(helper, scene.target());
            helper.assertTrue(AvernusApi.aspectAt(scene.target()) == null, "境外不能查到境相");
            scene.target().setPos(inside.add(0.0, -3.0, 0.0));
            scene.update();
            assertClear(helper, scene.target());
            helper.assertTrue(AvernusApi.realmAt(scene.target()) == null, "境面以下不应视为境内");
            scene.target().setPos(inside);
            scene.update();
            assertAmount(helper, scene.target(), Attributes.MOVEMENT_SPEED, -0.2);
            scene.target().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            assertClear(helper, scene.target());
        });
    }

    @GameTest(template = "empty", batch = "aspect_isolation", timeoutTicks = 260)
    public static void isolationDoesNotAffectOtherEntities(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), mob(helper, 5.5), List.of(AvernusRealmTraits.ISOLATION.getId()), scene -> {
            scene.update();
            assertAmount(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.2);
            assertClear(helper, scene.target());
            helper.assertTrue(AvernusApi.realmAt(scene.target()) == null, "隔绝境印不得把其他生物纳入当前境");
            helper.assertTrue(AvernusApi.realmAt(helper.getLevel(), scene.target().getBlockX(), scene.target().getBlockZ()) == scene.realm(), "坐标查询仍应保留不区分主体的语义");
        });
    }

    @GameTest(template = "empty", batch = "aspect_config", timeoutTicks = 260)
    public static void liveConfigurationAndZeroValuesReplaceOldModifiers(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            double speed = ServerConfig.UMBRAL_OWNER_SPEED_BONUS.get();
            double jump = ServerConfig.UMBRAL_OWNER_JUMP_BONUS.get();
            double penalty = ServerConfig.UMBRAL_OTHER_SPEED_PENALTY.get();
            try {
                scene.update();
                ServerConfig.UMBRAL_OWNER_SPEED_BONUS.set(0.5);
                ServerConfig.UMBRAL_OTHER_SPEED_PENALTY.set(1.0);
                scene.update();
                assertAmount(helper, scene.owner(), Attributes.MOVEMENT_SPEED, 0.5);
                assertValue(helper, scene.target(), Attributes.MOVEMENT_SPEED, 0.0);
                ServerConfig.UMBRAL_OWNER_SPEED_BONUS.set(0.0);
                ServerConfig.UMBRAL_OWNER_JUMP_BONUS.set(0.0);
                ServerConfig.UMBRAL_OTHER_SPEED_PENALTY.set(0.0);
                scene.update();
                assertClear(helper, scene.owner());
                assertClear(helper, scene.target());
            } finally {
                ServerConfig.UMBRAL_OWNER_SPEED_BONUS.set(speed);
                ServerConfig.UMBRAL_OWNER_JUMP_BONUS.set(jump);
                ServerConfig.UMBRAL_OTHER_SPEED_PENALTY.set(penalty);
            }
        });
    }

    @GameTest(template = "empty", batch = "aspect_overlap", timeoutTicks = 400)
    public static void newestRealmReplacesRatherThanStacksAttributes(GameTestHelper helper) {
        reset(helper);
        LivingEntity owner = mob(helper, 5.5);
        LivingEntity target = mob(helper, 6.5);
        LivingEntity newerOwner = mob(helper, 7.5);
        Realm older = open(helper, owner, AvernusRealmAspects.UMBRAL.getId(), List.of());
        var newer = new Realm[1];
        helper.startSequence().thenWaitUntil(() -> covered(helper, older, target))
                .thenExecute(() -> newer[0] = open(helper, newerOwner, AvernusRealmAspects.INFERNAL.getId(), List.of()))
                .thenWaitUntil(() -> {
                    covered(helper, newer[0], owner);
                    covered(helper, newer[0], target);
                    covered(helper, newer[0], newerOwner);
                }).thenExecute(() -> {
                    try {
                        RealmAttributeEffects.tick(helper.getLevel());
                        helper.assertTrue(AvernusApi.realmAt(target) == newer[0], "重叠处须选择最新适用境");
                        helper.assertFalse(attribute(owner, Attributes.MOVEMENT_SPEED).hasModifier(MODIFIER), "旧境移速不得残留");
                        assertAmount(helper, owner, Attributes.ATTACK_DAMAGE, -0.2);
                        assertAmount(helper, newerOwner, Attributes.ATTACK_DAMAGE, 0.2);
                        RealmManager.emergencyClose(helper.getLevel(), newer[0]);
                        RealmAttributeEffects.tick(helper.getLevel());
                        helper.assertTrue(AvernusApi.realmAt(target) == older, "新境移除后应回到旧境");
                        assertAmount(helper, owner, Attributes.MOVEMENT_SPEED, 0.2);
                        helper.assertFalse(attribute(owner, Attributes.ATTACK_DAMAGE).hasModifier(MODIFIER), "新境攻击减益不得残留");
                    } finally {
                        RealmManager.emergencyClose(helper.getLevel(), newer[0]);
                        RealmManager.emergencyClose(helper.getLevel(), older);
                    }
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "aspect_closing", timeoutTicks = 400)
    public static void normalContractionClearsEffectsWhenCoverageRecedes(GameTestHelper helper) {
        reset(helper);
        LivingEntity owner = mob(helper, 5.5);
        Realm realm = open(helper, owner, AvernusRealmAspects.UMBRAL.getId(), List.of());
        helper.assertTrue(AvernusApi.realmAt(owner) == null, "演出未覆盖时不应处于境内");
        helper.startSequence().thenWaitUntil(() -> covered(helper, realm, owner))
                .thenExecute(() -> {
                    RealmAttributeEffects.tick(helper.getLevel());
                    assertAmount(helper, owner, Attributes.MOVEMENT_SPEED, 0.2);
                    helper.assertTrue(RealmManager.requestClose(helper.getLevel(), realm), "应接受正常收境");
                })
                .thenWaitUntil(() -> helper.assertTrue(AvernusApi.realmAt(owner) == null, "收缩波面须退离实体所在列"))
                .thenExecute(() -> {
                    RealmAttributeEffects.tick(helper.getLevel());
                    assertClear(helper, owner);
                    RealmManager.emergencyClose(helper.getLevel(), realm);
                }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "aspect_event_mutation", timeoutTicks = 260)
    public static void listenersMayChangeTeamsOrCloseRealmsWithoutStaleEffects(GameTestHelper helper) {
        withRealm(helper, AvernusRealmAspects.UMBRAL.getId(), scene -> {
            scene.update();
            var scoreboard = helper.getLevel().getScoreboard();
            var team = scoreboard.addPlayerTeam("aspect_" + UUID.randomUUID().toString().substring(0, 8));
            scoreboard.addPlayerToTeam(scene.owner().getScoreboardName(), team);
            Consumer<RealmAttributeEvent.Apply> ally = event -> {
                if (event.getEntity() == scene.target()) scoreboard.addPlayerToTeam(scene.target().getScoreboardName(), team);
            };
            MinecraftForge.EVENT_BUS.addListener(ally);
            try {
                scene.update();
                assertClear(helper, scene.target());
            } finally {
                MinecraftForge.EVENT_BUS.unregister(ally);
                scoreboard.removePlayerTeam(team);
            }
            Consumer<RealmAttributeEvent.Apply> close = event -> {
                if (event.getEntity() == scene.target()) scene.close();
            };
            MinecraftForge.EVENT_BUS.addListener(close);
            try {
                scene.update();
                assertClear(helper, scene.owner());
                assertClear(helper, scene.target());
                assertClear(helper, scene.other());
                helper.assertTrue(AvernusApi.realmAt(scene.target()) == null, "事件中关闭境后不得重新施加旧属性");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(close);
            }
        });
    }

    @GameTest(template = "empty", batch = "aspect_event_validation")
    public static void modifierTablesValidateAndDefensivelyCopyValues(GameTestHelper helper) {
        Map<Holder<Attribute>, Double> initial = new HashMap<>();
        initial.put(Attributes.MOVEMENT_SPEED, 0.2);
        var table = new RealmAttributeEvent.Modifiers(initial);
        initial.clear();
        helper.assertTrue(table.values().get(Attributes.MOVEMENT_SPEED) == 0.2, "不得保留调用方的可变集合");
        table.set(Attributes.MOVEMENT_SPEED, 0.4);
        helper.assertTrue(table.originalValues().get(Attributes.MOVEMENT_SPEED) == 0.2, "修改最终表不得改变原始值");
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.01}) {
            boolean rejected = false;
            try {
                table.set(Attributes.MOVEMENT_SPEED, value);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "非法比例须立即拒绝：" + value);
        }
        boolean readOnly = false;
        try {
            table.values().clear();
        } catch (UnsupportedOperationException expected) {
            readOnly = true;
        }
        helper.assertTrue(readOnly, "结果集合不得允许绕过校验直接写入");
        table.clear();
        helper.assertTrue(table.values().isEmpty() && !table.originalValues().isEmpty(), "清空结果不得影响原始表");
        helper.succeed();
    }

    private record Scene(GameTestHelper helper, LivingEntity owner, Zombie target, Zombie other, Realm realm) {
        void update() {
            RealmAttributeEffects.tick(helper.getLevel());
        }

        void close() {
            RealmManager.emergencyClose(helper.getLevel(), realm);
        }
    }

    private static void withRealm(GameTestHelper helper, ResourceLocation aspect, Consumer<Scene> action) {
        withRealm(helper, aspect, mob(helper, 5.5), List.of(), action);
    }

    private static void withRealm(GameTestHelper helper, ResourceLocation aspect, LivingEntity owner,
                                  List<ResourceLocation> traits, Consumer<Scene> action) {
        reset(helper);
        configure(owner);
        var scene = new Scene(helper, owner, mob(helper, 6.5), mob(helper, 7.5), open(helper, owner, aspect, traits));
        helper.startSequence().thenWaitUntil(() -> {
                    covered(helper, scene.realm(), scene.owner());
                    covered(helper, scene.realm(), scene.target());
                    covered(helper, scene.realm(), scene.other());
                }).thenExecute(() -> {
                    try {
                        action.accept(scene);
                    } finally {
                        scene.close();
                        owner.discard();
                    }
                }).thenSucceed();
    }

    private static Realm open(GameTestHelper helper, LivingEntity owner, ResourceLocation aspect, List<ResourceLocation> traits) {
        helper.assertTrue(AvernusApi.open(owner, new MirrorForm(aspect, traits)) == RealmResult.OPENED, "测试领域应开启");
        return Objects.requireNonNull(RealmManager.find(helper.getLevel().getServer(), owner.getUUID()));
    }

    private static void reset(GameTestHelper helper) {
        for (Realm realm : RealmSets.of(helper.getLevel()).all()) RealmManager.emergencyClose(helper.getLevel(), realm);
    }

    private static Zombie mob(GameTestHelper helper, double x) {
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, 2.0, 8.5));
        configure(entity);
        return entity;
    }

    private static FakePlayer player(GameTestHelper helper, double x) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "aspect-test")) {
            @Override
            public boolean canHarmPlayer(Player other) {
                return true;
            }
        };
        player.setPos(helper.absoluteVec(new Vec3(x, 2.0, 8.5)));
        configure(player);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void configure(LivingEntity entity) {
        entity.setNoGravity(true);
        entity.setInvulnerable(true);
        attribute(entity, Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
        attribute(entity, Attributes.JUMP_STRENGTH).setBaseValue(0.42);
        attribute(entity, Attributes.ATTACK_DAMAGE).setBaseValue(10.0);
        attribute(entity, Attributes.ARMOR).setBaseValue(8.0);
        attribute(entity, Attributes.ARMOR_TOUGHNESS).setBaseValue(4.0);
    }

    private static void covered(GameTestHelper helper, Realm realm, LivingEntity target) {
        helper.assertTrue(realm.covers(target.getBlockX(), target.getBlockZ(), helper.getLevel().getGameTime()), "波面尚未覆盖目标列");
    }

    private static AttributeInstance attribute(LivingEntity entity, Holder<Attribute> key) {
        return Objects.requireNonNull(entity.getAttribute(key));
    }

    private static void assertAmount(GameTestHelper helper, LivingEntity entity, Holder<Attribute> key, double amount) {
        AttributeModifier modifier = attribute(entity, key).getModifier(MODIFIER);
        helper.assertTrue(modifier != null && Math.abs(modifier.amount() - amount) < 1.0E-9
                && modifier.operation() == AttributeModifier.Operation.MULTIPLY_TOTAL, "属性须采用预期的独立总乘区：" + key);
    }

    private static void assertValue(GameTestHelper helper, LivingEntity entity, Holder<Attribute> key, double expected) {
        helper.assertTrue(Math.abs(entity.getAttributeValue(key) - expected) < 1.0E-6,
                "属性值不符合预期：" + key + "，实际=" + entity.getAttributeValue(key) + "，预期=" + expected);
    }

    private static void assertClear(GameTestHelper helper, LivingEntity entity) {
        for (Holder<Attribute> key : List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.ATTACK_DAMAGE,
                Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.MAX_HEALTH)) {
            AttributeInstance value = entity.getAttribute(key);
            helper.assertTrue(value == null || !value.hasModifier(MODIFIER), "不应残留境相修饰：" + key);
        }
        helper.assertTrue(entity.getExistingDataOrNull(AvernusAttachments.REALM_ATTRIBUTES) == null, "不应残留属性来源附件");
    }
}
