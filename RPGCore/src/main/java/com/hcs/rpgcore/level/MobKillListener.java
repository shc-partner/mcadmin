package com.hcs.rpgcore.level;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.mana.ManaService;
import com.hcs.rpgcore.mob.EliteDemonKnightListener;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;
import com.hcs.rpgcore.stat.PlayerStatApplier;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;
import com.hcs.rpgcore.skill.SkillAccessService;
import com.hcs.rpgcore.skill.SkillIds;
import com.hcs.rpgcore.skill.SkillItemFactory;
import com.hcs.rpgcore.skill.SkillRewardService;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class MobKillListener implements Listener {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final LevelService levelService;
    private final MobExperienceService mobExperienceService;
    private final DungeonMobExperienceService dungeonMobExperienceService;

    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey simpleDungeonExpKey;

    private final StatService statService;
    private final PlayerStatApplier statApplier;

    private final ManaService manaService;
    private final HudService hudService;

    private final SkillItemFactory skillItemFactory;
    private final SkillAccessService skillAccessService;
    private final SkillRewardService skillRewardService;

    public MobKillListener(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            LevelService levelService,
            MobExperienceService mobExperienceService,
            StatService statService,
            PlayerStatApplier statApplier,
            ManaService manaService,
            HudService hudService,
            SkillItemFactory skillItemFactory,
            SkillAccessService skillAccessService,
            SkillRewardService skillRewardService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.levelService = levelService;
        this.mobExperienceService = mobExperienceService;

        this.dungeonMobExperienceService =
                new DungeonMobExperienceService();

        this.dungeonIdKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_id"
                );

        this.simpleDungeonExpKey =
                new NamespacedKey(
                        plugin,
                        "simple_dungeon_exp"
                );

        this.statService = statService;
        this.statApplier = statApplier;
        this.manaService = manaService;
        this.hudService = hudService;

        this.skillItemFactory = skillItemFactory;
        this.skillAccessService = skillAccessService;
        this.skillRewardService = skillRewardService;
    }

    @EventHandler
    public void onEntityDeath(
            EntityDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        Player killer =
                entity.getKiller();

        if (killer == null) {
            return;
        }

        long gainedExperience;

        /*
         * =====================================================
         * SIMPLE DUNGEON EXP
         * =====================================================
         *
         * 최우선 판정.
         *
         * Stage 4 데몬 나이트도 elite_mob PDC를 유지하지만
         * simple_dungeon에서는 180 EXP만 지급해야 한다.
         */
        Integer simpleDungeonExperience =
                entity.getPersistentDataContainer()
                        .get(
                                simpleDungeonExpKey,
                                PersistentDataType.INTEGER
                        );

        if (simpleDungeonExperience != null) {

            gainedExperience =
                    simpleDungeonExperience.longValue();

            event.setDroppedExp(
                    0
            );

        } else {

            NamespacedKey eliteMobKey =
                    new NamespacedKey(
                            plugin,
                            EliteDemonKnightListener.ELITE_MOB_KEY
                    );

            String eliteMobId =
                    entity.getPersistentDataContainer()
                            .get(
                                    eliteMobKey,
                                    PersistentDataType.STRING
                            );

            if (
                    EliteDemonKnightListener
                            .DEMON_KNIGHT_ID
                            .equals(eliteMobId)
            ) {

                /*
                 * 필드 데몬 나이트는 기존 300 EXP 유지.
                 */
                gainedExperience =
                        300L;

            } else {

                String dungeonId =
                        entity.getPersistentDataContainer()
                                .get(
                                        dungeonIdKey,
                                        PersistentDataType.STRING
                                );

                if (
                        dungeonId != null
                                && !dungeonId.isBlank()
                ) {

                    gainedExperience =
                            dungeonMobExperienceService
                                    .getExperience(
                                            entity.getType()
                                    );

                } else {

                    gainedExperience =
                            event.getDroppedExp();

                    event.setDroppedExp(
                            0
                    );
                }
            }
        }

        if (gainedExperience <= 0L) {
            return;
        }

        UUID uuid =
                killer.getUniqueId();

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> processExperience(
                                uuid,
                                gainedExperience
                        )
                );
    }

    /*
     * =========================================================
     * EXTERNAL RPG EXP AWARD
     * =========================================================
     *
     * 던전/퀘스트 등 몬스터 처치 이벤트 외부에서도
     * 기존 EXP 처리 파이프라인을 그대로 재사용한다.
     */
    public void awardExperience(
            UUID uuid,
            long gainedExperience
    ) {

        if (uuid == null
                || gainedExperience <= 0L) {

            return;
        }

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> processExperience(
                                uuid,
                                gainedExperience
                        )
                );
    }

    private void processExperience(
            UUID uuid,
            long gainedExperience
    ) {

        try {

            PlayerData playerData =
                    playerRepository.findPlayer(
                            uuid
                    );

            if (playerData == null) {
                return;
            }

            LevelUpResult result =
                    levelService.addExperience(
                            playerData,
                            gainedExperience
                    );

            /*
             * DB 저장
             */
            playerRepository
                    .updateLevelAndExperience(
                            uuid,
                            playerData.getLevel(),
                            playerData.getExperience()
                    );

            if (result.leveledUp() && playerData.getLevel() >= 80) {
                new com.hcs.rpgcore.title.LevelTitleUnlockService(plugin)
                        .checkLevel(uuid, playerData.getLevel());
            }

            plugin.registerLevel10ShieldReward(
                    uuid,
                    result.oldLevel(),
                    result.newLevel()
            );

            /*
             * HUD 캐시는 레벨업 여부와 관계없이
             * 항상 갱신한다.
             *
             * EXP만 변한 경우에도 HUD에
             * 즉시 반영되어야 하기 때문.
             */
            hudService.updateProfile(
                    playerData
            );

            PlayerStats newStats = null;

            if (result.leveledUp()) {

                newStats =
                        statService.calculate(
                                playerData
                        );

                /*
                 * 레벨업 시 최대 MANA 실시간 갱신.
                 * 현재 마나 비율 유지.
                 */
                manaService.updateMaximumMana(
                        uuid,
                        newStats.maxMana(),
                        true
                );
            }

            PlayerStats finalStats =
                    newStats;

            Bukkit.getScheduler()
                    .runTask(
                            plugin,
                            () -> {

                                Player onlinePlayer =
                                        Bukkit.getPlayer(
                                                uuid
                                        );

                                if (onlinePlayer == null
                                        || !onlinePlayer.isOnline()) {
                                    return;
                                }

                                skillRewardService.syncRewards(
                                        onlinePlayer,
                                        playerData
                                );

                            /*
                             * =================================================
                             * Lv.10 전직 즉시 안내
                             * =================================================
                             *
                             * Lv.9 이하에서 Lv.10에 도달한 순간
                             * 즉시 한 번 출력한다.
                             *
                             * 직업을 선택하지 않으면
                             * ClassSelectionReminderTask가
                             * 이후 30초마다 계속 안내한다.
                             */
                            if (
                                    result.oldLevel() < 10
                                    && result.newLevel() >= 10
                                    && result.newLevel() <= 10
                            ) {

                                onlinePlayer.sendMessage(
                                        Component.text(
                                                "[전직 안내] ",
                                                NamedTextColor.GOLD
                                        ).append(
                                                Component.text(
                                                        "Lv.10에 도달했습니다. "
                                                                + "직업을 선택해야 더 성장할 수 있습니다.",
                                                        NamedTextColor.YELLOW
                                                )
                                        )
                                );

                                onlinePlayer.sendMessage(
                                        Component.text(
                                                "전사: /class warrior",
                                                NamedTextColor.RED
                                        )
                                );

                                onlinePlayer.sendMessage(
                                        Component.text(
                                                "마법사: /class mage",
                                                NamedTextColor.AQUA
                                        )
                                );
                            }


                                /*
                                 * 레벨업 시 실제 MAX_HEALTH 갱신.
                                 */
                                if (result.leveledUp()
                                        && finalStats != null) {

                                    /*
                                     * 실제 Attribute 적용 시에는
                                     * 현재 착용 장비 공격력까지 포함한
                                     * 최종 스탯을 다시 계산한다.
                                     *
                                     * MAX HP / MAX MP는 장비 영향을
                                     * 받지 않으므로 기존 성장곡선 그대로 유지된다.
                                     */
                                    PlayerStats equippedStats =
                                            statService.calculate(
                                                    playerData,
                                                    onlinePlayer
                                            );

                                    statApplier.apply(
                                            onlinePlayer,
                                            equippedStats
                                    );
                                }

                                /*
                                 * 레벨업 채팅 메시지.
                                 */
                                if (result.leveledUp()) {

                                    onlinePlayer.sendMessage(
                                            Component.text(
                                                    "레벨 업! Lv."
                                                            + result.oldLevel()
                                                            + " → Lv."
                                                            + result.newLevel(),
                                                    NamedTextColor.YELLOW
                                            )
                                    );
                                }




                            }
                    );

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "몬스터 EXP 처리 실패: "
                                    + exception.getMessage()
                    );

            exception.printStackTrace();
        }
    }





}
