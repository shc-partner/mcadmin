package com.hcs.rpgcore.dungeon;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class EndermanDungeonService {

    public static final String DUNGEON_ID =
            "enderman_rift";

    /*
     * =========================================================
     * SPAWN AREA
     * =========================================================
     *
     * 엔더맨 던전 몬스터 스폰 기준점.
     */
    private static final double SPAWN_CENTER_X =
            -10000362.0D;

    private static final double SPAWN_FEET_Y =
            162.0D;

    private static final double SPAWN_CENTER_Z =
            -9999922.0D;


    /*
     * =========================================================
     * DUNGEON EXIT
     * =========================================================
     *
     * 성공/실패 후 공통 출구.
     */
    private static final double EXIT_X =
            878.0D;

    private static final double EXIT_Y =
            84.0D;

    private static final double EXIT_Z =
            -1563.0D;


    /*
     * 일반 웨이브는 중심점 주변에 분산 생성.
     */
    private static final int SPAWN_RADIUS =
            7;

    private static final double MIN_SPAWN_DISTANCE =
            2.0D;

    private static final int MAX_SPAWN_ATTEMPTS =
            3000;


    /*
     * =========================================================
     * WAVE CONFIG
     * =========================================================
     */

    private static final int WAVE_1_COUNT =
            10;

    private static final int WAVE_2_COUNT =
            14;

    private static final int WAVE_3_COUNT =
            20;

    private static final int NEXT_WAVE_COUNTDOWN_SECONDS =
            3;


    private static final long CLEAR_EXP =
            4000L;


    /*
     * =========================================================
     * WAVE STATS
     * =========================================================
     *
     * Lv.50 ~ Lv.60 권장 난이도.
     *
     * 입장 레벨 제한은 없다.
     */

    private static final double WAVE_1_EFFECTIVE_HEALTH =
            900.0D;

    private static final double WAVE_1_ATTACK_DAMAGE =
            65.0D;

    private static final double WAVE_2_EFFECTIVE_HEALTH =
            1100.0D;

    private static final double WAVE_2_ATTACK_DAMAGE =
            75.0D;

    private static final double WAVE_3_EFFECTIVE_HEALTH =
            1350.0D;

    private static final double WAVE_3_ATTACK_DAMAGE =
            85.0D;


    /*
     * =========================================================
     * FINAL BOSS
     * =========================================================
     */

    private static final String BOSS_ID =
            "abyss_giant_enderman";

    private static final String BOSS_NAME =
            "심연의 거대 엔더맨";

    public static final double BOSS_EFFECTIVE_HEALTH =
            7_500.0D;

    public static final double BOSS_VANILLA_HEALTH =
            1024.0D;

    private static final double BOSS_ATTACK_DAMAGE =
            30.0D;

    private static final double BOSS_ARMOR =
            80.0D;

    private static final double BOSS_SCALE =
            1.5D;

    private static final double BOSS_KNOCKBACK_RESISTANCE =
            1.0D;

    private static final double BOSS_SPEED_MULTIPLIER =
            1.15D;


    /*
     * =========================================================
     * BOSS PATTERN
     * =========================================================
     */

    private static final double PHASE_2_HEALTH_RATIO =
            0.70D;

    private static final double PHASE_3_HEALTH_RATIO =
            0.30D;

    private static final double SHOCKWAVE_RADIUS =
            8.0D;

    private static final double ROAR_RADIUS =
            12.0D;

    private static final double PHASE_1_SHOCKWAVE_DAMAGE =
            20.0D;

    private static final double PHASE_2_SHOCKWAVE_DAMAGE =
            25.0D;

    private static final double PHASE_3_SHOCKWAVE_DAMAGE =
            30.0D;

    private static final double ROAR_DAMAGE =
            18.0D;

    private static final long PHASE_1_SHOCKWAVE_INTERVAL =
            160L;

    private static final long PHASE_2_SHOCKWAVE_INTERVAL =
            140L;

    private static final long PHASE_3_SHOCKWAVE_INTERVAL =
            100L;


    /*
     * =========================================================
     * SERVICE STATE
     * =========================================================
     */

    private final RPGCorePlugin plugin;

    private DungeonRewardService dungeonRewardService;

    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey dungeonWaveKey;
    private final NamespacedKey dungeonBossKey;
    private final NamespacedKey effectiveHealthKey;

    private final Set<UUID> participants =
            new HashSet<>();

    private final Set<UUID> activeParticipants =
            new HashSet<>();

    /*
     * 던전에서 사망하여 즉시 텔레포트할 수 없는 플레이어.
     *
     * 던전 자체가 초기화된 뒤에도 유지하며
     * PlayerRespawnEvent에서 출구로 이동시킨다.
     */
    private final Set<UUID> pendingExitParticipants =
            new HashSet<>();

    private final Set<UUID> activeDungeonMobs =
            new HashSet<>();

    private EndermanDungeonState state =
            EndermanDungeonState.IDLE;

    private World dungeonWorld;

    private Enderman activeBoss;

    private BossBar bossBar;

    private BukkitTask bossBarUpdateTask;

    private BukkitTask bossPatternTask;

    private BukkitTask dungeonResetTask;

    private int currentBossPhase =
            1;

    private long bossPatternTicks =
            0L;

    private long lastShockwaveTick =
            0L;

    private long lastRoarTick =
            0L;


    public EndermanDungeonService(
            RPGCorePlugin plugin
    ) {

        this.plugin =
                plugin;

        this.dungeonIdKey =
                new NamespacedKey(
                        plugin,
                        "enderman_dungeon_id"
                );

        this.dungeonWaveKey =
                new NamespacedKey(
                        plugin,
                        "enderman_dungeon_wave"
                );

        this.dungeonBossKey =
                new NamespacedKey(
                        plugin,
                        "enderman_dungeon_boss"
                );

        this.effectiveHealthKey =
                new NamespacedKey(
                        plugin,
                        "enderman_effective_health"
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
     * PLAYER ENTER
     * =========================================================
     */

    public void onPlayerEntered(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        /*
         * 종료 처리 중에는 새 레이드를 시작하지 않는다.
         */
        if (
                state == EndermanDungeonState.CLEARED
                || state == EndermanDungeonState.RESETTING
        ) {

            player.sendMessage(
                    Component.text(
                            "[던전] 현재 던전이 종료 처리 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        participants.add(uuid);
        activeParticipants.add(uuid);

        /*
         * 진행 중인 던전에는 참가자로만 합류하며
         * 새 웨이브를 다시 생성하지 않는다.
         */
        if (state != EndermanDungeonState.IDLE) {

            player.sendMessage(
                    Component.text(
                            "[던전] ",
                            NamedTextColor.DARK_PURPLE
                    ).append(
                            Component.text(
                                    "현재 진행 중인 엔더 유적에 참가했습니다.",
                                    NamedTextColor.GRAY
                            )
                    )
            );

            if (bossBar != null) {
                bossBar.addPlayer(player);
            }

            return;
        }

        dungeonWorld =
                player.getWorld();

        state =
                EndermanDungeonState.WAVE_1;

        /*
         * 좀비 던전 UI 표준:
         * 입장 성공은 채팅으로 안내한다.
         */
        player.sendMessage(
                Component.text(
                        "[던전] ",
                        NamedTextColor.DARK_PURPLE
                ).append(
                        Component.text(
                                "엔더 유적에 입장합니다.",
                                NamedTextColor.LIGHT_PURPLE
                        )
                )
        );

        /*
         * 첫 번째 웨이브는 3→2→1 Subtitle을 사용하지 않는다.
         * 입장 후 안내 채팅을 보여주고 2초 뒤 바로 시작한다.
         */
        broadcast(
                Component.text(
                        "잠시 후 첫 번째 웨이브가 시작됩니다.",
                        NamedTextColor.GRAY
                )
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> startWave(
                                1,
                                WAVE_1_COUNT
                        ),
                        40L
                );
    }


    /*
     * =========================================================
     * WAVE START
     * =========================================================
     */

    private void startWave(
            int wave,
            int count
    ) {

        if (!isExpectedWaveState(wave)) {
            return;
        }

        if (dungeonWorld == null) {

            plugin.getLogger().warning(
                    "[EndermanDungeon] Dungeon world is null."
            );

            return;
        }

        activeDungeonMobs.clear();

        clearParticipantTitles();

        broadcast(
                Component.text(
                        "Wave " + wave,
                        NamedTextColor.RED
                ).append(
                        Component.text(
                                " - 엔더맨 "
                                        + count
                                        + "마리를 처치하십시오.",
                                NamedTextColor.WHITE
                        )
                )
        );

        List<Location> locations =
                findSafeSpawnLocations(
                        dungeonWorld,
                        count
                );

        for (Location location : locations) {

            spawnWaveEnderman(
                    dungeonWorld,
                    location,
                    wave
            );
        }

        plugin.getLogger().info(
                "[EndermanDungeon] Wave "
                        + wave
                        + " spawned: "
                        + activeDungeonMobs.size()
                        + " / "
                        + count
        );

        if (activeDungeonMobs.isEmpty()) {

            plugin.getLogger().severe(
                    "[EndermanDungeon] Wave "
                            + wave
                            + " spawn failed."
            );

            broadcast(
                    Component.text(
                            "엔더맨 생성에 실패했습니다. 관리자에게 문의하십시오.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        if (activeDungeonMobs.size() < count) {

            plugin.getLogger().warning(
                    "[EndermanDungeon] Wave "
                            + wave
                            + " spawned only "
                            + activeDungeonMobs.size()
                            + " / "
                            + count
            );
        }
    }


    private boolean isExpectedWaveState(
            int wave
    ) {

        return switch (wave) {

            case 1 ->
                    state == EndermanDungeonState.WAVE_1;

            case 2 ->
                    state == EndermanDungeonState.WAVE_2;

            case 3 ->
                    state == EndermanDungeonState.WAVE_3;

            default -> false;
        };
    }


    /*
     * =========================================================
     * WAVE END / MOB DEATH
     * =========================================================
     */

    public void onDungeonMobDeath(
            LivingEntity entity
    ) {

        if (!isDungeonMob(entity)) {
            return;
        }

        activeDungeonMobs.remove(
                entity.getUniqueId()
        );

        if (!activeDungeonMobs.isEmpty()) {
            return;
        }

        switch (state) {

            case WAVE_1 -> {

                state =
                        EndermanDungeonState.WAVE_2;

                broadcast(
                        Component.text(
                                "Wave 1 Clear!",
                                NamedTextColor.GREEN
                        )
                );

                broadcast(
                        Component.text(
                                "첫 번째 웨이브를 완료했습니다.",
                                NamedTextColor.GRAY
                        )
                );

                scheduleNextWave(
                        2,
                        WAVE_2_COUNT
                );
            }

            case WAVE_2 -> {

                state =
                        EndermanDungeonState.WAVE_3;

                broadcast(
                        Component.text(
                                "Wave 2 Clear!",
                                NamedTextColor.GREEN
                        )
                );

                broadcast(
                        Component.text(
                                "두 번째 웨이브를 완료했습니다.",
                                NamedTextColor.GRAY
                        )
                );

                scheduleNextWave(
                        3,
                        WAVE_3_COUNT
                );
            }

            case WAVE_3 -> {

                state =
                        EndermanDungeonState.BOSS;

                clearParticipantTitles();

                broadcast(
                        Component.text(
                                "Wave 3 Clear!",
                                NamedTextColor.GREEN
                        )
                );

                broadcast(
                        Component.text(
                                "세 번째 웨이브를 완료했습니다.",
                                NamedTextColor.GRAY
                        )
                );

                startBossIntroduction();
            }

            case BOSS -> {

                if (!isBoss(entity)) {
                    return;
                }

                activeBoss = null;

                state =
                        EndermanDungeonState.CLEARED;

                rewardClear();

                removeBossBar();
                clearParticipantTitles();

                broadcast(
                        Component.text(
                                "엔더의 지배자를 처치했습니다.",
                                NamedTextColor.GOLD
                        )
                );

                showBossSubtitle(
                        Component.text(
                                "던전을 클리어했습니다.",
                                NamedTextColor.GREEN
                        ),
                        10000L
                );

                /*
                 * 클리어 Subtitle은 stay 10초로 설정하지만,
                 * 5초 후 퇴장 카운트다운 Subtitle이 같은 채널을 덮어쓴다.
                 * 실제 단독 노출 시간은 약 5초다.
                 */
                scheduleClearExitCountdown(
                        100L
                );
            }

            default -> {
            }
        }
    }


    /*
     * =========================================================
     * NEXT WAVE COUNTDOWN
     * =========================================================
     */

    private void scheduleNextWave(
            int nextWave,
            int mobCount
    ) {

        runWaveCountdown(
                nextWave,
                mobCount,
                NEXT_WAVE_COUNTDOWN_SECONDS
        );
    }


    private void runWaveCountdown(
            int nextWave,
            int mobCount,
            int seconds
    ) {

        if (!isExpectedWaveState(nextWave)) {
            return;
        }

        if (seconds <= 0) {

            clearParticipantTitles();

            startWave(
                    nextWave,
                    mobCount
            );

            return;
        }

        Component subtitle =
                Component.text(
                        "Wave "
                                + nextWave
                                + " 시작까지 "
                                + seconds
                                + "초 남았습니다.",
                        NamedTextColor.LIGHT_PURPLE
                );

        Title title =
                Title.title(
                        Component.empty(),
                        subtitle,
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(1100L),
                                Duration.ZERO
                        )
                );

        showTitleToParticipants(title);

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> runWaveCountdown(
                                nextWave,
                                mobCount,
                                seconds - 1
                        ),
                        20L
                );
    }


    /*
     * =========================================================
     * WAVE MOB SPAWN
     * =========================================================
     */

    private void spawnWaveEnderman(
            World world,
            Location location,
            int wave
    ) {

        Enderman enderman =
                world.spawn(
                        location,
                        Enderman.class
                );

        PersistentDataContainer pdc =
                enderman.getPersistentDataContainer();

        pdc.set(
                dungeonIdKey,
                PersistentDataType.STRING,
                DUNGEON_ID
        );

        pdc.set(
                dungeonWaveKey,
                PersistentDataType.INTEGER,
                wave
        );

        double effectiveHealth;
        double attackDamage;

        switch (wave) {

            case 1 -> {
                effectiveHealth =
                        WAVE_1_EFFECTIVE_HEALTH;

                attackDamage =
                        WAVE_1_ATTACK_DAMAGE;
            }

            case 2 -> {
                effectiveHealth =
                        WAVE_2_EFFECTIVE_HEALTH;

                attackDamage =
                        WAVE_2_ATTACK_DAMAGE;
            }

            case 3 -> {
                effectiveHealth =
                        WAVE_3_EFFECTIVE_HEALTH;

                attackDamage =
                        WAVE_3_ATTACK_DAMAGE;
            }

            default -> {
                effectiveHealth =
                        WAVE_1_EFFECTIVE_HEALTH;

                attackDamage =
                        WAVE_1_ATTACK_DAMAGE;
            }
        }

        pdc.set(
                effectiveHealthKey,
                PersistentDataType.DOUBLE,
                effectiveHealth
        );

        /*
         * Paper Entity HP 상한 대응.
         *
         * 1024 이하:
         * 실제 HP = RPG HP
         *
         * 1024 초과:
         * 실제 HP = 1024
         * DamageListener에서 피해 비율 보정.
         */
        double vanillaHealth =
                Math.min(
                        effectiveHealth,
                        1024.0D
                );

        setAttribute(
                enderman,
                Attribute.MAX_HEALTH,
                vanillaHealth
        );

        enderman.setHealth(
                vanillaHealth
        );

        setAttribute(
                enderman,
                Attribute.ATTACK_DAMAGE,
                attackDamage
        );

        enderman.setPersistent(true);
        enderman.setRemoveWhenFarAway(false);

        activeDungeonMobs.add(
                enderman.getUniqueId()
        );


        /*
         * WAVE FORCED TARGET
         *
         * 시선 여부와 관계없이
         * 가장 가까운 activeParticipant를 적대한다.
         */
        targetNearestActiveParticipant(
                enderman
        );
    }


    /*
     * =========================================================
     * FINAL BOSS
     * =========================================================
     */

    /*
     * =========================================================
     * BOSS INTRODUCTION
     * =========================================================
     *
     * 좀비 던전 UI 표준:
     * 1. 모든 웨이브 종료 안내       GOLD     2초
     * 2. 던전별 고유 분위기 문구    DARK_RED  3초
     * 3. 보스 등장 안내             RED       3초
     * 4. 실제 보스 생성 + BossBar 표시
     */
    private void startBossIntroduction() {

        if (state != EndermanDungeonState.BOSS) {
            return;
        }

        showBossSubtitle(
                Component.text(
                        "모든 웨이브가 종료되었습니다.",
                        NamedTextColor.GOLD
                ),
                2000L
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (state != EndermanDungeonState.BOSS) {
                                return;
                            }

                            showBossSubtitle(
                                    Component.text(
                                            "강력한 엔더의 기운이 느껴집니다...",
                                            NamedTextColor.DARK_RED
                                    ),
                                    3000L
                            );

                            plugin.getServer()
                                    .getScheduler()
                                    .runTaskLater(
                                            plugin,
                                            () -> {

                                                if (state != EndermanDungeonState.BOSS) {
                                                    return;
                                                }

                                                showBossSubtitle(
                                                        Component.text(
                                                                BOSS_NAME
                                                                        + "가 나타납니다",
                                                                NamedTextColor.RED
                                                        ),
                                                        3000L
                                                );

                                                plugin.getServer()
                                                        .getScheduler()
                                                        .runTaskLater(
                                                                plugin,
                                                                () -> {

                                                                    if (state != EndermanDungeonState.BOSS) {
                                                                        return;
                                                                    }

                                                                    clearParticipantTitles();
                                                                    spawnBoss();
                                                                },
                                                                60L
                                                        );
                                            },
                                            60L
                                    );
                        },
                        40L
                );
    }


    /*
     * =========================================================
     * DUNGEON SUBTITLE
     * =========================================================
     *
     * 좀비 던전 UI 표준과 동일하게 Title 본문은 비우고
     * Subtitle 채널만 사용한다.
     */
    private void showBossSubtitle(
            Component subtitle,
            long stayMillis
    ) {

        Title title =
                Title.title(
                        Component.empty(),
                        subtitle,
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(
                                        stayMillis
                                ),
                                Duration.ZERO
                        )
                );

        showTitleToParticipants(
                title
        );
    }


    private void spawnBoss() {

        if (
                state != EndermanDungeonState.BOSS
                || dungeonWorld == null
        ) {
            return;
        }

        Location location =
                new Location(
                        dungeonWorld,
                        SPAWN_CENTER_X + 0.5D,
                        SPAWN_FEET_Y,
                        SPAWN_CENTER_Z + 0.5D
                );

        Enderman boss =
                dungeonWorld.spawn(
                        location,
                        Enderman.class
                );

        PersistentDataContainer pdc =
                boss.getPersistentDataContainer();

        pdc.set(
                dungeonIdKey,
                PersistentDataType.STRING,
                DUNGEON_ID
        );

        pdc.set(
                dungeonBossKey,
                PersistentDataType.STRING,
                BOSS_ID
        );

        pdc.set(
                effectiveHealthKey,
                PersistentDataType.DOUBLE,
                BOSS_EFFECTIVE_HEALTH
        );

        boss.customName(
                Component.text(
                        BOSS_NAME,
                        NamedTextColor.DARK_PURPLE
                )
        );

        boss.setCustomNameVisible(true);

        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);

        setAttribute(
                boss,
                Attribute.MAX_HEALTH,
                BOSS_VANILLA_HEALTH
        );

        boss.setHealth(
                BOSS_VANILLA_HEALTH
        );

        setAttribute(
                boss,
                Attribute.ATTACK_DAMAGE,
                BOSS_ATTACK_DAMAGE
        );

        setAttribute(
                boss,
                Attribute.ARMOR,
                BOSS_ARMOR
        );

        setAttribute(
                boss,
                Attribute.KNOCKBACK_RESISTANCE,
                BOSS_KNOCKBACK_RESISTANCE
        );

        AttributeInstance speed =
                boss.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );

        if (speed != null) {

            speed.setBaseValue(
                    speed.getBaseValue()
                            * BOSS_SPEED_MULTIPLIER
            );
        }

        setAttribute(
                boss,
                Attribute.SCALE,
                BOSS_SCALE
        );

        activeBoss =
                boss;

        activeDungeonMobs.clear();

        activeDungeonMobs.add(
                boss.getUniqueId()
        );


        /*
         * BOSS FORCED TARGET
         *
         * 보스도 시선 여부와 관계없이
         * 가장 가까운 activeParticipant를 적대한다.
         */
        targetNearestActiveParticipant(
                boss
        );


        createBossBar();

        startBossPatternTask();

        broadcast(
                Component.text(
                        "[BOSS] ",
                        NamedTextColor.DARK_RED
                ).append(
                        Component.text(
                                BOSS_NAME,
                                NamedTextColor.RED
                        )
                )
        );

        plugin.getLogger().info(
                "[EndermanDungeon] Boss spawned at "
                        + SPAWN_CENTER_X
                        + ", "
                        + SPAWN_FEET_Y
                        + ", "
                        + SPAWN_CENTER_Z
        );
    }


    /*
     * =========================================================
     * BOSS BAR
     * =========================================================
     *
     * 좀비 던전 UI 표준:
     * RED / SOLID / 초기 100%
     * 생성 1 tick 후부터 2 ticks 주기로 갱신한다.
     */
    private void createBossBar() {

        removeBossBar();

        bossBar =
                plugin.getServer()
                        .createBossBar(
                                BOSS_NAME,
                                BarColor.RED,
                                BarStyle.SOLID
                        );

        bossBar.setVisible(true);
        bossBar.setProgress(1.0D);

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }

            bossBar.addPlayer(
                    player
            );
        }

        bossBarUpdateTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> {

                                    if (
                                            activeBoss == null
                                            || !activeBoss.isValid()
                                            || activeBoss.isDead()
                                    ) {

                                        removeBossBar();
                                        return;
                                    }

                                    double progress =
                                            Math.max(
                                                    0.0D,
                                                    Math.min(
                                                            1.0D,
                                                            activeBoss.getHealth()
                                                                    / BOSS_VANILLA_HEALTH
                                                    )
                                            );

                                    if (bossBar != null) {
                                        bossBar.setProgress(
                                                progress
                                        );
                                    }
                                },
                                1L,
                                2L
                        );
    }


    private void removeBossBar() {

        stopBossPatternTask();

        if (bossBarUpdateTask != null) {

            bossBarUpdateTask.cancel();

            bossBarUpdateTask = null;
        }

        if (bossBar != null) {

            bossBar.removeAll();

            bossBar = null;
        }
    }



    /*
     * =========================================================
     * BOSS PATTERN
     * =========================================================
     */

    private void startBossPatternTask() {

        stopBossPatternTask();

        currentBossPhase =
                1;

        bossPatternTicks =
                0L;

        lastShockwaveTick =
                0L;

        lastRoarTick =
                0L;

        bossPatternTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::tickBossPattern,
                                20L,
                                20L
                        );
    }


    private void tickBossPattern() {

        if (
                state != EndermanDungeonState.BOSS
                || activeBoss == null
                || activeBoss.isDead()
                || !activeBoss.isValid()
        ) {

            stopBossPatternTask();
            return;
        }

        bossPatternTicks +=
                20L;

        double healthRatio =
                activeBoss.getHealth()
                        / BOSS_VANILLA_HEALTH;

        updateBossPhase(
                healthRatio
        );

        long shockwaveInterval =
                switch (currentBossPhase) {

                    case 3 ->
                            PHASE_3_SHOCKWAVE_INTERVAL;

                    case 2 ->
                            PHASE_2_SHOCKWAVE_INTERVAL;

                    default ->
                            PHASE_1_SHOCKWAVE_INTERVAL;
                };

        if (
                bossPatternTicks - lastShockwaveTick
                >= shockwaveInterval
        ) {

            lastShockwaveTick =
                    bossPatternTicks;

            performShockwave();
        }

        /*
         * Phase 2 이상:
         * 10초마다 엔더의 포효.
         */
        if (
                currentBossPhase >= 2
                && bossPatternTicks - lastRoarTick >= 200L
        ) {

            lastRoarTick =
                    bossPatternTicks;

            performEnderRoar();
        }
    }


    private void updateBossPhase(
            double healthRatio
    ) {

        int nextPhase;

        if (healthRatio <= PHASE_3_HEALTH_RATIO) {

            nextPhase =
                    3;

        } else if (healthRatio <= PHASE_2_HEALTH_RATIO) {

            nextPhase =
                    2;

        } else {

            nextPhase =
                    1;
        }

        if (nextPhase == currentBossPhase) {
            return;
        }

        currentBossPhase =
                nextPhase;

        if (currentBossPhase == 2) {

            broadcast(
                    Component.text(
                            BOSS_NAME
                                    + "의 기운이 폭주하기 시작합니다!",
                            NamedTextColor.LIGHT_PURPLE
                    )
            );

            setBossMovementSpeedMultiplier(
                    1.30D
            );

        } else if (currentBossPhase == 3) {

            broadcast(
                    Component.text(
                            BOSS_NAME
                                    + "이 광폭화했습니다!",
                            NamedTextColor.RED
                    )
            );

            setAttribute(
                    activeBoss,
                    Attribute.ATTACK_DAMAGE,
                    BOSS_ATTACK_DAMAGE * 1.25D
            );

            setBossMovementSpeedMultiplier(
                    1.50D
            );
        }
    }


    private void performShockwave() {

        if (activeBoss == null) {
            return;
        }

        Location center =
                activeBoss.getLocation();

        double damage =
                switch (currentBossPhase) {

                    case 3 ->
                            PHASE_3_SHOCKWAVE_DAMAGE;

                    case 2 ->
                            PHASE_2_SHOCKWAVE_DAMAGE;

                    default ->
                            PHASE_1_SHOCKWAVE_DAMAGE;
                };

        dungeonWorld.spawnParticle(
                Particle.PORTAL,
                center.clone().add(0.0D, 1.0D, 0.0D),
                180,
                4.0D,
                1.0D,
                4.0D,
                0.3D
        );

        dungeonWorld.playSound(
                center,
                Sound.ENTITY_ENDERMAN_SCREAM,
                2.0F,
                0.7F
        );

        for (UUID uuid : activeParticipants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
                    || player.getWorld() != dungeonWorld
            ) {
                continue;
            }

            Location playerLocation =
                    player.getLocation();

            if (
                    playerLocation.distanceSquared(center)
                    > SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS
            ) {
                continue;
            }

            player.damage(
                    damage,
                    activeBoss
            );

            Vector direction =
                    playerLocation
                            .toVector()
                            .subtract(
                                    center.toVector()
                            );

            direction.setY(
                    0.0D
            );

            if (direction.lengthSquared() > 0.0D) {

                direction.normalize();
            }

            direction.multiply(
                    1.4D
            );

            direction.setY(
                    0.55D
            );

            player.setVelocity(
                    direction
            );
        }
    }


    private void performEnderRoar() {

        if (activeBoss == null) {
            return;
        }

        Location center =
                activeBoss.getLocation();

        dungeonWorld.spawnParticle(
                Particle.REVERSE_PORTAL,
                center.clone().add(0.0D, 2.0D, 0.0D),
                250,
                5.0D,
                2.0D,
                5.0D,
                0.2D
        );

        dungeonWorld.playSound(
                center,
                Sound.ENTITY_ENDER_DRAGON_GROWL,
                2.0F,
                0.8F
        );

        for (UUID uuid : activeParticipants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
                    || player.getWorld() != dungeonWorld
            ) {
                continue;
            }

            Location playerLocation =
                    player.getLocation();

            if (
                    playerLocation.distanceSquared(center)
                    > ROAR_RADIUS * ROAR_RADIUS
            ) {
                continue;
            }

            player.damage(
                    ROAR_DAMAGE,
                    activeBoss
            );

            Vector direction =
                    playerLocation
                            .toVector()
                            .subtract(
                                    center.toVector()
                            );

            direction.setY(
                    0.0D
            );

            if (direction.lengthSquared() > 0.0D) {

                direction.normalize();
            }

            direction.multiply(
                    2.0D
            );

            direction.setY(
                    0.70D
            );

            player.setVelocity(
                    direction
            );
        }
    }


    private void setBossMovementSpeedMultiplier(
            double multiplier
    ) {

        if (activeBoss == null) {
            return;
        }

        AttributeInstance speed =
                activeBoss.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );

        if (speed == null) {
            return;
        }

        /*
         * Enderman 기본 이동속도를 기준으로 다시 계산.
         *
         * Phase 변경 때 기존 값을 계속 곱해서
         * 속도가 누적 증가하지 않도록 한다.
         */
        double vanillaBaseSpeed =
                0.30D;

        speed.setBaseValue(
                vanillaBaseSpeed
                        * multiplier
        );
    }


    private void stopBossPatternTask() {

        if (bossPatternTask != null) {

            bossPatternTask.cancel();

            bossPatternTask =
                    null;
        }
    }


    /*
     * =========================================================
     * SAFE SPAWN
     * =========================================================
     *
     * Enderman은 높이가 약 3블록이므로
     * 발 + 머리 + 머리 위 공간까지 확인한다.
     */

    private List<Location> findSafeSpawnLocations(
            World world,
            int required
    ) {

        List<Location> result =
                new ArrayList<>();

        /*
         * 전투 영역:
         *
         * X 287 ~ 304
         * Z -602 ~ -585
         *
         * 구조물의 실제 바닥 Y를 자동 탐색한다.
         * 예상 높이 78을 중심으로 Y 70 ~ 90 검색.
         */
        final int minX =
                288;

        final int maxX =
                303;

        final int minZ =
                -601;

        final int maxZ =
                -586;

        final int scanMinY =
                70;

        final int scanMaxY =
                90;

        List<Location> candidates =
                new ArrayList<>();

        for (
                int blockX = minX;
                blockX <= maxX;
                blockX++
        ) {

            for (
                    int blockZ = minZ;
                    blockZ <= maxZ;
                    blockZ++
            ) {

                /*
                 * 위쪽부터 내려오면서
                 * 실제로 설 수 있는 가장 높은 바닥을 찾는다.
                 */
                for (
                        int floorY = scanMaxY;
                        floorY >= scanMinY;
                        floorY--
                ) {

                    Block floor =
                            world.getBlockAt(
                                    blockX,
                                    floorY,
                                    blockZ
                            );

                    Block feet =
                            world.getBlockAt(
                                    blockX,
                                    floorY + 1,
                                    blockZ
                            );

                    Block head1 =
                            world.getBlockAt(
                                    blockX,
                                    floorY + 2,
                                    blockZ
                            );

                    Block head2 =
                            world.getBlockAt(
                                    blockX,
                                    floorY + 3,
                                    blockZ
                            );

                    if (!floor.getType().isSolid()) {
                        continue;
                    }

                    if (
                            !feet.isPassable()
                            || !head1.isPassable()
                            || !head2.isPassable()
                    ) {
                        continue;
                    }

                    candidates.add(
                            new Location(
                                    world,
                                    blockX + 0.5D,
                                    floorY + 1.0D,
                                    blockZ + 0.5D
                            )
                    );

                    /*
                     * 이 X/Z에서 사용할 수 있는
                     * 가장 높은 바닥 하나만 사용한다.
                     */
                    break;
                }
            }
        }

        /*
         * 전투 영역 중앙에서 가까운 위치부터 사용.
         */
        candidates.sort(
                java.util.Comparator.comparingDouble(
                        location -> {

                            double dx =
                                    location.getX()
                                            - SPAWN_CENTER_X;

                            double dz =
                                    location.getZ()
                                            - SPAWN_CENTER_Z;

                            return (dx * dx)
                                    + (dz * dz);
                        }
                )
        );

        for (Location candidate : candidates) {

            if (
                    !isFarEnoughFromExisting(
                            candidate,
                            result
                    )
            ) {
                continue;
            }

            result.add(candidate);

            if (result.size() >= required) {
                break;
            }
        }

        plugin.getLogger().info(
                "[EndermanDungeon] Safe spawn candidates="
                        + candidates.size()
                        + ", selected="
                        + result.size()
                        + " / "
                        + required
        );

        if (!result.isEmpty()) {

            Location first =
                    result.get(0);

            plugin.getLogger().info(
                    "[EndermanDungeon] First spawn location: "
                            + first.getX()
                            + ", "
                            + first.getY()
                            + ", "
                            + first.getZ()
            );
        }

        return result;
    }


    private boolean isFarEnoughFromExisting(
            Location candidate,
            List<Location> existing
    ) {

        double minimumSquared =
                MIN_SPAWN_DISTANCE
                        * MIN_SPAWN_DISTANCE;

        for (Location location : existing) {

            double dx =
                    candidate.getX()
                            - location.getX();

            double dz =
                    candidate.getZ()
                            - location.getZ();

            double distanceSquared =
                    (dx * dx)
                            + (dz * dz);

            if (distanceSquared < minimumSquared) {
                return false;
            }
        }

        return true;
    }


    /*
     * =========================================================
     * ENTITY IDENTIFICATION
     * =========================================================
     */

    public boolean isDungeonMob(
            LivingEntity entity
    ) {

        String dungeonId =
                entity
                        .getPersistentDataContainer()
                        .get(
                                dungeonIdKey,
                                PersistentDataType.STRING
                        );

        return DUNGEON_ID.equals(
                dungeonId
        );
    }


    public boolean isBoss(
            LivingEntity entity
    ) {

        if (!isDungeonMob(entity)) {
            return false;
        }

        String bossId =
                entity
                        .getPersistentDataContainer()
                        .get(
                                dungeonBossKey,
                                PersistentDataType.STRING
                        );

        return BOSS_ID.equals(
                bossId
        );
    }


    public double getEffectiveHealth(
            LivingEntity entity
    ) {

        Double value =
                entity
                        .getPersistentDataContainer()
                        .get(
                                effectiveHealthKey,
                                PersistentDataType.DOUBLE
                        );

        if (value == null) {
            return 0.0D;
        }

        return value;
    }


    /*
     * =========================================================
     * PLAYER DEATH / QUIT
     * =========================================================
     */

    public void onParticipantUnavailable(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        /*
         * 던전 참가자가 아니면 무시한다.
         */
        if (!participants.contains(uuid)) {
            return;
        }

        activeParticipants.remove(uuid);


        /*
         * PARTICIPANT RETARGET
         *
         * 사망/로그아웃한 참가자를 추적하던 몹은
         * 남아 있는 참가자로 즉시 타겟을 변경한다.
         */
        retargetAllDungeonMobs();


        /*
         * 사망한 플레이어는 즉시 텔레포트할 수 없으므로
         * 리스폰 후 출구 이동 대상으로 보관한다.
         *
         * 로그아웃 시에도 안전하게 같은 대상으로 등록하며,
         * 재접속/리스폰 처리에서 사용할 수 있다.
         */
        pendingExitParticipants.add(uuid);

        if (!isRunning()) {
            return;
        }

        if (!activeParticipants.isEmpty()) {
            return;
        }

        failDungeon();
    }


    /*
     * =========================================================
     * DUNGEON TARGET
     * =========================================================
     *
     * 현재 살아있는 activeParticipant 중
     * 기준 위치와 가장 가까운 플레이어를 반환한다.
     */
    public Player findNearestActiveParticipant(
            Location origin
    ) {

        if (
                origin == null
                || dungeonWorld == null
        ) {
            return null;
        }

        Player nearest =
                null;

        double nearestDistanceSquared =
                Double.MAX_VALUE;

        for (UUID uuid : activeParticipants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
                    || player.getWorld()
                            != dungeonWorld
            ) {
                continue;
            }

            double distanceSquared =
                    player.getLocation()
                            .distanceSquared(
                                    origin
                            );

            if (
                    distanceSquared
                            >= nearestDistanceSquared
            ) {
                continue;
            }

            nearest =
                    player;

            nearestDistanceSquared =
                    distanceSquared;
        }

        return nearest;
    }


    public boolean isActiveParticipant(
            Player player
    ) {

        if (
                player == null
                || !player.isOnline()
                || player.isDead()
                || dungeonWorld == null
                || player.getWorld()
                        != dungeonWorld
        ) {
            return false;
        }

        if (
                !activeParticipants.contains(
                        player.getUniqueId()
                )
        ) {
            return false;
        }

        return true;
    }


    public void targetNearestActiveParticipant(
            Enderman enderman
    ) {

        if (
                enderman == null
                || enderman.isDead()
                || !enderman.isValid()
                || !isDungeonMob(enderman)
        ) {
            return;
        }

        Player target =
                findNearestActiveParticipant(
                        enderman.getLocation()
                );

        if (target == null) {
            enderman.setTarget(null);
            return;
        }

        if (enderman.getTarget() == target) {
            return;
        }

        enderman.setTarget(
                target
        );
    }


    public void scheduleDungeonRetarget(
            Enderman enderman
    ) {

        if (enderman == null) {
            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> targetNearestActiveParticipant(
                                enderman
                        )
                );
    }


    private void retargetAllDungeonMobs() {

        for (
                UUID uuid :
                new HashSet<>(
                        activeDungeonMobs
                )
        ) {

            org.bukkit.entity.Entity entity =
                    plugin.getServer()
                            .getEntity(uuid);

            if (
                    entity instanceof
                            Enderman enderman
            ) {

                targetNearestActiveParticipant(
                        enderman
                );
            }
        }
    }


    public boolean isParticipant(
            Player player
    ) {

        return participants.contains(
                player.getUniqueId()
        );
    }


    public boolean hasPendingExit(
            Player player
    ) {

        return pendingExitParticipants.contains(
                player.getUniqueId()
        );
    }


    public void completePendingExit(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        if (!pendingExitParticipants.remove(uuid)) {
            return;
        }

        teleportToExit(
                player
        );
    }


    private void failDungeon() {

        state =
                EndermanDungeonState.RESETTING;

        /*
         * 좀비 던전 UI 표준:
         * 실패 채팅 + RED Subtitle 5초.
         */
        broadcast(
                Component.text(
                        "던전 공략에 실패했습니다.",
                        NamedTextColor.RED
                )
        );

        clearParticipantTitles();

        showBossSubtitle(
                Component.text(
                        "잠시 후 던전에서 퇴장합니다.",
                        NamedTextColor.RED
                ),
                5000L
        );

        teleportAliveParticipantsToExit();

        removeAllDungeonMobs();

        removeBossBar();

        scheduleReset(
                100L
        );
    }


    /*
     * =========================================================
     * REWARD
     * =========================================================
     */

    private void rewardClear() {

        if (dungeonRewardService == null) {

            plugin.getLogger().severe(
                    "[EndermanDungeon] "
                            + "DungeonRewardService is null."
            );

            return;
        }

        Set<UUID> clearParticipants =
                Set.copyOf(
                        participants
                );

        dungeonRewardService
                .rewardDungeonClearExperience(
                        clearParticipants,
                        CLEAR_EXP
                );

        dungeonRewardService
                .rewardEndermanRuinsBasicRewards(
                        clearParticipants
                );

        /*
         * 던전 클리어 주괴 보상
         * 영웅 / 전설 / 신화 독립 추첨
         */
        dungeonRewardService
                .rewardEquipmentIngots(
                clearParticipants,
                0.10D,
                0.05D,
                0.02D
                );

        dungeonRewardService
                .rewardTeyvatRareWeapons(
                        clearParticipants
                );

        dungeonRewardService
                .rewardDungeonGold(
                        clearParticipants,
                        700L,
                        300L,
                        0.20D
                );

        dungeonRewardService
                .rewardEnhancementStones(
                        clearParticipants,
                        2,
                        0.40D,
                        1,
                        0
                );


    }


    /*
     * =========================================================
     * DUNGEON EXIT
     * =========================================================
     */

    private void scheduleClearExitCountdown(
            long delayTicks
    ) {

        if (dungeonResetTask != null) {
            dungeonResetTask.cancel();
        }

        dungeonResetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    dungeonResetTask =
                                            null;

                                    clearParticipantTitles();

                                    runClearExitCountdown(
                                            5
                                    );
                                },
                                delayTicks
                        );
    }


    /*
     * =========================================================
     * DUNGEON EXIT COUNTDOWN
     * =========================================================
     *
     * 좀비 던전 UI 표준:
     * 5~4초 YELLOW, 3~1초 RED.
     * Subtitle만 사용하며 각 숫자는 1초 간격이다.
     */
    private void runClearExitCountdown(
            int seconds
    ) {

        if (
                state
                        != EndermanDungeonState.CLEARED
        ) {
            return;
        }

        if (seconds <= 0) {

            clearParticipantTitles();

            teleportAliveParticipantsToExit();

            resetDungeon();

            return;
        }

        NamedTextColor countdownColor =
                seconds <= 3
                        ? NamedTextColor.RED
                        : NamedTextColor.YELLOW;

        showBossSubtitle(
                Component.text(
                        "던전 퇴장까지 "
                                + seconds
                                + "초 남았습니다.",
                        countdownColor
                ),
                1100L
        );

        dungeonResetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    dungeonResetTask =
                                            null;

                                    runClearExitCountdown(
                                            seconds - 1
                                    );
                                },
                                20L
                        );
    }


    private void teleportAliveParticipantsToExit() {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
            ) {
                continue;
            }

            pendingExitParticipants.remove(uuid);

            teleportToExit(
                    player
            );
        }
    }


    /*
     * PlayerRespawnEvent에서도 사용할 수 있도록
     * public 메서드로 제공한다.
     *
     * 죽은 플레이어는 다음 단계에서
     * 리스폰 직후 이 메서드를 호출한다.
     */
    public void teleportToExit(
            Player player
    ) {

        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }

        Location destination =
                new Location(
                        player.getWorld(),
                        EXIT_X,
                        EXIT_Y,
                        EXIT_Z,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );

        player.teleport(
                destination
        );
    }


    /*
     * =========================================================
     * RESET
     * =========================================================
     */

    private void scheduleReset(
            long delayTicks
    ) {

        if (dungeonResetTask != null) {
            dungeonResetTask.cancel();
        }

        dungeonResetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                this::resetDungeon,
                                delayTicks
                        );
    }


    private void resetDungeon() {

        removeAllDungeonMobs();

        removeBossBar();

        clearParticipantTitles();

        participants.clear();
        activeParticipants.clear();
        activeDungeonMobs.clear();

        activeBoss =
                null;

        dungeonWorld =
                null;

        dungeonResetTask =
                null;

        state =
                EndermanDungeonState.IDLE;

        plugin.getLogger().info(
                "[EndermanDungeon] Reset complete."
        );
    }


    private void removeAllDungeonMobs() {

        for (UUID uuid :
                new HashSet<>(
                        activeDungeonMobs
                )) {

            org.bukkit.entity.Entity entity =
                    plugin.getServer()
                            .getEntity(uuid);

            if (entity != null) {
                entity.remove();
            }
        }

        activeDungeonMobs.clear();
    }


    /*
     * =========================================================
     * DUNGEON BOUNDARY
     * =========================================================
     */

    public boolean isInsideBoundary(
            Location location
    ) {

        if (
                location == null
                || dungeonWorld == null
                || !dungeonWorld.equals(
                        location.getWorld()
                )
        ) {
            return false;
        }

        double x =
                location.getX();

        double z =
                location.getZ();

        return x >= -10000370.0D
                && x < -10000352.0D
                && z >= -9999931.0D
                && z < -9999913.0D;
    }


    /*
     * =========================================================
     * PUBLIC STATE
     * =========================================================
     */

    public boolean isRunning() {

        return state != EndermanDungeonState.IDLE
                && state != EndermanDungeonState.CLEARED
                && state != EndermanDungeonState.RESETTING;
    }


    public EndermanDungeonState getState() {
        return state;
    }


    /*
     * =========================================================
     * DISPLAY
     * =========================================================
     */

    private void broadcast(
            Component component
    ) {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }

            player.sendMessage(
                    component
            );
        }
    }


    private void showTitleToParticipants(
            Title title
    ) {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }

            player.showTitle(title);
        }
    }


    private void clearParticipantTitles() {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
            ) {
                continue;
            }

            player.clearTitle();
        }
    }


    /*
     * =========================================================
     * ATTRIBUTE
     * =========================================================
     */

    private void setAttribute(
            LivingEntity entity,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                entity.getAttribute(
                        attribute
                );

        if (instance != null) {

            instance.setBaseValue(
                    value
            );
        }
    }
}
