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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;


/*
 * =============================================================
 * Lv40~50 NETHER FORTRESS
 * =============================================================
 */
public final class NetherFortressDungeonService {

    public static final String DUNGEON_ID =
            "nether_fortress_30_40";


    /*
     * =========================================================
     * DUNGEON BOUNDARY
     * =========================================================
     *
     * 새 전투 영역 중심: 7999565 229 80002041
     * 몬스터 이동 경계는 중심 주변 19x19 영역
     *
     * 반열린 경계:
     *
     * X >= 7999556 && X < 7999575
     * Z >= 80002032 && Z < 80002051
     */
    public static final double BOUNDARY_MIN_X =
            7999556.0D;

    public static final double BOUNDARY_MAX_X =
            7999575.0D;

    public static final double BOUNDARY_MIN_Z =
            80002032.0D;

    public static final double BOUNDARY_MAX_Z =
            80002051.0D;


    /*
     * =========================================================
     * RANDOM WAVE SPAWN
     * =========================================================
     *
     * 중심:
     *
     * 7999565 229 80002041
     *
     * Y=229 = 엔티티 발 위치
     * Y=228 = 바닥 블록
     */
    private static final double SPAWN_CENTER_X =
            7999565.0D;

    private static final double SPAWN_FEET_Y =
            229.0D;

    private static final double SPAWN_CENTER_Z =
            80002041.0D;


    private static final double SPAWN_RADIUS_X =
            8.0D;

    private static final double SPAWN_RADIUS_Z =
            8.0D;

    private static final double SPAWN_RADIUS_SQUARED =
            64.0D;


    private static final double MIN_SPAWN_DISTANCE =
            1.5D;

    private static final int MAX_SPAWN_ATTEMPTS =
            2500;


    /*
     * =========================================================
     * BOSS SPAWN
     * =========================================================
     *
     * 사용자 지정:
     *
     * 7999565 229 80002041
     */
    private static final double BOSS_X =
            7999565.0D;

    private static final double BOSS_Y =
            229.0D;

    private static final double BOSS_Z =
            80002041.0D;


    /*
     * =========================================================
     * RAID RETURN
     * =========================================================
     *
     * 클리어 / 실패:
     *
     * 1257 69 -1661
     */
    private static final double RETURN_X =
            1257.0D;

    private static final double RETURN_Y =
            69.0D;

    private static final double RETURN_Z =
            -1661.0D;


    /*
     * =========================================================
     * FLOW
     * =========================================================
     */
    private static final int NEXT_WAVE_SECONDS =
            3;

    private static final long CLEAR_EXP =
            2500L;


    /*
     * =========================================================
     * BOSS
     * =========================================================
     */
    private static final String BOSS_NAME =
            "타락한 피글린 군주";

    private static final double BOSS_EFFECTIVE_HEALTH =
            6500.0D;

    /*
     * Paper 실제 HP는 1024로 제한하고
     * Effective HP 4500은 Damage Scaling으로 구현.
     */
    private static final double BOSS_VANILLA_HEALTH =
            1024.0D;

    private static final double BOSS_ATTACK_DAMAGE =
            30.0D;


    /*
     * =========================================================
     * BOSS PATTERN
     * =========================================================
     */

    private static final double BOSS_ENRAGE_HEALTH_RATIO =
            0.35D;

    private static final double BOSS_ENRAGED_ATTACK_DAMAGE =
            32.0D;

    private static final double BOSS_ENRAGED_MOVEMENT_SPEED =
            0.38D;


    private static final double BOSS_AXE_SLAM_DAMAGE =
            18.0D;

    private static final double BOSS_AXE_SLAM_RANGE =
            4.5D;


    private static final double BOSS_JUMP_SLAM_DAMAGE =
            22.0D;

    private static final double BOSS_JUMP_SLAM_RANGE =
            5.0D;


    private static final double BOSS_FIRE_AOE_DAMAGE =
            16.0D;

    private static final double BOSS_FIRE_AOE_RANGE =
            6.0D;

    private static final int BOSS_FIRE_TICKS =
            80;


    private static final double BOSS_SUMMON_HEALTH =
            600.0D;

    private static final double BOSS_SUMMON_ATTACK =
            18.0D;

    private static final int BOSS_SUMMON_COUNT =
            2;

    private static final int BOSS_SUMMON_MAX_ALIVE =
            4;


    private static final long BOSS_PATTERN_NORMAL_TICKS =
            80L;

    private static final long BOSS_PATTERN_ENRAGED_TICKS =
            55L;

    private static final long BOSS_PATTERN_CHECK_TICKS =
            5L;


    /*
     * =========================================================
     * SERVICES / KEYS
     * =========================================================
     */
    private final JavaPlugin plugin;

    private final NamespacedKey dungeonIdKey;

    private final NamespacedKey dungeonWaveKey;

    private final NamespacedKey dungeonBossKey;

    private final NamespacedKey effectiveHealthKey;

    private final NamespacedKey attackDamageKey;


    /*
     * =========================================================
     * RUNTIME
     * =========================================================
     */
    private final Set<UUID> participants =
            new HashSet<>();

    private final Set<UUID> activeParticipants =
            new HashSet<>();

    private final Set<UUID> activeDungeonMobs =
            new HashSet<>();

    private final Map<UUID, Double> attackDamageMap =
            new HashMap<>();


    /*
     * 죽거나 로그아웃한 플레이어는 즉시 teleport할 수 없으므로
     * respawn / join 시 사용할 귀환 위치 저장.
     */
    private final Map<UUID, Location> pendingReturnLocations =
            new HashMap<>();


    private DungeonState state =
            DungeonState.IDLE;

    private World dungeonWorld;

    private DungeonRewardService dungeonRewardService;

    private LivingEntity activeBoss;

    private BossBar bossBar;

    private BukkitTask bossBarTask;

    private BukkitTask bossPatternTask;

    private BukkitTask resetTask;


    private final Set<UUID> bossSummons =
            new HashSet<>();

    private int bossPatternIndex =
            0;

    private long bossPatternElapsedTicks =
            0L;

    private boolean bossEnraged =
            false;


    public NetherFortressDungeonService(
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

        return state
                != DungeonState.IDLE;
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
         * 진행 중인 던전에는 참가자로만 합류하며
         * 새 웨이브를 다시 생성하지 않는다.
         */
        if (state != DungeonState.IDLE) {

            if (
                    dungeonWorld != null
                    && dungeonWorld.equals(
                            player.getWorld()
                    )
            ) {

                participants.add(uuid);
                activeParticipants.add(uuid);

                if (bossBar != null) {
                    bossBar.addPlayer(player);
                }

                player.sendMessage(
                        Component.text(
                                "[던전] ",
                                NamedTextColor.DARK_PURPLE
                        ).append(
                                Component.text(
                                        "현재 진행 중인 네더 요새에 참가했습니다.",
                                        NamedTextColor.GRAY
                                )
                        )
                );
            }

            return;
        }

        dungeonWorld =
                player.getWorld();

        participants.clear();
        activeParticipants.clear();

        participants.add(uuid);
        activeParticipants.add(uuid);

        state =
                DungeonState.WAVE_1;

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
                                "네더 요새에 입장합니다.",
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
                                1
                        ),
                        40L
                );
    }


    /*
     * =========================================================
     * PARTICIPANT DEATH / QUIT
     * =========================================================
     */
    public void onParticipantDeath(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (
                !participants.contains(
                        uuid
                )
        ) {
            return;
        }


        activeParticipants.remove(
                uuid
        );


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


        if (
                !participants.contains(
                        uuid
                )
        ) {
            return;
        }


        activeParticipants.remove(
                uuid
        );


        checkFailure();
    }


    /*
     * 사망 Respawn 또는 Offline Join 시
     * 레이드 귀환 위치 소비.
     */
    public Location consumePendingReturnLocation(
            UUID uuid
    ) {

        Location location =
                pendingReturnLocations.remove(
                        uuid
                );


        if (
                location == null
        ) {

            return null;
        }


        return location.clone();
    }


    /*
     * =========================================================
     * FAILURE
     * =========================================================
     */
    private void checkFailure() {

        if (
                state == DungeonState.IDLE
                || !activeParticipants.isEmpty()
        ) {
            return;
        }

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

        clearTitles();

        showBossSubtitle(
                Component.text(
                        "잠시 후 던전에서 퇴장합니다.",
                        NamedTextColor.RED
                ),
                5000L
        );

        /*
         * 기존 던전 복귀 로직은 유지한다.
         * Subtitle은 복귀 후에도 5초 stay 설정으로 유지된다.
         */
        markAndReturnParticipants();

        scheduleReset(
                100L
        );
    }


    /*
     * =========================================================
     * WAVE
     * =========================================================
     */
    private void startWave(
            int wave
    ) {

        if (
                !isExpectedWave(
                        wave
                )
        ) {

            return;
        }


        if (
                dungeonWorld == null
        ) {

            return;
        }


        activeDungeonMobs.clear();

        attackDamageMap.clear();


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

            plugin.getLogger()
                    .severe(
                            "[NetherFortress] "
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


            markAndReturnParticipants();


            scheduleReset(
                    20L
            );


            return;
        }


        broadcast(
                Component.text(
                        "Wave "
                                + wave,
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


    /*
     * =========================================================
     * WAVE CONFIG
     * =========================================================
     */
    private List<MobSpec> getWaveSpecs(
            int wave
    ) {

        List<MobSpec> specs =
                new ArrayList<>();


        switch (
                wave
        ) {

            /*
             * Wave 1
             *
             * Piglin x4
             * HP 500 / ATK 15
             *
             * Zombified Piglin x3
             * HP 550 / ATK 16
             */
            case 1 -> {

                add(
                        specs,
                        EntityType.PIGLIN,
                        4,
                        675.0D,
                        20.0D,
                        "요새 피글린"
                );


                add(
                        specs,
                        EntityType.ZOMBIFIED_PIGLIN,
                        3,
                        750.0D,
                        21.0D,
                        "요새 좀비화 피글린"
                );
            }


            /*
             * Wave 2
             */
            case 2 -> {

                add(
                        specs,
                        EntityType.PIGLIN,
                        3,
                        825.0D,
                        22.0D,
                        "요새 피글린"
                );


                add(
                        specs,
                        EntityType.BLAZE,
                        3,
                        750.0D,
                        25.0D,
                        "요새 블레이즈"
                );


                add(
                        specs,
                        EntityType.MAGMA_CUBE,
                        3,
                        925.0D,
                        23.0D,
                        "요새 마그마 큐브"
                );
            }


            /*
             * Wave 3
             */
            case 3 -> {

                add(
                        specs,
                        EntityType.PIGLIN_BRUTE,
                        2,
                        1100.0D,
                        30.0D,
                        "요새 피글린 브루트"
                );


                add(
                        specs,
                        EntityType.BLAZE,
                        3,
                        825.0D,
                        26.0D,
                        "요새 블레이즈"
                );


                add(
                        specs,
                        EntityType.MAGMA_CUBE,
                        3,
                        950.0D,
                        25.0D,
                        "요새 마그마 큐브"
                );


                add(
                        specs,
                        EntityType.PIGLIN,
                        2,
                        825.0D,
                        23.0D,
                        "요새 피글린"
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

        for (
                int i = 0;
                i < count;
                i++
        ) {

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


    /*
     * =========================================================
     * SPAWN WAVE MOB
     * =========================================================
     */
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


        if (
                !(raw instanceof Mob mob)
        ) {

            raw.remove();

            return;
        }


        configureNetherMob(
                mob
        );


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


        activeDungeonMobs.add(
                mob.getUniqueId()
        );


        attackDamageMap.put(
                mob.getUniqueId(),
                spec.attack()
        );
    }


    /*
     * Piglin / Piglin Brute가
     * 오버월드에서 좀비화되지 않도록 처리.
     */
    private void configureNetherMob(
            Mob mob
    ) {

        if (
                mob
                        instanceof PiglinAbstract piglin
        ) {

            piglin.setImmuneToZombification(
                    true
            );
        }
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


        int attempts =
                0;


        while (
                result.size()
                        < required
                && attempts
                        < MAX_SPAWN_ATTEMPTS
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
             * 기준점으로부터 수평 거리 8블럭 이내만 허용.
             * X/Z 사각 범위의 모서리는 제외한다.
             */
            double centerDx =
                    x - SPAWN_CENTER_X;

            double centerDz =
                    z - SPAWN_CENTER_Z;

            if (
                    centerDx * centerDx
                            + centerDz * centerDz
                            > SPAWN_RADIUS_SQUARED
            ) {

                continue;
            }


            /*
             * 외벽에서 최소 1블록 안쪽만 허용.
             */
            if (
                    x
                            < BOUNDARY_MIN_X
                            + 1.0D
                    || x
                            >= BOUNDARY_MAX_X
                            - 1.0D
                    || z
                            < BOUNDARY_MIN_Z
                            + 1.0D
                    || z
                            >= BOUNDARY_MAX_Z
                            - 1.0D
            ) {

                continue;
            }


            int blockX =
                    (int) Math.floor(
                            x
                    );


            int blockZ =
                    (int) Math.floor(
                            z
                    );


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


            if (
                    !floor.getType()
                            .isSolid()
            ) {

                continue;
            }


            if (
                    !feet.isPassable()
            ) {

                continue;
            }


            if (
                    !head.isPassable()
            ) {

                continue;
            }


            Location candidate =
                    new Location(
                            world,
                            blockX + 0.5D,
                            SPAWN_FEET_Y,
                            blockZ + 0.5D
                    );


            if (
                    !isFarEnough(
                            candidate,
                            result
                    )
            ) {

                continue;
            }


            result.add(
                    candidate
            );
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


        for (
                Location location
                        : existing
        ) {

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
     * MOB DEATH / WAVE TRANSITION
     * =========================================================
     */
    public void onDungeonMobDeath(
            LivingEntity entity
    ) {

        UUID uuid =
                entity.getUniqueId();

        if (
                !activeDungeonMobs.remove(
                        uuid
                )
        ) {
            return;
        }

        attackDamageMap.remove(uuid);

        if (!activeDungeonMobs.isEmpty()) {
            return;
        }

        switch (state) {

            case WAVE_1 -> {

                state =
                        DungeonState.WAVE_2;

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
                        DungeonState.WAVE_3;

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
                        DungeonState.BOSS;

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

                removeMobsInsideExpandedCleanupArea();

                rewardClear();

                removeBossBar();
                clearTitles();

                broadcast(
                        Component.text(
                                "타락한 피글린 군주를 처치했습니다.",
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
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> runClearExitCountdown(
                                        5
                                ),
                                100L
                        );
            }

            default -> {
            }
        }
    }


    /*
     * =========================================================
     * WAVE COUNTDOWN
     * =========================================================
     */
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

        if (state != DungeonState.BOSS) {
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

                            if (state != DungeonState.BOSS) {
                                return;
                            }

                            showBossSubtitle(
                                    Component.text(
                                            "강력한 네더의 기운이 느껴집니다...",
                                            NamedTextColor.DARK_RED
                                    ),
                                    3000L
                            );

                            plugin.getServer()
                                    .getScheduler()
                                    .runTaskLater(
                                            plugin,
                                            () -> {

                                                if (state != DungeonState.BOSS) {
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

                                                                    if (state != DungeonState.BOSS) {
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
     * 채팅 + 화면 중앙 안내.
     */
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


    /*
     * =========================================================
     * BOSS SPAWN
     * =========================================================
     */
    private void spawnBoss() {

        if (
                state
                        != DungeonState.BOSS
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
                        EntityType.PIGLIN_BRUTE
                );


        if (
                !(raw instanceof Mob boss)
        ) {

            raw.remove();

            return;
        }


        configureNetherMob(
                boss
        );


        applyDungeonTags(
                boss,
                4,
                true,
                BOSS_EFFECTIVE_HEALTH,
                BOSS_ATTACK_DAMAGE
        );


        /*
         * 실제 HP = 1024
         * Effective HP = 4500
         */
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


        boss.setCustomNameVisible(
                true
        );

        boss.setPersistent(
                true
        );

        boss.setRemoveWhenFarAway(
                false
        );

        boss.setCanPickupItems(
                false
        );


        Player target =
                findNearestActiveParticipant(
                        boss.getLocation()
                );


        if (
                target != null
        ) {

            boss.setTarget(
                    target
            );
        }


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


        startBossPatternTask();
    }


    /*
     * =========================================================
     * BOSS PATTERN
     * =========================================================
     */

    private void startBossPatternTask() {

        stopBossPatternTask();

        bossPatternIndex =
                0;

        bossPatternElapsedTicks =
                0L;

        bossEnraged =
                false;


        bossPatternTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != DungeonState.BOSS
                                            || activeBoss == null
                                            || !activeBoss.isValid()
                                            || activeBoss.isDead()
                                    ) {

                                        stopBossPatternTask();

                                        return;
                                    }


                                    checkBossEnrage();


                                    bossPatternElapsedTicks +=
                                            BOSS_PATTERN_CHECK_TICKS;


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


                                    executeNextBossPattern();
                                },
                                BOSS_PATTERN_CHECK_TICKS,
                                BOSS_PATTERN_CHECK_TICKS
                        );
    }


    private void stopBossPatternTask() {

        if (
                bossPatternTask != null
        ) {

            bossPatternTask.cancel();

            bossPatternTask =
                    null;
        }
    }


    private void checkBossEnrage() {

        if (
                bossEnraged
                || activeBoss == null
                || activeBoss.isDead()
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


        AttributeInstance movement =
                activeBoss.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );


        if (
                movement != null
        ) {

            movement.setBaseValue(
                    Math.max(
                            movement.getBaseValue(),
                            BOSS_ENRAGED_MOVEMENT_SPEED
                    )
            );
        }


        broadcast(
                Component.text(
                        "타락한 피글린 군주가 광폭화합니다!",
                        NamedTextColor.DARK_RED
                )
        );


        Title title =
                Title.title(
                        Component.empty(),
                        Component.text(
                                "군주가 광폭화합니다!",
                                NamedTextColor.RED
                        ),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(
                                        1600L
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


        World world =
                activeBoss.getWorld();


        world.spawnParticle(
                Particle.FLAME,
                activeBoss.getLocation()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                80,
                1.4D,
                1.0D,
                1.4D,
                0.08D
        );


        world.playSound(
                activeBoss.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                1.2F,
                0.7F
        );
    }


    private void executeNextBossPattern() {

        if (
                activeBoss == null
                || activeBoss.isDead()
                || !activeBoss.isValid()
        ) {

            return;
        }


        int pattern =
                bossPatternIndex
                        % 4;


        bossPatternIndex++;


        switch (
                pattern
        ) {

            case 0 ->
                    performAxeSlam();

            case 1 ->
                    performJumpSlam();

            case 2 ->
                    performFireAoe();

            case 3 ->
                    summonPiglinReinforcements();

            default -> {
            }
        }
    }


    /*
     * =========================================================
     * PATTERN 1 - AXE SLAM
     * =========================================================
     */
    private void performAxeSlam() {

        if (
                activeBoss == null
        ) {

            return;
        }


        Location origin =
                activeBoss.getLocation();


        Vector facing =
                origin.getDirection()
                        .setY(
                                0.0D
                        );


        if (
                facing.lengthSquared()
                        <= 0.0001D
        ) {

            facing =
                    new Vector(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        }


        facing.normalize();


        activeBoss.getWorld()
                .spawnParticle(
                        Particle.FLAME,
                        origin.clone()
                                .add(
                                        0.0D,
                                        0.6D,
                                        0.0D
                                ),
                        35,
                        1.2D,
                        0.3D,
                        1.2D,
                        0.03D
                );


        activeBoss.getWorld()
                .playSound(
                        origin,
                        Sound.ENTITY_GENERIC_EXPLODE,
                        0.8F,
                        1.15F
                );


        for (
                UUID uuid
                        : new HashSet<>(
                                activeParticipants
                        )
        ) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    !isValidBossTarget(
                            player
                    )
            ) {

                continue;
            }


            Vector toward =
                    player.getLocation()
                            .toVector()
                            .subtract(
                                    origin.toVector()
                            );


            toward.setY(
                    0.0D
            );


            double distance =
                    toward.length();


            if (
                    distance
                            > BOSS_AXE_SLAM_RANGE
                    || distance
                            <= 0.01D
            ) {

                continue;
            }


            Vector direction =
                    toward.clone()
                            .normalize();


            if (
                    facing.dot(
                            direction
                    ) < 0.15D
            ) {

                continue;
            }


            damagePatternTarget(
                    player,
                    BOSS_AXE_SLAM_DAMAGE
            );


            Vector knockback =
                    direction.multiply(
                            0.85D
                    );


            knockback.setY(
                    0.28D
            );


            player.setVelocity(
                    knockback
            );
        }
    }


    /*
     * =========================================================
     * PATTERN 2 - JUMP SLAM
     * =========================================================
     */
    private void performJumpSlam() {

        if (
                activeBoss == null
        ) {

            return;
        }


        Player target =
                findNearestActiveParticipant(
                        activeBoss.getLocation()
                );


        if (
                target == null
        ) {

            return;
        }


        Vector direction =
                target.getLocation()
                        .toVector()
                        .subtract(
                                activeBoss.getLocation()
                                        .toVector()
                        );


        direction.setY(
                0.0D
        );


        if (
                direction.lengthSquared()
                        > 0.0001D
        ) {

            direction.normalize()
                    .multiply(
                            0.85D
                    );
        }


        direction.setY(
                0.65D
        );


        activeBoss.setVelocity(
                direction
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    state
                                            != DungeonState.BOSS
                                    || activeBoss == null
                                    || !activeBoss.isValid()
                                    || activeBoss.isDead()
                            ) {

                                return;
                            }


                            performJumpSlamImpact();
                        },
                        14L
                );
    }


    private void performJumpSlamImpact() {

        if (
                activeBoss == null
                || !activeBoss.isValid()
                || activeBoss.isDead()
        ) {

            return;
        }


        Location origin =
                activeBoss.getLocation();


        activeBoss.getWorld()
                .spawnParticle(
                        Particle.FLAME,
                        origin.clone()
                                .add(
                                        0.0D,
                                        0.2D,
                                        0.0D
                                ),
                        70,
                        2.4D,
                        0.25D,
                        2.4D,
                        0.05D
                );


        activeBoss.getWorld()
                .playSound(
                        origin,
                        Sound.ENTITY_GENERIC_EXPLODE,
                        1.1F,
                        0.85F
                );


        for (
                UUID uuid
                        : new HashSet<>(
                                activeParticipants
                        )
        ) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    !isValidBossTarget(
                            player
                    )
            ) {

                continue;
            }


            if (
                    player.getLocation()
                            .distanceSquared(
                                    origin
                            )
                            > BOSS_JUMP_SLAM_RANGE
                            * BOSS_JUMP_SLAM_RANGE
            ) {

                continue;
            }


            damagePatternTarget(
                    player,
                    BOSS_JUMP_SLAM_DAMAGE
            );


            Vector knockback =
                    player.getLocation()
                            .toVector()
                            .subtract(
                                    origin.toVector()
                            );


            knockback.setY(
                    0.0D
            );


            if (
                    knockback.lengthSquared()
                            > 0.0001D
            ) {

                knockback.normalize()
                        .multiply(
                                1.05D
                        );
            }


            knockback.setY(
                    0.42D
            );


            player.setVelocity(
                    knockback
            );
        }
    }


    /*
     * =========================================================
     * PATTERN 3 - FIRE AOE
     * =========================================================
     */
    private void performFireAoe() {

        if (
                activeBoss == null
        ) {

            return;
        }


        Location origin =
                activeBoss.getLocation();


        activeBoss.getWorld()
                .spawnParticle(
                        Particle.FLAME,
                        origin.clone()
                                .add(
                                        0.0D,
                                        1.0D,
                                        0.0D
                                ),
                        120,
                        3.0D,
                        0.8D,
                        3.0D,
                        0.08D
                );


        activeBoss.getWorld()
                .playSound(
                        origin,
                        Sound.ENTITY_GENERIC_EXPLODE,
                        1.0F,
                        1.35F
                );


        for (
                UUID uuid
                        : new HashSet<>(
                                activeParticipants
                        )
        ) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    !isValidBossTarget(
                            player
                    )
            ) {

                continue;
            }


            if (
                    player.getLocation()
                            .distanceSquared(
                                    origin
                            )
                            > BOSS_FIRE_AOE_RANGE
                            * BOSS_FIRE_AOE_RANGE
            ) {

                continue;
            }


            damagePatternTarget(
                    player,
                    BOSS_FIRE_AOE_DAMAGE
            );


            player.setFireTicks(
                    Math.max(
                            player.getFireTicks(),
                            BOSS_FIRE_TICKS
                    )
            );
        }
    }


    /*
     * =========================================================
     * PATTERN 4 - PIGLIN REINFORCEMENTS
     * =========================================================
     */
    private void summonPiglinReinforcements() {

        if (
                dungeonWorld == null
                || activeBoss == null
        ) {

            return;
        }


        cleanupBossSummonSet();


        int available =
                BOSS_SUMMON_MAX_ALIVE
                        - bossSummons.size();


        if (
                available <= 0
        ) {

            return;
        }


        int count =
                Math.min(
                        BOSS_SUMMON_COUNT,
                        available
                );


        List<Location> locations =
                findSafeSpawnLocations(
                        dungeonWorld,
                        count
                );


        if (
                locations.size()
                        < count
        ) {

            return;
        }


        broadcast(
                Component.text(
                        "타락한 피글린 군주가 지원군을 소환합니다!",
                        NamedTextColor.RED
                )
        );


        for (
                int i = 0;
                i < count;
                i++
        ) {

            Entity raw =
                    dungeonWorld.spawnEntity(
                            locations.get(i),
                            EntityType.PIGLIN
                    );


            if (
                    !(raw instanceof Mob summon)
            ) {

                raw.remove();

                continue;
            }


            configureNetherMob(
                    summon
            );


            applyDungeonTags(
                    summon,
                    4,
                    false,
                    BOSS_SUMMON_HEALTH,
                    BOSS_SUMMON_ATTACK
            );


            setHealth(
                    summon,
                    BOSS_SUMMON_HEALTH
            );


            setAttackAttribute(
                    summon,
                    BOSS_SUMMON_ATTACK
            );


            summon.customName(
                    Component.text(
                            "군주의 지원병",
                            NamedTextColor.RED
                    )
            );


            summon.setCustomNameVisible(
                    false
            );

            summon.setPersistent(
                    true
            );

            summon.setRemoveWhenFarAway(
                    false
            );

            summon.setCanPickupItems(
                    false
            );


            Player target =
                    findNearestActiveParticipant(
                            summon.getLocation()
                    );


            if (
                    target != null
            ) {

                summon.setTarget(
                        target
                );
            }


            bossSummons.add(
                    summon.getUniqueId()
            );
        }
    }


    private void cleanupBossSummonSet() {

        bossSummons.removeIf(
                uuid -> {

                    Entity entity =
                            plugin.getServer()
                                    .getEntity(
                                            uuid
                                    );


                    return entity == null
                            || !entity.isValid()
                            || entity.isDead();
                }
        );
    }


    private void removeBossSummons() {

        for (
                UUID uuid
                        : new HashSet<>(
                                bossSummons
                        )
        ) {

            Entity entity =
                    plugin.getServer()
                            .getEntity(
                                    uuid
                            );


            if (
                    entity != null
                    && entity.isValid()
            ) {

                entity.remove();
            }
        }


        bossSummons.clear();
    }


    private boolean isValidBossTarget(
            Player player
    ) {

        return player != null
                && player.isOnline()
                && !player.isDead()
                && activeBoss != null
                && player.getWorld()
                        .equals(
                                activeBoss.getWorld()
                        );
    }


    /*
     * 특수 패턴은 자체 피해량을 사용한다.
     *
     * boss를 damage source로 넘기면 MobListener에서
     * 기본 공격력 25/32로 덮어쓸 수 있으므로
     * source 없는 damage를 사용한다.
     */
    private void damagePatternTarget(
            Player player,
            double damage
    ) {

        if (
                damage <= 0.0D
        ) {

            return;
        }


        player.damage(
                damage
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
     * PDC
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


        if (
                boss
        ) {

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


        AttributeInstance attribute =
                entity.getAttribute(
                        Attribute.MAX_HEALTH
                );


        if (
                attribute != null
        ) {

            attribute.setBaseValue(
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
                attribute == null
        ) {

            return;
        }


        attribute.setBaseValue(
                attack
        );
    }


    /*
     * =========================================================
     * PUBLIC MOB API
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


        return value != null
                ? value
                : 0.0D;
    }


    public double getAttackDamage(
            LivingEntity entity
    ) {

        Double value =
                attackDamageMap.get(
                        entity.getUniqueId()
                );


        if (
                value != null
        ) {

            return value;
        }


        Double persistent =
                entity
                        .getPersistentDataContainer()
                        .get(
                                attackDamageKey,
                                PersistentDataType.DOUBLE
                        );


        return persistent != null
                ? persistent
                : 0.0D;
    }


    /*
     * =========================================================
     * MOB BOUNDARY
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


        return x
                >= BOUNDARY_MIN_X
                && x
                < BOUNDARY_MAX_X
                && z
                >= BOUNDARY_MIN_Z
                && z
                < BOUNDARY_MAX_Z;
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
    private void runClearExitCountdown(
            int seconds
    ) {

        if (state != DungeonState.BOSS) {
            return;
        }

        if (seconds <= 0) {

            clearTitles();

            markAndReturnParticipants();

            scheduleReset(
                    20L
            );

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

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> runClearExitCountdown(
                                seconds - 1
                        ),
                        20L
                );
    }

    private void rewardClear() {

        if (
                dungeonRewardService == null
        ) {

            plugin.getLogger()
                    .warning(
                            "[NetherFortress] "
                                    + "DungeonRewardService is null"
                    );

            return;
        }


        Set<UUID> clearParticipants =
                new HashSet<>(
                        participants
                );

        dungeonRewardService
                .rewardDungeonClearExperience(
                        clearParticipants,
                        CLEAR_EXP
                );

        /*
         * =====================================================
         * SPECIAL WEAPON REWARD
         * =====================================================
         *
         * 직업 제한 없이 공용 무기 4종 중 하나를
         * 총 10% 확률로 획득한다.
         *
         * 기존 네더 군주 일반 / 전설 장비는
         * 네더 요새 클리어 보상에서 지급하지 않는다.
         */
    

        dungeonRewardService
                .rewardNetherFortressBasicRewards(
                        new HashSet<>(
                                participants
                        )
                );

        /*
         * =====================================================
         * ENHANCEMENT STONE REWARD
         * =====================================================
         *
         * 2개 확정 지급.
         * 추가로 25% 확률로 1개를 더 지급한다.
         *
         * 최소 2개 / 최대 3개.
         */
        /*
         * 던전 클리어 주괴 보상
         * 영웅 / 전설 / 신화 독립 추첨
         */
        dungeonRewardService
                .rewardEquipmentIngots(
                clearParticipants,
                0.05D,
                0.03D,
                0.01D
                );

        dungeonRewardService
                .rewardTeyvatRareWeapons(
                        clearParticipants
                );

        dungeonRewardService
                .rewardDungeonGold(
                        clearParticipants,
                        500L,
                        200L,
                        0.20D
                );

        dungeonRewardService
                .rewardEnhancementStones(
                        clearParticipants,
                        2,
                        0.25D,
                        1,
                        0
                );
}


    /*
     * =========================================================
     * RETURN
     * =========================================================
     */
    private void markAndReturnParticipants() {

        if (
                dungeonWorld == null
        ) {

            return;
        }


        Location returnLocation =
                new Location(
                        dungeonWorld,
                        RETURN_X,
                        RETURN_Y,
                        RETURN_Z
                );


        for (
                UUID uuid
                        : new HashSet<>(
                                participants
                        )
        ) {

            /*
             * 일단 pending 등록.
             *
             * 즉시 teleport 성공하면 제거.
             * 사망 / offline이면 respawn/join 때 사용.
             */
            pendingReturnLocations.put(
                    uuid,
                    returnLocation.clone()
            );


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

                continue;
            }


            if (
                    player.teleport(
                            returnLocation
                    )
            ) {

                pendingReturnLocations.remove(
                        uuid
                );
            }
        }
    }


    /*
     * =========================================================
     * TARGET
     * =========================================================
     */
    private Player findNearestActiveParticipant(
            Location origin
    ) {

        Player nearest =
                null;


        double nearestDistanceSquared =
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


            double distanceSquared =
                    player.getLocation()
                            .distanceSquared(
                                    origin
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


    /*
     * =========================================================
     * RESET
     * =========================================================
     */
    private void scheduleReset(
            long delay
    ) {

        if (
                resetTask != null
        ) {

            return;
        }


        resetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                this::reset,
                                delay
                        );
    }


    private void reset() {

        for (
                UUID uuid
                        : new HashSet<>(
                                activeDungeonMobs
                        )
        ) {

            Entity entity =
                    plugin.getServer()
                            .getEntity(
                                    uuid
                            );


            if (
                    entity != null
                    && entity.isValid()
            ) {

                entity.remove();
            }
        }


        activeDungeonMobs.clear();

        attackDamageMap.clear();


        removeMobsInsideExpandedCleanupArea();

        removeDroppedItemsInsideDungeon();


        removeBossBar();


        stopBossPatternTask();

        removeBossSummons();


        bossPatternIndex =
                0;

        bossPatternElapsedTicks =
                0L;

        bossEnraged =
                false;


        activeBoss =
                null;


        participants.clear();

        activeParticipants.clear();


        dungeonWorld =
                null;


        state =
                DungeonState.IDLE;


        resetTask =
                null;


        /*
         * pendingReturnLocations는 삭제하지 않는다.
         *
         * 죽은 플레이어 / offline 플레이어 귀환에 필요.
         */
    }


    /*
     * =========================================================
     * REMAINING MAGMA CUBE CLEANUP
     * =========================================================
     *
     * 던전에서 생성된 Magma Cube가 죽으면서
     * 분열한 작은 개체들은 activeDungeonMobs에
     * 등록되지 않을 수 있다.
     *
     * 분열 자체는 허용하지만,
     * 던전 종료/실패/리셋 시 내부에 남은
     * 모든 Magma Cube를 제거한다.
     */
    /*
     * =========================================================
     * EXPANDED DUNGEON MOB CLEANUP
     * =========================================================
     *
     * 기본 전투 영역에서 X/Z 방향으로 4블록 확장한 범위의
     * 모든 Mob 엔티티를 제거한다.
     *
     * 기존:
     * X 188 ~ 204
     * Z -505 ~ -487
     *
     * 확장:
     * X 184 ~ 208
     * Z -509 ~ -483
     */
    private void removeMobsInsideExpandedCleanupArea() {

        if (dungeonWorld == null) {
            return;
        }

        final double cleanupMinX =
                BOUNDARY_MIN_X - 4.0D;

        final double cleanupMaxX =
                BOUNDARY_MAX_X + 4.0D;

        final double cleanupMinZ =
                BOUNDARY_MIN_Z - 4.0D;

        final double cleanupMaxZ =
                BOUNDARY_MAX_Z + 4.0D;

        for (
                Entity entity
                        : new ArrayList<>(
                                dungeonWorld.getEntities()
                        )
        ) {

            if (!(entity instanceof Mob)) {
                continue;
            }

            Location location =
                    entity.getLocation();

            double x =
                    location.getX();

            double z =
                    location.getZ();

            if (
                    x < cleanupMinX
                    || x >= cleanupMaxX
                    || z < cleanupMinZ
                    || z >= cleanupMaxZ
            ) {
                continue;
            }

            entity.remove();
        }
    }


    private void removeRemainingMagmaCubes() {

        if (dungeonWorld == null) {
            return;
        }


        for (
                Entity entity
                        : new ArrayList<>(
                                dungeonWorld.getEntities()
                        )
        ) {

            if (
                    entity.getType()
                            != EntityType.MAGMA_CUBE
            ) {

                continue;
            }


            Location location =
                    entity.getLocation();


            double x =
                    location.getX();

            double z =
                    location.getZ();


            if (
                    x < BOUNDARY_MIN_X
                    || x >= BOUNDARY_MAX_X
                    || z < BOUNDARY_MIN_Z
                    || z >= BOUNDARY_MAX_Z
            ) {

                continue;
            }


            entity.remove();
        }
    }


    /*
     * =========================================================
     * DROPPED ITEM CLEANUP
     * =========================================================
     */
    private void removeDroppedItemsInsideDungeon() {

        if (dungeonWorld == null) {
            return;
        }

        for (
                Entity entity
                        : new ArrayList<>(
                                dungeonWorld.getEntities()
                        )
        ) {

            if (!(entity instanceof Item)) {
                continue;
            }

            Location location =
                    entity.getLocation();

            double x =
                    location.getX();

            double z =
                    location.getZ();

            if (
                    x < BOUNDARY_MIN_X
                    || x >= BOUNDARY_MAX_X
                    || z < BOUNDARY_MIN_Z
                    || z >= BOUNDARY_MAX_Z
            ) {
                continue;
            }

            entity.remove();
        }
    }


    private void removeBossBar() {

        if (
                bossBarTask != null
        ) {

            bossBarTask.cancel();

            bossBarTask =
                    null;
        }


        if (
                bossBar != null
        ) {

            bossBar.removeAll();

            bossBar =
                    null;
        }
    }


    /*
     * =========================================================
     * STATE
     * =========================================================
     */
    private boolean isExpectedWave(
            int wave
    ) {

        return switch (
                wave
        ) {

            case 1 ->
                    state
                            == DungeonState.WAVE_1;

            case 2 ->
                    state
                            == DungeonState.WAVE_2;

            case 3 ->
                    state
                            == DungeonState.WAVE_3;

            default ->
                    false;
        };
    }


    /*
     * =========================================================
     * MESSAGE
     * =========================================================
     */
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
                    player == null
                    || !player.isOnline()
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
     * RECORD / STATE
     * =========================================================
     */
    private record MobSpec(
            EntityType type,
            double health,
            double attack,
            String name
    ) {
    }


    private enum DungeonState {

        IDLE,

        WAVE_1,

        WAVE_2,

        WAVE_3,

        BOSS
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
