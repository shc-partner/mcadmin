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
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
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


/*
 * =============================================================
 * Lv20~30 UNDEAD FORTRESS
 * =============================================================
 */
public final class UndeadFortressDungeonService {

    public static final String DUNGEON_ID =
            "undead_fortress_20_30";

    public static final double BOUNDARY_MIN_X =
            5500953.0D;

    public static final double BOUNDARY_MAX_X =
            5500975.0D;

    public static final double BOUNDARY_MIN_Z =
            5501177.0D;

    public static final double BOUNDARY_MAX_Z =
            5501194.0D;


    /*
     * 언데드 성채 몬스터 스폰 기준점:
     *
     * 5500965 76 5501185
     */
    private static final double SPAWN_CENTER_X =
            5500965.0D;

    private static final double SPAWN_FEET_Y =
            76.0D;

    private static final double SPAWN_CENTER_Z =
            5501185.0D;


    /*
     * 벽과 너무 가까운 위치는 사용하지 않는다.
     */
    private static final double SPAWN_RADIUS_X =
            9.0D;

    private static final double SPAWN_RADIUS_Z =
            7.0D;

    private static final double MIN_SPAWN_DISTANCE =
            1.5D;

    private static final int MAX_SPAWN_ATTEMPTS =
            2000;


    private static final int NEXT_WAVE_SECONDS =
            3;

    private static final long CLEAR_EXP =
            1200L;


    /*
     * Boss.
     */
    private static final String BOSS_NAME =
            "언데드 기사";

    private static final double BOSS_EFFECTIVE_HEALTH =
            3000.0D;

    /*
     * Paper/LivingEntity 실제 HP는 1024로 두고
     * 피해량을 비례 보정한다.
     */
    private static final double BOSS_VANILLA_HEALTH =
            1024.0D;

    private static final double BOSS_ATTACK_DAMAGE =
            20.0D;


    /*
     * =========================================================
     * BOSS PATTERN
     * =========================================================
     */

    private static final double BOSS_ENRAGED_ATTACK_DAMAGE =
            26.0D;

    private static final double BOSS_ENRAGE_HEALTH_RATIO =
            0.40D;

    private static final double BOSS_SLASH_DAMAGE =
            14.0D;

    private static final double BOSS_ROAR_DAMAGE =
            10.0D;

    private static final double BOSS_SLASH_RANGE =
            4.5D;

    private static final double BOSS_ROAR_RANGE =
            6.0D;

    private static final long BOSS_PATTERN_NORMAL_TICKS =
            80L;

    private static final long BOSS_PATTERN_ENRAGED_TICKS =
            55L;


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


    private UndeadFortressDungeonState state =
            UndeadFortressDungeonState.IDLE;

    private World dungeonWorld;

    private DungeonRewardService dungeonRewardService;

    private LivingEntity activeBoss;

    private BossBar bossBar;

    private BukkitTask bossBarTask;

    private BukkitTask bossPatternTask;

    private BukkitTask resetTask;

    private int bossPatternIndex = 0;

    private long bossPatternElapsedTicks = 0L;

    private boolean bossEnraged = false;


    public UndeadFortressDungeonService(
            JavaPlugin plugin
    ) {

        this.plugin = plugin;

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

        return state != UndeadFortressDungeonState.IDLE;
    }


    /*
     * =========================================================
     * PLAYER ENTER
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
                state == UndeadFortressDungeonState.CLEARED
                || state == UndeadFortressDungeonState.RESETTING
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


        participants.add(
                uuid
        );

        activeParticipants.add(
                uuid
        );


        if (returnLocation != null) {

            returnLocations.putIfAbsent(
                    uuid,
                    returnLocation.clone()
            );

            pendingReturnLocations.remove(
                    uuid
            );
        }


        /*
         * 이미 던전이 진행 중이라면 웨이브를 다시 만들지 않는다.
         */
        if (state != UndeadFortressDungeonState.IDLE) {

            if (bossBar != null) {
                bossBar.addPlayer(
                        player
                );
            }

            player.sendMessage(
                    Component.text(
                            "[던전] ",
                            NamedTextColor.DARK_PURPLE
                    ).append(
                            Component.text(
                                    "현재 진행 중인 언데드 성채에 참가했습니다.",
                                    NamedTextColor.GRAY
                            )
                    )
            );

            return;
        }


        dungeonWorld =
                player.getWorld();

        state =
                UndeadFortressDungeonState.WAVE_1;


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
                                1
                        ),
                        40L
                );
    }


    public void onParticipantDeath(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        if (!participants.contains(uuid)) {
            return;
        }

        if (
                state == UndeadFortressDungeonState.IDLE
                || state == UndeadFortressDungeonState.CLEARED
                || state == UndeadFortressDungeonState.RESETTING
        ) {
            return;
        }

        activeParticipants.remove(
                uuid
        );

        Location returnLocation =
                returnLocations.get(
                        uuid
                );

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

        UUID uuid =
                player.getUniqueId();

        if (!participants.contains(uuid)) {
            return;
        }

        if (
                state == UndeadFortressDungeonState.IDLE
                || state == UndeadFortressDungeonState.RESETTING
        ) {
            return;
        }

        activeParticipants.remove(
                uuid
        );

        Location returnLocation =
                returnLocations.get(
                        uuid
                );

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
                pendingReturnLocations.remove(
                        uuid
                );

        if (location == null) {
            return null;
        }

        return location.clone();
    }


    /*
     * 좀비 던전 호환 이름.
     */
    public Location consumePendingExitLocation(
            UUID uuid
    ) {
        return consumePendingReturnLocation(
                uuid
        );
    }


    public void restorePendingExitOnJoin(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        Location location =
                consumePendingReturnLocation(
                        uuid
                );

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
                state == UndeadFortressDungeonState.IDLE
                || state == UndeadFortressDungeonState.CLEARED
                || state == UndeadFortressDungeonState.RESETTING
                || !activeParticipants.isEmpty()
        ) {
            return;
        }

        failDungeon();
    }


    private void failDungeon() {

        if (
                state == UndeadFortressDungeonState.IDLE
                || state == UndeadFortressDungeonState.CLEARED
                || state == UndeadFortressDungeonState.RESETTING
        ) {
            return;
        }

        state =
                UndeadFortressDungeonState.RESETTING;

        stopBossPatterns();
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
     * WAVES
     * =========================================================
     */
    private void startWave(
            int wave
    ) {

        if (!isExpectedWave(wave)) {
            return;
        }

        if (dungeonWorld == null) {
            return;
        }

        activeDungeonMobs.clear();
        attackDamageMap.clear();

        List<MobSpec> specs =
                getWaveSpecs(wave);

        List<Location> locations =
                findSafeSpawnLocations(
                        dungeonWorld,
                        specs.size()
                );

        if (locations.size()
                < specs.size()) {

            plugin.getLogger().severe(
                    "[UndeadFortress] "
                            + "safe spawn locations "
                            + locations.size()
                            + " / "
                            + specs.size()
            );

            broadcast(
                    Component.text(
                            "던전 몬스터 생성 위치를 찾지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            scheduleReset(40L);
            return;
        }


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
                    dungeonWorld,
                    locations.get(i),
                    wave,
                    specs.get(i)
            );
        }
    }


    private List<MobSpec> getWaveSpecs(
            int wave
    ) {

        List<MobSpec> specs =
                new ArrayList<>();

        switch (wave) {

            case 1 -> {

                add(
                        specs,
                        EntityType.ZOMBIE,
                        4,
                        350.0D,
                        10.0D,
                        "성채 좀비"
                );

                add(
                        specs,
                        EntityType.HUSK,
                        2,
                        425.0D,
                        12.0D,
                        "성채 허스크"
                );
            }

            case 2 -> {

                add(
                        specs,
                        EntityType.ZOMBIE,
                        3,
                        400.0D,
                        11.0D,
                        "성채 좀비"
                );

                add(
                        specs,
                        EntityType.SKELETON,
                        3,
                        350.0D,
                        13.0D,
                        "성채 스켈레톤"
                );

                add(
                        specs,
                        EntityType.STRAY,
                        2,
                        425.0D,
                        14.0D,
                        "성채 스트레이"
                );
            }

            case 3 -> {

                add(
                        specs,
                        EntityType.HUSK,
                        3,
                        500.0D,
                        14.0D,
                        "성채 허스크"
                );

                add(
                        specs,
                        EntityType.SKELETON,
                        3,
                        425.0D,
                        15.0D,
                        "성채 스켈레톤"
                );

                add(
                        specs,
                        EntityType.STRAY,
                        3,
                        525.0D,
                        17.0D,
                        "성채 스트레이"
                );
            }

            default -> {
            }
        }

        return specs;
    }


    private void add(
            List<MobSpec> list,
            EntityType type,
            int count,
            double health,
            double attack,
            String name
    ) {

        for (int i = 0; i < count; i++) {

            list.add(
                    new MobSpec(
                            type,
                            health,
                            attack,
                            name
                    )
            );
        }
    }


    private void spawnWaveMob(
            World world,
            Location location,
            int wave,
            MobSpec spec
    ) {

        Entity raw =
                world.spawnEntity(
                        location,
                        spec.type()
                );

        if (!(raw instanceof Mob mob)) {

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

        mob.customName(
                Component.text(
                        spec.name(),
                        NamedTextColor.RED
                )
        );

        mob.setCustomNameVisible(false);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);

        activeDungeonMobs.add(
                mob.getUniqueId()
        );

        attackDamageMap.put(
                mob.getUniqueId(),
                spec.attack()
        );
    }


    /*
     * =========================================================
     * SAFE RANDOM SPAWN
     * =========================================================
     */
    private List<Location> findSafeSpawnLocations(
            World world,
            int required
    ) {

        List<Location> result =
                new ArrayList<>();

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        int attempts = 0;

        while (
                result.size() < required
                && attempts < MAX_SPAWN_ATTEMPTS
        ) {

            attempts++;

            double x =
                    SPAWN_CENTER_X
                            + random.nextDouble(
                                    -SPAWN_RADIUS_X,
                                    SPAWN_RADIUS_X
                            );

            double z =
                    SPAWN_CENTER_Z
                            + random.nextDouble(
                                    -SPAWN_RADIUS_Z,
                                    SPAWN_RADIUS_Z
                            );

            /*
             * 던전 경계에서 최소 1블록 안쪽.
             */
            if (
                    x < BOUNDARY_MIN_X + 1.0D
                    || x >= BOUNDARY_MAX_X - 1.0D
                    || z < BOUNDARY_MIN_Z + 1.0D
                    || z >= BOUNDARY_MAX_Z - 1.0D
            ) {
                continue;
            }


            int blockX =
                    (int) Math.floor(x);

            int blockZ =
                    (int) Math.floor(z);

            int feetY =
                    (int) SPAWN_FEET_Y;

            Block floor =
                    world.getBlockAt(
                            blockX,
                            feetY - 1,
                            blockZ
                    );

            Block feet =
                    world.getBlockAt(
                            blockX,
                            feetY,
                            blockZ
                    );

            Block head =
                    world.getBlockAt(
                            blockX,
                            feetY + 1,
                            blockZ
                    );


            if (!floor.getType().isSolid()) {
                continue;
            }

            if (!feet.isPassable()) {
                continue;
            }

            if (!head.isPassable()) {
                continue;
            }


            Location candidate =
                    new Location(
                            world,
                            blockX + 0.5D,
                            SPAWN_FEET_Y,
                            blockZ + 0.5D
                    );


            if (!isFarEnough(
                    candidate,
                    result
            )) {
                continue;
            }

            result.add(candidate);
        }

        return result;
    }


    private boolean isFarEnough(
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

            if (
                    dx * dx
                            + dz * dz
                            < minimumSquared
            ) {
                return false;
            }
        }

        return true;
    }


    /*
     * =========================================================
     * MOB DEATH
     * =========================================================
     */
    public void onDungeonMobDeath(
            LivingEntity entity
    ) {

        UUID uuid =
                entity.getUniqueId();

        if (!activeDungeonMobs.remove(uuid)) {
            return;
        }

        attackDamageMap.remove(uuid);

        if (!activeDungeonMobs.isEmpty()) {
            return;
        }

        switch (state) {

            case WAVE_1 -> {

                state =
                        UndeadFortressDungeonState.WAVE_2;

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
                        UndeadFortressDungeonState.WAVE_3;

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
                        UndeadFortressDungeonState.BOSS;

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
                        UndeadFortressDungeonState.CLEARED;

                rewardClear();

                stopBossPatterns();
                removeBossBar();
                clearTitles();

                broadcast(
                        Component.text(
                                "언데드 기사를 처치했습니다.",
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


    /*
     * =========================================================
     * CLEAR EXIT COUNTDOWN
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

        if (state != UndeadFortressDungeonState.CLEARED) {
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


    private void scheduleNextWave(
            int wave
    ) {

        runWaveCountdown(
                wave,
                NEXT_WAVE_SECONDS
        );
    }


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
     * BOSS
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

        if (state != UndeadFortressDungeonState.BOSS) {
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

                            if (state != UndeadFortressDungeonState.BOSS) {
                                return;
                            }

                            showBossSubtitle(
                                    Component.text(
                                            "강력한 언데드의 기운이 느껴집니다...",
                                            NamedTextColor.DARK_RED
                                    ),
                                    3000L
                            );

                            plugin.getServer()
                                    .getScheduler()
                                    .runTaskLater(
                                            plugin,
                                            () -> {

                                                if (state != UndeadFortressDungeonState.BOSS) {
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

                                                                    if (state != UndeadFortressDungeonState.BOSS) {
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


    private void spawnBoss() {

        if (
                state != UndeadFortressDungeonState.BOSS
                || dungeonWorld == null
        ) {
            return;
        }


        Location location =
                new Location(
                        dungeonWorld,
                        5500965.5D,
                        76.0D,
                        5501185.5D
                );


        Entity raw =
                dungeonWorld.spawnEntity(
                        location,
                        EntityType.WITHER_SKELETON
                );

        if (!(raw instanceof Mob boss)) {

            raw.remove();
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
                BOSS_VANILLA_HEALTH
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

        boss.setCustomNameVisible(true);
        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);


        activeBoss =
                boss;

        activeDungeonMobs.clear();

        activeDungeonMobs.add(
                boss.getUniqueId()
        );

        attackDamageMap.clear();

        attackDamageMap.put(
                boss.getUniqueId(),
                BOSS_ATTACK_DAMAGE
        );


        createBossBar();

        startBossPatterns();

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
     * BOSS PATTERNS
     * =========================================================
     */

    private void startBossPatterns() {

        stopBossPatterns();

        bossPatternIndex = 0;
        bossPatternElapsedTicks = 0L;
        bossEnraged = false;

        bossPatternTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> {

                                    if (!isActiveBoss()) {
                                        return;
                                    }

                                    checkBossEnrage();

                                    bossPatternElapsedTicks +=
                                            10L;

                                    long interval =
                                            bossEnraged
                                                    ? BOSS_PATTERN_ENRAGED_TICKS
                                                    : BOSS_PATTERN_NORMAL_TICKS;

                                    if (
                                            bossPatternElapsedTicks
                                                    < interval
                                    ) {
                                        return;
                                    }

                                    bossPatternElapsedTicks =
                                            0L;

                                    runNextBossPattern();
                                },
                                20L,
                                10L
                        );
    }


    private void stopBossPatterns() {

        if (bossPatternTask != null) {

            bossPatternTask.cancel();

            bossPatternTask = null;
        }

        bossPatternElapsedTicks =
                0L;

        bossPatternIndex =
                0;

        bossEnraged =
                false;
    }


    private boolean isActiveBoss() {

        return state == UndeadFortressDungeonState.BOSS
                && activeBoss != null
                && activeBoss.isValid()
                && !activeBoss.isDead();
    }


    private void checkBossEnrage() {

        if (
                bossEnraged
                || !isActiveBoss()
        ) {
            return;
        }

        double healthRatio =
                activeBoss.getHealth()
                        / BOSS_VANILLA_HEALTH;

        if (
                healthRatio
                        > BOSS_ENRAGE_HEALTH_RATIO
        ) {
            return;
        }

        bossEnraged =
                true;

        setAttackAttribute(
                activeBoss,
                BOSS_ENRAGED_ATTACK_DAMAGE
        );

        attackDamageMap.put(
                activeBoss.getUniqueId(),
                BOSS_ENRAGED_ATTACK_DAMAGE
        );

        activeBoss
                .getPersistentDataContainer()
                .set(
                        attackDamageKey,
                        PersistentDataType.DOUBLE,
                        BOSS_ENRAGED_ATTACK_DAMAGE
                );

        org.bukkit.attribute.AttributeInstance
                movementSpeed =
                        activeBoss.getAttribute(
                                org.bukkit.attribute.Attribute.MOVEMENT_SPEED
                        );

        if (movementSpeed != null) {

            movementSpeed.setBaseValue(
                    Math.max(
                            movementSpeed.getBaseValue(),
                            0.34D
                    )
            );
        }

        dungeonWorld.playSound(
                activeBoss.getLocation(),
                org.bukkit.Sound.ENTITY_WITHER_SPAWN,
                1.0F,
                1.35F
        );

        dungeonWorld.spawnParticle(
                org.bukkit.Particle.SOUL,
                activeBoss.getLocation().add(
                        0.0D,
                        1.0D,
                        0.0D
                ),
                50,
                1.0D,
                1.0D,
                1.0D,
                0.05D
        );

        broadcast(
                Component.text(
                        "언데드 기사가 광폭화했습니다!",
                        NamedTextColor.RED
                )
        );
    }


    private void runNextBossPattern() {

        if (!isActiveBoss()) {
            return;
        }

        switch (bossPatternIndex) {

            case 0 ->
                    bossFrontalSlash();

            case 1 ->
                    bossCharge();

            default ->
                    bossUndeadRoar();
        }

        bossPatternIndex =
                (bossPatternIndex + 1) % 3;
    }


    /*
     * ---------------------------------------------------------
     * PATTERN 1
     * 전방 베기
     * ---------------------------------------------------------
     */

    private void bossFrontalSlash() {

        if (!isActiveBoss()) {
            return;
        }

        org.bukkit.Location bossLocation =
                activeBoss.getLocation();

        org.bukkit.util.Vector forward =
                bossLocation
                        .getDirection()
                        .setY(0.0D);

        if (
                forward.lengthSquared()
                        < 0.0001D
        ) {
            return;
        }

        forward.normalize();

        dungeonWorld.playSound(
                bossLocation,
                org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.4F,
                0.7F
        );

        dungeonWorld.spawnParticle(
                org.bukkit.Particle.SWEEP_ATTACK,
                bossLocation.clone()
                        .add(
                                forward.clone()
                                        .multiply(2.0D)
                        )
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                8,
                1.2D,
                0.5D,
                1.2D,
                0.0D
        );

        forEachActiveParticipant(
                player -> {

                    org.bukkit.util.Vector offset =
                            player.getLocation()
                                    .toVector()
                                    .subtract(
                                            bossLocation
                                                    .toVector()
                                    );

                    offset.setY(
                            0.0D
                    );

                    double distance =
                            offset.length();

                    if (
                            distance <= 0.01D
                            || distance > BOSS_SLASH_RANGE
                    ) {
                        return;
                    }

                    org.bukkit.util.Vector direction =
                            offset.clone()
                                    .normalize();

                    /*
                     * 정면 약 120도.
                     */
                    if (
                            forward.dot(direction)
                                    < 0.50D
                    ) {
                        return;
                    }

                    player.damage(
                            BOSS_SLASH_DAMAGE
                    );

                    direction
                            .multiply(0.8D)
                            .setY(0.25D);

                    player.setVelocity(
                            direction
                    );
                }
        );
    }


    /*
     * ---------------------------------------------------------
     * PATTERN 2
     * 돌진
     * ---------------------------------------------------------
     */

    private void bossCharge() {

        if (!isActiveBoss()) {
            return;
        }

        Player target =
                findNearestActiveParticipant();

        if (target == null) {
            return;
        }

        org.bukkit.util.Vector direction =
                target.getLocation()
                        .toVector()
                        .subtract(
                                activeBoss
                                        .getLocation()
                                        .toVector()
                        );

        direction.setY(
                0.0D
        );

        if (
                direction.lengthSquared()
                        < 0.0001D
        ) {
            return;
        }

        direction
                .normalize()
                .multiply(
                        bossEnraged
                                ? 1.65D
                                : 1.40D
                )
                .setY(0.18D);

        dungeonWorld.playSound(
                activeBoss.getLocation(),
                org.bukkit.Sound.ENTITY_RAVAGER_ROAR,
                0.8F,
                1.4F
        );

        dungeonWorld.spawnParticle(
                org.bukkit.Particle.LARGE_SMOKE,
                activeBoss.getLocation().add(
                        0.0D,
                        0.5D,
                        0.0D
                ),
                20,
                0.6D,
                0.3D,
                0.6D,
                0.02D
        );

        activeBoss.setVelocity(
                direction
        );
    }


    /*
     * ---------------------------------------------------------
     * PATTERN 3
     * 언데드 포효
     * ---------------------------------------------------------
     */

    private void bossUndeadRoar() {

        if (!isActiveBoss()) {
            return;
        }

        org.bukkit.Location bossLocation =
                activeBoss.getLocation();

        dungeonWorld.playSound(
                bossLocation,
                org.bukkit.Sound.ENTITY_WITHER_AMBIENT,
                1.3F,
                0.65F
        );

        dungeonWorld.spawnParticle(
                org.bukkit.Particle.SOUL,
                bossLocation.clone().add(
                        0.0D,
                        1.0D,
                        0.0D
                ),
                70,
                2.0D,
                1.0D,
                2.0D,
                0.08D
        );

        forEachActiveParticipant(
                player -> {

                    if (
                            player.getWorld()
                                    != bossLocation.getWorld()
                    ) {
                        return;
                    }

                    double distance =
                            player.getLocation()
                                    .distance(
                                            bossLocation
                                    );

                    if (
                            distance
                                    > BOSS_ROAR_RANGE
                    ) {
                        return;
                    }

                    player.damage(
                            BOSS_ROAR_DAMAGE
                    );

                    org.bukkit.util.Vector knockback =
                            player.getLocation()
                                    .toVector()
                                    .subtract(
                                            bossLocation
                                                    .toVector()
                                    );

                    knockback.setY(
                            0.0D
                    );

                    if (
                            knockback.lengthSquared()
                                    > 0.0001D
                    ) {

                        knockback
                                .normalize()
                                .multiply(1.1D)
                                .setY(0.35D);

                        player.setVelocity(
                                knockback
                        );
                    }
                }
        );
    }


    private Player findNearestActiveParticipant() {

        if (!isActiveBoss()) {
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
                            != activeBoss.getWorld()
            ) {
                continue;
            }

            double distanceSquared =
                    player.getLocation()
                            .distanceSquared(
                                    activeBoss
                                            .getLocation()
                            );

            if (
                    distanceSquared
                            < nearestDistanceSquared
            ) {

                nearest =
                        player;

                nearestDistanceSquared =
                        distanceSquared;
            }
        }

        return nearest;
    }


    private void forEachActiveParticipant(
            java.util.function.Consumer<Player> action
    ) {

        for (UUID uuid
                : new HashSet<>(
                        activeParticipants
                )) {

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

            action.accept(
                    player
            );
        }
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
     * PDC / STATS
     * =========================================================
     */
    private void applyDungeonTags(
            LivingEntity entity,
            int wave,
            boolean boss,
            double effectiveHealth,
            double attackDamage
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

        if (boss) {

            pdc.set(
                    dungeonBossKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }

        pdc.set(
                effectiveHealthKey,
                PersistentDataType.DOUBLE,
                effectiveHealth
        );

        pdc.set(
                attackDamageKey,
                PersistentDataType.DOUBLE,
                attackDamage
        );
    }


    private void setHealth(
            LivingEntity entity,
            double health
    ) {

        AttributeInstance attribute =
                entity.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (attribute == null) {
            return;
        }

        attribute.setBaseValue(
                health
        );

        entity.setHealth(
                health
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

        if (attribute == null) {
            return;
        }

        attribute.setBaseValue(
                attack
        );
    }


    public boolean isDungeonMob(
            LivingEntity entity
    ) {

        if (entity == null) {
            return false;
        }

        String id =
                entity.getPersistentDataContainer()
                        .get(
                                dungeonIdKey,
                                PersistentDataType.STRING
                        );

        return DUNGEON_ID.equals(id);
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

        return value == null
                ? 0.0D
                : value;
    }


    public double getAttackDamage(
            LivingEntity entity
    ) {

        Double mapped =
                attackDamageMap.get(
                        entity.getUniqueId()
                );

        if (mapped != null) {
            return mapped;
        }

        Double pdc =
                entity.getPersistentDataContainer()
                        .get(
                                attackDamageKey,
                                PersistentDataType.DOUBLE
                        );

        return pdc == null
                ? 0.0D
                : pdc;
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
                location == null
                || dungeonWorld == null
                || !dungeonWorld.equals(
                        location.getWorld()
                )
        ) {
            return false;
        }

        double x = location.getX();
        double z = location.getZ();

        return x >= BOUNDARY_MIN_X
                && x < BOUNDARY_MAX_X
                && z >= BOUNDARY_MIN_Z
                && z < BOUNDARY_MAX_Z;
    }


    /*
     * =========================================================
     * REWARD
     * =========================================================
     */
    /*
     * =========================================================
     * RAID RETURN
     * =========================================================
     */
    private void markAndReturnParticipants() {
        returnParticipants(false);
    }


    private void returnParticipants(
            boolean cleared
    ) {

        for (UUID uuid : Set.copyOf(participants)) {

            Location returnLocation =
                    returnLocations.get(
                            uuid
                    );

            if (returnLocation == null) {
                continue;
            }

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );

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

            pendingReturnLocations.remove(
                    uuid
            );
        }
    }


    private void rewardClear() {

        if (dungeonRewardService == null) {

            plugin.getLogger().severe(
                    "[UndeadFortress] "
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
                .rewardUndeadFortressBasicRewards(
                        clearParticipants
                );


        /*
         * 던전 클리어 주괴 보상
         * 영웅 / 전설 / 신화 독립 추첨
         */
        dungeonRewardService
                .rewardEquipmentIngots(
                clearParticipants,
                0.05D,
                0.01D,
                0.00D
                );

        dungeonRewardService
                .rewardTeyvatRareWeapons(
                        clearParticipants
                );

        dungeonRewardService
                .rewardDungeonGold(
                        clearParticipants,
                        200L,
                        100L,
                        0.20D
                );

        dungeonRewardService
                .rewardEnhancementStones(
                        clearParticipants,
                        1,
                        0.0D,
                        0,
                        0
                );
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
                UndeadFortressDungeonState.RESETTING;

        for (UUID uuid : Set.copyOf(activeDungeonMobs)) {

            Entity entity =
                    plugin.getServer()
                            .getEntity(
                                    uuid
                            );

            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        activeDungeonMobs.clear();
        attackDamageMap.clear();

        stopBossPatterns();

        if (activeBoss != null && activeBoss.isValid()) {
            activeBoss.remove();
        }

        activeBoss = null;

        removeBossBar();
        clearTitles();

        returnParticipants(
                cleared
        );

        participants.clear();
        activeParticipants.clear();
        returnLocations.clear();

        dungeonWorld = null;
        resetTask = null;

        state =
                UndeadFortressDungeonState.IDLE;

        plugin.getLogger().info(
                "[UndeadFortress] Dungeon reset complete. State = IDLE"
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


    private boolean isExpectedWave(
            int wave
    ) {

        return switch (wave) {

            case 1 ->
                    state == UndeadFortressDungeonState.WAVE_1;

            case 2 ->
                    state == UndeadFortressDungeonState.WAVE_2;

            case 3 ->
                    state == UndeadFortressDungeonState.WAVE_3;

            default -> false;
        };
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


    private void clearTitles() {

        forEachParticipant(
                Player::clearTitle
        );
    }


    private void forEachParticipant(
            java.util.function.Consumer<Player> action
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

            action.accept(player);
        }
    }


    private record MobSpec(
            EntityType type,
            double health,
            double attack,
            String name
    ) {
    }



    public UndeadFortressDungeonState getState() {
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
