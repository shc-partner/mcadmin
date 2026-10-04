package com.hcs.rpgcore.dungeon;

import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;

import io.lumine.mythic.bukkit.MythicBukkit;

import java.time.Duration;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


/*
 * =========================================================
 * VOID SANCTUM DUNGEON
 * =========================================================
 *
 * 던전명:
 * 공허의 성전
 *
 * 권장 레벨:
 * Lv.70 ~ 80
 *
 * 보스:
 * [공허의 사도] 네크론
 *
 * MythicMob:
 * Phase 1 = ncr_Faceless
 * Phase 2 = ncr_Faceless_phase2
 */
public final class VoidSanctumDungeonService {

    public static final String DUNGEON_ID =
            "void_sanctum";

    private static final String BOSS_NAME =
            "[공허의 사도] 네크론";

    private static final String PHASE_1_MYTHIC_ID =
            "ncr_Faceless";

    private static final String PHASE_2_MYTHIC_ID =
            "ncr_Faceless_phase2";


    /*
     * =========================================================
     * BOSS STATS
     * =========================================================
     */

    private static final double PHASE_1_HEALTH =
            45000.0D;

    private static final double PHASE_1_ATTACK_DAMAGE =
            160.0D;

    private static final double PHASE_2_HEALTH =
            70000.0D;

    private static final double PHASE_2_ATTACK_DAMAGE =
            210.0D;


    /*
     * =========================================================
     * FULL DUNGEON BOUNDARY
     * =========================================================
     */

    private static final int DUNGEON_MIN_X =
            -9999678;

    private static final int DUNGEON_MAX_X =
            -9999523;

    private static final int DUNGEON_MIN_Y =
            51;

    private static final int DUNGEON_MAX_Y =
            213;

    private static final int DUNGEON_MIN_Z =
            19999044;

    private static final int DUNGEON_MAX_Z =
            19999165;


    /*
     * =========================================================
     * BOSS SPAWN
     * =========================================================
     */

    private static final double BOSS_SPAWN_X =
            -9999598.0D;

    private static final double BOSS_SPAWN_Y =
            129.0D;

    private static final double BOSS_SPAWN_Z =
            19999105.0D;


    /*
     * =========================================================
     * EXIT
     * =========================================================
     */

    private static final double EXIT_X =
            -13.0D;

    private static final double EXIT_Y =
            72.0D;

    private static final double EXIT_Z =
            -694.0D;


    private final JavaPlugin plugin;

    private final NamespacedKey dungeonIdKey;

    private final NamespacedKey bossPhaseKey;


    private final Set<UUID> participants =
            new HashSet<>();

    private final Set<UUID> activeParticipants =
            new HashSet<>();

    private final Set<UUID> pendingExitPlayers =
            new HashSet<>();


    private VoidSanctumDungeonState state =
            VoidSanctumDungeonState.IDLE;


    private World dungeonWorld;

    private LivingEntity activeBoss;

    private UUID phase1BossUuid;

    private UUID phase2BossUuid;


    private BossBar bossBar;

    private BukkitTask bossBarUpdateTask;

    private BukkitTask sequenceTask;

    private BukkitTask dungeonExitTask;


    private DungeonRewardService dungeonRewardService;


    public VoidSanctumDungeonService(
            JavaPlugin plugin
    ) {

        this.plugin =
                plugin;

        this.dungeonIdKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_id"
                );

        this.bossPhaseKey =
                new NamespacedKey(
                        plugin,
                        "void_sanctum_boss_phase"
                );
    }


    public void setRewardService(
            DungeonRewardService dungeonRewardService
    ) {

        this.dungeonRewardService =
                dungeonRewardService;
    }


    /*
     * =========================================================
     * ENTRY
     * =========================================================
     */

    public boolean canEnterDungeon() {

        return state
                == VoidSanctumDungeonState.IDLE;
    }


    public void onPlayerEntered(
            Player player
    ) {

        if (player == null) {
            return;
        }


        if (
                state
                        != VoidSanctumDungeonState.IDLE
        ) {

            player.sendMessage(
                    Component.text(
                            "[던전] 현재 공허의 성전은 공략 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        UUID uuid =
                player.getUniqueId();


        dungeonWorld =
                player.getWorld();


        participants.clear();

        activeParticipants.clear();

        pendingExitPlayers.clear();


        participants.add(
                uuid
        );

        activeParticipants.add(
                uuid
        );


        state =
                VoidSanctumDungeonState.BOSS_INTRO;


        runEntryIntro();
    }


    public boolean isParticipant(
            UUID uuid
    ) {

        return uuid != null
                && participants.contains(
                        uuid
                );
    }


    /*
     * =========================================================
     * ENTRY INTRO
     * =========================================================
     */

    private void runEntryIntro() {

        if (
                state
                        != VoidSanctumDungeonState.BOSS_INTRO
        ) {
            return;
        }


        clearTitles();


        showSubtitle(
                "공허의 성전에 발을 들였습니다.",
                NamedTextColor.LIGHT_PURPLE,
                3000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    sequenceTask =
                                            null;


                                    if (
                                            state
                                                    != VoidSanctumDungeonState.BOSS_INTRO
                                    ) {
                                        return;
                                    }


                                    showSubtitle(
                                            "공허의 사도가 모습을 드러냅니다.",
                                            NamedTextColor.DARK_RED,
                                            2000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                sequenceTask =
                                                                        null;

                                                                runBattleStartCountdown(
                                                                        3
                                                                );
                                                            },
                                                            40L
                                                    );
                                },
                                60L
                        );
    }


    /*
     * =========================================================
     * BATTLE START COUNTDOWN
     * =========================================================
     */

    private void runBattleStartCountdown(
            int seconds
    ) {

        if (
                state
                        != VoidSanctumDungeonState.BOSS_INTRO
        ) {

            sequenceTask =
                    null;

            return;
        }


        if (seconds <= 0) {

            sequenceTask =
                    null;


            clearTitles();


            spawnPhase1Boss();


            return;
        }


        showSubtitle(
                "전투 시작까지 "
                        + seconds
                        + "초 남았습니다.",
                NamedTextColor.RED,
                1100L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    sequenceTask =
                                            null;


                                    runBattleStartCountdown(
                                            seconds - 1
                                    );
                                },
                                20L
                        );
    }


    /*
     * =========================================================
     * PHASE 1
     * =========================================================
     */

    private void spawnPhase1Boss() {

        if (
                state
                        != VoidSanctumDungeonState.BOSS_INTRO
        ) {

            return;
        }


        if (dungeonWorld == null) {

            failDungeon();

            return;
        }


        Location spawnLocation =
                new Location(
                        dungeonWorld,
                        BOSS_SPAWN_X,
                        BOSS_SPAWN_Y,
                        BOSS_SPAWN_Z
                );


        Entity spawned =
                null;


        try {

            spawned =
                    MythicBukkit.inst()
                            .getAPIHelper()
                            .spawnMythicMob(
                                    PHASE_1_MYTHIC_ID,
                                    spawnLocation
                            );

        } catch (Exception exception) {

            plugin.getLogger()
                    .severe(
                            "[VoidSanctumDungeon] "
                                    + "Failed to spawn "
                                    + PHASE_1_MYTHIC_ID
                                    + ": "
                                    + exception.getMessage()
                    );

            exception.printStackTrace();
        }


        if (!(spawned instanceof LivingEntity boss)) {

            if (spawned != null) {
                spawned.remove();
            }

            failDungeon();

            return;
        }


        prepareBoss(
                boss,
                1,
                PHASE_1_HEALTH,
                PHASE_1_ATTACK_DAMAGE
        );


        activeBoss =
                boss;

        phase1BossUuid =
                boss.getUniqueId();

        phase2BossUuid =
                null;


        state =
                VoidSanctumDungeonState.PHASE_1;


        createBossBar(
                boss
        );


        plugin.getLogger()
                .info(
                        "[VoidSanctumDungeon] "
                                + "Phase 1 spawned: "
                                + boss.getUniqueId()
                );
    }


    /*
     * =========================================================
     * BOSS COMMON SETUP
     * =========================================================
     */

    public void preparePhase2Boss(
            LivingEntity boss
    ) {

        if (boss == null) {
            return;
        }


        prepareBoss(
                boss,
                2,
                PHASE_2_HEALTH,
                PHASE_2_ATTACK_DAMAGE
        );


        activeBoss =
                boss;

        phase2BossUuid =
                boss.getUniqueId();


        state =
                VoidSanctumDungeonState.PHASE_2;


        createBossBar(
                boss
        );


        plugin.getLogger()
                .info(
                        "[VoidSanctumDungeon] "
                                + "Phase 2 captured: "
                                + boss.getUniqueId()
                );
    }


    private void prepareBoss(
            LivingEntity boss,
            int phase,
            double health,
            double attackDamage
    ) {

        PersistentDataContainer pdc =
                boss.getPersistentDataContainer();


        pdc.set(
                dungeonIdKey,
                PersistentDataType.STRING,
                DUNGEON_ID
        );

        pdc.set(
                bossPhaseKey,
                PersistentDataType.INTEGER,
                phase
        );


        boss.setPersistent(
                true
        );


        /*
         * 네크론의 Health / Damage는 MythicMobs가 관리한다.
         *
         * MythicMobs의 basedamage mechanic 역시
         * Mob의 Damage 값을 기준으로 계산하므로
         * RPGCore에서 Bukkit Attribute를 다시 덮지 않는다.
         *
         * RPGCore는 던전 식별 / 페이즈 추적 / BossBar /
         * 클리어 및 보상 흐름만 담당한다.
         */
    }


    private void setBaseAttribute(
            LivingEntity entity,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                entity.getAttribute(
                        attribute
                );


        if (instance == null) {
            return;
        }


        instance.setBaseValue(
                value
        );
    }


    /*
     * =========================================================
     * BOUNDARY
     * =========================================================
     */

    public boolean isInsideDungeonBoundary(
            Location location
    ) {

        if (location == null) {
            return false;
        }


        if (
                dungeonWorld != null
                        &&
                location.getWorld()
                        != dungeonWorld
        ) {

            return false;
        }


        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();


        return
                x >= DUNGEON_MIN_X
                && x <= DUNGEON_MAX_X

                && y >= DUNGEON_MIN_Y
                && y <= DUNGEON_MAX_Y

                && z >= DUNGEON_MIN_Z
                && z <= DUNGEON_MAX_Z;
    }


    /*
     * =========================================================
     * BOSS UUID
     * =========================================================
     */

    public boolean isPhase1Boss(
            LivingEntity entity
    ) {

        return entity != null
                && phase1BossUuid != null
                && phase1BossUuid.equals(
                        entity.getUniqueId()
                );
    }


    public boolean isPhase2Boss(
            LivingEntity entity
    ) {

        return entity != null
                && phase2BossUuid != null
                && phase2BossUuid.equals(
                        entity.getUniqueId()
                );
    }


    public VoidSanctumDungeonState getState() {

        return state;
    }


    /*
     * =========================================================
     * PHASE 2 SPAWN CAPTURE
     * =========================================================
     */

    public boolean isExpectedPhase2Spawn(
            String mythicMobId,
            Location location
    ) {

        if (
                state
                        != VoidSanctumDungeonState.PHASE_1
        ) {

            return false;
        }


        if (
                mythicMobId == null
                        ||
                !PHASE_2_MYTHIC_ID.equals(
                        mythicMobId
                )
        ) {

            return false;
        }


        if (!isInsideDungeonBoundary(
                location
        )) {

            return false;
        }


        /*
         * 1페이즈는 전투 중 이동할 수 있고,
         * MythicMobs는 1페이즈 사망 위치에
         * 2페이즈를 소환한다.
         *
         * 따라서 고정 스폰 좌표와의 거리로 제한하지 않고
         * 공허의 성전 내부에서 생성된 정확한
         * ncr_Faceless_phase2만 인정한다.
         */
        return true;
    }


    /*
     * =========================================================
     * DUNGEON MOB DEATH
     * =========================================================
     */

    public void onDungeonMobDeath(
            LivingEntity entity
    ) {

        if (entity == null) {
            return;
        }


        /*
         * 1페이즈 사망은 클리어가 아니다.
         *
         * MythicMobs의 ncr_faceless_death 스킬이
         * ncr_Faceless_phase2를 직접 소환한다.
         */
        if (
                state == VoidSanctumDungeonState.PHASE_1
                        &&
                isPhase1Boss(
                        entity
                )
        ) {

            removeBossBar();

            activeBoss =
                    null;


            plugin.getLogger()
                    .info(
                            "[VoidSanctumDungeon] "
                                    + "Phase 1 defeated. "
                                    + "Waiting for phase 2."
                    );

            return;
        }


        /*
         * 최종 클리어는 2페이즈 사망만 인정한다.
         */
        if (
                state == VoidSanctumDungeonState.PHASE_2
                        &&
                isPhase2Boss(
                        entity
                )
        ) {

            finishDungeon();
        }
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */

    private void finishDungeon() {

        if (
                state
                        != VoidSanctumDungeonState.PHASE_2
        ) {

            return;
        }


        state =
                VoidSanctumDungeonState.CLEARED;


        removeBossBar();


        activeBoss =
                null;


        clearTitles();


        showSubtitle(
                "공허의 사도를 처치했습니다.",
                NamedTextColor.GOLD,
                3000L
        );


        /*
         * =====================================================
         * VOID SANCTUM CLEAR REWARDS
         * =====================================================
         *
         * RPG EXP              : 9,000
         * 강화석               : 4 확정 + 30% 확률 +1
         * 다이아몬드           : 5~8
         * 메아리 조각          : 30% / 1~2
         * 코러스 열매          : 6~12
         * 전설 장비            : 20%
         * 신화 세트 보관함     : 7%
         *
         * 전설 / 신화 판정은 독립적이다.
         */

        if (dungeonRewardService == null) {

            plugin.getLogger()
                    .severe(
                            "[VoidSanctumDungeon] "
                                    + "DungeonRewardService is null."
                    );

        } else {

            dungeonRewardService
                    .rewardDungeonClearExperience(
                            participants,
                            9000L
                    );

            dungeonRewardService
                    .rewardVoidSanctumBasicRewards(
                            participants
                    );

            dungeonRewardService
                    .rewardEquipmentIngots(
                            participants,
                            0.21D,
                            0.14D,
                            0.07D
                    );

            dungeonRewardService
                    .rewardDungeonGold(
                            participants,
                            1500L,
                            600L,
                            0.20D
                    );

            dungeonRewardService
                    .rewardEnhancementStones(
                            participants,
                            4,
                            0.30D,
                            1,
                            0
                    );


        }


        scheduleSuccessfulDungeonExit();
    }


    /*
     * =========================================================
     * SUCCESSFUL EXIT
     * =========================================================
     */

    private void scheduleSuccessfulDungeonExit() {

        if (dungeonExitTask != null) {
            return;
        }


        /*
         * "공허의 사도를 처치했습니다."
         * 3초 표시 후 다음 안내.
         */
        dungeonExitTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    dungeonExitTask =
                                            null;


                                    if (
                                            state
                                                    != VoidSanctumDungeonState.CLEARED
                                    ) {
                                        return;
                                    }


                                    showSubtitle(
                                            "5초 후 퇴장합니다.",
                                            NamedTextColor.LIGHT_PURPLE,
                                            2000L
                                    );


                                    dungeonExitTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                dungeonExitTask =
                                                                        null;


                                                                runDungeonExitCountdown(
                                                                        5
                                                                );
                                                            },
                                                            40L
                                                    );
                                },
                                60L
                        );
    }


    /*
     * =========================================================
     * DUNGEON EXIT COUNTDOWN
     * =========================================================
     */

    private void runDungeonExitCountdown(
            int seconds
    ) {

        if (
                state
                        != VoidSanctumDungeonState.CLEARED
        ) {

            dungeonExitTask =
                    null;

            return;
        }


        if (seconds <= 0) {

            dungeonExitTask =
                    null;


            clearTitles();

            teleportParticipantsToExit();

            resetDungeon();


            return;
        }


        NamedTextColor color =
                seconds >= 4
                        ? NamedTextColor.YELLOW
                        : NamedTextColor.RED;


        showSubtitle(
                "던전 퇴장까지 "
                        + seconds
                        + "초 남았습니다.",
                color,
                1100L
        );


        dungeonExitTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    dungeonExitTask =
                                            null;


                                    runDungeonExitCountdown(
                                            seconds - 1
                                    );
                                },
                                20L
                        );
    }


    /*
     * =========================================================
     * TELEPORT PARTICIPANTS
     * =========================================================
     */

    private void teleportParticipantsToExit() {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                            ||
                    !player.isOnline()
                            ||
                    player.isDead()
            ) {

                pendingExitPlayers.add(
                        uuid
                );

                continue;
            }


            Location exit =
                    createExitLocation(
                            player.getWorld()
                    );


            if (exit != null) {

                player.teleport(
                        exit
                );
            }
        }
    }


    /*
     * =========================================================
     * SUBTITLE
     * =========================================================
     */

    private void showSubtitle(
            String text,
            NamedTextColor color,
            long durationMillis
    ) {

        Title title =
                Title.title(
                        Component.empty(),
                        Component.text(
                                text,
                                color
                        ),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(
                                        durationMillis
                                ),
                                Duration.ZERO
                        )
                );


        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                            ||
                    !player.isOnline()
            ) {
                continue;
            }


            player.showTitle(
                    title
            );
        }
    }


    private void clearTitles() {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                            ||
                    !player.isOnline()
            ) {
                continue;
            }


            player.clearTitle();
        }
    }


    private void cancelSequenceTask() {

        if (sequenceTask == null) {
            return;
        }


        sequenceTask.cancel();

        sequenceTask =
                null;
    }


    /*
     * =========================================================
     * BOSS BAR
     * =========================================================
     */

    private void createBossBar(
            LivingEntity boss
    ) {

        removeBossBar();


        bossBar =
                plugin.getServer()
                        .createBossBar(
                                BOSS_NAME,
                                BarColor.PURPLE,
                                BarStyle.SOLID
                        );


        bossBar.setProgress(
                1.0D
        );

        bossBar.setVisible(
                true
        );


        for (UUID uuid : activeParticipants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player != null
                            &&
                    player.isOnline()
            ) {

                bossBar.addPlayer(
                        player
                );
            }
        }


        bossBarUpdateTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> updateBossBar(
                                        boss
                                ),
                                1L,
                                2L
                        );
    }


    private void updateBossBar(
            LivingEntity boss
    ) {

        if (
                bossBar == null
                        ||
                boss == null
                        ||
                !boss.isValid()
                        ||
                boss.isDead()
        ) {

            return;
        }


        AttributeInstance maxHealthAttribute =
                boss.getAttribute(
                        Attribute.MAX_HEALTH
                );


        if (maxHealthAttribute == null) {
            return;
        }


        double maxHealth =
                maxHealthAttribute.getValue();


        if (maxHealth <= 0.0D) {
            return;
        }


        double progress =
                boss.getHealth()
                        / maxHealth;


        progress =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                progress
                        )
                );


        bossBar.setProgress(
                progress
        );
    }


    private void removeBossBar() {

        if (bossBarUpdateTask != null) {

            bossBarUpdateTask.cancel();

            bossBarUpdateTask =
                    null;
        }


        if (bossBar != null) {

            bossBar.removeAll();

            bossBar =
                    null;
        }
    }


    /*
     * =========================================================
     * PLAYER FAILURE
     * =========================================================
     */

    public void onParticipantUnavailable(
            Player player
    ) {

        if (player == null) {
            return;
        }


        UUID uuid =
                player.getUniqueId();


        if (!participants.contains(
                uuid
        )) {

            return;
        }


        activeParticipants.remove(
                uuid
        );

        pendingExitPlayers.add(
                uuid
        );


        if (!activeParticipants.isEmpty()) {
            return;
        }


        failDungeon();
    }


    private void failDungeon() {

        if (
                state == VoidSanctumDungeonState.IDLE
                        ||
                state == VoidSanctumDungeonState.RESETTING
        ) {

            return;
        }


        cancelSequenceTask();

        clearTitles();

        removeBossBar();


        if (
                activeBoss != null
                        &&
                activeBoss.isValid()
        ) {

            activeBoss.remove();
        }


        activeBoss =
                null;


        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                            ||
                    !player.isOnline()
                            ||
                    player.isDead()
            ) {

                pendingExitPlayers.add(
                        uuid
                );

                continue;
            }


            Location exit =
                    createExitLocation(
                            player.getWorld()
                    );


            if (exit != null) {

                player.teleport(
                        exit
                );
            }
        }


        plugin.getLogger()
                .info(
                        "[VoidSanctumDungeon] Dungeon failed."
                );


        resetDungeon();
    }


    /*
     * =========================================================
     * EXIT
     * =========================================================
     */

    public boolean consumePendingExit(
            UUID uuid
    ) {

        return uuid != null
                && pendingExitPlayers.remove(
                        uuid
                );
    }


    public Location createExitLocation(
            World world
    ) {

        if (world == null) {
            return null;
        }


        return new Location(
                world,
                EXIT_X,
                EXIT_Y,
                EXIT_Z
        );
    }


    /*
     * =========================================================
     * RESET
     * =========================================================
     */

    private void resetDungeon() {

        state =
                VoidSanctumDungeonState.RESETTING;


        cancelSequenceTask();

        clearTitles();


        if (dungeonExitTask != null) {

            dungeonExitTask.cancel();

            dungeonExitTask =
                    null;
        }


        removeBossBar();


        if (
                activeBoss != null
                        &&
                activeBoss.isValid()
        ) {

            activeBoss.remove();
        }


        activeBoss =
                null;

        phase1BossUuid =
                null;

        phase2BossUuid =
                null;

        dungeonWorld =
                null;


        participants.clear();

        activeParticipants.clear();


        state =
                VoidSanctumDungeonState.IDLE;


        plugin.getLogger()
                .info(
                        "[VoidSanctumDungeon] Dungeon reset."
                );
    }
}
