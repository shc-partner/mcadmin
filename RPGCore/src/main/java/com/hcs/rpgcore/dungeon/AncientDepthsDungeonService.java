package com.hcs.rpgcore.dungeon;

import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


public final class AncientDepthsDungeonService {


    /*
     * =========================================================
     * DUNGEON
     * =========================================================
     */

    public static final String DUNGEON_ID =
            "ancient_depths_40_50";


    private static final double BOUNDARY_MIN_X =
            -5000005.0D;

    private static final double BOUNDARY_MAX_X =
            -4999966.0D;

    private static final double BOUNDARY_MIN_Z =
            -5000006.0D;

    private static final double BOUNDARY_MAX_Z =
            -4999965.0D;


    private static final double SPAWN_CENTER_X =
            -4999984.0D;

    private static final double SPAWN_FEET_Y =
            131.0D;

    private static final double SPAWN_CENTER_Z =
            -4999986.0D;


    private static final double SPAWN_RADIUS_X =
            18.0D;

    private static final double SPAWN_RADIUS_Z =
            19.0D;

    /*
     * 심층 엔더마이트는 기본 크기가 너무 작아 전투 중 식별하기 어렵다.
     * 전투 중 식별하기 쉽도록 엔더마이트에만 SCALE 3.0을 적용한다.
     */
    private static final double DEEP_ENDERMITE_SCALE =
            3.0D;


    private static final double MIN_SPAWN_DISTANCE =
            1.5D;

    private static final int MAX_SPAWN_ATTEMPTS =
            4000;


    private static final double BOSS_X =
            -4999986.0D;

    private static final double BOSS_Y =
            131.0D;

    private static final double BOSS_Z =
            -4999986.0D;



    private static final long CLEAR_EXP =
            1500L;


    /*
     * =========================================================
     * BOSS
     * =========================================================
     */

    private static final String BOSS_NAME =
            "심연의 감시자";

    private static final double BOSS_EFFECTIVE_HEALTH =
            4500.0D;

    private static final double BOSS_VANILLA_HEALTH =
            1024.0D;

    private static final double BOSS_ATTACK_DAMAGE =
            25.0D;


    private static final int NEXT_WAVE_SECONDS =
            3;


    /*
     * =========================================================
     * RUNTIME
     * =========================================================
     */

    private final JavaPlugin plugin;

    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey dungeonWaveKey;
    private final NamespacedKey dungeonBossKey;
    private final NamespacedKey effectiveHealthKey;
    private final NamespacedKey attackDamageKey;


    private final Set<UUID> participants =
            new HashSet<>();

    private final Set<UUID> activeParticipants =
            new HashSet<>();

    private final Set<UUID> activeDungeonMobs =
            new HashSet<>();

    private final Map<UUID, Double> attackDamageMap =
            new HashMap<>();

    /*
     * 플레이어별 고정 퇴장 위치.
     * 입장 리스너가 전달한 외부 퇴장 좌표를 저장한다.
     */
    private final Map<UUID, Location> returnLocations =
            new HashMap<>();

    /*
     * 사망/로그아웃 상태에서는 즉시 teleport할 수 없으므로
     * respawn 또는 join 시 사용할 귀환 위치를 저장한다.
     */
    private final Map<UUID, Location> pendingReturnLocations =
            new HashMap<>();


    private AncientDepthsDungeonState state =
            AncientDepthsDungeonState.IDLE;

    private World dungeonWorld;

    private DungeonRewardService dungeonRewardService;

    private LivingEntity activeBoss;

    private BossBar bossBar;

    private BukkitTask bossBarTask;

    private BukkitTask resetTask;


    public AncientDepthsDungeonService(
            JavaPlugin plugin
    ) {

        this.plugin =
                plugin;

        this.dungeonIdKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_id"
                );

        this.dungeonWaveKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_wave"
                );

        this.dungeonBossKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_boss"
                );

        this.effectiveHealthKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_effective_health"
                );

        this.attackDamageKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_attack_damage"
                );
    }


    public void setRewardService(
            DungeonRewardService dungeonRewardService
    ) {

        this.dungeonRewardService =
                dungeonRewardService;
    }


    public boolean isRunning() {

        return state != AncientDepthsDungeonState.IDLE;
    }


    /*
     * =========================================================
     * ENTRY
     * =========================================================
     */

    public void onPlayerEntered(
            Player player,
            Location returnLocation
    ) {

        UUID uuid =
                player.getUniqueId();

        /*
         * 종료 처리 중에는 새 참가자를 받지 않는다.
         */
        if (
                state == AncientDepthsDungeonState.CLEARED
                || state == AncientDepthsDungeonState.RESETTING
        ) {

            if (returnLocation != null) {
                player.teleport(
                        returnLocation
                );
            }

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

        if (returnLocation != null) {
            returnLocations.putIfAbsent(
                    uuid,
                    returnLocation.clone()
            );
            pendingReturnLocations.remove(uuid);
        }

        /*
         * 이미 던전이 진행 중이라면 웨이브를 다시 만들지 않는다.
         */
        if (state != AncientDepthsDungeonState.IDLE) {

            if (bossBar != null) {
                bossBar.addPlayer(player);
            }

            player.sendMessage(
                    Component.text(
                            "[던전] ",
                            NamedTextColor.DARK_PURPLE
                    ).append(
                            Component.text(
                                    "현재 진행 중인 고대 심층에 참가했습니다.",
                                    NamedTextColor.GRAY
                            )
                    )
            );
            return;
        }

        dungeonWorld = player.getWorld();
        state = AncientDepthsDungeonState.WAVE_1;

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
                        () -> startWave(1),
                        40L
                );
    }


    public void onParticipantDeath(
            Player player
    ) {

        UUID uuid = player.getUniqueId();

        if (!participants.contains(uuid)) {
            return;
        }

        if (
                state == AncientDepthsDungeonState.IDLE
                || state == AncientDepthsDungeonState.CLEARED
                || state == AncientDepthsDungeonState.RESETTING
        ) {
            return;
        }

        activeParticipants.remove(uuid);

        Location returnLocation =
                returnLocations.get(uuid);

        if (returnLocation != null) {
            pendingReturnLocations.put(
                    uuid,
                    returnLocation.clone()
            );
        }

        player.sendMessage(
                Component.text(
                        "[던전] 전투 불능 상태가 되었습니다.",
                        NamedTextColor.RED
                )
        );

        checkFailure();
    }


    public void onParticipantQuit(
            Player player
    ) {

        UUID uuid = player.getUniqueId();

        if (!participants.contains(uuid)) {
            return;
        }

        if (
                state == AncientDepthsDungeonState.IDLE
                || state == AncientDepthsDungeonState.RESETTING
        ) {
            return;
        }

        activeParticipants.remove(uuid);

        Location returnLocation =
                returnLocations.get(uuid);

        if (returnLocation != null) {
            pendingReturnLocations.put(
                    uuid,
                    returnLocation.clone()
            );
        }

        checkFailure();
    }


    public Location consumePendingReturnLocation(
            UUID uuid
    ) {

        Location location =
                pendingReturnLocations.remove(uuid);

        if (location == null) {
            return null;
        }

        return location.clone();
    }


    public Location consumePendingExitLocation(
            UUID uuid
    ) {
        return consumePendingReturnLocation(uuid);
    }


    public void restorePendingExitOnJoin(
            Player player
    ) {

        UUID uuid = player.getUniqueId();
        Location location = consumePendingReturnLocation(uuid);

        if (location == null) {
            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {
                            if (!player.isOnline() || player.isDead()) {
                                pendingReturnLocations.put(
                                        uuid,
                                        location.clone()
                                );
                                return;
                            }

                            if (!player.teleport(location)) {
                                pendingReturnLocations.put(
                                        uuid,
                                        location.clone()
                                );
                            }
                        }
                );
    }


    private void checkFailure() {

        if (
                state == AncientDepthsDungeonState.IDLE
                || state == AncientDepthsDungeonState.CLEARED
                || state == AncientDepthsDungeonState.RESETTING
                || !activeParticipants.isEmpty()
        ) {
            return;
        }

        failDungeon();
    }


    private void failDungeon() {

        if (
                state == AncientDepthsDungeonState.IDLE
                || state == AncientDepthsDungeonState.CLEARED
                || state == AncientDepthsDungeonState.RESETTING
        ) {
            return;
        }

        state = AncientDepthsDungeonState.RESETTING;

        removeBossBar();
        clearTitles();

        broadcast(
                Component.text(
                        "던전 공략에 실패했습니다.",
                        NamedTextColor.RED
                )
        );

        showBossSubtitle(
                Component.text(
                        "잠시 후 던전에서 퇴장합니다.",
                        NamedTextColor.RED
                ),
                5000L
        );

        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }

        resetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> resetDungeon(false),
                                100L
                        );
    }


    /*
     * =========================================================
     * WAVE COUNTDOWN
     * =========================================================
     */

    private void runWaveCountdown(
            int wave,
            int seconds
    ) {

        if (!isExpectedWave(wave)) {
            return;
        }

        if (seconds <= 0) {

            clearTitles();
            startWave(wave);
            return;
        }

        Title title =
                Title.title(
                        Component.empty(),
                        Component.text(
                                "Wave "
                                        + wave
                                        + " 시작까지 "
                                        + seconds
                                        + "초 남았습니다.",
                                NamedTextColor.LIGHT_PURPLE
                        ),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(1100L),
                                Duration.ZERO
                        )
                );

        forEachParticipant(
                player ->
                        player.showTitle(title)
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> runWaveCountdown(
                                wave,
                                seconds - 1
                        ),
                        20L
                );
    }


    /*
     * =========================================================
     * WAVES
     * =========================================================
     */

    private void startWave(
            int wave
    ) {

        if (
                !isExpectedWave(
                        wave
                )
                || dungeonWorld == null
        ) {

            return;
        }


        List<MobSpec> specs =
                getWaveSpecs(
                        wave
                );


        List<Location> locations =
                findSafeSpawnLocations(
                        dungeonWorld,
                        specs.size()
                );


        if (
                locations.size()
                        < specs.size()
        ) {

            broadcast(
                    Component.text(
                            "던전 몬스터 생성 위치를 찾지 못했습니다.",
                            NamedTextColor.RED
                    )
            );


            markAndReturnParticipants();

            scheduleReset(
                    40L
            );

            return;
        }


        activeDungeonMobs.clear();


        broadcast(
                Component.text(
                        "Wave " + wave,
                        NamedTextColor.RED
                ).append(
                        Component.text(
                                " 시작!",
                                NamedTextColor.WHITE
                        )
                )
        );


        for (
                int i = 0;
                i < specs.size();
                i++
        ) {

            spawnWaveMob(
                    specs.get(
                            i
                    ),
                    wave,
                    locations.get(
                            i
                    )
            );
        }
    }


    private List<MobSpec> getWaveSpecs(
            int wave
    ) {

        List<MobSpec> list =
                new ArrayList<>();


        switch (
                wave
        ) {

            case 1 -> {

                addSpecs(
                        list,
                        EntityType.CAVE_SPIDER,
                        "심층 동굴 거미",
                        4,
                        525.0D,
                        15.0D
                );

                addSpecs(
                        list,
                        EntityType.ENDERMITE,
                        "심층 엔더마이트",
                        4,
                        500.0D,
                        15.0D
                );

                addSpecs(
                        list,
                        EntityType.SKELETON,
                        "강화 스켈레톤",
                        2,
                        600.0D,
                        17.0D
                );
            }


            case 2 -> {

                addSpecs(
                        list,
                        EntityType.BOGGED,
                        "심층 보그드",
                        3,
                        575.0D,
                        18.0D
                );

                addSpecs(
                        list,
                        EntityType.CAVE_SPIDER,
                        "심층 동굴 거미",
                        3,
                        575.0D,
                        17.0D
                );

                addSpecs(
                        list,
                        EntityType.SKELETON,
                        "강화 스켈레톤",
                        3,
                        650.0D,
                        19.0D
                );
            }


            case 3 -> {

                addSpecs(
                        list,
                        EntityType.BOGGED,
                        "심층 보그드",
                        4,
                        750.0D,
                        21.0D
                );

                addSpecs(
                        list,
                        EntityType.SKELETON,
                        "강화 스켈레톤",
                        3,
                        825.0D,
                        23.0D
                );

                addSpecs(
                        list,
                        EntityType.ENDERMITE,
                        "심층 엔더마이트",
                        4,
                        625.0D,
                        19.0D
                );
            }


            default -> {
            }
        }


        return list;
    }


    private void addSpecs(
            List<MobSpec> list,
            EntityType type,
            String name,
            int count,
            double health,
            double attack
    ) {

        for (
                int i = 0;
                i < count;
                i++
        ) {

            list.add(
                    new MobSpec(
                            type,
                            name,
                            health,
                            attack
                    )
            );
        }
    }


    private void spawnWaveMob(
            MobSpec spec,
            int wave,
            Location location
    ) {

        Entity raw =
                dungeonWorld.spawnEntity(
                        location,
                        spec.type()
                );


        if (
                !(raw instanceof Mob mob)
        ) {

            raw.remove();

            return;
        }


        applyDungeonTags(
                mob,
                wave,
                false,
                spec.health(),
                spec.attack()
        );


        setHealth(
                mob,
                spec.health()
        );


        setAttackAttribute(
                mob,
                spec.attack()
        );


        /*
         * 심층 엔더마이트만 확대한다.
         * 다른 웨이브 몬스터의 크기는 변경하지 않는다.
         */
        if (spec.type() == EntityType.ENDERMITE) {

            AttributeInstance scaleAttribute =
                    mob.getAttribute(
                            Attribute.SCALE
                    );

            if (scaleAttribute != null) {

                scaleAttribute.setBaseValue(
                        DEEP_ENDERMITE_SCALE
                );
            }
        }


        mob.customName(
                Component.text(
                        spec.name(),
                        NamedTextColor.DARK_AQUA
                )
        );


        mob.setCustomNameVisible(
                false
        );

        mob.setPersistent(
                true
        );

        mob.setRemoveWhenFarAway(
                false
        );

        mob.setCanPickupItems(
                false
        );


        Player target =
                findNearestActiveParticipant(
                        mob.getLocation()
                );


        if (
                target != null
        ) {

            mob.setTarget(
                    target
            );
        }


        UUID uuid =
                mob.getUniqueId();


        activeDungeonMobs.add(
                uuid
        );

        attackDamageMap.put(
                uuid,
                spec.attack()
        );
    }


    /*
     * =========================================================
     * MOB DEATH / TRANSITION
     * =========================================================
     */

    public void onDungeonMobDeath(
            LivingEntity entity
    ) {

        UUID uuid =
                entity.getUniqueId();

        boolean removed =
                activeDungeonMobs.remove(
                        uuid
                );

        attackDamageMap.remove(uuid);

        if (!removed) {
            return;
        }

        if (!activeDungeonMobs.isEmpty()) {
            return;
        }

        switch (state) {

            case WAVE_1 -> {

                state =
                        AncientDepthsDungeonState.WAVE_2;

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

                scheduleNextWave(2);
            }

            case WAVE_2 -> {

                state =
                        AncientDepthsDungeonState.WAVE_3;

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

                scheduleNextWave(3);
            }

            case WAVE_3 -> {

                state =
                        AncientDepthsDungeonState.BOSS;

                clearTitles();

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

                state =
                        AncientDepthsDungeonState.CLEARED;

                rewardClear();

                removeBossBar();
                clearTitles();

                broadcast(
                        Component.text(
                                "심연의 감시자를 처치했습니다.",
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

                scheduleSuccessfulDungeonExit();
            }

            default -> {
            }
        }
    }


    private void scheduleNextWave(
            int wave
    ) {

        runWaveCountdown(
                wave,
                NEXT_WAVE_SECONDS
        );
    }


    /*
     * =========================================================
     * BOSS INTRO
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

        if (state != AncientDepthsDungeonState.BOSS) {
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

                            if (state != AncientDepthsDungeonState.BOSS) {
                                return;
                            }

                            showBossSubtitle(
                                    Component.text(
                                            "깊은 어둠 속에서 강력한 기운이 느껴집니다...",
                                            NamedTextColor.DARK_RED
                                    ),
                                    3000L
                            );

                            plugin.getServer()
                                    .getScheduler()
                                    .runTaskLater(
                                            plugin,
                                            () -> {

                                                if (state != AncientDepthsDungeonState.BOSS) {
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

                                                                    if (state != AncientDepthsDungeonState.BOSS) {
                                                                        return;
                                                                    }

                                                                    clearTitles();
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
     * BOSS
     * =========================================================
     */

    private void spawnBoss() {

        if (
                state
                        != AncientDepthsDungeonState.BOSS
                || dungeonWorld == null
        ) {

            return;
        }


        Location location =
                new Location(
                        dungeonWorld,
                        BOSS_X,
                        BOSS_Y,
                        BOSS_Z
                );


        Entity raw =
                dungeonWorld.spawnEntity(
                        location,
                        EntityType.WARDEN
                );


        if (
                !(raw instanceof LivingEntity boss)
        ) {

            raw.remove();

            scheduleReset(
                    40L
            );

            return;
        }


        applyDungeonTags(
                boss,
                4,
                true,
                BOSS_EFFECTIVE_HEALTH,
                BOSS_ATTACK_DAMAGE
        );


        setHealth(
                boss,
                BOSS_EFFECTIVE_HEALTH
        );


        setAttackAttribute(
                boss,
                BOSS_ATTACK_DAMAGE
        );


        boss.customName(
                Component.text(
                        BOSS_NAME,
                        NamedTextColor.DARK_RED
                )
        );


        boss.setCustomNameVisible(
                true
        );

        boss.setPersistent(
                true
        );

        boss.setRemoveWhenFarAway(
                false
        );


        if (
                boss instanceof Mob mob
        ) {

            mob.setCanPickupItems(
                    false
            );


            Player target =
                    findNearestActiveParticipant(
                            boss.getLocation()
                    );


            if (
                    target != null
            ) {

                mob.setTarget(
                        target
                );
            }
        }


        activeBoss =
                boss;


        activeDungeonMobs.clear();

        activeDungeonMobs.add(
                boss.getUniqueId()
        );


        attackDamageMap.put(
                boss.getUniqueId(),
                BOSS_ATTACK_DAMAGE
        );


        createBossBar();


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
    }


    /*
     * =========================================================
     * BOSS BAR
     * =========================================================
     */

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

        forEachParticipant(
                player ->
                        bossBar.addPlayer(
                                player
                        )
        );

        bossBarTask =
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


    /*
     * =========================================================
     * CLEAR EXIT
     * =========================================================
     */

    /*
     * =========================================================
     * DUNGEON EXIT COUNTDOWN
     * =========================================================
     *
     * 좀비 던전 UI 표준:
     * 5~4초 YELLOW, 3~1초 RED.
     * Subtitle만 사용하며 각 숫자는 1초 간격이다.
     */
    private void scheduleSuccessfulDungeonExit() {

        if (resetTask != null) {
            return;
        }

        resetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {
                                    resetTask = null;
                                    runClearExitCountdown(5);
                                },
                                100L
                        );
    }


    private void runClearExitCountdown(
            int seconds
    ) {

        if (state != AncientDepthsDungeonState.CLEARED) {
            resetTask = null;
            return;
        }

        if (seconds <= 0) {
            resetTask = null;
            resetDungeon(true);
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

        resetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {
                                    resetTask = null;
                                    runClearExitCountdown(
                                            seconds - 1
                                    );
                                },
                                20L
                        );
    }


    private void markAndReturnParticipants() {
        returnParticipants(false);
    }


    private void returnParticipants(
            boolean cleared
    ) {

        for (UUID uuid : Set.copyOf(participants)) {

            Location returnLocation =
                    returnLocations.get(uuid);

            if (returnLocation == null) {
                continue;
            }

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
            ) {
                pendingReturnLocations.put(
                        uuid,
                        returnLocation.clone()
                );
                continue;
            }

            if (!player.teleport(returnLocation)) {
                pendingReturnLocations.put(
                        uuid,
                        returnLocation.clone()
                );
                continue;
            }

            pendingReturnLocations.remove(uuid);
        }
    }


    /*
     * =========================================================
     * REWARD
     * =========================================================
     */

    private void rewardClear() {

        if (
                dungeonRewardService == null
        ) {

            return;
        }


        Set<UUID> rewardParticipants =
                new HashSet<>(
                        participants
                );

        dungeonRewardService
                .rewardDungeonClearExperience(
                        rewardParticipants,
                        CLEAR_EXP
                );

    

        dungeonRewardService
                .rewardAncientDepthsBasicRewards(
                        rewardParticipants
                );

        /*
         * 던전 클리어 주괴 보상
         * 영웅 / 전설 / 신화 독립 추첨
         */
        dungeonRewardService
                .rewardEquipmentIngots(
                rewardParticipants,
                0.05D,
                0.02D,
                0.00D
                );

        dungeonRewardService
                .rewardTeyvatRareWeapons(
                        rewardParticipants
                );

        dungeonRewardService
                .rewardDungeonGold(
                        rewardParticipants,
                        350L,
                        150L,
                        0.20D
                );

        dungeonRewardService
                .rewardEnhancementStones(
                        rewardParticipants,
                        2,
                        0.0D,
                        0,
                        0
                );
}


    /*
     * =========================================================
     * PDC
     * =========================================================
     */

    private void applyDungeonTags(
            LivingEntity entity,
            int wave,
            boolean boss,
            double effectiveHealth,
            double attack
    ) {

        PersistentDataContainer pdc =
                entity.getPersistentDataContainer();


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

        pdc.set(
                dungeonBossKey,
                PersistentDataType.BYTE,
                boss
                        ? (byte) 1
                        : (byte) 0
        );

        pdc.set(
                effectiveHealthKey,
                PersistentDataType.DOUBLE,
                effectiveHealth
        );

        pdc.set(
                attackDamageKey,
                PersistentDataType.DOUBLE,
                attack
        );
    }


    public boolean isDungeonMob(
            LivingEntity entity
    ) {

        String id =
                entity.getPersistentDataContainer()
                        .get(
                                dungeonIdKey,
                                PersistentDataType.STRING
                        );


        return DUNGEON_ID.equals(
                id
        );
    }


    public double getEffectiveHealth(
            LivingEntity entity
    ) {

        Double value =
                entity.getPersistentDataContainer()
                        .get(
                                effectiveHealthKey,
                                PersistentDataType.DOUBLE
                        );


        if (
                value == null
        ) {

            return entity.getMaxHealth();
        }


        return value;
    }


    public double getAttackDamage(
            LivingEntity entity
    ) {

        Double mapped =
                attackDamageMap.get(
                        entity.getUniqueId()
                );


        if (
                mapped != null
        ) {

            return mapped;
        }


        Double stored =
                entity.getPersistentDataContainer()
                        .get(
                                attackDamageKey,
                                PersistentDataType.DOUBLE
                        );


        if (
                stored == null
        ) {

            return 0.0D;
        }


        return stored;
    }


    /*
     * =========================================================
     * HEALTH / ATTACK
     * =========================================================
     */

    private void setHealth(
            LivingEntity entity,
            double effectiveHealth
    ) {

        double actualHealth =
                Math.min(
                        effectiveHealth,
                        BOSS_VANILLA_HEALTH
                );


        AttributeInstance maxHealth =
                entity.getAttribute(
                        Attribute.MAX_HEALTH
                );


        if (
                maxHealth != null
        ) {

            maxHealth.setBaseValue(
                    actualHealth
            );
        }


        entity.setHealth(
                actualHealth
        );
    }


    private void setAttackAttribute(
            LivingEntity entity,
            double attack
    ) {

        AttributeInstance attribute =
                entity.getAttribute(
                        Attribute.ATTACK_DAMAGE
                );


        if (
                attribute != null
        ) {

            attribute.setBaseValue(
                    attack
            );
        }
    }


    /*
     * =========================================================
     * SAFE SPAWN
     * =========================================================
     */

    private List<Location> findSafeSpawnLocations(
            World world,
            int count
    ) {

        List<Location> result =
                new ArrayList<>();


        int attempts =
                0;


        while (
                result.size()
                        < count
                && attempts
                        < MAX_SPAWN_ATTEMPTS
        ) {

            attempts++;


            double x =
                    SPAWN_CENTER_X
                            + (
                            Math.random()
                                    * 2.0D
                                    - 1.0D
                    )
                            * SPAWN_RADIUS_X;


            double z =
                    SPAWN_CENTER_Z
                            + (
                            Math.random()
                                    * 2.0D
                                    - 1.0D
                    )
                            * SPAWN_RADIUS_Z;


            Location location =
                    new Location(
                            world,
                            Math.floor(
                                    x
                            ) + 0.5D,
                            SPAWN_FEET_Y,
                            Math.floor(
                                    z
                            ) + 0.5D
                    );


            if (
                    !isSafeSpawnLocation(
                            location,
                            result
                    )
            ) {

                continue;
            }


            result.add(
                    location
            );
        }


        return result;
    }


    private boolean isSafeSpawnLocation(
            Location location,
            List<Location> existing
    ) {

        if (
                location.getX()
                        < BOUNDARY_MIN_X + 1.0D
                || location.getX()
                        >= BOUNDARY_MAX_X - 1.0D
                || location.getZ()
                        < BOUNDARY_MIN_Z + 1.0D
                || location.getZ()
                        >= BOUNDARY_MAX_Z - 1.0D
        ) {

            return false;
        }


        Material floor =
                location.clone()
                        .add(
                                0.0D,
                                -1.0D,
                                0.0D
                        )
                        .getBlock()
                        .getType();


        Material feet =
                location.getBlock()
                        .getType();


        Material head =
                location.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        )
                        .getBlock()
                        .getType();


        if (
                !floor.isSolid()
                || !feet.isAir()
                || !head.isAir()
        ) {

            return false;
        }


        double minDistanceSquared =
                MIN_SPAWN_DISTANCE
                        * MIN_SPAWN_DISTANCE;


        for (
                Location other
                        : existing
        ) {

            if (
                    other.distanceSquared(
                            location
                    )
                            < minDistanceSquared
            ) {

                return false;
            }
        }


        return true;
    }


    /*
     * =========================================================
     * BOUNDARY
     * =========================================================
     */

    public boolean isInsideBoundary(
            Location location
    ) {

        if (
                dungeonWorld == null
                || location == null
                || !location.getWorld()
                        .equals(
                                dungeonWorld
                        )
        ) {

            return false;
        }


        double x =
                location.getX();

        double z =
                location.getZ();


        return x >= BOUNDARY_MIN_X
                && x < BOUNDARY_MAX_X
                && z >= BOUNDARY_MIN_Z
                && z < BOUNDARY_MAX_Z;
    }


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private Player findNearestActiveParticipant(
            Location origin
    ) {

        Player nearest =
                null;

        double best =
                Double.MAX_VALUE;


        for (
                UUID uuid
                        : activeParticipants
        ) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
                    || !player.getWorld()
                            .equals(
                                    origin.getWorld()
                            )
            ) {

                continue;
            }


            double distance =
                    player.getLocation()
                            .distanceSquared(
                                    origin
                            );


            if (
                    distance
                            < best
            ) {

                best =
                        distance;

                nearest =
                        player;
            }
        }


        return nearest;
    }


    private void broadcast(
            Component component
    ) {

        forEachParticipant(
                player ->
                        player.sendMessage(
                                component
                        )
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

        forEachParticipant(
                player ->
                        player.showTitle(
                                title
                        )
        );
    }


    private void clearTitles() {

        forEachParticipant(
                Player::clearTitle
        );
    }


    private void forEachParticipant(
            java.util.function.Consumer<Player> action
    ) {

        for (
                UUID uuid
                        : new HashSet<>(
                                participants
                        )
        ) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player != null
                    && player.isOnline()
            ) {

                action.accept(
                        player
                );
            }
        }
    }


    private boolean isExpectedWave(
            int wave
    ) {

        return switch (wave) {

            case 1 ->
                    state == AncientDepthsDungeonState.WAVE_1;

            case 2 ->
                    state == AncientDepthsDungeonState.WAVE_2;

            case 3 ->
                    state == AncientDepthsDungeonState.WAVE_3;

            default -> false;
        };
    }


    /*
     * =========================================================
     * RESET
     * =========================================================
     */

    private void scheduleReset(
            long delay
    ) {

        if (resetTask != null) {
            return;
        }

        resetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> resetDungeon(false),
                                delay
                        );
    }


    private void resetDungeon(
            boolean cleared
    ) {

        state =
                AncientDepthsDungeonState.RESETTING;

        for (UUID uuid : Set.copyOf(activeDungeonMobs)) {

            Entity entity =
                    plugin.getServer()
                            .getEntity(uuid);

            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        activeDungeonMobs.clear();
        attackDamageMap.clear();

        if (activeBoss != null && activeBoss.isValid()) {
            activeBoss.remove();
        }

        activeBoss = null;

        removeBossBar();
        clearTitles();

        returnParticipants(cleared);

        participants.clear();
        activeParticipants.clear();
        returnLocations.clear();

        dungeonWorld = null;
        resetTask = null;

        state =
                AncientDepthsDungeonState.IDLE;

        plugin.getLogger().info(
                "[AncientDepths] Dungeon reset complete. State = IDLE"
        );
    }


    private void removeBossBar() {

        if (bossBarTask != null) {

            bossBarTask.cancel();

            bossBarTask = null;
        }

        if (bossBar != null) {

            bossBar.removeAll();

            bossBar = null;
        }
    }


    private record MobSpec(
            EntityType type,
            String name,
            double health,
            double attack
    ) {
    }



    public AncientDepthsDungeonState getState() {
        return state;
    }


    public int getActiveMobCount() {
        return activeDungeonMobs.size();
    }


    /*
     * =========================================================
     * PARTICIPANT CHECK
     * =========================================================
     */
    public boolean isParticipant(
            Player player
    ) {

        return player != null
                && participants.contains(
                        player.getUniqueId()
                );
    }

}
