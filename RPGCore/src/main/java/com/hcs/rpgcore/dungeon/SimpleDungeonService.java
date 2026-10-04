package com.hcs.rpgcore.dungeon;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Zombie;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


public final class SimpleDungeonService
        implements Listener {

    public static final String DUNGEON_ID =
            "simple_dungeon";


    /*
     * =========================================================
     * ENTIRE DUNGEON AREA
     * =========================================================
     */

    private static final int DUNGEON_MIN_X = 3999547;
    private static final int DUNGEON_MAX_X = 3999936;

    private static final int DUNGEON_MIN_Y = 251;
    private static final int DUNGEON_MAX_Y = 334;

    private static final int DUNGEON_MIN_Z = 3999670;
    private static final int DUNGEON_MAX_Z = 3999756;


    /*
     * =========================================================
     * OUTSIDE ENTRANCE
     * =========================================================
     */

    private static final Cuboid ENTRY_GATE =
            new Cuboid(
                    961, 64, -1180,
                    963, 64, -1178
            );

    private static final LocationData EXIT_LOCATION =
            new LocationData(
                    964.0D,
                    64.0D,
                    -1187.0D
            );


    /*
     * =========================================================
     * SPAWN SETTINGS
     * =========================================================
     */

    private static final int MAX_MOBS =
            36;

    private static final int REFILL_THRESHOLD =
            12;

    private static final long REFILL_DELAY_TICKS =
            40L;

    private static final long RETRY_DELAY_TICKS =
            20L;

    private static final double PLAYER_SAFE_DISTANCE =
            2.0D;

    private static final double PLAYER_SAFE_DISTANCE_SQUARED =
            PLAYER_SAFE_DISTANCE
                    * PLAYER_SAFE_DISTANCE;


    private final JavaPlugin plugin;

    private final NamespacedKey simpleDungeonMobKey;
    private final NamespacedKey simpleDungeonStageKey;
    private final NamespacedKey simpleDungeonExpKey;
    private final NamespacedKey eliteMobKey;

    private final com.hcs.rpgcore.mob.RedstoneGolemSpawnService
            redstoneGolemSpawnService;

    private final com.hcs.rpgcore.mob.EliteDemonKnightListener
            eliteDemonKnightListener;

    private final Map<UUID, Integer> playerStages =
            new HashMap<>();

    private final Set<UUID> pendingRespawnExit =
            new HashSet<>();

    private final Map<Integer, StageRuntime> stageRuntimes =
            new HashMap<>();

    private final Map<UUID, BukkitTask> entranceCountdownTasks =
            new HashMap<>();

    private BukkitTask maintenanceTask;


    public SimpleDungeonService(
            JavaPlugin plugin
    ) {

        this.plugin = plugin;

        this.simpleDungeonMobKey =
                new NamespacedKey(
                        plugin,
                        "simple_dungeon_mob"
                );

        this.simpleDungeonStageKey =
                new NamespacedKey(
                        plugin,
                        "simple_dungeon_stage"
                );

        this.simpleDungeonExpKey =
                new NamespacedKey(
                        plugin,
                        "simple_dungeon_exp"
                );

        this.eliteMobKey =
                new NamespacedKey(
                        plugin,
                        com.hcs.rpgcore.mob.EliteDemonKnightListener
                                .ELITE_MOB_KEY
                );

        this.redstoneGolemSpawnService =
                new com.hcs.rpgcore.mob.RedstoneGolemSpawnService(
                        plugin
                );

        this.eliteDemonKnightListener =
                new com.hcs.rpgcore.mob.EliteDemonKnightListener(
                        plugin
                );

        for (int stage = 1; stage <= 4; stage++) {
            stageRuntimes.put(
                    stage,
                    new StageRuntime()
            );
        }

        startMaintenanceTask();
    }


    /*
     * =========================================================
     * STAGE CONFIG
     * =========================================================
     */

    private StageConfig getStage(
            int stage
    ) {

        return switch (stage) {

            case 1 -> new StageConfig(
                    1,
                    new LocationData(
                            3999603.5D,
                            262.0D,
                            3999712.5D
                    ),
                    new Cuboid(
                            3999602, 262, 3999747,
                            3999604, 262, 3999749
                    ),
                    new Cuboid(
                            3999602, 262, 3999675,
                            3999602, 262, 3999677
                    )
            );

            case 2 -> new StageConfig(
                    2,
                    new LocationData(
                            3999701.5D,
                            262.0D,
                            3999712.5D
                    ),
                    new Cuboid(
                            3999700, 262, 3999747,
                            3999702, 262, 3999749
                    ),
                    new Cuboid(
                            3999700, 262, 3999675,
                            3999702, 262, 3999677
                    )
            );

            case 3 -> new StageConfig(
                    3,
                    new LocationData(
                            3999799.5D,
                            262.0D,
                            3999712.5D
                    ),
                    new Cuboid(
                            3999798, 262, 3999747,
                            3999800, 262, 3999749
                    ),
                    new Cuboid(
                            3999798, 262, 3999675,
                            3999800, 262, 3999677
                    )
            );

            case 4 -> new StageConfig(
                    4,
                    new LocationData(
                            3999897.5D,
                            262.0D,
                            3999712.5D
                    ),
                    null,
                    new Cuboid(
                            3999896, 262, 3999675,
                            3999898, 262, 3999677
                    )
            );

            default -> null;
        };
    }


    /*
     * =========================================================
     * STAGE MONSTER CONFIG
     * =========================================================
     */

    private MobDefinition[] getStageMobs(
            int stage
    ) {

        return switch (stage) {

            /*
             * Stage 1 / Lv.20~30
             *
             * Zombie     18
             * Skeleton   12
             * Husk        6
             */
            case 1 -> new MobDefinition[] {

                    new MobDefinition(
                            EntityType.ZOMBIE,
                            18,
                            80.0D,
                            8.0D,
                            2.0D,
                            15
                    ),

                    new MobDefinition(
                            EntityType.SKELETON,
                            12,
                            70.0D,
                            9.0D,
                            1.0D,
                            18
                    ),

                    new MobDefinition(
                            EntityType.HUSK,
                            6,
                            90.0D,
                            10.0D,
                            3.0D,
                            20
                    )
            };


            /*
             * Stage 2 / Lv.30~40
             */
            case 2 -> new MobDefinition[] {

                    new MobDefinition(
                            EntityType.ZOMBIE,
                            8,
                            110.0D,
                            11.0D,
                            3.0D,
                            22
                    ),

                    new MobDefinition(
                            EntityType.HUSK,
                            10,
                            130.0D,
                            13.0D,
                            5.0D,
                            26
                    ),

                    new MobDefinition(
                            EntityType.SKELETON,
                            8,
                            105.0D,
                            12.0D,
                            2.0D,
                            24
                    ),

                    new MobDefinition(
                            EntityType.STRAY,
                            6,
                            120.0D,
                            14.0D,
                            3.0D,
                            28
                    ),

                    new MobDefinition(
                            EntityType.CAVE_SPIDER,
                            4,
                            100.0D,
                            12.0D,
                            2.0D,
                            25
                    )
            };


            /*
             * Stage 3 / Lv.40~50
             */
            case 3 -> new MobDefinition[] {

                    new MobDefinition(
                            EntityType.HUSK,
                            8,
                            175.0D,
                            15.0D,
                            6.0D,
                            32
                    ),

                    new MobDefinition(
                            EntityType.DROWNED,
                            8,
                            180.0D,
                            16.0D,
                            5.0D,
                            34
                    ),

                    new MobDefinition(
                            EntityType.STRAY,
                            8,
                            165.0D,
                            17.0D,
                            4.0D,
                            35
                    ),

                    new MobDefinition(
                            EntityType.ZOMBIFIED_PIGLIN,
                            8,
                            200.0D,
                            18.0D,
                            7.0D,
                            38
                    ),

                    new MobDefinition(
                            EntityType.WITHER_SKELETON,
                            4,
                            240.0D,
                            19.0D,
                            8.0D,
                            45
                    )
            };


            /*
             * Stage 4 / Lv.50~60
             */
            case 4 -> new MobDefinition[] {

                    new MobDefinition(
                            EntityType.ZOMBIFIED_PIGLIN,
                            7,
                            260.0D,
                            20.0D,
                            8.0D,
                            45
                    ),

                    new MobDefinition(
                            EntityType.PIGLIN,
                            7,
                            275.0D,
                            21.0D,
                            8.0D,
                            48
                    ),

                    new MobDefinition(
                            EntityType.WITHER_SKELETON,
                            10,
                            310.0D,
                            24.0D,
                            10.0D,
                            55
                    ),

                    new MobDefinition(
                            EntityType.BLAZE,
                            6,
                            280.0D,
                            23.0D,
                            7.0D,
                            55
                    ),

                    new MobDefinition(
                            EntityType.PIGLIN_BRUTE,
                            4,
                            380.0D,
                            27.0D,
                            12.0D,
                            70
                    )
            };

            default -> new MobDefinition[0];
        };
    }


    /*
     * =========================================================
     * PLAYER MOVE
     * =========================================================
     */

    @EventHandler
    public void onMove(
            PlayerMoveEvent event
    ) {

        if (
                event.getTo() == null
                        || sameBlock(
                                event.getFrom(),
                                event.getTo()
                        )
        ) {
            return;
        }

        Player player =
                event.getPlayer();

        Location location =
                event.getTo();


        /*
         * 외부 입장 게이트.
         */
        if (ENTRY_GATE.contains(location)) {

            startEntranceCountdown(
                    player
            );

            return;
        }

        cancelEntranceCountdown(
                player.getUniqueId(),
                true
        );


        Integer currentStage =
                playerStages.get(
                        player.getUniqueId()
                );

        if (currentStage == null) {
            return;
        }

        StageConfig config =
                getStage(
                        currentStage
                );

        if (config == null) {
            return;
        }


        /*
         * 퇴장 게이트가 우선.
         */
        if (
                config.exitGate()
                        .contains(location)
        ) {

            exitDungeon(
                    player
            );

            return;
        }


        /*
         * Stage 4에는 다음 Stage가 없다.
         */
        if (
                config.nextGate() != null
                        && config.nextGate()
                                .contains(location)
        ) {

            enterStage(
                    player,
                    currentStage + 1
            );
        }
    }


    /*
     * =========================================================
     * ENTRANCE COUNTDOWN
     * =========================================================
     */

    private void startEntranceCountdown(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        if (
                entranceCountdownTasks
                        .containsKey(uuid)
        ) {
            return;
        }

        final int[] seconds = {3};

        BukkitTask task =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> {

                                    if (
                                            !player.isOnline()
                                                    || !ENTRY_GATE
                                                            .contains(
                                                                    player.getLocation()
                                                            )
                                    ) {

                                        cancelEntranceCountdown(
                                                uuid,
                                                true
                                        );

                                        return;
                                    }

                                    if (seconds[0] <= 0) {

                                        cancelEntranceCountdown(
                                                uuid,
                                                false
                                        );

                                        player.clearTitle();

                                        enterStage(
                                                player,
                                                1
                                        );

                                        return;
                                    }

                                    showEntranceCountdown(
                                            player,
                                            seconds[0]
                                    );

                                    seconds[0]--;
                                },
                                0L,
                                20L
                        );

        entranceCountdownTasks.put(
                uuid,
                task
        );
    }


    private void showEntranceCountdown(
            Player player,
            int seconds
    ) {

        Title title =
                Title.title(
                        Component.empty(),
                        Component.text(
                                "던전 입장까지 "
                                        + seconds
                                        + "초 남았습니다.",
                                NamedTextColor.LIGHT_PURPLE
                        ),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(
                                        1100L
                                ),
                                Duration.ZERO
                        )
                );

        player.showTitle(
                title
        );
    }


    private void cancelEntranceCountdown(
            UUID uuid,
            boolean clearTitle
    ) {

        BukkitTask task =
                entranceCountdownTasks
                        .remove(uuid);

        if (task != null) {
            task.cancel();
        }

        if (clearTitle) {

            Player player =
                    Bukkit.getPlayer(uuid);

            if (player != null) {
                player.clearTitle();
            }
        }
    }


    /*
     * =========================================================
     * STAGE ENTER
     * =========================================================
     */

    private void enterStage(
            Player player,
            int stage
    ) {

        StageConfig config =
                getStage(stage);

        if (config == null) {
            return;
        }

        World world =
                player.getWorld();

        playerStages.put(
                player.getUniqueId(),
                stage
        );

        player.teleport(
                config.spawn()
                        .toLocation(world)
        );

        ensureStagePopulation(
                world,
                stage
        );
    }


    /*
     * =========================================================
     * EXIT
     * =========================================================
     */

    private void exitDungeon(
            Player player
    ) {

        playerStages.remove(
                player.getUniqueId()
        );

        player.clearTitle();

        player.teleport(
                EXIT_LOCATION
                        .toLocation(
                                player.getWorld()
                        )
        );
    }


    /*
     * =========================================================
     * DEATH / RESPAWN
     * =========================================================
     */

    @EventHandler
    public void onDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getPlayer();

        if (
                !playerStages
                        .containsKey(
                                player.getUniqueId()
                        )
                        && !isInsideDungeon(
                                player.getLocation()
                        )
        ) {
            return;
        }

        playerStages.remove(
                player.getUniqueId()
        );

        pendingRespawnExit.add(
                player.getUniqueId()
        );
    }


    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        UUID uuid =
                event.getPlayer()
                        .getUniqueId();

        if (
                !pendingRespawnExit
                        .remove(uuid)
        ) {
            return;
        }

        event.setRespawnLocation(
                EXIT_LOCATION
                        .toLocation(
                                event.getPlayer()
                                        .getWorld()
                        )
        );
    }


    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        UUID uuid =
                event.getPlayer()
                        .getUniqueId();

        cancelEntranceCountdown(
                uuid,
                false
        );

        playerStages.remove(
                uuid
        );
    }


    /*
     * =========================================================
     * NATURAL SPAWN PROTECTION
     * =========================================================
     */

    @EventHandler
    public void onCreatureSpawn(
            CreatureSpawnEvent event
    ) {

        if (
                !isInsideDungeon(
                        event.getLocation()
                )
        ) {
            return;
        }

        /*
         * simple_dungeon에서 직접 생성한 CUSTOM 몹만 허용.
         */
        if (
                event.getSpawnReason()
                        != CreatureSpawnEvent
                                .SpawnReason
                                .CUSTOM
        ) {

            event.setCancelled(
                    true
            );
        }
    }


    /*
     * =========================================================
     * DUNGEON MOB DEATH
     * =========================================================
     */

    @EventHandler
    public void onMobDeath(
            EntityDeathEvent event
    ) {

        Integer stage =
                event.getEntity()
                        .getPersistentDataContainer()
                        .get(
                                simpleDungeonStageKey,
                                PersistentDataType.INTEGER
                        );

        Byte dungeonMob =
                event.getEntity()
                        .getPersistentDataContainer()
                        .get(
                                simpleDungeonMobKey,
                                PersistentDataType.BYTE
                        );

        if (
                dungeonMob == null
                        || dungeonMob != (byte) 1
                        || stage == null
        ) {
            return;
        }

        /*
         * simple_dungeon 커스텀 몹은
         * 바닐라 아이템을 드롭하지 않는다.
         */
        event.getDrops().clear();

        StageRuntime runtime =
                stageRuntimes.get(stage);

        if (runtime != null) {

            runtime.mobUuids.remove(
                    event.getEntity()
                            .getUniqueId()
            );
        }

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> checkRefill(stage)
                );
    }


    /*
     * =========================================================
     * POPULATION
     * =========================================================
     */

    private void ensureStagePopulation(
            World world,
            int stage
    ) {

        StageRuntime runtime =
                stageRuntimes.get(stage);

        if (runtime == null) {
            return;
        }

        cleanupMobSet(
                world,
                stage,
                runtime
        );

        if (
                runtime.mobUuids.isEmpty()
                        && runtime.state
                                == SpawnState.ACTIVE
        ) {

            beginSpawning(
                    world,
                    stage,
                    0L
            );
        }
    }


    private void checkRefill(
            int stage
    ) {

        StageRuntime runtime =
                stageRuntimes.get(stage);

        if (runtime == null) {
            return;
        }

        if (
                runtime.state
                        != SpawnState.ACTIVE
        ) {
            return;
        }

        World world =
                findDungeonWorld();

        if (world == null) {
            return;
        }

        cleanupMobSet(
                world,
                stage,
                runtime
        );

        int alive =
                runtime.mobUuids.size();

        if (
                alive
                        <= REFILL_THRESHOLD
        ) {

            beginSpawning(
                    world,
                    stage,
                    REFILL_DELAY_TICKS
            );
        }
    }


    private void beginSpawning(
            World world,
            int stage,
            long delay
    ) {

        StageRuntime runtime =
                stageRuntimes.get(stage);

        if (
                runtime == null
                        || runtime.state
                                == SpawnState.SPAWNING
        ) {
            return;
        }

        runtime.state =
                SpawnState.SPAWNING;

        if (runtime.spawnTask != null) {
            runtime.spawnTask.cancel();
        }

        runtime.spawnTask =
                Bukkit.getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> attemptFill(
                                        world,
                                        stage
                                ),
                                delay
                        );
    }


    private void attemptFill(
            World world,
            int stage
    ) {

        StageRuntime runtime =
                stageRuntimes.get(stage);

        if (runtime == null) {
            return;
        }

        runtime.spawnTask =
                null;

        if (!hasAnyDungeonPlayer()) {

            runtime.state =
                    SpawnState.ACTIVE;

            return;
        }

        cleanupMobSet(
                world,
                stage,
                runtime
        );

        int needed =
                MAX_MOBS
                        - runtime.mobUuids.size();

        if (needed <= 0) {

            runtime.state =
                    SpawnState.ACTIVE;

            return;
        }

        StageConfig config =
                getStage(stage);

        if (config == null) {

            runtime.state =
                    SpawnState.ACTIVE;

            return;
        }

        java.util.List<Location> slots =
                new java.util.ArrayList<>(
                        createSpawnSlots(
                                world,
                                config.spawn()
                        )
                );

        /*
         * Stage 4는 일반몹 34 + 엘리트 2.
         *
         * 전체 보충이 발동한 시점에만 검사한다.
         * 엘리트가 살아 있으면 추가 생성하지 않는다.
         */
        if (stage == 4) {

            ensureStageFourElites(
                    world,
                    stage,
                    runtime,
                    slots
            );

            cleanupMobSet(
                    world,
                    stage,
                    runtime
            );

            /*
             * 엘리트 생성 결과를 반영하여
             * 남은 일반몹 생성 가능 수를 다시 계산한다.
             */
            needed =
                    MAX_MOBS
                            - runtime.mobUuids.size();

            if (needed <= 0) {

                runtime.state =
                        SpawnState.ACTIVE;

                return;
            }
        }

        Map<EntityType, Integer> aliveByType =
                countAliveByType(
                        runtime
                );

        java.util.List<MobDefinition> spawnPlan =
                new java.util.ArrayList<>();

        for (
                MobDefinition definition
                : getStageMobs(stage)
        ) {

            int aliveOfType =
                    aliveByType.getOrDefault(
                            definition.type(),
                            0
                    );

            int deficit =
                    Math.max(
                            0,
                            definition.count()
                                    - aliveOfType
                    );

            for (int i = 0; i < deficit; i++) {

                spawnPlan.add(
                        definition
                );
            }
        }

        int planLimit =
                Math.min(
                        needed,
                        spawnPlan.size()
                );

        int spawned =
                0;

        int planIndex =
                0;

        for (
                Location location
                : slots
        ) {

            if (
                    spawned >= planLimit
                            || planIndex >= spawnPlan.size()
            ) {
                break;
            }

            if (
                    !isSafeSpawnLocation(
                            location
                    )
            ) {
                continue;
            }

            MobDefinition definition =
                    spawnPlan.get(
                            planIndex
                    );

            Entity entity =
                    world.spawnEntity(
                            location,
                            definition.type(),
                            CreatureSpawnEvent
                                    .SpawnReason
                                    .CUSTOM
                    );

            if (
                    !(entity
                            instanceof LivingEntity mob)
            ) {

                entity.remove();

                planIndex++;

                continue;
            }

            configureDungeonMob(
                    mob,
                    stage,
                    definition
            );

            runtime.mobUuids.add(
                    mob.getUniqueId()
            );

            spawned++;
            planIndex++;
        }

        cleanupMobSet(
                world,
                stage,
                runtime
        );

        if (
                runtime.mobUuids.size()
                        >= MAX_MOBS
        ) {

            runtime.state =
                    SpawnState.ACTIVE;

            return;
        }

        /*
         * 안전하지 않아 생성하지 못한 슬롯만
         * 다음 1초 주기에서 다시 시도한다.
         *
         * SPAWNING 상태는 그대로 유지하므로
         * 별도 보충 작업이 중복 생성되지 않는다.
         */
        runtime.spawnTask =
                Bukkit.getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> attemptFill(
                                        world,
                                        stage
                                ),
                                RETRY_DELAY_TICKS
                        );
    }


    /*
     * =========================================================
     * 4 DIRECTIONS x 3x3 = 36 SPAWN SLOTS
     * =========================================================
     */

    private Set<Location> createSpawnSlots(
            World world,
            LocationData center
    ) {

        Set<Location> locations =
                new HashSet<>();

        int centerX =
                (int) Math.floor(
                        center.x()
                );

        int centerZ =
                (int) Math.floor(
                        center.z()
                );

        int spawnY =
                261;

        int[][] directions = {
                {0, -20},
                {0, 20},
                {-20, 0},
                {20, 0}
        };

        for (int[] direction : directions) {

            int baseX =
                    centerX
                            + direction[0];

            int baseZ =
                    centerZ
                            + direction[1];

            for (int dx = -1; dx <= 1; dx++) {

                for (int dz = -1; dz <= 1; dz++) {

                    locations.add(
                            new Location(
                                    world,
                                    baseX + dx + 0.5D,
                                    spawnY,
                                    baseZ + dz + 0.5D
                            )
                    );
                }
            }
        }

        return locations;
    }


    private boolean isSafeSpawnLocation(
            Location location
    ) {

        /*
         * 플레이어와 2블록 이내면 이번 시도에서는 보류.
         */
        for (
                Player player
                : location.getWorld()
                        .getPlayers()
        ) {

            if (
                    player.getLocation()
                            .distanceSquared(
                                    location
                            )
                            < PLAYER_SAFE_DISTANCE_SQUARED
            ) {
                return false;
            }
        }

        /*
         * 이미 LivingEntity가 해당 지점에 있으면 보류.
         */
        boolean occupied =
                !location.getWorld()
                        .getNearbyLivingEntities(
                                location,
                                0.8D,
                                1.5D,
                                0.8D
                        )
                        .isEmpty();

        return !occupied;
    }


    private void configureDungeonMob(
            LivingEntity mob,
            int stage,
            MobDefinition definition
    ) {

        mob.setPersistent(
                true
        );

        mob.setRemoveWhenFarAway(
                false
        );

        mob.setCanPickupItems(
                false
        );

        /*
         * 오버월드에서도 Piglin 계열이
         * 좀비화되지 않도록 고정한다.
         */
        if (mob instanceof Piglin piglin) {

            piglin.setImmuneToZombification(
                    true
            );
        }

        if (mob instanceof PiglinBrute brute) {

            brute.setImmuneToZombification(
                    true
            );
        }

        if (mob instanceof Zombie zombie) {

            zombie.setBaby(
                    false
            );
        }

        disableSunBurn(
                mob
        );

        setAttribute(
                mob,
                Attribute.MAX_HEALTH,
                definition.health()
        );

        setAttribute(
                mob,
                Attribute.ATTACK_DAMAGE,
                definition.attackDamage()
        );

        setAttribute(
                mob,
                Attribute.ARMOR,
                definition.armor()
        );

        mob.setHealth(
                definition.health()
        );

        mob.getPersistentDataContainer()
                .set(
                        simpleDungeonMobKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        mob.getPersistentDataContainer()
                .set(
                        simpleDungeonStageKey,
                        PersistentDataType.INTEGER,
                        stage
                );

        mob.getPersistentDataContainer()
                .set(
                        simpleDungeonExpKey,
                        PersistentDataType.INTEGER,
                        definition.experience()
                );
    }


    private void setAttribute(
            LivingEntity mob,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                mob.getAttribute(
                        attribute
                );

        if (instance != null) {

            instance.setBaseValue(
                    value
            );
        }
    }


    private Map<EntityType, Integer> countAliveByType(
            StageRuntime runtime
    ) {

        Map<EntityType, Integer> result =
                new EnumMap<>(
                        EntityType.class
                );

        for (
                UUID uuid
                : runtime.mobUuids
        ) {

            Entity entity =
                    Bukkit.getEntity(
                            uuid
                    );

            if (
                    entity == null
                            || !entity.isValid()
                            || entity.isDead()
            ) {
                continue;
            }

            String eliteId =
                    entity.getPersistentDataContainer()
                            .get(
                                    eliteMobKey,
                                    PersistentDataType.STRING
                            );

            if (
                    com.hcs.rpgcore.mob.RedstoneGolemSpawnService
                            .REDSTONE_GOLEM_ID
                            .equals(eliteId)
                            || com.hcs.rpgcore.mob.EliteDemonKnightListener
                                    .DEMON_KNIGHT_ID
                                    .equals(eliteId)
            ) {
                continue;
            }

            result.merge(
                    entity.getType(),
                    1,
                    Integer::sum
            );
        }

        return result;
    }


    /*
     * =========================================================
     * STAGE 4 ELITE MOBS
     * =========================================================
     */

    private void ensureStageFourElites(
            World world,
            int stage,
            StageRuntime runtime,
            java.util.List<Location> slots
    ) {

        if (
                countElite(
                        runtime,
                        com.hcs.rpgcore.mob.RedstoneGolemSpawnService
                                .REDSTONE_GOLEM_ID
                ) <= 0
        ) {

            spawnEliteFromSafeSlot(
                    world,
                    stage,
                    runtime,
                    slots,
                    com.hcs.rpgcore.mob.RedstoneGolemSpawnService
                            .REDSTONE_GOLEM_ID,
                    120
            );
        }

        if (
                runtime.mobUuids.size()
                        >= MAX_MOBS
        ) {
            return;
        }

        if (
                countElite(
                        runtime,
                        com.hcs.rpgcore.mob.EliteDemonKnightListener
                                .DEMON_KNIGHT_ID
                ) <= 0
        ) {

            spawnEliteFromSafeSlot(
                    world,
                    stage,
                    runtime,
                    slots,
                    com.hcs.rpgcore.mob.EliteDemonKnightListener
                            .DEMON_KNIGHT_ID,
                    180
            );
        }
    }


    private int countElite(
            StageRuntime runtime,
            String eliteId
    ) {

        int count =
                0;

        for (
                UUID uuid
                : runtime.mobUuids
        ) {

            Entity entity =
                    Bukkit.getEntity(
                            uuid
                    );

            if (
                    entity == null
                            || !entity.isValid()
                            || entity.isDead()
            ) {
                continue;
            }

            String currentEliteId =
                    entity.getPersistentDataContainer()
                            .get(
                                    eliteMobKey,
                                    PersistentDataType.STRING
                            );

            if (
                    eliteId.equals(
                            currentEliteId
                    )
            ) {
                count++;
            }
        }

        return count;
    }


    private boolean spawnEliteFromSafeSlot(
            World world,
            int stage,
            StageRuntime runtime,
            java.util.List<Location> slots,
            String eliteId,
            int experience
    ) {

        if (
                runtime.mobUuids.size()
                        >= MAX_MOBS
        ) {
            return false;
        }

        for (
                Location location
                : slots
        ) {

            if (
                    !isSafeSpawnLocation(
                            location
                    )
            ) {
                continue;
            }

            LivingEntity elite =
                    spawnElite(
                            world,
                            location,
                            runtime,
                            eliteId
                    );

            if (elite == null) {
                continue;
            }

            configureSimpleDungeonElite(
                    elite,
                    stage,
                    experience
            );

            runtime.mobUuids.add(
                    elite.getUniqueId()
            );

            return true;
        }

        return false;
    }


    private LivingEntity spawnElite(
            World world,
            Location location,
            StageRuntime runtime,
            String eliteId
    ) {

        Set<UUID> before =
                new HashSet<>();

        for (
                Entity entity
                : world.getNearbyEntities(
                        location,
                        3.0D,
                        3.0D,
                        3.0D
                )
        ) {

            String currentEliteId =
                    entity.getPersistentDataContainer()
                            .get(
                                    eliteMobKey,
                                    PersistentDataType.STRING
                            );

            if (
                    eliteId.equals(
                            currentEliteId
                    )
            ) {

                before.add(
                        entity.getUniqueId()
                );
            }
        }


        if (
                com.hcs.rpgcore.mob.RedstoneGolemSpawnService
                        .REDSTONE_GOLEM_ID
                        .equals(eliteId)
        ) {

            boolean success =
                    redstoneGolemSpawnService
                            .spawnForAdmin(
                                    location
                            );

            if (!success) {
                return null;
            }

        } else if (
                com.hcs.rpgcore.mob.EliteDemonKnightListener
                        .DEMON_KNIGHT_ID
                        .equals(eliteId)
        ) {

            eliteDemonKnightListener
                    .spawnForAdmin(
                            location
                    );

        } else {

            return null;
        }


        /*
         * 기존 spawnForAdmin은 관리 명령용이라
         * 생성 엔티티 자체를 반환하지 않는다.
         *
         * 같은 위치 주변에서 방금 새로 생긴
         * elite_mob PDC 엔티티를 찾아 추적한다.
         */
        LivingEntity result =
                null;

        double nearestDistance =
                Double.MAX_VALUE;

        for (
                Entity entity
                : world.getNearbyEntities(
                        location,
                        3.0D,
                        3.0D,
                        3.0D
                )
        ) {

            if (
                    before.contains(
                            entity.getUniqueId()
                    )
            ) {
                continue;
            }

            if (
                    !(entity
                            instanceof LivingEntity living)
            ) {
                continue;
            }

            String currentEliteId =
                    entity.getPersistentDataContainer()
                            .get(
                                    eliteMobKey,
                                    PersistentDataType.STRING
                            );

            if (
                    !eliteId.equals(
                            currentEliteId
                    )
            ) {
                continue;
            }

            double distance =
                    entity.getLocation()
                            .distanceSquared(
                                    location
                            );

            if (distance < nearestDistance) {

                nearestDistance =
                        distance;

                result =
                        living;
            }
        }

        return result;
    }


    private void configureSimpleDungeonElite(
            LivingEntity elite,
            int stage,
            int experience
    ) {

        /*
         * 원래 엘리트 설정/BetterModel/PDC는 그대로 유지한다.
         * simple_dungeon용 추적 정보만 추가한다.
         */
        elite.setPersistent(
                true
        );

        elite.setRemoveWhenFarAway(
                false
        );

        elite.setCanPickupItems(
                false
        );

        disableSunBurn(
                elite
        );

        elite.getPersistentDataContainer()
                .set(
                        simpleDungeonMobKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        elite.getPersistentDataContainer()
                .set(
                        simpleDungeonStageKey,
                        PersistentDataType.INTEGER,
                        stage
                );

        elite.getPersistentDataContainer()
                .set(
                        simpleDungeonExpKey,
                        PersistentDataType.INTEGER,
                        experience
                );
    }


    /*
     * =========================================================
     * COMMON DUNGEON MOB RULES
     * =========================================================
     */

    private void disableSunBurn(
            org.bukkit.entity.LivingEntity entity
    ) {

        /*
         * Zombie 계열:
         * Zombie, Husk, Drowned 등
         */
        if (
                entity
                        instanceof org.bukkit.entity.Zombie zombie
        ) {

            zombie.setShouldBurnInDay(
                    false
            );
        }

        /*
         * Skeleton 계열:
         * Skeleton, Stray 등
         */
        if (
                entity
                        instanceof org.bukkit.entity.AbstractSkeleton skeleton
        ) {

            skeleton.setShouldBurnInDay(
                    false
            );
        }
    }


    /*
     * =========================================================
     * MAINTENANCE / RESET
     * =========================================================
     */

    private void startMaintenanceTask() {

        maintenanceTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                plugin,
                                () -> {

                                    if (
                                            !hasAnyDungeonPlayer()
                                    ) {

                                        if (
                                                hasRuntimeActivity()
                                        ) {
                                            resetDungeon();
                                        }
                                    }

                                },
                                20L,
                                20L
                        );
    }


    private boolean hasAnyDungeonPlayer() {

        for (
                Player player
                : Bukkit.getOnlinePlayers()
        ) {

            if (
                    isInsideDungeon(
                            player.getLocation()
                    )
            ) {
                return true;
            }
        }

        return false;
    }


    private boolean hasRuntimeActivity() {

        if (!playerStages.isEmpty()) {
            return true;
        }

        for (
                StageRuntime runtime
                : stageRuntimes.values()
        ) {

            if (
                    !runtime.mobUuids.isEmpty()
                            || runtime.spawnTask != null
                            || runtime.state
                                    == SpawnState.SPAWNING
            ) {
                return true;
            }
        }

        return false;
    }


    private void resetDungeon() {

        playerStages.clear();

        World world =
                findDungeonWorld();

        for (
                StageRuntime runtime
                : stageRuntimes.values()
        ) {

            if (runtime.spawnTask != null) {

                runtime.spawnTask.cancel();

                runtime.spawnTask =
                        null;
            }

            runtime.mobUuids.clear();

            runtime.state =
                    SpawnState.ACTIVE;
        }

        if (world == null) {
            return;
        }

        for (
                Entity entity
                : world.getEntities()
        ) {

            Byte value =
                    entity.getPersistentDataContainer()
                            .get(
                                    simpleDungeonMobKey,
                                    PersistentDataType.BYTE
                            );

            if (
                    value != null
                            && value == (byte) 1
            ) {

                entity.remove();
            }
        }
    }


    private void cleanupMobSet(
            World world,
            int stage,
            StageRuntime runtime
    ) {

        runtime.mobUuids.removeIf(
                uuid -> {

                    Entity entity =
                            Bukkit.getEntity(uuid);

                    if (
                            entity == null
                                    || !entity.isValid()
                                    || entity.isDead()
                    ) {
                        return true;
                    }

                    Integer entityStage =
                            entity.getPersistentDataContainer()
                                    .get(
                                            simpleDungeonStageKey,
                                            PersistentDataType.INTEGER
                                    );

                    return entityStage == null
                            || entityStage != stage;
                }
        );
    }


    private World findDungeonWorld() {

        for (
                Player player
                : Bukkit.getOnlinePlayers()
        ) {

            if (
                    isInsideDungeon(
                            player.getLocation()
                    )
            ) {
                return player.getWorld();
            }
        }

        if (
                !Bukkit.getWorlds()
                        .isEmpty()
        ) {

            return Bukkit.getWorlds()
                    .get(0);
        }

        return null;
    }


    /*
     * =========================================================
     * AREA
     * =========================================================
     */

    private boolean isInsideDungeon(
            Location location
    ) {

        if (location == null) {
            return false;
        }

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();

        return x >= DUNGEON_MIN_X
                && x <= DUNGEON_MAX_X
                && y >= DUNGEON_MIN_Y
                && y <= DUNGEON_MAX_Y
                && z >= DUNGEON_MIN_Z
                && z <= DUNGEON_MAX_Z;
    }


    private boolean sameBlock(
            Location a,
            Location b
    ) {

        return a.getWorld()
                .equals(
                        b.getWorld()
                )
                && a.getBlockX()
                        == b.getBlockX()
                && a.getBlockY()
                        == b.getBlockY()
                && a.getBlockZ()
                        == b.getBlockZ();
    }


    /*
     * =========================================================
     * INTERNAL DATA
     * =========================================================
     */

    private enum SpawnState {
        ACTIVE,
        SPAWNING
    }


    private static final class StageRuntime {

        private SpawnState state =
                SpawnState.ACTIVE;

        private final Set<UUID> mobUuids =
                new HashSet<>();

        private BukkitTask spawnTask;
    }


    private record StageConfig(
            int stage,
            LocationData spawn,
            Cuboid nextGate,
            Cuboid exitGate
    ) {
    }


    private record MobDefinition(
            EntityType type,
            int count,
            double health,
            double attackDamage,
            double armor,
            int experience
    ) {
    }


    private record LocationData(
            double x,
            double y,
            double z
    ) {

        private Location toLocation(
                World world
        ) {

            return new Location(
                    world,
                    x,
                    y,
                    z
            );
        }
    }


    private record Cuboid(
            int x1,
            int y1,
            int z1,
            int x2,
            int y2,
            int z2
    ) {

        private boolean contains(
                Location location
        ) {

            if (location == null) {
                return false;
            }

            int x =
                    location.getBlockX();

            int y =
                    location.getBlockY();

            int z =
                    location.getBlockZ();

            return x >= Math.min(x1, x2)
                    && x <= Math.max(x1, x2)
                    && y >= Math.min(y1, y2)
                    && y <= Math.max(y1, y2)
                    && z >= Math.min(z1, z2)
                    && z <= Math.max(z1, z2);
        }
    }
}
