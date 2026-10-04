package com.hcs.rpgcore.dungeon;

import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BarColor;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.Attribute;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZombieDungeonService {

    public static final String DUNGEON_ID =
            "zombie_catacomb";

    /*
     * =========================================================
     * SPAWN AREA
     * =========================================================
     *
     * 일반 웨이브 몬스터 랜덤 스폰 기준점.
     *
     * 묘지 수호자는 아래 BOSS_X / BOSS_Y / BOSS_Z의
     * 별도 고정 좌표에 소환한다.
     */
    private static final double SPAWN_CENTER_X =
            4000046.0D;

    private static final double SPAWN_FEET_Y =
            205.0D;

    private static final double SPAWN_CENTER_Z =
            4000080.0D;

    private static final int SPAWN_RADIUS = 8;

    /*
     * =========================================================
     * WAVE CONFIG
     * =========================================================
     */

    private static final int WAVE_1_COUNT = 6;
    private static final int WAVE_2_COUNT = 10;
    private static final int WAVE_3_COUNT = 14;

    /*
     * 웨이브 사이 대기시간.
     */
    private static final int NEXT_WAVE_COUNTDOWN_SECONDS = 3;

    /*
     * 랜덤 스폰 위치 최소 간격.
     */
    private static final double MIN_SPAWN_DISTANCE =
            2.0D;

    private static final int MAX_SPAWN_ATTEMPTS =
            1000;

    /*
     * =========================================================
     * GRAVE GUARDIAN
     * =========================================================
     */

    private static final String BOSS_ID =
            "grave_guardian";

    private static final String BOSS_NAME =
            "묘지 수호자";

    /*
     * 사용자가 지정한 보스 고정 위치.
     */
    private static final double BOSS_X =
            4000046.0D;

    private static final double BOSS_Y =
            205.0D;

    private static final double BOSS_Z =
            4000080.0D;

    /*
     * RPG에서 사용하는 묘지 수호자의 실질 체력.
     */
    private static final double BOSS_MAX_HEALTH =
            600.0D;

    /*
     * Minecraft/Paper의 LivingEntity 실제 체력 상한 대응.
     *
     * 실제 Entity HP와 RPG 실질 HP를 모두 600으로 사용한다.
     * 피해 보정 비율은 1:1이다.
     */
    public static final double BOSS_VANILLA_HEALTH =
            600.0D;

    public static final double BOSS_EFFECTIVE_HEALTH =
            600.0D;

    private static final double BOSS_ATTACK_DAMAGE =
            25.0D;

    private static final double BOSS_DEFENSE =
            8.0D;

    private static final double BOSS_SCALE =
            3.0D;

    private static final double BOSS_SPEED_MULTIPLIER =
            1.10D;

    private static final double BOSS_KNOCKBACK_RESISTANCE =
            0.80D;


    private final JavaPlugin plugin;

    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey dungeonWaveKey;
    private final NamespacedKey dungeonBossKey;

    private final Set<UUID> participants =
            new HashSet<>();

    /*
     * =========================================================
     * ACTIVE PARTICIPANTS
     * =========================================================
     *
     * participants:
     * 던전에 참가한 전체 플레이어.
     *
     * activeParticipants:
     * 현재 살아 있고 전투 가능한 참가자.
     *
     * 마지막 active participant가 사망하거나
     * 로그아웃하면 던전을 실패 처리한다.
     */
    private final Set<UUID> activeParticipants =
            new HashSet<>();


    /*
     * 플레이어별 던전 입장 직전 위치.
     *
     * 성공/실패 시 이 위치로 복귀시킨다.
     */
    private final Map<UUID, Location> returnLocations =
            new HashMap<>();


    /*
     * 사망 또는 로그아웃 상태여서
     * 즉시 텔레포트하지 못한 플레이어의 복귀 위치.
     *
     * 리스폰 또는 재접속 때 사용한다.
     */
    private final Map<UUID, Location> pendingExitLocations =
            new HashMap<>();


    /*
     * 성공/실패 종료 예약 중복 방지.
     */
    private BukkitTask dungeonResetTask;



    private final Set<UUID> activeDungeonMobs =
            new HashSet<>();

    private ZombieDungeonState state =
            ZombieDungeonState.IDLE;

    private World dungeonWorld;

    private DungeonRewardService dungeonRewardService;

    private Zombie activeBoss;

    private BossBar bossBar;

    private BukkitTask bossBarUpdateTask;

    public ZombieDungeonService(
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


        /*
         * 던전이 종료 처리 중이라면
         * 새로운 참가자를 받지 않는다.
         */
        if (
                state == ZombieDungeonState.CLEARED
                || state == ZombieDungeonState.RESETTING
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


        /*
         * 반드시 던전 입장 전 위치를 보관한다.
         */
        if (returnLocation != null) {

            returnLocations.putIfAbsent(
                    uuid,
                    returnLocation.clone()
            );


            pendingExitLocations.remove(
                    uuid
            );
        }


        /*
         * 이미 던전이 진행 중이라면
         * 추가 웨이브는 생성하지 않는다.
         */
        if (state != ZombieDungeonState.IDLE) {

            player.sendMessage(
                    Component.text(
                            "[던전] ",
                            NamedTextColor.DARK_PURPLE
                    ).append(
                            Component.text(
                                    "현재 진행 중인 폐허가 된 지하묘지에 참가했습니다.",
                                    NamedTextColor.GRAY
                            )
                    )
            );


            return;
        }


        dungeonWorld =
                player.getWorld();


        state =
                ZombieDungeonState.WAVE_1;


        broadcast(
                Component.text(
                        "잠시 후 첫 번째 웨이브가 시작됩니다.",
                        NamedTextColor.GRAY
                )
        );


        /*
         * 기존 Wave 1 시작 지연 유지.
         */
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
                    "[ZombieDungeon] Dungeon world is null."
            );

            return;
        }

        activeDungeonMobs.clear();

        /*
         * 이전 웨이브 카운트다운 Subtitle 제거.
         *
         * RPG HUD는 ActionBar이므로 영향을 받지 않는다.
         */
        clearParticipantTitles();

        broadcast(
                Component.text(
                        "Wave " + wave,
                        NamedTextColor.RED
                ).append(
                        Component.text(
                                " - 좀비 "
                                        + count
                                        + "마리를 처치하십시오.",
                                NamedTextColor.WHITE
                        )
                )
        );

        List<Location> selectedLocations =
                findSafeSpawnLocations(
                        dungeonWorld,
                        count
                );

        for (Location location
                : selectedLocations) {

            spawnWaveZombie(
                    dungeonWorld,
                    location,
                    wave
            );
        }

        plugin.getLogger().info(
                "[ZombieDungeon] Wave "
                        + wave
                        + " spawned: "
                        + activeDungeonMobs.size()
                        + " / "
                        + count
        );

        if (activeDungeonMobs.isEmpty()) {

            plugin.getLogger().severe(
                    "[ZombieDungeon] Wave "
                            + wave
                            + " spawn failed. "
                            + "No safe spawn locations found."
            );

            broadcast(
                    Component.text(
                            "던전 몬스터 생성에 실패했습니다. 관리자에게 문의하십시오.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        /*
         * 필요한 수보다 적게 생성된 경우에는
         * 진행 자체는 가능하게 두되 서버 로그에 경고.
         */
        if (activeDungeonMobs.size() < count) {

            plugin.getLogger().warning(
                    "[ZombieDungeon] Wave "
                            + wave
                            + " spawned only "
                            + activeDungeonMobs.size()
                            + " / "
                            + count
                            + " mobs."
            );
        }
    }

    private boolean isExpectedWaveState(
            int wave
    ) {

        return switch (wave) {

            case 1 ->
                    state
                            == ZombieDungeonState.WAVE_1;

            case 2 ->
                    state
                            == ZombieDungeonState.WAVE_2;

            case 3 ->
                    state
                            == ZombieDungeonState.WAVE_3;

            default -> false;
        };
    }

    /*
     * =========================================================
     * WAVE COUNTDOWN
     * =========================================================
     *
     * ActionBar는 RPG HUD가 사용 중이므로
     * 웨이브 카운트다운에는 사용하지 않는다.
     *
     * Subtitle 사용:
     *
     * Wave 2 시작까지 3초 남았습니다.
     * Wave 2 시작까지 2초 남았습니다.
     * Wave 2 시작까지 1초 남았습니다.
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

        showWaveCountdown(
                nextWave,
                seconds
        );

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

    private void showWaveCountdown(
            int wave,
            int seconds
    ) {

        Component subtitle =
                Component.text(
                        "Wave "
                                + wave
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

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (player == null
                    || !player.isOnline()) {
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

            if (player == null
                    || !player.isOnline()) {
                continue;
            }

            player.clearTitle();
        }
    }

    /*
     * =========================================================
     * MOB SPAWN
     * =========================================================
     */

    private void spawnWaveZombie(
            World world,
            Location location,
            int wave
    ) {

        Zombie zombie =
                world.spawn(
                        location,
                        Zombie.class
                );

        PersistentDataContainer pdc =
                zombie
                        .getPersistentDataContainer();

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

        zombie.setPersistent(true);
        zombie.setRemoveWhenFarAway(false);
        zombie.setCanPickupItems(false);

        activeDungeonMobs.add(
                zombie.getUniqueId()
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

        while (result.size() < required
                && attempts < MAX_SPAWN_ATTEMPTS) {

            attempts++;

            double offsetX =
                    random.nextDouble(
                            -SPAWN_RADIUS,
                            SPAWN_RADIUS
                    );

            double offsetZ =
                    random.nextDouble(
                            -SPAWN_RADIUS,
                            SPAWN_RADIUS
                    );

            /*
             * 반경 8블록의 원 내부만 사용.
             */
            if ((offsetX * offsetX)
                    + (offsetZ * offsetZ)
                    > (SPAWN_RADIUS
                    * SPAWN_RADIUS)) {

                continue;
            }

            double rawX =
                    SPAWN_CENTER_X
                            + offsetX;

            double rawZ =
                    SPAWN_CENTER_Z
                            + offsetZ;

            int blockX =
                    (int) Math.floor(rawX);

            int blockZ =
                    (int) Math.floor(rawZ);

            int feetY =
                    (int) Math.floor(
                            SPAWN_FEET_Y
                    );

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

            if (!isFarEnoughFromExisting(
                    candidate,
                    result
            )) {
                continue;
            }

            result.add(candidate);
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

            if (distanceSquared
                    < minimumSquared) {

                return false;
            }
        }

        return true;
    }

    /*
     * =========================================================
     * MOB IDENTIFICATION
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


    public boolean isGraveGuardian(
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

    /*
     * =========================================================
     * MOB DEATH
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

        /*
         * 아직 해당 웨이브 몬스터가 남아 있으면
         * 아무것도 하지 않는다.
         */
        if (!activeDungeonMobs.isEmpty()) {
            return;
        }

        switch (state) {

            /*
             * =================================================
             * WAVE 1 CLEAR
             * =================================================
             */
            case WAVE_1 -> {

                state =
                        ZombieDungeonState.WAVE_2;

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

                plugin.getLogger().info(
                        "[ZombieDungeon] Wave 1 cleared."
                );

                scheduleNextWave(
                        2,
                        WAVE_2_COUNT
                );
            }

            /*
             * =================================================
             * WAVE 2 CLEAR
             * =================================================
             */
            case WAVE_2 -> {

                state =
                        ZombieDungeonState.WAVE_3;

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

                plugin.getLogger().info(
                        "[ZombieDungeon] Wave 2 cleared."
                );

                scheduleNextWave(
                        3,
                        WAVE_3_COUNT
                );
            }

            /*
             * =================================================
             * WAVE 3 CLEAR
             * =================================================
             *
             * Wave 3 종료 후 BOSS 상태로 전환하고
             * 보스 등장 Subtitle 연출을 시작한다.
             * 연출 완료 후 묘지 수호자를 고정 위치에 소환한다.
             */
            case WAVE_3 -> {

                state =
                        ZombieDungeonState.BOSS;

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

                plugin.getLogger().info(
                        "[ZombieDungeon] Wave 3 cleared. "
                                + "Starting boss introduction."
                );

                startBossIntroduction();
            }

            /*
             * =================================================
             * BOSS CLEAR
             * =================================================
             */
            case BOSS -> {

                /*
                 * BOSS 상태라고 해도
                 * 실제 묘지 수호자가 아니면
                 * 클리어 처리하지 않는다.
                 */
                if (!isGraveGuardian(entity)) {
                    return;
                }

                state =
                        ZombieDungeonState.CLEARED;

                activeBoss = null;

                /*
                 * 화면 상단 BossBar 제거.
                 */
                removeBossBar();

                clearParticipantTitles();

                /*
                 * 보스 처치 채팅 메시지.
                 */
                broadcast(
                        Component.text(
                                "묘지 수호자를 처치했습니다.",
                                NamedTextColor.GOLD
                        )
                );

                /*
                 * 던전 클리어 Subtitle.
                 *
                 * Title stay 값은 10초로 설정하지만,
                 * 약 5초 뒤 시작되는 퇴장 카운트다운 Subtitle이
                 * 같은 채널을 덮어쓰므로 실제 단독 노출은 약 5초다.
                 */
                showBossSubtitle(
                        Component.text(
                                "던전을 클리어했습니다.",
                                NamedTextColor.GREEN
                        ),
                        10000L
                );

                /*
                 * =================================================
                 * DUNGEON CLEAR REWARD
                 * =================================================
                 *
                 * 실제 클리어 보상 구성과 수량은
                 * DungeonRewardService에서 결정하고 지급한다.
                 * 이 서비스에서는 참가자 집합만 전달한다.
                 */
                if (dungeonRewardService != null) {

                    dungeonRewardService
                            .rewardDungeonClear(
                                    Set.copyOf(
                                            participants
                                    )
                            );

                    /*
                     * 던전 클리어 주괴 보상
                     * 영웅 / 전설 / 신화 독립 추첨
                     */
                    dungeonRewardService
                            .rewardEquipmentIngots(
                            Set.copyOf(participants),
                            0.02D,
                            0.00D,
                            0.00D
                            );

                    dungeonRewardService
                            .rewardTeyvatRareWeapons(
                                    Set.copyOf(participants)
                            );

                    dungeonRewardService
                            .rewardDungeonGold(
                                    Set.copyOf(participants),
                                    100L,
                                    50L,
                                    0.20D
                            );

                    dungeonRewardService
                            .rewardEnhancementStones(
                                    Set.copyOf(
                                            participants
                                    ),
                                    0,
                                    0.20D,
                                    1,
                                    0
                            );

                } else {

                    plugin.getLogger().severe(
                            "[ZombieDungeon] "
                                    + "DungeonRewardService "
                                    + "is not initialized."
                    );
                }

                plugin.getLogger().info(
                        "[ZombieDungeon] "
                                + "Grave Guardian defeated. "
                                + "Dungeon cleared."
                );

                /*
                 * 클리어 Subtitle을 실제 화면에서 약 5초 보여준 뒤
                 * 5 → 4 → 3 → 2 → 1 퇴장 카운트다운을 진행한다.
                 * 카운트다운 종료 후 참가자를 자동 퇴장시키고
                 * 던전을 IDLE 상태로 초기화한다.
                 */
                scheduleSuccessfulDungeonExit();
            }

            default -> {
                /*
                 * 현재 사망 엔티티와 던전 상태의 조합에서
                 * 별도 진행 처리가 필요하지 않으면 아무 동작도 하지 않는다.
                 */
            }
        }
    }


    /*
     * =========================================================
     * BOSS INTRODUCTION
     * =========================================================
     *
     * Wave 3 Clear 후 Subtitle 연출:
     *
     * 1) "모든 웨이브가 종료되었습니다." - GOLD, 2초
     * 2) "묘지 깊은 곳에서 죽음의 기운이 느껴집니다." - DARK_RED, 3초
     * 3) "묘지 수호자가 나타납니다" - RED, 3초
     * 4) 기존 Subtitle 제거 후 묘지 수호자 실제 소환
     */

    private void startBossIntroduction() {

        if (state != ZombieDungeonState.BOSS) {
            return;
        }

        /*
         * =====================================================
         * BOSS INTRO SEQUENCE
         * =====================================================
         *
         * 1. 모든 웨이브가 종료되었습니다.                 - GOLD, 2초
         * 2. 묘지 깊은 곳에서 죽음의 기운이 느껴집니다.      - DARK_RED, 3초
         * 3. 묘지 수호자가 나타납니다                         - RED, 3초
         * 4. 기존 Subtitle 제거 후 보스 실제 소환
         */

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

                            if (state
                                    != ZombieDungeonState.BOSS) {
                                return;
                            }

                            showBossSubtitle(
                                    Component.text(
                                            "묘지 깊은 곳에서 죽음의 기운이 느껴집니다.",
                                            NamedTextColor.DARK_RED
                                    ),
                                    3000L
                            );

                            plugin.getServer()
                                    .getScheduler()
                                    .runTaskLater(
                                            plugin,
                                            () -> {

                                                if (state
                                                        != ZombieDungeonState.BOSS) {
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

                                                                    if (state
                                                                            != ZombieDungeonState.BOSS) {
                                                                        return;
                                                                    }

                                                                    clearParticipantTitles();

                                                                    spawnGraveGuardian();
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

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (player == null
                    || !player.isOnline()) {
                continue;
            }

            player.showTitle(title);
        }
    }

    /*
     * =========================================================
     * GRAVE GUARDIAN SPAWN
     * =========================================================
     */

    private void spawnGraveGuardian() {

        if (state != ZombieDungeonState.BOSS) {
            return;
        }

        if (dungeonWorld == null) {

            plugin.getLogger().severe(
                    "[ZombieDungeon] Cannot spawn boss: "
                            + "dungeonWorld is null."
            );

            return;
        }

        Location bossLocation =
                new Location(
                        dungeonWorld,
                        BOSS_X,
                        BOSS_Y,
                        BOSS_Z
                );

        Zombie boss =
                dungeonWorld.spawn(
                        bossLocation,
                        Zombie.class
                );

        /*
         * 이름
         */
        boss.customName(
                Component.text(
                        BOSS_NAME,
                        NamedTextColor.DARK_RED
                )
        );

        boss.setCustomNameVisible(false);

        /*
         * 던전 식별 PDC
         */
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
                dungeonBossKey,
                PersistentDataType.STRING,
                BOSS_ID
        );

        /*
         * 자연 디스폰/아이템 줍기 방지.
         */
        boss.setPersistent(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);

        /*
         * =====================================================
         * BOSS ATTRIBUTES
         * =====================================================
         */

        /*
         * Minecraft/Paper 실제 Entity HP는 1024 사용.
         *
         * RPG 실질 HP 600은
         * GraveGuardianDamageListener에서
         * 실제 HP와 동일하게 적용한다.
         */
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

        /*
         * DEF 8.
         *
         * 현재 보스에는 Bukkit Armor Attribute로 적용.
         */
        setBaseAttribute(
                boss,
                Attribute.ARMOR,
                BOSS_DEFENSE
        );

        setBaseAttribute(
                boss,
                Attribute.KNOCKBACK_RESISTANCE,
                BOSS_KNOCKBACK_RESISTANCE
        );

        setBaseAttribute(
                boss,
                Attribute.SCALE,
                BOSS_SCALE
        );

        AttributeInstance movementSpeed =
                boss.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );

        if (movementSpeed != null) {

            movementSpeed.setBaseValue(
                    movementSpeed.getBaseValue()
                            * BOSS_SPEED_MULTIPLIER
            );
        }

        activeBoss = boss;

        activeDungeonMobs.clear();

        activeDungeonMobs.add(
                boss.getUniqueId()
        );

        createBossBar(
                boss
        );

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
                "[ZombieDungeon] Grave Guardian spawned at "
                        + BOSS_X
                        + ", "
                        + BOSS_Y
                        + ", "
                        + BOSS_Z
        );
    }

    private void setBaseAttribute(
            Zombie zombie,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                zombie.getAttribute(
                        attribute
                );

        if (instance == null) {

            plugin.getLogger().warning(
                    "[ZombieDungeon] Attribute unavailable: "
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
     * BOSS BAR
     * =========================================================
     */

    private void createBossBar(
            Zombie boss
    ) {

        removeBossBar();

        bossBar =
                plugin.getServer()
                        .createBossBar(
                                BOSS_NAME,
                                BarColor.RED,
                                BarStyle.SOLID
                        );

	bossBar.setVisible(true);

        bossBar.setProgress(
                1.0D
        );

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (player == null
                    || !player.isOnline()) {
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

                                    if (activeBoss == null
                                            || !activeBoss.isValid()
                                            || activeBoss.isDead()) {

                                        removeBossBar();
                                        return;
                                    }

                                    double maxHealth =
                                            BOSS_VANILLA_HEALTH;

                                    double health =
                                            Math.max(
                                                    0.0D,
                                                    activeBoss.getHealth()
                                            );

                                    double progress =
                                            health / maxHealth;

                                    progress =
                                            Math.max(
                                                    0.0D,
                                                    Math.min(
                                                            1.0D,
                                                            progress
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

            bossBarUpdateTask = null;
        }

        if (bossBar != null) {

            bossBar.removeAll();

            bossBar = null;
        }
    }

        /*
     * =========================================================
     * DUNGEON END / RESET
     * =========================================================
     */


    /*
     * =========================================================
     * SUCCESS
     * =========================================================
     *
     * 보스 처치 보상은 기존 코드에서 즉시 지급된다.
     *
     * 보스 처치 후 약 5초 동안 클리어 Subtitle을 보여주고,
     * 이어서 5초 퇴장 카운트다운을 진행한다.
     * 보스 처치 시점부터 약 10초 뒤 자동 퇴장/초기화된다.
     */
    private void scheduleSuccessfulDungeonExit() {

        /*
         * 성공 종료 예약이 이미 존재한다면
         * 중복 카운트다운을 만들지 않는다.
         */
        if (dungeonResetTask != null) {
            return;
        }


        /*
         * 기존 "던전을 클리어했습니다." Subtitle을
         * 약 5초 동안 보여준 뒤
         * 퇴장 카운트다운을 시작한다.
         *
         * 최초 5초 대기 작업도 dungeonResetTask에
         * 저장하여 중복 예약을 방지한다.
         */
        dungeonResetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    /*
                                     * 현재 예약 작업은 실행 완료됐으므로
                                     * 다음 카운트다운 작업을 저장할 수 있게
                                     * 참조를 먼저 비운다.
                                     */
                                    dungeonResetTask = null;


                                    runDungeonExitCountdown(
                                            5
                                    );
                                },
                                100L
                        );
    }


    /*
     * =========================================================
     * DUNGEON EXIT COUNTDOWN
     * =========================================================
     *
     * 성공 클리어 후 마지막 5초:
     *
     * 던전 퇴장까지 5초 남았습니다.
     * 던전 퇴장까지 4초 남았습니다.
     * 던전 퇴장까지 3초 남았습니다.
     * 던전 퇴장까지 2초 남았습니다.
     * 던전 퇴장까지 1초 남았습니다.
     *
     * 이후 resetDungeon(true) 실행.
     */
    private void runDungeonExitCountdown(
            int seconds
    ) {

        /*
         * CLEARED 상태가 아니라면
         * 이미 실패/리셋/기타 종료 처리가 시작된 것이므로
         * 카운트다운을 중단한다.
         */
        if (
                state != ZombieDungeonState.CLEARED
        ) {

            dungeonResetTask = null;

            return;
        }


        /*
         * 1초 표시까지 끝난 다음 호출에서는
         * 즉시 정상 클리어 리셋을 실행한다.
         */
        if (seconds <= 0) {

            dungeonResetTask = null;


            resetDungeon(
                    true
            );


            return;
        }


        /*
         * 5~4초: 노란색
         * 3~1초: 빨간색
         */
        NamedTextColor countdownColor =
                seconds <= 3
                        ? NamedTextColor.RED
                        : NamedTextColor.YELLOW;


        /*
         * RPG HUD는 ActionBar를 사용하므로
         * 퇴장 카운트다운은 기존 던전 연출과 동일하게
         * Subtitle 채널을 사용한다.
         */
        showBossSubtitle(
                Component.text(
                        "던전 퇴장까지 "
                                + seconds
                                + "초 남았습니다.",
                        countdownColor
                ),
                1100L
        );


        /*
         * 1초 뒤 다음 숫자를 표시한다.
         *
         * 예약 작업을 dungeonResetTask에 저장해서
         * 동시에 여러 종료 작업이 실행되지 않도록 한다.
         */
        dungeonResetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    dungeonResetTask = null;


                                    runDungeonExitCountdown(
                                            seconds - 1
                                    );
                                },
                                20L
                        );
    }


    /*
     * =========================================================
     * FAILURE
     * =========================================================
     */
    private void failDungeon() {

        if (
                state == ZombieDungeonState.IDLE
                || state == ZombieDungeonState.CLEARED
                || state == ZombieDungeonState.RESETTING
        ) {

            return;
        }


        /*
         * 실패 확정 후에는 새로운 진행을 막는다.
         */
        state =
                ZombieDungeonState.RESETTING;


        removeBossBar();


        clearParticipantTitles();


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


        plugin.getLogger().info(
                "[ZombieDungeon] Dungeon failed. "
                        + "No active participants remain."
        );


        if (dungeonResetTask != null) {

            dungeonResetTask.cancel();

            dungeonResetTask = null;
        }


        /*
         * 실패 안내 5초 후 정리.
         */
        dungeonResetTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> resetDungeon(
                                        false
                                ),
                                100L
                        );
    }


    /*
     * =========================================================
     * PLAYER DEATH
     * =========================================================
     *
     * 참가자 한 명이 사망했다고
     * 즉시 전체 던전을 실패시키지는 않는다.
     *
     * 마지막 active participant가 사망했을 때
     * 던전 실패.
     */
    public void onParticipantDeath(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (!participants.contains(uuid)) {
            return;
        }


        if (
                state == ZombieDungeonState.IDLE
                || state == ZombieDungeonState.CLEARED
                || state == ZombieDungeonState.RESETTING
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

            pendingExitLocations.put(
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


        checkDungeonFailure();
    }


    /*
     * =========================================================
     * PLAYER QUIT
     * =========================================================
     */
    public void onParticipantQuit(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        if (!participants.contains(uuid)) {
            return;
        }


        /*
         * CLEARED 상태라면
         * 던전 실패 판정은 하지 않는다.
         *
         * 단, 재접속 시 복귀할 수 있도록
         * 위치는 pending에 저장한다.
         */
        if (
                state == ZombieDungeonState.IDLE
                || state == ZombieDungeonState.RESETTING
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

            pendingExitLocations.put(
                    uuid,
                    returnLocation.clone()
            );
        }


        checkDungeonFailure();
    }


    /*
     * =========================================================
     * FAILURE CHECK
     * =========================================================
     */
    private void checkDungeonFailure() {

        if (
                state == ZombieDungeonState.IDLE
                || state == ZombieDungeonState.CLEARED
                || state == ZombieDungeonState.RESETTING
        ) {

            return;
        }


        if (!activeParticipants.isEmpty()) {
            return;
        }


        failDungeon();
    }


    /*
     * =========================================================
     * RESPAWN PENDING LOCATION
     * =========================================================
     */
    public Location consumePendingExitLocation(
            UUID uuid
    ) {

        Location location =
                pendingExitLocations.remove(
                        uuid
                );


        if (location == null) {
            return null;
        }


        return location.clone();
    }


    /*
     * =========================================================
     * RECONNECT RETURN
     * =========================================================
     */
    public void restorePendingExitOnJoin(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        Location location =
                consumePendingExitLocation(
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

                            if (
                                    !player.isOnline()
                                    || player.isDead()
                            ) {

                                /*
                                 * 아직 이동할 수 없다면
                                 * pending에 다시 넣는다.
                                 */
                                pendingExitLocations.put(
                                        uuid,
                                        location.clone()
                                );

                                return;
                            }


                            boolean teleported =
                                    player.teleport(
                                            location
                                    );


                            if (!teleported) {

                                pendingExitLocations.put(
                                        uuid,
                                        location.clone()
                                );

                                return;
                            }


                            player.sendMessage(
                                    Component.text(
                                            "[던전] 이전 던전의 복귀 위치로 이동했습니다.",
                                            NamedTextColor.GRAY
                                    )
                            );
                        }
                );
    }


    /*
     * =========================================================
     * RESET DUNGEON
     * =========================================================
     *
     * cleared=true
     *   정상 클리어
     *
     * cleared=false
     *   실패 종료
     */
    private void resetDungeon(
            boolean cleared
    ) {

        state =
                ZombieDungeonState.RESETTING;


        /*
         * =====================================================
         * ACTIVE DUNGEON MOBS
         * =====================================================
         */
        for (
                UUID mobUuid
                : Set.copyOf(
                        activeDungeonMobs
                )
        ) {

            org.bukkit.entity.Entity entity =
                    plugin.getServer()
                            .getEntity(
                                    mobUuid
                            );


            if (
                    entity != null
                    && entity.isValid()
            ) {

                entity.remove();
            }
        }


        activeDungeonMobs.clear();


        /*
         * Boss가 activeDungeonMobs에 존재하지 않는
         * 상황까지 대비.
         */
        if (
                activeBoss != null
                && activeBoss.isValid()
        ) {

            activeBoss.remove();
        }


        activeBoss = null;


        /*
         * =====================================================
         * DISPLAY CLEANUP
         * =====================================================
         */
        removeBossBar();

        clearParticipantTitles();


        /*
         * =====================================================
         * RETURN PLAYERS
         * =====================================================
         */
        for (
                UUID uuid
                : Set.copyOf(
                        participants
                )
        ) {

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


            /*
             * 오프라인 또는 사망 중이면
             * 리스폰/재접속 때 복귀.
             */
            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
            ) {

                pendingExitLocations.put(
                        uuid,
                        returnLocation.clone()
                );

                continue;
            }


            boolean teleported =
                    player.teleport(
                            returnLocation
                    );


            if (!teleported) {

                pendingExitLocations.put(
                        uuid,
                        returnLocation.clone()
                );


                player.sendMessage(
                        Component.text(
                                "[던전] 자동 퇴장 이동에 실패했습니다. "
                                        + "재접속 시 복귀를 다시 시도합니다.",
                                NamedTextColor.RED
                        )
                );


                continue;
            }


            pendingExitLocations.remove(
                    uuid
            );


            if (cleared) {

                player.sendMessage(
                        Component.text(
                                "[던전] 던전을 클리어하여 원래 위치로 복귀했습니다.",
                                NamedTextColor.GREEN
                        )
                );

            } else {

                player.sendMessage(
                        Component.text(
                                "[던전] 던전이 종료되어 원래 위치로 복귀했습니다.",
                                NamedTextColor.GRAY
                        )
                );
            }
        }


        /*
         * =====================================================
         * INTERNAL STATE RESET
         * =====================================================
         */
        participants.clear();

        activeParticipants.clear();

        returnLocations.clear();


        dungeonWorld = null;


        dungeonResetTask = null;


        state =
                ZombieDungeonState.IDLE;


        plugin.getLogger().info(
                "[ZombieDungeon] Dungeon reset complete. "
                        + "State = IDLE"
        );
    }


/*
     * =========================================================
     * PARTICIPANTS
     * =========================================================
     */

    private void broadcast(
            Component component
    ) {

        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(uuid);

            if (player == null
                    || !player.isOnline()) {
                continue;
            }

            player.sendMessage(
                    component
            );
        }
    }

    public ZombieDungeonState getState() {
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
