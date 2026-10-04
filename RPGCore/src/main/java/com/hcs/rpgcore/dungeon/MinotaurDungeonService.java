package com.hcs.rpgcore.dungeon;

import java.time.Duration;

import java.util.HashMap;
import java.util.HashSet;
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

import org.bukkit.block.Block;

import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Zombie;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import org.bukkit.event.entity.CreatureSpawnEvent;

import com.hcs.rpgcore.mob.BossMobKeys;
import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


public final class MinotaurDungeonService {

    private static final long CLEAR_EXP =
            7000L;


    public static final String DUNGEON_ID =
            "minotaur_labyrinth";


    /*
     * =========================================================
     * ENTRY GATE
     * =========================================================
     *
     * 입장 직후 생성되는 23블록 철창.
     *
     * 2000127 193 2000195
     * ~
     * 2000149 193 2000195
     */

    private static final int ENTRY_GATE_MIN_X =
            2000127;

    private static final int ENTRY_GATE_MAX_X =
            2000149;

    private static final int ENTRY_GATE_Y =
            193;

    private static final int ENTRY_GATE_Z =
            2000195;


    /*
     * =========================================================
     * WAVE START TRIGGER
     * =========================================================
     *
     * 2000133 188 2000226
     * ~
     * 2000143 188 2000223
     */

    private static final int WAVE_TRIGGER_MIN_X =
            2000133;

    private static final int WAVE_TRIGGER_MAX_X =
            2000143;

    private static final int WAVE_TRIGGER_Y =
            188;

    private static final int WAVE_TRIGGER_MIN_Z =
            2000223;

    private static final int WAVE_TRIGGER_MAX_Z =
            2000226;


    /*
     * 창살 1:
     *
     * 2000133 188 2000213
     * ~
     * 2000143 188 2000213
     */

    private static final int WAVE_GATE_1_MIN_X =
            2000133;

    private static final int WAVE_GATE_1_MAX_X =
            2000143;

    private static final int WAVE_GATE_1_Y =
            188;

    private static final int WAVE_GATE_1_Z =
            2000213;


    /*
     * 창살 2:
     *
     * 2000136 188 2000261
     * ~
     * 2000140 188 2000261
     */

    private static final int WAVE_GATE_2_MIN_X =
            2000136;

    private static final int WAVE_GATE_2_MAX_X =
            2000140;

    private static final int WAVE_GATE_2_Y =
            188;

    private static final int WAVE_GATE_2_Z =
            2000261;


    /*
     * =========================================================
     * BOSS START TRIGGER
     * =========================================================
     *
     * 2000127 188 2000276
     * ~
     * 2000149 188 2000276
     */

    private static final int BOSS_TRIGGER_MIN_X =
            2000127;

    private static final int BOSS_TRIGGER_MAX_X =
            2000149;

    private static final int BOSS_TRIGGER_Y =
            188;

    private static final int BOSS_TRIGGER_Z =
            2000276;


    /*
     * =========================================================
     * MINOTAUR BOSS
     * =========================================================
     */

    public static final String BOSS_ID =
            "minotaur";

    private static final String MODEL_ID =
            "minotaur";

    private static final String BOSS_NAME =
            "[미궁의 주인] 미노타우로스";


    private static final double BOSS_SPAWN_X =
            2000138.0D;

    private static final double BOSS_SPAWN_Y =
            188.0D;

    private static final double BOSS_SPAWN_Z =
            2000292.0D;


    /*
     * 실제 Paper Entity HP와 RPG 실질 HP를 분리한다.
     */
    public static final double BOSS_VANILLA_HEALTH =
            1024.0D;

    public static final double BOSS_EFFECTIVE_HEALTH =
            1600.0D;


    private static final double BOSS_ATTACK_DAMAGE =
            50.0D;

    private static final double BOSS_ARMOR =
            25.0D;

    private static final double BOSS_MOVEMENT_SPEED =
            0.28D;

    private static final double BOSS_KNOCKBACK_RESISTANCE =
            1.0D;


    /*
     * 보스 소환 위치를 중심으로 수평 반경 15블록.
     */
    private static final double BOSS_COMBAT_RADIUS =
            15.0D;

    private static final double BOSS_COMBAT_RADIUS_SQUARED =
            BOSS_COMBAT_RADIUS
                    * BOSS_COMBAT_RADIUS;


    /*
     * =========================================================
     * FIXED WAVE SPAWN LOCATIONS
     * =========================================================
     *
     * 지정된 좌표를 그대로 사용한다.
     * X/Z 0.5 중앙 보정을 하지 않는다.
     */

    private static final double[][] WAVE_1_SPAWNS = {
            {2000135.0D, 188.0D, 2000229.0D},
            {2000138.0D, 188.0D, 2000229.0D},
            {2000141.0D, 188.0D, 2000229.0D},

            {2000135.0D, 188.0D, 2000233.0D},
            {2000138.0D, 188.0D, 2000233.0D},
            {2000141.0D, 188.0D, 2000233.0D}
    };


    private static final double[][] WAVE_2_SPAWNS = {
            {2000135.0D, 188.0D, 2000237.0D},
            {2000138.0D, 188.0D, 2000237.0D},
            {2000141.0D, 188.0D, 2000237.0D},

            {2000135.0D, 188.0D, 2000241.0D},
            {2000138.0D, 188.0D, 2000241.0D},
            {2000141.0D, 188.0D, 2000241.0D}
    };


    private static final double[][] WAVE_3_SPAWNS = {
            {2000135.0D, 188.0D, 2000245.0D},
            {2000138.0D, 188.0D, 2000245.0D},
            {2000141.0D, 188.0D, 2000245.0D},

            {2000135.0D, 188.0D, 2000249.0D},
            {2000138.0D, 188.0D, 2000249.0D},
            {2000141.0D, 188.0D, 2000249.0D}
    };


    /*
     * =========================================================
     * MINOTAUR DUNGEON FULL BOUNDARY
     * =========================================================
     *
     * WorldGuard region:
     * mino_dungeon_no_fire
     *
     * 이 전체 영역에서는 RPGCore CUSTOM spawn만 허용하고
     * 자연 생성 등 일반 몬스터 spawn은 차단한다.
     */

    private static final int DUNGEON_BOUNDARY_MIN_X =
            2000073;

    private static final int DUNGEON_BOUNDARY_MIN_Y =
            169;

    private static final int DUNGEON_BOUNDARY_MIN_Z =
            2000116;

    private static final int DUNGEON_BOUNDARY_MAX_X =
            2000200;

    private static final int DUNGEON_BOUNDARY_MAX_Y =
            362;

    private static final int DUNGEON_BOUNDARY_MAX_Z =
            2000376;


    private static final long TRIGGER_POLL_PERIOD =
            2L;


    private final JavaPlugin plugin;

    private final NamespacedKey dungeonIdKey;

    private final NamespacedKey dungeonWaveKey;

    private final NamespacedKey bossMobKey;


    private final Set<UUID> participants =
            new HashSet<>();

    private final Set<UUID> activeParticipants =
            new HashSet<>();

    private final Set<UUID> activeDungeonMobs =
            new HashSet<>();


    /*
     * =========================================================
     * AUTHORIZED DUNGEON SPAWN
     * =========================================================
     *
     * SpawnReason.CUSTOM 자체를 신뢰하지 않는다.
     *
     * MinotaurDungeonService가 현재 직접 생성하려는
     * 정확한 타입 + 정확한 위치의 엔티티 하나만 허용한다.
     */
    private boolean authorizedSpawnPending =
            false;

    private Class<? extends LivingEntity>
            authorizedSpawnClass;

    private Location authorizedSpawnLocation;

    /*
     * 사망/로그아웃으로 즉시 퇴장 위치로 이동할 수 없는
     * 참가자.
     *
     * resetDungeon()에서 지우지 않는다.
     * 리스폰/재접속 시 소비한다.
     */
    private final Set<UUID> pendingExitPlayers =
            new HashSet<>();


    /*
     * =========================================================
     * BOSS FLOOR FALL RECOVERY
     * =========================================================
     *
     * 보스전 중 Y < 188로 떨어진 플레이어는
     * 5초 후 보스 스폰 위치로 복귀시킨다.
     *
     * 플레이어당 하나의 복귀 예약만 허용한다.
     */
    private final Set<UUID> fallRecoveryPending =
            new HashSet<>();


    private final Map<UUID, Location> returnLocations =
            new HashMap<>();


    private MinotaurDungeonState state =
            MinotaurDungeonState.IDLE;


    private World dungeonWorld;

    private DungeonRewardService dungeonRewardService;

    private BukkitTask triggerPollingTask;

    private BukkitTask sequenceTask;

    private Ravager activeBoss;

    private BossBar bossBar;

    private BukkitTask bossBarUpdateTask;

    private BukkitTask dungeonExitTask;


    public MinotaurDungeonService(
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

        this.bossMobKey =
                new NamespacedKey(
                        plugin,
                        BossMobKeys.BOSS_MOB_KEY
                );

        startTriggerPolling();
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
            Player player,
            Location returnLocation
    ) {

        UUID uuid =
                player.getUniqueId();


        if (state != MinotaurDungeonState.IDLE) {

            player.sendMessage(
                    Component.text(
                            "[던전] 현재 미궁은 진행 중입니다.",
                            NamedTextColor.RED
                    )
            );

            if (returnLocation != null) {

                player.teleport(
                        returnLocation
                );
            }

            return;
        }


        participants.add(
                uuid
        );

        activeParticipants.add(
                uuid
        );


        if (returnLocation != null) {

            returnLocations.put(
                    uuid,
                    returnLocation.clone()
            );
        }


        dungeonWorld =
                player.getWorld();


        state =
                MinotaurDungeonState.ENTRY_INTRO;


        /*
         * 플레이어를 던전 내부에 가둔 뒤
         * 입장 전용 Subtitle 연출을 시작한다.
         */
        setEntryGate(
                Material.ANVIL
        );


        runEntryIntro();
    }


    public boolean isParticipant(
            UUID uuid
    ) {

        return participants.contains(
                uuid
        );
    }


    /*
     * =========================================================
     * BOSS FLOOR FALL RECOVERY
     * =========================================================
     */
    public void scheduleBossFloorRecovery(
            Player player
    ) {

        if (player == null) {
            return;
        }


        UUID uuid =
                player.getUniqueId();


        if (
                state
                        != MinotaurDungeonState.BOSS_FIGHT
        ) {
            return;
        }


        if (!participants.contains(uuid)) {
            return;
        }


        if (player.getLocation().getY() >= 188.0D) {
            return;
        }


        if (!fallRecoveryPending.add(uuid)) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            fallRecoveryPending.remove(
                                    uuid
                            );


                            if (
                                    state
                                            != MinotaurDungeonState.BOSS_FIGHT
                            ) {
                                return;
                            }


                            if (!participants.contains(uuid)) {
                                return;
                            }


                            if (!player.isOnline()) {
                                return;
                            }


                            if (dungeonWorld == null) {
                                return;
                            }


                            player.teleport(
                                    new Location(
                                            dungeonWorld,
                                            2000138.0D,
                                            188.0D,
                                            2000292.0D
                                    )
                            );
                        },
                        100L
                );
    }


    /*
     * =========================================================
     * FULL DUNGEON SPAWN CONTROL BOUNDARY
     * =========================================================
     */

    public boolean isInsideDungeonBoundary(
            Location location
    ) {

        if (location == null) {
            return false;
        }

        if (dungeonWorld != null
                && location.getWorld() != dungeonWorld) {

            return false;
        }

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();

        return x >= DUNGEON_BOUNDARY_MIN_X
                && x <= DUNGEON_BOUNDARY_MAX_X
                && y >= DUNGEON_BOUNDARY_MIN_Y
                && y <= DUNGEON_BOUNDARY_MAX_Y
                && z >= DUNGEON_BOUNDARY_MIN_Z
                && z <= DUNGEON_BOUNDARY_MAX_Z;
    }


    /*
     * =========================================================
     * ENTRY INTRO
     * =========================================================
     */

    public boolean consumeAuthorizedDungeonSpawn(
            CreatureSpawnEvent event
    ) {

        if (!authorizedSpawnPending) {
            return false;
        }

        if (
                event.getSpawnReason()
                        != CreatureSpawnEvent.SpawnReason.CUSTOM
        ) {
            return false;
        }

        if (
                authorizedSpawnClass == null
                        ||
                !authorizedSpawnClass.isInstance(
                        event.getEntity()
                )
        ) {
            return false;
        }

        if (authorizedSpawnLocation == null) {
            return false;
        }

        Location actual =
                event.getLocation();

        if (
                actual.getWorld()
                        != authorizedSpawnLocation.getWorld()
        ) {
            return false;
        }

        if (
                Double.compare(
                        actual.getX(),
                        authorizedSpawnLocation.getX()
                ) != 0
                        ||
                Double.compare(
                        actual.getY(),
                        authorizedSpawnLocation.getY()
                ) != 0
                        ||
                Double.compare(
                        actual.getZ(),
                        authorizedSpawnLocation.getZ()
                ) != 0
        ) {
            return false;
        }


        /*
         * 정확히 일치하는 spawn event 하나가
         * 승인 토큰을 소비한다.
         */
        authorizedSpawnPending =
                false;

        return true;
    }


    private void runEntryIntro() {

        showSubtitle(
                "미궁의 시련에 발을 들였습니다.",
                NamedTextColor.LIGHT_PURPLE,
                3000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != MinotaurDungeonState.ENTRY_INTRO
                                    ) {
                                        return;
                                    }

                                    showSubtitle(
                                            "시련을 뚫고 앞으로 나아가십시오.",
                                            NamedTextColor.GOLD,
                                            3000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                if (
                                                                        state
                                                                                != MinotaurDungeonState.ENTRY_INTRO
                                                                ) {
                                                                    return;
                                                                }

                                                                clearTitles();

                                                                setEntryGate(
                                                                        Material.AIR
                                                                );

                                                                state =
                                                                        MinotaurDungeonState.WAITING_FOR_WAVE_1_TRIGGER;

                                                                sequenceTask =
                                                                        null;
                                                            },
                                                            60L
                                                    );
                                },
                                60L
                        );
    }


    /*
     * =========================================================
     * TRIGGER POLLING
     * =========================================================
     */

    private void startTriggerPolling() {

        triggerPollingTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::checkProgressTriggers,
                                1L,
                                TRIGGER_POLL_PERIOD
                        );
    }


    private void checkProgressTriggers() {

        if (participants.isEmpty()) {
            return;
        }


        if (
                state
                        == MinotaurDungeonState.WAITING_FOR_WAVE_1_TRIGGER
        ) {

            Player player =
                    firstActivePlayer();

            if (
                    player != null
                            &&
                    isInsideWaveStartTrigger(
                            player.getLocation()
                    )
            ) {

                startWaveSection();
            }

            return;
        }


        if (
                state
                        == MinotaurDungeonState.BOSS_FIGHT
        ) {

            keepBossInsideArena();

            return;
        }


        if (
                state
                        == MinotaurDungeonState.WAITING_FOR_BOSS_TRIGGER
        ) {

            Player player =
                    firstActivePlayer();

            if (
                    player != null
                            &&
                    isInsideBossStartTrigger(
                            player.getLocation()
                    )
            ) {

                startBossIntroPlaceholder();
            }
        }
    }


    /*
     * =========================================================
     * WAVE 1 START
     * =========================================================
     */

    private void startWaveSection() {

        if (
                state
                        != MinotaurDungeonState.WAITING_FOR_WAVE_1_TRIGGER
        ) {
            return;
        }


        /*
         * 웨이브 구간 진입과 동시에
         * 앞/뒤 모루 장벽을 모두 닫는다.
         */
        setWaveGate1(
                Material.ANVIL
        );

        setWaveGate2(
                Material.ANVIL
        );


        state =
                MinotaurDungeonState.WAVE_1_INTRO;


        showSubtitle(
                "미궁의 주인이 침입자를 감지했습니다.",
                NamedTextColor.DARK_RED,
                2000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != MinotaurDungeonState.WAVE_1_INTRO
                                    ) {
                                        return;
                                    }

                                    showSubtitle(
                                            "첫 번째 시련이 시작됩니다.",
                                            NamedTextColor.RED,
                                            2000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                if (
                                                                        state
                                                                                != MinotaurDungeonState.WAVE_1_INTRO
                                                                ) {
                                                                    return;
                                                                }

                                                                clearTitles();

                                                                state =
                                                                        MinotaurDungeonState.WAVE_1;

                                                                spawnWave(
                                                                        1
                                                                );

                                                                sequenceTask =
                                                                        null;
                                                            },
                                                            40L
                                                    );
                                },
                                40L
                        );
    }


    /*
     * =========================================================
     * WAVE SPAWN
     * =========================================================
     */

    private <T extends LivingEntity>
    T spawnAuthorizedDungeonEntity(
            Location location,
            Class<T> entityClass
    ) {

        authorizedSpawnPending =
                true;

        authorizedSpawnClass =
                entityClass;

        authorizedSpawnLocation =
                location.clone();

        try {

            return dungeonWorld.spawn(
                    location,
                    entityClass,
                    CreatureSpawnEvent
                            .SpawnReason
                            .CUSTOM
            );

        } finally {

            authorizedSpawnPending =
                    false;

            authorizedSpawnClass =
                    null;

            authorizedSpawnLocation =
                    null;
        }
    }


    private void spawnWave(
            int wave
    ) {

        if (dungeonWorld == null) {

            plugin.getLogger()
                    .severe(
                            "[MinotaurDungeon] dungeonWorld is null."
                    );

            return;
        }


        activeDungeonMobs.clear();


        double[][] points =
                switch (wave) {

                    case 1 ->
                            WAVE_1_SPAWNS;

                    case 2 ->
                            WAVE_2_SPAWNS;

                    case 3 ->
                            WAVE_3_SPAWNS;

                    default ->
                            new double[0][0];
                };


        for (
                double[] point
                : points
        ) {

            Location location =
                    new Location(
                            dungeonWorld,
                            point[0],
                            point[1],
                            point[2]
                    );


            LivingEntity entity =
                    switch (wave) {

                        case 1 ->
                                spawnAuthorizedDungeonEntity(
                                        location,
                                        Zombie.class
                                );

                        case 2 ->
                                spawnAuthorizedDungeonEntity(
                                        location,
                                        Husk.class
                                );

                        case 3 ->
                                spawnAuthorizedDungeonEntity(
                                        location,
                                        Vindicator.class
                                );

                        default ->
                                null;
                    };


            if (entity == null) {
                continue;
            }


            /*
             * =================================================
             * WAVE COMBAT STATS
             * =================================================
             *
             * 권장 레벨 50 ~ 60 기준.
             *
             * Wave 1 Zombie
             * HP 300 / Attack 20 / Armor 6
             *
             * Wave 2 Husk
             * HP 420 / Attack 26 / Armor 10
             *
             * Wave 3 Vindicator
             * HP 560 / Attack 34 / Armor 14
             */
            switch (wave) {

                case 1 -> {

                    setBaseAttribute(
                            entity,
                            Attribute.MAX_HEALTH,
                            300.0D
                    );

                    setBaseAttribute(
                            entity,
                            Attribute.ATTACK_DAMAGE,
                            20.0D
                    );

                    setBaseAttribute(
                            entity,
                            Attribute.ARMOR,
                            6.0D
                    );

                    entity.setHealth(
                            300.0D
                    );
                }

                case 2 -> {

                    setBaseAttribute(
                            entity,
                            Attribute.MAX_HEALTH,
                            420.0D
                    );

                    setBaseAttribute(
                            entity,
                            Attribute.ATTACK_DAMAGE,
                            26.0D
                    );

                    setBaseAttribute(
                            entity,
                            Attribute.ARMOR,
                            10.0D
                    );

                    entity.setHealth(
                            420.0D
                    );
                }

                case 3 -> {

                    setBaseAttribute(
                            entity,
                            Attribute.MAX_HEALTH,
                            560.0D
                    );

                    setBaseAttribute(
                            entity,
                            Attribute.ATTACK_DAMAGE,
                            34.0D
                    );

                    setBaseAttribute(
                            entity,
                            Attribute.ARMOR,
                            14.0D
                    );

                    entity.setHealth(
                            560.0D
                    );
                }

                default -> {
                }
            }


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


            entity.setPersistent(
                    true
            );

            entity.setRemoveWhenFarAway(
                    false
            );

            entity.setCanPickupItems(
                    false
            );


            activeDungeonMobs.add(
                    entity.getUniqueId()
            );


            /*
             * Wave spawn runtime diagnostic.
             *
             * 생성 직후에는 정상 객체가 반환됐지만
             * 다른 플러그인/이벤트에 의해 제거되는 경우를 확인한다.
             */
            UUID spawnedUuid =
                    entity.getUniqueId();

            double expectedX =
                    point[0];

            double expectedY =
                    point[1];

            double expectedZ =
                    point[2];

            plugin.getServer()
                    .getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> {

                                org.bukkit.entity.Entity spawned =
                                        org.bukkit.Bukkit.getEntity(
                                                spawnedUuid
                                        );

                                if (spawned == null) {

                                    plugin.getLogger()
                                            .warning(
                                                    "[MinotaurDungeon] "
                                                            + "Wave "
                                                            + wave
                                                            + " entity missing after 1s: "
                                                            + spawnedUuid
                                                            + " expected="
                                                            + expectedX
                                                            + ","
                                                            + expectedY
                                                            + ","
                                                            + expectedZ
                                            );

                                    return;
                                }


                                Location actual =
                                        spawned.getLocation();

                                plugin.getLogger()
                                        .info(
                                                "[MinotaurDungeon] "
                                                        + "Wave "
                                                        + wave
                                                        + " entity after 1s: "
                                                        + "uuid="
                                                        + spawnedUuid
                                                        + " type="
                                                        + spawned.getType()
                                                        + " valid="
                                                        + spawned.isValid()
                                                        + " dead="
                                                        + spawned.isDead()
                                                        + " world="
                                                        + actual.getWorld().getName()
                                                        + " xyz="
                                                        + actual.getX()
                                                        + ","
                                                        + actual.getY()
                                                        + ","
                                                        + actual.getZ()
                                                        + " feetBlock="
                                                        + actual.getBlock().getType()
                                                        + " belowBlock="
                                                        + actual.clone()
                                                                .subtract(
                                                                        0.0D,
                                                                        1.0D,
                                                                        0.0D
                                                                )
                                                                .getBlock()
                                                                .getType()
                                        );
                            },
                            20L
                    );
        }


        plugin.getLogger()
                .info(
                        "[MinotaurDungeon] Wave "
                                + wave
                                + " spawned: "
                                + activeDungeonMobs.size()
                                + " / 6"
                );
    }


    /*
     * =========================================================
     * MOB IDENTIFICATION / DEATH
     * =========================================================
     */

    public boolean isDungeonMob(
            LivingEntity entity
    ) {

        String dungeonId =
                entity.getPersistentDataContainer()
                        .get(
                                dungeonIdKey,
                                PersistentDataType.STRING
                        );


        return DUNGEON_ID.equals(
                dungeonId
        );
    }


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

            case WAVE_1 ->
                    finishWave1();

            case WAVE_2 ->
                    finishWave2();

            case WAVE_3 ->
                    finishWave3();

            case BOSS_FIGHT ->
                    finishBoss();

            default -> {
            }
        }
    }


    /*
     * =========================================================
     * WAVE TRANSITIONS
     * =========================================================
     */

    private void finishWave1() {

        state =
                MinotaurDungeonState.WAVE_2_INTRO;


        showSubtitle(
                "첫 번째 시련을 돌파했습니다.",
                NamedTextColor.GREEN,
                2000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != MinotaurDungeonState.WAVE_2_INTRO
                                    ) {
                                        return;
                                    }


                                    showSubtitle(
                                            "두 번째 시련이 시작됩니다.",
                                            NamedTextColor.RED,
                                            2000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                if (
                                                                        state
                                                                                != MinotaurDungeonState.WAVE_2_INTRO
                                                                ) {
                                                                    return;
                                                                }


                                                                clearTitles();

                                                                state =
                                                                        MinotaurDungeonState.WAVE_2;

                                                                spawnWave(
                                                                        2
                                                                );

                                                                sequenceTask =
                                                                        null;
                                                            },
                                                            40L
                                                    );
                                },
                                40L
                        );
    }


    private void finishWave2() {

        state =
                MinotaurDungeonState.WAVE_3_INTRO;


        showSubtitle(
                "두 번째 시련을 돌파했습니다.",
                NamedTextColor.GREEN,
                2000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != MinotaurDungeonState.WAVE_3_INTRO
                                    ) {
                                        return;
                                    }


                                    showSubtitle(
                                            "세 번째 시련이 시작됩니다.",
                                            NamedTextColor.RED,
                                            2000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                if (
                                                                        state
                                                                                != MinotaurDungeonState.WAVE_3_INTRO
                                                                ) {
                                                                    return;
                                                                }


                                                                clearTitles();

                                                                state =
                                                                        MinotaurDungeonState.WAVE_3;

                                                                spawnWave(
                                                                        3
                                                                );

                                                                sequenceTask =
                                                                        null;
                                                            },
                                                            40L
                                                    );
                                },
                                40L
                        );
    }


    private void finishWave3() {

        state =
                MinotaurDungeonState.WAVES_CLEARED_INTRO;


        showSubtitle(
                "미궁의 모든 시련을 극복했습니다.",
                NamedTextColor.GREEN,
                2000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != MinotaurDungeonState.WAVES_CLEARED_INTRO
                                    ) {
                                        return;
                                    }


                                    showSubtitle(
                                            "이제 미궁의 주인을 만나러 가십시오.",
                                            NamedTextColor.GOLD,
                                            2000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                if (
                                                                        state
                                                                                != MinotaurDungeonState.WAVES_CLEARED_INTRO
                                                                ) {
                                                                    return;
                                                                }


                                                                clearTitles();


                                                                /*
                                                                 * 마지막 문구가 모두 끝난 뒤에만
                                                                 * 모루 장벽 2를 제거한다.
                                                                 */
                                                                setWaveGate2(
                                                                        Material.AIR
                                                                );


                                                                state =
                                                                        MinotaurDungeonState.WAITING_FOR_BOSS_TRIGGER;


                                                                sequenceTask =
                                                                        null;
                                                            },
                                                            40L
                                                    );
                                },
                                40L
                        );
    }


    /*
     * =========================================================
     * BOSS INTRO
     * =========================================================
     */

    private void startBossIntroPlaceholder() {

        if (
                state
                        != MinotaurDungeonState.WAITING_FOR_BOSS_TRIGGER
        ) {
            return;
        }


        /*
         * 보스룸 진입 즉시 모루 장벽 2를 다시 닫는다.
         */
        setWaveGate2(
                Material.ANVIL
        );


        state =
                MinotaurDungeonState.BOSS_INTRO;


        showSubtitle(
                "미궁의 주인이 모습을 드러냅니다.",
                NamedTextColor.DARK_RED,
                2000L
        );


        sequenceTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    if (
                                            state
                                                    != MinotaurDungeonState.BOSS_INTRO
                                    ) {
                                        return;
                                    }


                                    showSubtitle(
                                            "미궁의 주인을 처치하십시오.",
                                            NamedTextColor.RED,
                                            3000L
                                    );


                                    sequenceTask =
                                            plugin.getServer()
                                                    .getScheduler()
                                                    .runTaskLater(
                                                            plugin,
                                                            () -> {

                                                                if (
                                                                        state
                                                                                != MinotaurDungeonState.BOSS_INTRO
                                                                ) {
                                                                    return;
                                                                }


                                                                clearTitles();

                                                                spawnMinotaur();

                                                                sequenceTask =
                                                                        null;
                                                            },
                                                            60L
                                                    );
                                },
                                40L
                        );
    }


    /*
     * =========================================================
     * MINOTAUR SPAWN
     * =========================================================
     */

    private void spawnMinotaur() {

        if (
                state
                        != MinotaurDungeonState.BOSS_INTRO
        ) {
            return;
        }


        if (dungeonWorld == null) {

            plugin.getLogger()
                    .severe(
                            "[MinotaurDungeon] "
                                    + "Cannot spawn boss: dungeonWorld is null."
                    );

            return;
        }


        Location spawnLocation =
                new Location(
                        dungeonWorld,
                        BOSS_SPAWN_X,
                        BOSS_SPAWN_Y,
                        BOSS_SPAWN_Z
                );


        Ravager boss =
                spawnAuthorizedDungeonEntity(
                        spawnLocation,
                        Ravager.class
                );


        PersistentDataContainer pdc =
                boss.getPersistentDataContainer();


        pdc.set(
                dungeonIdKey,
                PersistentDataType.STRING,
                DUNGEON_ID
        );


        pdc.set(
                dungeonWaveKey,
                PersistentDataType.INTEGER,
                4
        );


        pdc.set(
                bossMobKey,
                PersistentDataType.STRING,
                BossMobKeys.MINOTAUR_ID
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


        setBaseAttribute(
                boss,
                Attribute.MAX_HEALTH,
                BOSS_VANILLA_HEALTH
        );

        boss.setHealth(
                BOSS_VANILLA_HEALTH
        );


        setBaseAttribute(
                boss,
                Attribute.ATTACK_DAMAGE,
                BOSS_ATTACK_DAMAGE
        );


        setBaseAttribute(
                boss,
                Attribute.ARMOR,
                BOSS_ARMOR
        );


        setBaseAttribute(
                boss,
                Attribute.MOVEMENT_SPEED,
                BOSS_MOVEMENT_SPEED
        );


        setBaseAttribute(
                boss,
                Attribute.KNOCKBACK_RESISTANCE,
                BOSS_KNOCKBACK_RESISTANCE
        );


        boss.setAI(
                true
        );

        boss.setAware(
                true
        );


        Player target =
                firstActivePlayer();

        if (target != null) {

            boss.setTarget(
                    target
            );
        }


        boolean modelAttached =
                attachBetterModel(
                        boss
                );


        plugin.getLogger()
                .info(
                        "[MinotaurDungeon] "
                                + "BetterModel attached="
                                + modelAttached
                );


        if (!modelAttached) {

            boss.remove();

            setWaveGate2(
                    Material.AIR
            );

            state =
                    MinotaurDungeonState.WAITING_FOR_BOSS_TRIGGER;


            broadcastMessage(
                    "미노타우로스 모델을 불러오지 못했습니다.",
                    NamedTextColor.RED
            );


            plugin.getLogger()
                    .warning(
                            "[MinotaurDungeon] "
                                    + "BetterModel model not found: "
                                    + MODEL_ID
                    );

            return;
        }


        activeBoss =
                boss;


        activeDungeonMobs.clear();

        activeDungeonMobs.add(
                boss.getUniqueId()
        );


        state =
                MinotaurDungeonState.BOSS_FIGHT;


        createBossBar(
                boss
        );


        plugin.getLogger()
                .info(
                        "[MinotaurDungeon] "
                                + "Minotaur spawned at "
                                + BOSS_SPAWN_X
                                + ", "
                                + BOSS_SPAWN_Y
                                + ", "
                                + BOSS_SPAWN_Z
                                + " HP="
                                + BOSS_VANILLA_HEALTH
                                + " effectiveHP="
                                + BOSS_EFFECTIVE_HEALTH
                                + " attack="
                                + BOSS_ATTACK_DAMAGE
                                + " armor="
                                + BOSS_ARMOR
                );
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

            plugin.getLogger()
                    .warning(
                            "[MinotaurDungeon] "
                                    + "Attribute unavailable: "
                                    + attribute
                    );

            return;
        }


        instance.setBaseValue(
                value
        );
    }


    /*
     * =========================================================
     * BETTERMODEL
     * =========================================================
     */

    private boolean attachBetterModel(
            LivingEntity entity
    ) {

        ModelRenderer renderer =
                BetterModel.modelOrNull(
                        MODEL_ID
                );


        if (renderer == null) {
            return false;
        }


        renderer.getOrCreate(
                BukkitAdapter.adapt(
                        entity
                )
        );


        return true;
    }


    /*
     * =========================================================
     * BOSS IDENTIFICATION
     * =========================================================
     */

    public boolean isMinotaur(
            LivingEntity entity
    ) {

        if (!isDungeonMob(entity)) {
            return false;
        }


        String bossId =
                entity.getPersistentDataContainer()
                        .get(
                                bossMobKey,
                                PersistentDataType.STRING
                        );


        return BossMobKeys.MINOTAUR_ID
                .equals(
                        bossId
                );
    }


    /*
     * =========================================================
     * BOSS BAR
     * =========================================================
     */

    private void createBossBar(
            Ravager boss
    ) {

        removeBossBar();


        bossBar =
                plugin.getServer()
                        .createBossBar(
                                BOSS_NAME,
                                BarColor.RED,
                                BarStyle.SOLID
                        );


        bossBar.setProgress(
                1.0D
        );

        bossBar.setVisible(
                true
        );


        for (
                UUID uuid
                : participants
        ) {

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
                                                    ||
                                            !activeBoss.isValid()
                                                    ||
                                            activeBoss.isDead()
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
     * BOSS COMBAT BOUNDS
     * =========================================================
     */

    private void keepBossInsideArena() {

        if (
                activeBoss == null
                        ||
                !activeBoss.isValid()
                        ||
                activeBoss.isDead()
        ) {
            return;
        }


        Location current =
                activeBoss.getLocation();


        double dx =
                current.getX()
                        - BOSS_SPAWN_X;

        double dz =
                current.getZ()
                        - BOSS_SPAWN_Z;


        double horizontalDistanceSquared =
                dx * dx
                        + dz * dz;


        if (
                horizontalDistanceSquared
                        <= BOSS_COMBAT_RADIUS_SQUARED
        ) {
            return;
        }


        Location center =
                new Location(
                        activeBoss.getWorld(),
                        BOSS_SPAWN_X,
                        BOSS_SPAWN_Y,
                        BOSS_SPAWN_Z,
                        current.getYaw(),
                        current.getPitch()
                );


        activeBoss.teleport(
                center
        );
    }


    /*
     * =========================================================
     * BOSS CLEAR
     * =========================================================
     */

    private void finishBoss() {

        if (
                state
                        != MinotaurDungeonState.BOSS_FIGHT
        ) {
            return;
        }


        state =
                MinotaurDungeonState.CLEARED;


        activeBoss =
                null;


        removeBossBar();

        clearTitles();


        showSubtitle(
                "미궁의 주인을 처치하셨습니다.",
                NamedTextColor.GOLD,
                3000L
        );


        /*
         * =====================================================
         * MINOTAUR CLEAR REWARD
         * =====================================================
         *
         * 다이아몬드 5~8.
         * RPG EXP 없음.
         *
         * CLEARED 전환은 한 번만 가능하므로
         * 보상 역시 한 번만 지급된다.
         */
        if (dungeonRewardService != null) {

            Set<UUID> rewardParticipants =
                    Set.copyOf(
                            participants
                    );

            dungeonRewardService
                    .rewardDungeonClearExperience(
                            rewardParticipants,
                            CLEAR_EXP
                    );

            dungeonRewardService
                    .rewardMinotaurBasicRewards(
                            rewardParticipants
                    );

            /*
             * =================================================
             * ENHANCEMENT STONE REWARD
             * =================================================
             *
             * 미노타우로스 던전 클리어 시
             * 강화석 5개를 확정 지급한다.
             *
             * 추가 확률 보상은 없다.
             */
            dungeonRewardService
                    .rewardEquipmentIngots(
                            rewardParticipants,
                            0.15D,
                            0.10D,
                            0.05D
                    );

            dungeonRewardService
                    .rewardDungeonGold(
                            rewardParticipants,
                            1000L,
                            400L,
                            0.20D
                    );

            dungeonRewardService
                    .rewardEnhancementStones(
                            rewardParticipants,
                            5,
                            0.0D,
                            0,
                            0
                    );


        } else {

            plugin.getLogger()
                    .severe(
                            "[MinotaurDungeon] "
                                    + "DungeonRewardService is null."
                    );
        }


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
                                                    != MinotaurDungeonState.CLEARED
                                    ) {
                                        return;
                                    }


                                    clearTitles();

                                    runDungeonExitCountdown(
                                            5
                                    );
                                },
                                60L
                        );
    }


    private void runDungeonExitCountdown(
            int seconds
    ) {

        if (
                state
                        != MinotaurDungeonState.CLEARED
        ) {
            return;
        }


        if (seconds <= 0) {

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


    private void teleportParticipantsToExit() {

        for (
                UUID uuid
                : participants
        ) {

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


            Location exit =
                    new Location(
                            player.getWorld(),
                            -79.0D,
                            70.0D,
                            -8.0D,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );


            player.teleport(
                    exit
            );
        }
    }


    /*
     * =========================================================
     * PARTICIPANT DEATH / QUIT
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


        if (!participants.contains(uuid)) {
            return;
        }


        /*
         * 클리어 후 퇴장 카운트다운 중이라면
         * 던전을 실패로 되돌리지 않는다.
         */
        if (
                state == MinotaurDungeonState.CLEARED
                        ||
                state == MinotaurDungeonState.RESETTING
                        ||
                state == MinotaurDungeonState.IDLE
        ) {
            return;
        }


        activeParticipants.remove(
                uuid
        );


        pendingExitPlayers.add(
                uuid
        );


        /*
         * 다른 생존 참가자가 있다면
         * 던전 자체는 계속 진행한다.
         */
        if (!activeParticipants.isEmpty()) {
            return;
        }


        failDungeon();
    }


    /*
     * =========================================================
     * DUNGEON FAILURE
     * =========================================================
     */

    private void failDungeon() {

        if (
                state == MinotaurDungeonState.IDLE
                        ||
                state == MinotaurDungeonState.RESETTING
        ) {
            return;
        }


        /*
         * 아직 온라인 상태인 참가자가 있다면
         * 실패 메시지를 보여준다.
         */
        for (
                UUID uuid
                : participants
        ) {

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


            player.sendMessage(
                    Component.text(
                            "[던전] ",
                            NamedTextColor.DARK_PURPLE
                    ).append(
                            Component.text(
                                    "던전 공략에 실패했습니다.",
                                    NamedTextColor.RED
                            )
                    )
            );
        }


        plugin.getLogger()
                .info(
                        "[MinotaurDungeon] Dungeon failed."
                );


        resetDungeon();
    }


    /*
     * =========================================================
     * PENDING EXIT
     * =========================================================
     */

    public boolean consumePendingExit(
            UUID uuid
    ) {

        if (uuid == null) {
            return false;
        }


        return pendingExitPlayers.remove(
                uuid
        );
    }


    public Location createExitLocation(
            World world
    ) {

        if (world == null) {
            return null;
        }


        /*
         * 사용자가 지정한 정확한 퇴장 좌표.
         * 임의 0.5 보정 없음.
         */
        return new Location(
                world,
                -100.0D,
                95.0D,
                -132.0D
        );
    }


    private void resetDungeon() {

        state =
                MinotaurDungeonState.RESETTING;


        if (sequenceTask != null) {

            sequenceTask.cancel();

            sequenceTask =
                    null;
        }


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


        if (dungeonWorld != null) {

            for (
                    UUID uuid
                    : new HashSet<>(
                            activeDungeonMobs
                    )
            ) {

                org.bukkit.entity.Entity entity =
                        plugin.getServer()
                                .getEntity(
                                        uuid
                                );


                if (
                        entity != null
                                &&
                        entity.isValid()
                ) {

                    entity.remove();
                }
            }


            /*
             * 다음 입장에 영향을 주는 임시 모루 장벽을 제거한다.
             */
            setEntryGate(
                    Material.AIR
            );

            setWaveGate1(
                    Material.AIR
            );

            setWaveGate2(
                    Material.AIR
            );
        }


        activeDungeonMobs.clear();

        participants.clear();

        activeParticipants.clear();

        returnLocations.clear();


        dungeonWorld =
                null;


        state =
                MinotaurDungeonState.IDLE;


        plugin.getLogger()
                .info(
                        "[MinotaurDungeon] "
                                + "Dungeon reset complete. State = IDLE"
                );
    }


    private void broadcastMessage(
            String message,
            NamedTextColor color
    ) {

        for (
                UUID uuid
                : participants
        ) {

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


            player.sendMessage(
                    Component.text(
                            message,
                            color
                    )
            );
        }
    }


    /*
     * =========================================================
     * GATES
     * =========================================================
     */

    private void setEntryGate(
            Material material
    ) {

        if (dungeonWorld == null) {
            return;
        }


        for (
                int x = ENTRY_GATE_MIN_X;
                x <= ENTRY_GATE_MAX_X;
                x++
        ) {

            Block block =
                    dungeonWorld.getBlockAt(
                            x,
                            ENTRY_GATE_Y,
                            ENTRY_GATE_Z
                    );

            block.setType(
                    material,
                    false
            );
        }
    }


    private void setWaveGate1(
            Material material
    ) {

        if (dungeonWorld == null) {
            return;
        }


        for (
                int x = WAVE_GATE_1_MIN_X;
                x <= WAVE_GATE_1_MAX_X;
                x++
        ) {

            dungeonWorld.getBlockAt(
                            x,
                            WAVE_GATE_1_Y,
                            WAVE_GATE_1_Z
                    )
                    .setType(
                            material,
                            false
                    );
        }
    }


    private void setWaveGate2(
            Material material
    ) {

        if (dungeonWorld == null) {
            return;
        }


        for (
                int x = WAVE_GATE_2_MIN_X;
                x <= WAVE_GATE_2_MAX_X;
                x++
        ) {

            dungeonWorld.getBlockAt(
                            x,
                            WAVE_GATE_2_Y,
                            WAVE_GATE_2_Z
                    )
                    .setType(
                            material,
                            false
                    );
        }
    }


    /*
     * =========================================================
     * AREA CHECK
     * =========================================================
     */

    private boolean isInsideWaveStartTrigger(
            Location location
    ) {

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();


        return x >= WAVE_TRIGGER_MIN_X
                && x <= WAVE_TRIGGER_MAX_X
                && y == WAVE_TRIGGER_Y
                && z >= WAVE_TRIGGER_MIN_Z
                && z <= WAVE_TRIGGER_MAX_Z;
    }


    private boolean isInsideBossStartTrigger(
            Location location
    ) {

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();


        return x >= BOSS_TRIGGER_MIN_X
                && x <= BOSS_TRIGGER_MAX_X
                && y == BOSS_TRIGGER_Y
                && z == BOSS_TRIGGER_Z;
    }


    /*
     * =========================================================
     * TITLE
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


        for (
                UUID uuid
                : participants
        ) {

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

        for (
                UUID uuid
                : participants
        ) {

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


    private Player firstActivePlayer() {

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
                    player != null
                            &&
                    player.isOnline()
                            &&
                    !player.isDead()
            ) {

                return player;
            }
        }


        return null;
    }
}
