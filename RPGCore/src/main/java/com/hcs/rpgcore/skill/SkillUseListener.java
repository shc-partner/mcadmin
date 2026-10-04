package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;
import com.hcs.rpgcore.mana.ManaService;

import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Sound;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.Action;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.bukkit.projectiles.ProjectileSource;


public final class SkillUseListener
        implements Listener {

    /*
     * 개발/테스트 계정.
     *
     * 이 플레이어는 모든 RPG 스킬의
     * 쿨타임 검사 및 등록을 생략한다.
     */
    private static final String COOLDOWN_BYPASS_PLAYER =
            "Mal_Nyun";


    private final SkillItemFactory skillItemFactory;
    private final SkillCooldownService cooldownService;

    private final ManaService manaService;
    private final HudService hudService;

    private final WarriorBashSkill bashSkill;
    private final MageFireBoltSkill fireBoltSkill;

    private final WarriorWhirlwindSkill whirlwindSkill;
    private final MageFrostNovaSkill frostNovaSkill;

    private final WarriorExecutionSlashSkill executionSlashSkill;
    private final MageChainLightningSkill chainLightningSkill;

    private final WarriorIronWallSkill ironWallSkill;
    private final MageManaShieldSkill manaShieldSkill;

    private final WarriorEarthquakeSlamSkill earthquakeSlamSkill;
    private final MageBlizzardSkill blizzardSkill;

    private final WarriorBattleCrySkill battleCrySkill;
    private final MageBlackHoleSkill blackHoleSkill;

    private final WarriorBerserkerRageSkill berserkerRageSkill;
    private final MageManaOverloadSkill manaOverloadSkill;

    private final WarriorBladeStormSkill bladeStormSkill;
    private final MageThunderStormSkill thunderStormSkill;

    private final MageMeteorSkill meteorSkill;
    private final WarriorInfiniteBladeSkill gateOfBabylonSkill;

    private final SkillAccessService accessService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public SkillUseListener(
            SkillItemFactory skillItemFactory,
            SkillCooldownService cooldownService,
            ManaService manaService,
            HudService hudService,
            WarriorBashSkill bashSkill,
            MageFireBoltSkill fireBoltSkill,
            WarriorWhirlwindSkill whirlwindSkill,
            MageFrostNovaSkill frostNovaSkill,
            WarriorExecutionSlashSkill executionSlashSkill,
            MageChainLightningSkill chainLightningSkill,
            WarriorIronWallSkill ironWallSkill,
            MageManaShieldSkill manaShieldSkill,
            WarriorEarthquakeSlamSkill earthquakeSlamSkill,
            MageBlizzardSkill blizzardSkill,
            WarriorBattleCrySkill battleCrySkill,
            MageBlackHoleSkill blackHoleSkill,
            WarriorBerserkerRageSkill berserkerRageSkill,
            MageManaOverloadSkill manaOverloadSkill,
            WarriorBladeStormSkill bladeStormSkill,
            MageThunderStormSkill thunderStormSkill,
            MageMeteorSkill meteorSkill,
            WarriorInfiniteBladeSkill gateOfBabylonSkill,
            SkillAccessService accessService,
            SkillOffenseBuffService offenseBuffService
    ) {

        this.skillItemFactory =
                skillItemFactory;

        this.cooldownService =
                cooldownService;

        this.manaService =
                manaService;

        this.hudService =
                hudService;

        this.bashSkill =
                bashSkill;

        this.fireBoltSkill =
                fireBoltSkill;

        this.whirlwindSkill =
                whirlwindSkill;

        this.frostNovaSkill =
                frostNovaSkill;

        this.executionSlashSkill =
                executionSlashSkill;

        this.chainLightningSkill =
                chainLightningSkill;

        this.ironWallSkill =
                ironWallSkill;

        this.manaShieldSkill =
                manaShieldSkill;

        this.earthquakeSlamSkill =
                earthquakeSlamSkill;

        this.blizzardSkill =
                blizzardSkill;

        this.battleCrySkill =
                battleCrySkill;

        this.blackHoleSkill =
                blackHoleSkill;

        this.berserkerRageSkill =
                berserkerRageSkill;

        this.manaOverloadSkill =
                manaOverloadSkill;

        this.bladeStormSkill =
                bladeStormSkill;

        this.thunderStormSkill =
                thunderStormSkill;

        this.meteorSkill =
                meteorSkill;

        this.gateOfBabylonSkill =
                gateOfBabylonSkill;

        this.accessService =
                accessService;

        this.offenseBuffService =
                offenseBuffService;
    }


    /*
     * =========================================================
     * COMMON SKILL EXECUTION
     * =========================================================
     *
     * 우클릭과 핫바 단축키가
     * 정확히 동일한 스킬 실행 경로를 사용한다.
     */
    public void executeSkill(
            Player player,
            ItemStack item
    ) {

        if (
                player == null
                || !skillItemFactory.isSkillItem(
                        item
                )
        ) {
            return;
        }


        String skillId =
                skillItemFactory.getSkillId(
                        item
                );


        if (skillId == null) {
            return;
        }


        /*
         * =====================================================
         * SKILL BOOK OWNER CHECK
         * =====================================================
         *
         * owner_uuid가 없거나 현재 플레이어와 다르면
         * 어떠한 스킬 효과도 실행하지 않는다.
         *
         * MP 소모 / 쿨타임 적용 이전에 차단한다.
         */
        if (
                !skillItemFactory.isOwnedBy(
                        item,
                        player.getUniqueId()
                )
        ) {

            sendNotSkillOwner(
                    player
            );

            return;
        }


        /*
         * 실제 스킬 실행은 skillId 기반 공통 경로로 넘긴다.
         *
         * 기존 스킬북 우클릭 / 기존 핫바 실행은
         * owner_uuid 검증을 그대로 거친다.
         */
        executeSkill(
                player,
                skillId
        );
    }

    /*
     * =========================================================
     * COMMON SKILL EXECUTION BY ID
     * =========================================================
     *
     * 스킬 도감 / 스킬 슬롯 시스템 전용 공통 실행 경로.
     *
     * 실제 스킬북 ItemStack이 없어도
     * 등록된 skillId만으로 기존 스킬 로직을 실행한다.
     *
     * 직업 / 레벨 / MP / 쿨타임 검사는
     * 기존 코드 그대로 유지한다.
     */
    public void executeSkill(
            Player player,
            String skillId
    ) {

        if (
                player == null
                || skillId == null
                || skillId.isBlank()
        ) {
            return;
        }


        HudSnapshot snapshot =
                hudService.getProfile(
                        player.getUniqueId()
                );


        if (snapshot == null) {

            player.sendMessage(
                    Component.text(
                            "RPG 데이터를 불러오는 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        String playerClass =
                snapshot.playerClass();


        if (
                SkillIds.WARRIOR_BASH.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            useBash(
                    player
            );

            return;
        }


        if (
                SkillIds.MAGE_FIRE_BOLT.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            useFireBolt(
                    player
            );

            return;
        }


                        /*
         * =====================================================
         * LEVEL 20 - WARRIOR WHIRLWIND
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_WHIRLWIND.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 20
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.20부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useWhirlwind(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 20 - MAGE FROST NOVA
         * =====================================================
         */
        if (
                SkillIds.MAGE_FROST_NOVA.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 20
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.20부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useFrostNova(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 30 - WARRIOR EXECUTION SLASH
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_EXECUTION_SLASH.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 30
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.30부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useExecutionSlash(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 30 - MAGE CHAIN LIGHTNING
         * =====================================================
         */
        if (
                SkillIds.MAGE_CHAIN_LIGHTNING.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 30
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.30부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useChainLightning(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 40 - WARRIOR IRON WALL
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_IRON_WALL.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 40
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.40부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useIronWall(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 40 - MAGE MANA SHIELD
         * =====================================================
         */
        if (
                SkillIds.MAGE_MANA_SHIELD.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 40
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.40부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useManaShield(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 50 - WARRIOR EARTHQUAKE SLAM
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_EARTHQUAKE_SLAM.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 50
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.50부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useEarthquakeSlam(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 50 - MAGE BLIZZARD
         * =====================================================
         */
        if (
                SkillIds.MAGE_BLIZZARD.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 50
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.50부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useBlizzard(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 60 - WARRIOR BATTLE CRY
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_BATTLE_CRY.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 60
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.60부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useBattleCry(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 60 - MAGE BLACK HOLE
         * =====================================================
         */
        if (
                SkillIds.MAGE_BLACK_HOLE.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 60
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.60부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useBlackHole(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 70 - WARRIOR BERSERKER RAGE
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_BERSERKER_RAGE.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(player);
                return;
            }


            if (snapshot.level() < 70) {

                player.sendActionBar(
                        Component.text(
                                "Lv.70부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useBerserkerRage(player);
            return;
        }


        /*
         * =====================================================
         * LEVEL 70 - MAGE MANA OVERLOAD
         * =====================================================
         */
        if (
                SkillIds.MAGE_MANA_OVERLOAD.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(player);
                return;
            }


            if (snapshot.level() < 70) {

                player.sendActionBar(
                        Component.text(
                                "Lv.70부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useManaOverload(player);
            return;
        }


        /*
         * =====================================================
         * LEVEL 80 - WARRIOR BLADE STORM
         * =====================================================
         */
        if (
                SkillIds.WARRIOR_BLADE_STORM.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(player);
                return;
            }


            if (snapshot.level() < 80) {

                player.sendActionBar(
                        Component.text(
                                "Lv.80부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useBladeStorm(
                    player
            );

            return;
        }


        /*
         * =====================================================
         * LEVEL 80 - MAGE THUNDER STORM
         * =====================================================
         */
        if (
                SkillIds.MAGE_THUNDER_STORM.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(player);
                return;
            }


            if (snapshot.level() < 80) {

                player.sendActionBar(
                        Component.text(
                                "Lv.80부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useThunderStorm(
                    player
            );

            return;
        }


        if (
                SkillIds.WARRIOR_GATE_OF_BABYLON.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "WARRIOR"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            if (
                    snapshot.level()
                            < 90
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.90부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useGateOfBabylon(
                    player
            );

            return;
        }


        if (
                SkillIds.MAGE_METEOR.equals(
                        skillId
                )
        ) {

            if (
                    !accessService.canUseClass(
                            player,
                            playerClass,
                            "MAGE"
                    )
            ) {

                sendWrongClass(
                        player
                );

                return;
            }


            /*
             * Mal_Nyun도 직업 제한만 무시한다.
             *
             * Lv.90 요구 조건은 그대로 적용.
             */
            if (
                    snapshot.level()
                            < 90
            ) {

                player.sendActionBar(
                        Component.text(
                                "Lv.90부터 사용할 수 있습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }


            useMeteor(
                    player
            );
        }
        }



    /*
     * =========================================================
     * BASH
     * =========================================================
     */
    private void useBash(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_BASH
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorBashSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                bashSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorBashSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_BASH,
                WarriorBashSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "배쉬",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -20",
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * FIRE BOLT
     * =========================================================
     */
    private void useFireBolt(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_FIRE_BOLT
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageFireBoltSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                fireBoltSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageFireBoltSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_FIRE_BOLT,
                MageFireBoltSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "파이어 볼트",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageFireBoltSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * WHIRLWIND
     * =========================================================
     */
    private void useWhirlwind(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_WHIRLWIND
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorWhirlwindSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                whirlwindSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorWhirlwindSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_WHIRLWIND,
                WarriorWhirlwindSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "휠윈드",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -8",
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * FROST NOVA
     * =========================================================
     */
    private void useFrostNova(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_FROST_NOVA
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageFrostNovaSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                frostNovaSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageFrostNovaSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_FROST_NOVA,
                MageFrostNovaSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "프로스트 노바",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageFrostNovaSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * EXECUTION SLASH
     * =========================================================
     */
    private void useExecutionSlash(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_EXECUTION_SLASH
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorExecutionSlashSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                executionSlashSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorExecutionSlashSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_EXECUTION_SLASH,
                WarriorExecutionSlashSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "익스큐션 슬래시",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -12",
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * CHAIN LIGHTNING
     * =========================================================
     */
    private void useChainLightning(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_CHAIN_LIGHTNING
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageChainLightningSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                chainLightningSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageChainLightningSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_CHAIN_LIGHTNING,
                MageChainLightningSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "체인 라이트닝",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageChainLightningSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * IRON WALL
     * =========================================================
     */
    private void useIronWall(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_IRON_WALL
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorIronWallSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                ironWallSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorIronWallSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_IRON_WALL,
                WarriorIronWallSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "아이언 월",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -15",
                                NamedTextColor.AQUA
                        )
                ).append(
                        Component.text(
                                "  6초",
                                NamedTextColor.GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * MANA SHIELD
     * =========================================================
     */
    private void useManaShield(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_MANA_SHIELD
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageManaShieldSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                manaShieldSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageManaShieldSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_MANA_SHIELD,
                MageManaShieldSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "마나 실드",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageManaShieldSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                ).append(
                        Component.text(
                                "  8초",
                                NamedTextColor.GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * EARTHQUAKE SLAM
     * =========================================================
     */
    private void useEarthquakeSlam(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_EARTHQUAKE_SLAM
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorEarthquakeSlamSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                earthquakeSlamSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorEarthquakeSlamSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_EARTHQUAKE_SLAM,
                WarriorEarthquakeSlamSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "어스퀘이크 슬램",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -20",
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * BLIZZARD
     * =========================================================
     */
    private void useBlizzard(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_BLIZZARD
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageBlizzardSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                blizzardSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageBlizzardSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_BLIZZARD,
                MageBlizzardSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "블리자드",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageBlizzardSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * BATTLE CRY
     * =========================================================
     */
    private void useBattleCry(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_BATTLE_CRY
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorBattleCrySkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                battleCrySkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorBattleCrySkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_BATTLE_CRY,
                WarriorBattleCrySkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "배틀 크라이",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -18",
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * BLACK HOLE
     * =========================================================
     */
    private void useBlackHole(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_BLACK_HOLE
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageBlackHoleSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                blackHoleSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageBlackHoleSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_BLACK_HOLE,
                MageBlackHoleSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "블랙홀",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageBlackHoleSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * BERSERKER RAGE
     * =========================================================
     */
    private void useBerserkerRage(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_BERSERKER_RAGE
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorBerserkerRageSkill.MANA_COST
                )
        ) {

            sendManaLack(player);
            return;
        }


        boolean success =
                berserkerRageSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorBerserkerRageSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_BERSERKER_RAGE,
                WarriorBerserkerRageSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "버서커 레이지",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -25",
                                NamedTextColor.AQUA
                        )
                ).append(
                        Component.text(
                                "  10초",
                                NamedTextColor.GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * MANA OVERLOAD
     * =========================================================
     */
    private void useManaOverload(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_MANA_OVERLOAD
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        MageManaOverloadSkill.MANA_COST
                )
        ) {

            sendManaLack(player);
            return;
        }


        boolean success =
                manaOverloadSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    MageManaOverloadSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_MANA_OVERLOAD,
                MageManaOverloadSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "마나 오버로드",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -60",
                                NamedTextColor.AQUA
                        )
                ).append(
                        Component.text(
                                "  10초",
                                NamedTextColor.GRAY
                        )
                )
        );
    }


    /*
     * =========================================================
     * BLADE STORM
     * =========================================================
     */
    private void useBladeStorm(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_BLADE_STORM
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorBladeStormSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                bladeStormSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorBladeStormSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_BLADE_STORM,
                WarriorBladeStormSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "블레이드 스톰",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -35",
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    /*
     * =========================================================
     * GATE OF BABYLON
     * =========================================================
     */
    /*
     * =========================================================
     * THUNDER STORM
     * =========================================================
     */
    private void useThunderStorm(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_THUNDER_STORM
                )
        ) {
            return;
        }


        /*
         * 마나 오버로드가 활성 상태라면
         * MP 소모량 +25%.
         *
         * 한 번 계산한 값을 consume / restore 모두에서
         * 동일하게 사용한다.
         */
        double manaCost =
                resolveMageManaCost(
                        uuid,
                        MageThunderStormSkill.MANA_COST
                );


        if (
                !manaService.consume(
                        uuid,
                        manaCost
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                thunderStormSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    manaCost
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_THUNDER_STORM,
                MageThunderStormSkill.COOLDOWN_MILLIS
        );


        String manaCostText;

        if (
                Math.abs(
                        manaCost
                                - Math.rint(manaCost)
                ) < 0.000001
        ) {

            manaCostText =
                    Long.toString(
                            Math.round(
                                    manaCost
                            )
                    );

        } else {

            manaCostText =
                    Double.toString(
                            manaCost
                    );
        }


        player.sendActionBar(
                Component.text(
                        "썬더 스톰",
                        NamedTextColor.LIGHT_PURPLE
                ).append(
                        Component.text(
                                "  MP -" + manaCostText,
                                NamedTextColor.AQUA
                        )
                )
        );
    }


    private void useGateOfBabylon(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.WARRIOR_GATE_OF_BABYLON
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        WarriorInfiniteBladeSkill.MANA_COST
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                gateOfBabylonSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    WarriorInfiniteBladeSkill.MANA_COST
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.WARRIOR_GATE_OF_BABYLON,
                WarriorInfiniteBladeSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "인피니트 블레이드",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "  MP -30",
                                NamedTextColor.AQUA
                        )
                )
        );
    }



    /*
     * =========================================================
     * METEOR
     * =========================================================
     */
    private void useMeteor(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !checkCooldown(
                        player,
                        SkillIds.MAGE_METEOR
                )
        ) {
            return;
        }


        if (
                !manaService.consume(
                        uuid,
                        resolveMageManaCost(
                                uuid,
                                MageMeteorSkill.MANA_COST
                        )
                )
        ) {

            sendManaLack(
                    player
            );

            return;
        }


        boolean success =
                meteorSkill.cast(
                        player
                );


        if (!success) {

            manaService.restore(
                    uuid,
                    resolveMageManaCost(
                            uuid,
                            MageMeteorSkill.MANA_COST
                    )
            );

            return;
        }


        startCooldown(
                player,
                uuid,
                SkillIds.MAGE_METEOR,
                MageMeteorSkill.COOLDOWN_MILLIS
        );


        player.sendActionBar(
                Component.text(
                        "메테오 스트라이크",
                        NamedTextColor.RED
                ).append(
                        Component.text(
                                "  MP -" + formatManaCost(resolveMageManaCost(uuid, MageMeteorSkill.MANA_COST)),
                                NamedTextColor.AQUA
                        )
                )
        );
    }



    /*
     * =========================================================
     * MANA COST DISPLAY
     * =========================================================
     */
    private String formatManaCost(
            double manaCost
    ) {

        if (
                Math.abs(
                        manaCost
                                - Math.rint(manaCost)
                ) < 0.000001
        ) {

            return Long.toString(
                    Math.round(
                            manaCost
                    )
            );
        }


        String text =
                String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        manaCost
                );


        while (
                text.contains(".")
                && text.endsWith("0")
        ) {

            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }


        if (text.endsWith(".")) {

            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }


        return text;
    }


    /*
     * =========================================================
     * MAGE MANA COST
     * =========================================================
     */
    private double resolveMageManaCost(
            UUID uuid,
            double baseCost
    ) {

        if (
                offenseBuffService == null
        ) {
            return baseCost;
        }


        return offenseBuffService
                .applyMageManaCostMultiplier(
                        uuid,
                        baseCost
                );
    }


    /*
     * =========================================================
     * FIRE BOLT HIT
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onFireBoltHit(
            ProjectileHitEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof LargeFireball fireball)
        ) {
            return;
        }


        if (!fireBoltSkill.isFireBolt(fireball)) {
            return;
        }


        ProjectileSource shooter =
                fireball.getShooter();


        if (!(shooter instanceof Player player)) {

            fireball.remove();

            return;
        }


        double damage =
                fireBoltSkill.getStoredDamage(
                        fireball
                );


        /*
         * =====================================================
         * MANA OVERLOAD
         * =====================================================
         *
         * 명중하는 순간 마나 오버로드가 활성 상태라면
         * 파이어 볼트 최종 스킬 피해 +30%.
         *
         * 저장된 기본 피해:
         *
         * 10 + ATK * 0.30 + 최대 MP * 0.03
         *
         * 오버로드 활성:
         *
         * stored damage * 1.30
         */
        if (offenseBuffService != null) {

            damage =
                    offenseBuffService
                            .applyMageDamageMultiplier(
                                    player.getUniqueId(),
                                    damage
                            );
        }


        for (
                Entity nearby
                : fireball.getNearbyEntities(
                        MageFireBoltSkill.EXPLOSION_RANGE,
                        MageFireBoltSkill.EXPLOSION_RANGE,
                        MageFireBoltSkill.EXPLOSION_RANGE
                )
        ) {

            /*
             * 현재 단계에서는 hostile mob만 피해.
             */
            if (!(nearby instanceof Enemy)) {
                continue;
            }


            if (!(nearby instanceof LivingEntity target)) {
                continue;
            }


            if (target.isDead()) {
                continue;
            }


            target.damage(
                    damage,
                    player
            );
        }


        fireball.getWorld()
                .playSound(
                        fireball.getLocation(),
                        Sound.ENTITY_GENERIC_EXPLODE,
                        1.0f,
                        1.25f
                );


        /*
         * 수동 광역 피해 적용 후 투사체 제거.
         */
        fireball.remove();
    }


    /*
     * =========================================================
     * BLOCK VANILLA FIREBALL DAMAGE
     * =========================================================
     *
     * RPGCore가 계산한 피해만 적용하기 위해
     * 해당 스킬 투사체의 바닐라 직접 피해를 차단한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onFireBoltVanillaDamage(
            EntityDamageByEntityEvent event
    ) {

        if (
                !(event.getDamager()
                        instanceof LargeFireball fireball)
        ) {
            return;
        }


        if (!fireBoltSkill.isFireBolt(fireball)) {
            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * BLOCK VANILLA EXPLOSION
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onFireBoltExplosion(
            ExplosionPrimeEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof LargeFireball fireball)
        ) {
            return;
        }


        if (!fireBoltSkill.isFireBolt(fireball)) {
            return;
        }


        /*
         * Paper ExplosionPrimeEvent는 폭발 자체를 취소할 수 있다.
         *
         * 지형 파괴 / 화재 / 바닐라 폭발 피해를
         * 모두 제거하고 RPGCore 피해만 사용한다.
         */
        event.setFire(
                false
        );

        event.setRadius(
                0.0f
        );

        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * COOLDOWN
     * =========================================================
     */
    private void startCooldown(
            Player player,
            UUID uuid,
            String skillId,
            long cooldownMillis
    ) {

        /*
         * Mal_Nyun 개발 계정에는
         * 쿨타임 종료 시각 자체를 저장하지 않는다.
         */
        if (isCooldownBypassPlayer(player)) {
            return;
        }


        cooldownService.startCooldown(
                uuid,
                skillId,
                cooldownMillis
        );
    }


    private boolean isCooldownBypassPlayer(
            Player player
    ) {

        return player != null
                && COOLDOWN_BYPASS_PLAYER
                        .equalsIgnoreCase(
                                player.getName()
                        );
    }


    private boolean checkCooldown(
            Player player,
            String skillId
    ) {

        /*
         * Mal_Nyun 개발 계정은
         * 쿨타임을 검사하지 않는다.
         */
        if (isCooldownBypassPlayer(player)) {
            return true;
        }


        long remaining =
                cooldownService
                        .getRemainingMillis(
                                player.getUniqueId(),
                                skillId
                        );


        if (remaining <= 0L) {
            return true;
        }


        double seconds =
                Math.ceil(
                        remaining / 100.0
                ) / 10.0;


        player.sendActionBar(
                Component.text(
                        "쿨타임 "
                                + seconds
                                + "초",
                        NamedTextColor.RED
                )
        );


        return false;
    }


    private void sendManaLack(
            Player player
    ) {

        player.sendActionBar(
                Component.text(
                        "MP가 부족합니다.",
                        NamedTextColor.RED
                )
        );


        player.playSound(
                player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS,
                0.7f,
                0.8f
        );
    }


    private void sendWrongClass(
            Player player
    ) {

        player.sendMessage(
                Component.text(
                        "현재 직업으로 사용할 수 없는 스킬입니다.",
                        NamedTextColor.RED
                )
        );
    }


    /*
     * =========================================================
     * SKILL BOOK OWNER ERROR
     * =========================================================
     */
    private void sendNotSkillOwner(
            Player player
    ) {

        player.sendMessage(
                Component.text(
                        "본인에게 귀속된 스킬북만 사용할 수 있습니다.",
                        NamedTextColor.RED
                )
        );
    }
}
