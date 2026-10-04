package com.hcs.rpgcore.mob;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.dungeon.reward.DungeonRewardService;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FallenAngelBossDungeonListener
        implements Listener {

    private static final String MODEL_ID =
            "fallen_angel";

    private static final double MAX_HEALTH =
            1024.0D;

    private static final long CLEAR_EXP =
            2000L;

    private static final double ATTACK_DAMAGE =
            45.0D;

    private static final double ARMOR =
            20.0D;

    /*
     * Fallen Angel 기본 이동속도.
     *
     * 기존 0.30은 근접 보스 기준으로 추격 압박이 너무 강해
     * 플레이어가 공격을 보고 대응할 시간을 확보하도록 낮춘다.
     */
    private static final double MOVEMENT_SPEED =
            0.24D;

    private static final double KNOCKBACK_RESISTANCE =
            1.0D;

    /*
     * =========================================================
     * ENTRANCE GATE
     * =========================================================
     */

    private static final int ENTRANCE_MIN_X = 20;
    private static final int ENTRANCE_MAX_X = 23;

    private static final int ENTRANCE_Y = 88;

    private static final int ENTRANCE_MIN_Z = 385;
    private static final int ENTRANCE_MAX_Z = 388;


    /*
     * =========================================================
     * DUNGEON ENTRY LOCATION
     * =========================================================
     */

    private static final double DUNGEON_ENTRY_X =
            999966.5D;

    private static final double DUNGEON_ENTRY_Y =
            -5.0D;

    private static final double DUNGEON_ENTRY_Z =
            1000016.5D;


    /*
     * =========================================================
     * BOSS START AREA
     * =========================================================
     */

    private static final int BOSS_START_MIN_X =
            999988;

    private static final int BOSS_START_MAX_X =
            1000014;

    private static final int BOSS_START_MIN_Y =
            -10;

    private static final int BOSS_START_MAX_Y =
            5;

    private static final int BOSS_START_MIN_Z =
            999979;

    private static final int BOSS_START_MAX_Z =
            1000006;


    /*
     * =========================================================
     * BOSS ROOM AREA
     * =========================================================
     *
     * Avalon Boss Arena 전체 영역.
     *
     * 자연 생성 몬스터 차단에 사용한다.
     * CUSTOM 소환은 차단하지 않는다.
     */

    private static final int BOSS_ROOM_MIN_X =
            999944;

    private static final int BOSS_ROOM_MAX_X =
            1000055;

    private static final int BOSS_ROOM_MIN_Y =
            -16;

    private static final int BOSS_ROOM_MAX_Y =
            15;

    private static final int BOSS_ROOM_MIN_Z =
            999944;

    private static final int BOSS_ROOM_MAX_Z =
            1000055;


    /*
     * =========================================================
     * BOSS SPAWN LOCATION
     * =========================================================
     */

    private static final double BOSS_SPAWN_X =
            999999.5D;

    private static final double BOSS_SPAWN_Y =
            -6.0D;

    private static final double BOSS_SPAWN_Z =
            999994.5D;


    /*
     * =========================================================
     * DUNGEON EXIT LOCATION
     * =========================================================
     */

    private static final double DUNGEON_EXIT_X =
            23.5D;

    private static final double DUNGEON_EXIT_Y =
            88.0D;

    private static final double DUNGEON_EXIT_Z =
            379.5D;


    /*
     * =========================================================
     * COUNTDOWN
     * =========================================================
     */

    private static final int ENTRANCE_CHECK_PERIOD_TICKS =
            5;

    private static final int ENTRANCE_REQUIRED_TICKS =
            60;

    private static final int BOSS_START_COUNTDOWN_SECONDS =
            3;


    private final Plugin plugin;

    private final DungeonRewardService dungeonRewardService;

    private final NamespacedKey bossMobKey;

    private final Map<UUID, Integer> entranceStandingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> entranceLastDisplayedSecond =
            new HashMap<>();

    private BukkitTask entrancePollingTask;

    private BukkitTask bossCountdownTask;

    private boolean bossCountdownRunning;

    private BossBar bossBar;

    private BukkitTask bossBarUpdateTask;

    private BukkitTask dungeonExitTask;

    /*
     * 사망 또는 로그아웃 때문에 즉시 퇴장시킬 수 없는
     * 플레이어의 복귀 대기 상태.
     *
     * resetDungeon()에서는 지우지 않는다.
     * 리스폰/재접속 시에만 제거한다.
     */
    private final Set<UUID> pendingExitPlayers =
            new HashSet<>();

    private UUID dungeonPlayerUuid;

    private UUID activeBossUuid;

    private boolean bossStarted;


    public FallenAngelBossDungeonListener(
            Plugin plugin
    ) {

        this.plugin = plugin;

        if (!(plugin instanceof RPGCorePlugin rpgCorePlugin)) {
            throw new IllegalArgumentException(
                    "FallenAngelBossDungeonListener requires RPGCorePlugin"
            );
        }

        this.dungeonRewardService =
                rpgCorePlugin.getDungeonRewardService();

        this.bossMobKey =
                new NamespacedKey(
                        plugin,
                        BossMobKeys.BOSS_MOB_KEY
                );

        startEntrancePollingTask();
    }


    /*
     * =========================================================
     * DUNGEON ENTRANCE COUNTDOWN
     * =========================================================
     */

    private void startEntrancePollingTask() {

        entrancePollingTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::checkEntrancePlayers,
                                1L,
                                ENTRANCE_CHECK_PERIOD_TICKS
                        );
    }


    private void checkEntrancePlayers() {

        for (
                Player player
                : plugin.getServer().getOnlinePlayers()
        ) {

            UUID uuid =
                    player.getUniqueId();

            if (!isInsideEntranceGate(
                    player.getLocation()
            )) {

                entranceStandingTicks.remove(
                        uuid
                );

                entranceLastDisplayedSecond.remove(
                        uuid
                );

                continue;
            }

            /*
             * 이미 현재 던전 참가자로 등록된 플레이어는
             * 입구 카운트다운을 다시 돌리지 않는다.
             */
            if (
                    dungeonPlayerUuid != null
                    &&
                    dungeonPlayerUuid.equals(uuid)
            ) {
                continue;
            }

            int ticks =
                    entranceStandingTicks.getOrDefault(
                            uuid,
                            0
                    )
                            + ENTRANCE_CHECK_PERIOD_TICKS;

            entranceStandingTicks.put(
                    uuid,
                    ticks
            );

            int seconds;

            if (ticks <= 20) {
                seconds = 3;
            } else if (ticks <= 40) {
                seconds = 2;
            } else {
                seconds = 1;
            }

            Integer lastSecond =
                    entranceLastDisplayedSecond.get(
                            uuid
                    );

            if (
                    ticks < ENTRANCE_REQUIRED_TICKS
                    &&
                    (
                            lastSecond == null
                            ||
                            lastSecond != seconds
                    )
            ) {

                entranceLastDisplayedSecond.put(
                        uuid,
                        seconds
                );

                showSubtitle(
                        player,
                        "던전 입장까지 "
                                + seconds
                                + "초 남았습니다.",
                        NamedTextColor.LIGHT_PURPLE,
                        1100L
                );
            }

            if (
                    ticks >= ENTRANCE_REQUIRED_TICKS
            ) {

                entranceStandingTicks.remove(
                        uuid
                );

                entranceLastDisplayedSecond.remove(
                        uuid
                );

                player.clearTitle();

                enterDungeon(
                        player
                );
            }
        }
    }


    /*
     * =========================================================
     * PLAYER MOVEMENT
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL,
            ignoreCancelled = true
    )
    public void onPlayerMove(
            PlayerMoveEvent event
    ) {

        if (event.getTo() == null) {
            return;
        }

        if (
                event.getFrom().getBlockX()
                        == event.getTo().getBlockX()
                &&
                event.getFrom().getBlockY()
                        == event.getTo().getBlockY()
                &&
                event.getFrom().getBlockZ()
                        == event.getTo().getBlockZ()
        ) {
            return;
        }

        Player player =
                event.getPlayer();

        Location location =
                event.getTo();


        /*
         * 현재 던전에 입장한 플레이어만
         * Fallen Angel 전투를 시작할 수 있다.
         */
        if (
                dungeonPlayerUuid == null
                ||
                !dungeonPlayerUuid.equals(
                        player.getUniqueId()
                )
        ) {
            return;
        }

        if (bossStarted) {
            return;
        }

        if (
                isInsideBossStartArea(location)
                &&
                !bossStarted
                &&
                !bossCountdownRunning
        ) {

            plugin.getLogger().info(
                    "[FallenAngel] boss start area entered by "
                            + player.getName()
            );

            startBossCountdown(
                    player
            );
        }
    }


    /*
     * =========================================================
     * DUNGEON ENTRY
     * =========================================================
     */

    private void enterDungeon(
            Player player
    ) {

        /*
         * 현재는 하나의 고정 보스룸만 사용하므로
         * 동시 진행 플레이어는 1명으로 제한한다.
         */
        if (
                dungeonPlayerUuid != null
                &&
                !dungeonPlayerUuid.equals(
                        player.getUniqueId()
                )
        ) {

            player.sendMessage(
                    Component.text(
                            "현재 다른 플레이어가 보스 던전을 진행 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        World world =
                player.getWorld();

        dungeonPlayerUuid =
                player.getUniqueId();

        bossStarted =
                false;

        activeBossUuid =
                null;

        Location destination =
                new Location(
                        world,
                        DUNGEON_ENTRY_X,
                        DUNGEON_ENTRY_Y,
                        DUNGEON_ENTRY_Z
                );

        destination.setYaw(
                player.getLocation().getYaw()
        );

        destination.setPitch(
                player.getLocation().getPitch()
        );

        player.teleport(
                destination
        );
    }


    /*
     * =========================================================
     * BOSS START COUNTDOWN
     * =========================================================
     */

    private void startBossCountdown(
            Player player
    ) {

        if (
                bossStarted
                ||
                bossCountdownRunning
        ) {
            return;
        }

        bossCountdownRunning =
                true;

        runBossCountdown(
                player,
                BOSS_START_COUNTDOWN_SECONDS
        );
    }


    private void runBossCountdown(
            Player player,
            int seconds
    ) {

        if (
                dungeonPlayerUuid == null
                ||
                !dungeonPlayerUuid.equals(
                        player.getUniqueId()
                )
                ||
                !player.isOnline()
        ) {

            bossCountdownRunning =
                    false;

            bossCountdownTask =
                    null;

            return;
        }

        if (seconds <= 0) {

            bossCountdownRunning =
                    false;

            bossCountdownTask =
                    null;

            player.clearTitle();

            startBossFight(
                    player
            );

            return;
        }

        if (seconds == 3) {

            showSubtitle(
                    player,
                    "검게 물든 성스러운 기운이 느껴집니다.",
                    NamedTextColor.DARK_PURPLE,
                    1100L
            );

        } else if (seconds == 2) {

            showSubtitle(
                    player,
                    "타락한 천사가 나타납니다.",
                    NamedTextColor.LIGHT_PURPLE,
                    1100L
            );

        } else {

            showSubtitle(
                    player,
                    "보스 레이드가 시작됩니다.",
                    NamedTextColor.RED,
                    1100L
            );
        }

        bossCountdownTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {

                                    bossCountdownTask =
                                            null;

                                    runBossCountdown(
                                            player,
                                            seconds - 1
                                    );
                                },
                                20L
                        );
    }


    private void showSubtitle(
            Player player,
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

        player.showTitle(
                title
        );
    }


    /*
     * =========================================================
     * BOSS FIGHT
     * =========================================================
     */

    private void startBossFight(
            Player player
    ) {

        if (bossStarted) {
            return;
        }

        bossStarted =
                true;

        World world =
                player.getWorld();

        Location spawnLocation =
                new Location(
                        world,
                        BOSS_SPAWN_X,
                        BOSS_SPAWN_Y,
                        BOSS_SPAWN_Z
                );

        WitherSkeleton boss =
                world.spawn(
                        spawnLocation,
                        WitherSkeleton.class,
                        CreatureSpawnEvent
                                .SpawnReason
                                .CUSTOM
                );

        boss.getPersistentDataContainer()
                .set(
                        bossMobKey,
                        PersistentDataType.STRING,
                        BossMobKeys.FALLEN_ANGEL_ID
                );

        boss.customName(
                Component.text(
                        "[보스] 타락한 천사",
                        NamedTextColor.DARK_PURPLE
                )
        );

        boss.setCustomNameVisible(
                true
        );

        boss.setRemoveWhenFarAway(
                false
        );

        /*
         * =========================================================
         * BOSS COMBAT STATS
         * =========================================================
         */

        var maxHealth =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                );

        if (maxHealth != null) {
            maxHealth.setBaseValue(
                    MAX_HEALTH
            );
        }

        boss.setHealth(
                MAX_HEALTH
        );

        var attackDamage =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.ATTACK_DAMAGE
                );

        if (attackDamage != null) {
            attackDamage.setBaseValue(
                    ATTACK_DAMAGE
            );
        }

        var armor =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.ARMOR
                );

        if (armor != null) {
            armor.setBaseValue(
                    ARMOR
            );
        }

        var movementSpeed =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.MOVEMENT_SPEED
                );

        if (movementSpeed != null) {
            movementSpeed.setBaseValue(
                    MOVEMENT_SPEED
            );
        }

        var knockbackResistance =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE
                );

        if (knockbackResistance != null) {
            knockbackResistance.setBaseValue(
                    KNOCKBACK_RESISTANCE
            );
        }

        /*
         * 보스 AI / 전투 대상 활성화.
         */
        boss.setAI(true);
        boss.setAware(true);
        boss.setTarget(player);

        activeBossUuid =
                boss.getUniqueId();

        plugin.getLogger().info(
                "[FallenAngel] spawned type="
                        + boss.getType()
                        + " hp="
                        + boss.getHealth()
                        + " max="
                        + (
                                boss.getAttribute(
                                        org.bukkit.attribute.Attribute.MAX_HEALTH
                                ) != null
                                ? boss.getAttribute(
                                        org.bukkit.attribute.Attribute.MAX_HEALTH
                                ).getBaseValue()
                                : -1.0D
                        )
                        + " attack="
                        + (
                                boss.getAttribute(
                                        org.bukkit.attribute.Attribute.ATTACK_DAMAGE
                                ) != null
                                ? boss.getAttribute(
                                        org.bukkit.attribute.Attribute.ATTACK_DAMAGE
                                ).getBaseValue()
                                : -1.0D
                        )
                        + " armor="
                        + (
                                boss.getAttribute(
                                        org.bukkit.attribute.Attribute.ARMOR
                                ) != null
                                ? boss.getAttribute(
                                        org.bukkit.attribute.Attribute.ARMOR
                                ).getBaseValue()
                                : -1.0D
                        )
        );

        boolean modelAttached =
                attachBetterModel(boss);

        plugin.getLogger().info(
                "[FallenAngel] BetterModel attached="
                        + modelAttached
        );

        if (!modelAttached) {

            boss.remove();

            activeBossUuid =
                    null;

            bossStarted =
                    false;

            player.sendMessage(
                    Component.text(
                            "Fallen Angel 모델을 불러오지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        createBossBar(
                boss,
                player
        );
    }


    /*
     * =========================================================
     * BOSS BAR
     * =========================================================
     */

    private void createBossBar(
            WitherSkeleton boss,
            Player player
    ) {

        removeBossBar();

        bossBar =
                Bukkit.createBossBar(
                        "타락한 천사",
                        BarColor.PURPLE,
                        BarStyle.SOLID
                );

        bossBar.setProgress(
                1.0D
        );

        bossBar.addPlayer(
                player
        );

        bossBar.setVisible(
                true
        );

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
            WitherSkeleton boss
    ) {

        if (
                bossBar == null
                ||
                !boss.isValid()
                ||
                boss.isDead()
        ) {

            removeBossBar();

            return;
        }

        var maxHealthAttribute =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                );

        double maxHealth =
                maxHealthAttribute != null
                        ? maxHealthAttribute.getValue()
                        : MAX_HEALTH;

        double progress =
                maxHealth > 0.0D
                        ? boss.getHealth()
                                / maxHealth
                        : 0.0D;

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

            bossBar.setVisible(
                    false
            );

            bossBar =
                    null;
        }
    }


    /*
     * =========================================================
     * BOSS DEATH / DUNGEON CLEAR
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onBossDeath(
            EntityDeathEvent event
    ) {

        String bossId =
                event.getEntity()
                        .getPersistentDataContainer()
                        .get(
                                bossMobKey,
                                PersistentDataType.STRING
                        );

        if (
                !BossMobKeys.FALLEN_ANGEL_ID.equals(
                        bossId
                )
        ) {
            return;
        }

        if (
                activeBossUuid == null
                ||
                !activeBossUuid.equals(
                        event.getEntity()
                                .getUniqueId()
                )
        ) {
            return;
        }

        /*
         * =========================================================
         * DISABLE VANILLA BOSS DROPS
         * =========================================================
         *
         * WitherSkeleton 기반 엔티티가 떨어뜨리는
         * 뼈 / 석탄 등의 바닐라 아이템을 제거한다.
         *
         * RPGCore 던전 보상은 별도 보상 시스템에서 처리한다.
         */
        plugin.getLogger().info(
                "[FallenAngel] vanilla drops before clear="
                        + event.getDrops()
        );

        event.getDrops().clear();

        plugin.getLogger().info(
                "[FallenAngel] vanilla drops after clear="
                        + event.getDrops()
        );

        removeBossBar();

        activeBossUuid =
                null;

        Player dungeonPlayer =
                dungeonPlayerUuid != null
                        ? Bukkit.getPlayer(
                                dungeonPlayerUuid
                        )
                        : null;

        if (
                dungeonPlayer == null
                ||
                !dungeonPlayer.isOnline()
        ) {

            resetDungeon();

            return;
        }

        /*
         * =========================================================
         * DUNGEON CLEAR REWARD
         * =========================================================
         *
         * 실제 Fallen Angel 처치가 확인된 경우에만
         * 정확히 한 번 지급한다.
         */

        Set<UUID> rewardParticipants =
                Set.of(
                        dungeonPlayer.getUniqueId()
                );

        dungeonRewardService
                .rewardDungeonClearExperience(
                        rewardParticipants,
                        CLEAR_EXP
                );

        dungeonRewardService
                .rewardFallenAngelBasicRewards(
                        rewardParticipants
                );

        dungeonPlayer.clearTitle();

        showSubtitle(
                dungeonPlayer,
                "던전을 클리어했습니다.",
                NamedTextColor.GOLD,
                5000L
        );

        dungeonPlayer.sendMessage(
                Component.text(
                        "[던전] ",
                        NamedTextColor.DARK_PURPLE
                ).append(
                        Component.text(
                                "타락한 천사를 처치했습니다.",
                                NamedTextColor.GOLD
                        )
                )
        );

        plugin.getLogger().info(
                "[FallenAngel] dungeon cleared"
        );

        if (dungeonExitTask != null) {

            dungeonExitTask.cancel();

            dungeonExitTask =
                    null;
        }

        /*
         * 좀비 던전과 동일:
         * 클리어 문구를 5초간 유지한 후
         * 퇴장 카운트다운 시작.
         */
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
                                100L
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

        if (dungeonPlayerUuid == null) {

            dungeonExitTask =
                    null;

            return;
        }

        Player player =
                Bukkit.getPlayer(
                        dungeonPlayerUuid
                );

        if (
                player == null
                ||
                !player.isOnline()
        ) {

            dungeonExitTask =
                    null;

            resetDungeon();

            return;
        }

        if (seconds <= 0) {

            dungeonExitTask =
                    null;

            player.clearTitle();

            Location exitLocation =
                    new Location(
                            player.getWorld(),
                            DUNGEON_EXIT_X,
                            DUNGEON_EXIT_Y,
                            DUNGEON_EXIT_Z
                    );

            boolean teleported =
                    player.teleport(
                            exitLocation
                    );

            if (!teleported) {

                plugin.getLogger().warning(
                        "[FallenAngel] dungeon exit teleport failed: "
                                + player.getName()
                );
            }

            resetDungeon();

            return;
        }

        NamedTextColor countdownColor =
                seconds <= 3
                        ? NamedTextColor.RED
                        : NamedTextColor.YELLOW;

        showSubtitle(
                player,
                "던전 퇴장까지 "
                        + seconds
                        + "초 남았습니다.",
                countdownColor,
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
     * DUNGEON FAILURE
     * =========================================================
     */

    private void failDungeon(
            Player player,
            boolean pendingExit
    ) {

        /*
         * 이미 진행 중인 던전이 아니라면
         * 실패 처리를 중복 실행하지 않는다.
         */
        if (dungeonPlayerUuid == null) {
            return;
        }

        UUID playerUuid =
                dungeonPlayerUuid;

        if (
                player != null
                &&
                !playerUuid.equals(
                        player.getUniqueId()
                )
        ) {
            return;
        }

        /*
         * 현재 보스 제거.
         */
        if (activeBossUuid != null) {

            Entity boss =
                    Bukkit.getEntity(
                            activeBossUuid
                    );

            if (
                    boss != null
                    &&
                    boss.isValid()
            ) {

                boss.remove();
            }
        }

        removeBossBar();

        if (bossCountdownTask != null) {

            bossCountdownTask.cancel();

            bossCountdownTask =
                    null;
        }

        bossCountdownRunning =
                false;

        if (dungeonExitTask != null) {

            dungeonExitTask.cancel();

            dungeonExitTask =
                    null;
        }

        if (pendingExit) {

            pendingExitPlayers.add(
                    playerUuid
            );
        }

        if (
                player != null
                &&
                player.isOnline()
        ) {

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

        plugin.getLogger().info(
                "[FallenAngel] dungeon failed: "
                        + playerUuid
        );

        resetDungeon();
    }


    /*
     * =========================================================
     * PLAYER DEATH
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onDungeonPlayerDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getPlayer();

        if (
                dungeonPlayerUuid == null
                ||
                !dungeonPlayerUuid.equals(
                        player.getUniqueId()
                )
        ) {
            return;
        }

        failDungeon(
                player,
                true
        );
    }


    /*
     * =========================================================
     * PLAYER QUIT
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onDungeonPlayerQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        if (
                dungeonPlayerUuid == null
                ||
                !dungeonPlayerUuid.equals(
                        player.getUniqueId()
                )
        ) {
            return;
        }

        failDungeon(
                player,
                true
        );
    }


    /*
     * =========================================================
     * PLAYER RESPAWN RETURN
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onDungeonPlayerRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();

        UUID uuid =
                player.getUniqueId();

        if (
                !pendingExitPlayers.remove(
                        uuid
                )
        ) {
            return;
        }

        event.setRespawnLocation(
                createDungeonExitLocation(
                        player.getWorld()
                )
        );
    }


    /*
     * =========================================================
     * PLAYER RECONNECT RETURN
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onDungeonPlayerJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        UUID uuid =
                player.getUniqueId();

        if (
                !pendingExitPlayers.remove(
                        uuid
                )
        ) {
            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> player.teleport(
                                createDungeonExitLocation(
                                        player.getWorld()
                                )
                        )
                );
    }


    private Location createDungeonExitLocation(
            World world
    ) {

        return new Location(
                world,
                DUNGEON_EXIT_X,
                DUNGEON_EXIT_Y,
                DUNGEON_EXIT_Z
        );
    }


    /*
     * =========================================================
     * DUNGEON RESET
     * =========================================================
     */

    private void resetDungeon() {

        if (bossCountdownTask != null) {

            bossCountdownTask.cancel();

            bossCountdownTask =
                    null;
        }

        bossCountdownRunning =
                false;

        if (dungeonExitTask != null) {

            dungeonExitTask.cancel();

            dungeonExitTask =
                    null;
        }

        removeBossBar();

        activeBossUuid =
                null;

        dungeonPlayerUuid =
                null;

        bossStarted =
                false;
    }


    /*
     * =========================================================
     * BETTERMODEL
     * =========================================================
     */

    private boolean attachBetterModel(
            Entity entity
    ) {

        ModelRenderer renderer =
                BetterModel.modelOrNull(
                        MODEL_ID
                );

        if (renderer == null) {

            plugin.getLogger().warning(
                    "BetterModel model not found: "
                            + MODEL_ID
            );

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
     * BOSS ROOM NATURAL SPAWN BLOCK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onCreatureSpawn(
            CreatureSpawnEvent event
    ) {

        /*
         * 자연 생성만 차단한다.
         *
         * Fallen Angel처럼 RPGCore에서 SpawnReason.CUSTOM으로
         * 생성한 엔티티는 정상적으로 허용한다.
         */
        if (
                event.getSpawnReason()
                        != CreatureSpawnEvent.SpawnReason.NATURAL
        ) {
            return;
        }

        if (
                !isInsideBossRoom(
                        event.getLocation()
                )
        ) {
            return;
        }

        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * AREA CHECKS
     * =========================================================
     */

    private boolean isInsideEntranceGate(
            Location location
    ) {

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= ENTRANCE_MIN_X
                && x <= ENTRANCE_MAX_X
                && y == ENTRANCE_Y
                && z >= ENTRANCE_MIN_Z
                && z <= ENTRANCE_MAX_Z;
    }


    private boolean isInsideBossRoom(
            Location location
    ) {

        if (
                location.getWorld() == null
                ||
                location.getWorld()
                        .getEnvironment()
                        != World.Environment.NORMAL
        ) {
            return false;
        }

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();

        return x >= BOSS_ROOM_MIN_X
                && x <= BOSS_ROOM_MAX_X
                && y >= BOSS_ROOM_MIN_Y
                && y <= BOSS_ROOM_MAX_Y
                && z >= BOSS_ROOM_MIN_Z
                && z <= BOSS_ROOM_MAX_Z;
    }


    private boolean isInsideBossStartArea(
            Location location
    ) {

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= BOSS_START_MIN_X
                && x <= BOSS_START_MAX_X
                && y >= BOSS_START_MIN_Y
                && y <= BOSS_START_MAX_Y
                && z >= BOSS_START_MIN_Z
                && z <= BOSS_START_MAX_Z;
    }
}
