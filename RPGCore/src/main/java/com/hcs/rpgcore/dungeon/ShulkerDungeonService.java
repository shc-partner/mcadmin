package com.hcs.rpgcore.dungeon;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;

import org.bukkit.inventory.ItemStack;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


public final class ShulkerDungeonService {

    /*
     * =========================================================
     * DUNGEON ID
     * =========================================================
     */
    public static final String DUNGEON_ID =
            "shulker_dungeon";


    /*
     * =========================================================
     * DUNGEON DESTINATION
     * =========================================================
     *
     * 사용자 지정 입장 위치:
     *
     * 5000000 102 4999995
     */
    private static final double ENTRY_X =
            5000000.0D;

    private static final double ENTRY_Y =
            102.0D;

    private static final double ENTRY_Z =
            4999995.0D;


    /*
     * =========================================================
     * EXIT
     * =========================================================
     *
     * 클리어 또는 실패 후:
     *
     * 1213 64 -1001
     */
    private static final double EXIT_X =
            1213.0D;

    private static final double EXIT_Y =
            64.0D;

    private static final double EXIT_Z =
            -1001.0D;


    /*
     * =========================================================
     * SHULKER SPAWN POSITIONS
     * =========================================================
     *
     * 1. 5000000 102 5000004
     * 2. 5000005 102 5000002
     * 3. 4999995 102 5000002
     * 4. 5000004 102 4999998
     * 5. 4999996 102 4999998
     */
    private static final double[][] SPAWN_POSITIONS = {

            {
                    5000000.0D,
                    102.0D,
                    5000004.0D
            },

            {
                    5000005.0D,
                    102.0D,
                    5000002.0D
            },

            {
                    4999995.0D,
                    102.0D,
                    5000002.0D
            },

            {
                    5000004.0D,
                    102.0D,
                    4999998.0D
            },

            {
                    4999996.0D,
                    102.0D,
                    4999998.0D
            }
    };


    /*
     * =========================================================
     * SHULKER TELEPORT BOUNDS
     * =========================================================
     *
     * 기존 셜커 전투실:
     *
     * 전체 내부:
     * X 4999990 ~ 5000010
     * Y 100 ~ 110
     * Z 4999990 ~ 5000010
     *
     * 실제 공기 공간 안쪽으로 제한한다.
     */
    private static final double MIN_X =
            4999991.0D;

    private static final double MAX_X =
            5000009.0D;

    private static final double MIN_Y =
            101.0D;

    private static final double MAX_Y =
            110.0D;

    private static final double MIN_Z =
            4999991.0D;

    private static final double MAX_Z =
            5000009.0D;


    /*
     * =========================================================
     * FIELDS
     * =========================================================
     */
    private final JavaPlugin plugin;

    private final NamespacedKey dungeonIdKey;


    /*
     * 현재 던전 셜커 UUID.
     */
    private final Set<UUID> activeDungeonMobs =
            new HashSet<>();


    /*
     * 사망/로그아웃 후 출구로 복귀해야 하는 플레이어.
     */
    private final Map<UUID, Location>
            pendingExitLocations =
            new HashMap<>();


    /*
     * 한 번에 한 명만 진행한다.
     */
    private UUID participantUuid;

    private World dungeonWorld;

    private boolean active =
            false;

    private boolean combatStarted =
            false;

    private BukkitTask countdownTask;


    public ShulkerDungeonService(
            JavaPlugin plugin
    ) {

        this.plugin =
                plugin;

        this.dungeonIdKey =
                new NamespacedKey(
                        plugin,
                        "shulker_dungeon_id"
                );
    }


    /*
     * =========================================================
     * ENTER
     * =========================================================
     */
    public boolean tryEnter(
            Player player
    ) {

        if (
                player == null
                || !player.isOnline()
                || player.isDead()
        ) {
            return false;
        }


        /*
         * 이미 다른 플레이어가 진행 중.
         */
        if (active) {

            player.sendMessage(
                    Component.text(
                            "[던전] 현재 셜커 던전을 다른 플레이어가 진행 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return false;
        }


        dungeonWorld =
                player.getWorld();

        participantUuid =
                player.getUniqueId();

        active =
                true;

        combatStarted =
                false;


        pendingExitLocations.remove(
                participantUuid
        );


        /*
         * 혹시 이전 서버 종료 등으로 남아 있던
         * 던전 셜커를 먼저 제거한다.
         */
        removeStaleDungeonEntities();


        Location destination =
                new Location(
                        dungeonWorld,
                        ENTRY_X,
                        ENTRY_Y,
                        ENTRY_Z,
                        player.getLocation()
                                .getYaw(),
                        player.getLocation()
                                .getPitch()
                );


        boolean teleported =
                player.teleport(
                        destination
                );


        if (!teleported) {

            player.sendMessage(
                    Component.text(
                            "[던전] 셜커 던전 입장 이동에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );

            resetState();

            return false;
        }


        player.sendMessage(
                Component.text(
                        "셜커 던전에 입장하셨습니다",
                        NamedTextColor.LIGHT_PURPLE
                )
        );


        startCountdown();

        return true;
    }


    /*
     * =========================================================
     * COUNTDOWN
     * =========================================================
     */
    private void startCountdown() {

        cancelCountdown();


        countdownTask =
                new BukkitRunnable() {

                    private int seconds =
                            3;


                    @Override
                    public void run() {

                        if (
                                !active
                                || participantUuid == null
                        ) {

                            cancel();
                            countdownTask = null;

                            return;
                        }


                        Player player =
                                plugin.getServer()
                                        .getPlayer(
                                                participantUuid
                                        );


                        if (
                                player == null
                                || !player.isOnline()
                                || player.isDead()
                        ) {

                            cancel();
                            countdownTask = null;

                            return;
                        }


                        if (seconds <= 0) {

                            player.clearTitle();

                            spawnShulkers();

                            cancel();
                            countdownTask = null;

                            return;
                        }


                        showCountdown(
                                player,
                                seconds
                        );


                        seconds--;
                    }

                }.runTaskTimer(
                        plugin,
                        0L,
                        20L
                );
    }


    private void showCountdown(
            Player player,
            int seconds
    ) {

        Title title =
                Title.title(
                        Component.text(
                                Integer.toString(
                                        seconds
                                ),
                                NamedTextColor.LIGHT_PURPLE
                        ),
                        Component.text(
                                "전투 시작",
                                NamedTextColor.GRAY
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


    private void cancelCountdown() {

        if (countdownTask == null) {
            return;
        }


        countdownTask.cancel();

        countdownTask =
                null;
    }


    /*
     * =========================================================
     * SPAWN 5 SHULKERS
     * =========================================================
     */
    private void spawnShulkers() {

        if (
                !active
                || dungeonWorld == null
        ) {
            return;
        }


        combatStarted =
                true;

        activeDungeonMobs.clear();


        for (
                double[] position
                : SPAWN_POSITIONS
        ) {

            Location location =
                    new Location(
                            dungeonWorld,
                            position[0],
                            position[1],
                            position[2]
                    );


            Shulker shulker =
                    dungeonWorld.spawn(
                            location,
                            Shulker.class
                    );


            PersistentDataContainer pdc =
                    shulker
                            .getPersistentDataContainer();


            pdc.set(
                    dungeonIdKey,
                    PersistentDataType.STRING,
                    DUNGEON_ID
            );


            shulker.setPersistent(
                    true
            );

            shulker.setRemoveWhenFarAway(
                    false
            );


            activeDungeonMobs.add(
                    shulker.getUniqueId()
            );
        }


        Player player =
                getParticipant();


        if (player != null) {

            player.sendMessage(
                    Component.text(
                            "[던전] 셜커 5마리를 모두 처치하십시오.",
                            NamedTextColor.WHITE
                    )
            );
        }


        plugin.getLogger().info(
                "[ShulkerDungeon] Spawned "
                        + activeDungeonMobs.size()
                        + " shulkers."
        );
    }


    /*
     * =========================================================
     * DUNGEON MOB IDENTIFICATION
     * =========================================================
     */
    public boolean isDungeonMob(
            LivingEntity entity
    ) {

        if (entity == null) {
            return false;
        }


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


    /*
     * =========================================================
     * MOB DEATH
     * =========================================================
     */
    public void onDungeonMobDeath(
            LivingEntity entity
    ) {

        if (
                !active
                || !combatStarted
                || !isDungeonMob(entity)
        ) {
            return;
        }


        activeDungeonMobs.remove(
                entity.getUniqueId()
        );


        if (!activeDungeonMobs.isEmpty()) {
            return;
        }


        completeDungeon();
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */
    private void completeDungeon() {

        if (!active) {
            return;
        }


        UUID winnerUuid =
                participantUuid;


        Player winner =
                winnerUuid == null
                        ? null
                        : plugin.getServer()
                                .getPlayer(
                                        winnerUuid
                                );


        /*
         * 상태부터 종료해서 중복 클리어를 차단한다.
         */
        active =
                false;

        combatStarted =
                false;


        cancelCountdown();

        removeDungeonEntities();


        if (
                winner != null
                && winner.isOnline()
        ) {

            winner.clearTitle();


            Location exit =
                    createExitLocation(
                            winner.getWorld(),
                            winner
                    );


            winner.teleport(
                    exit
            );


            Title clearTitle =
                    Title.title(
                            Component.text(
                                    "던전 클리어!",
                                    NamedTextColor.GREEN
                            ),
                            Component.text(
                                    "셜커 던전을 클리어했습니다.",
                                    NamedTextColor.LIGHT_PURPLE
                            ),
                            Title.Times.times(
                                    Duration.ofMillis(
                                            150L
                                    ),
                                    Duration.ofMillis(
                                            1800L
                                    ),
                                    Duration.ofMillis(
                                            300L
                                    )
                            )
                    );


            winner.showTitle(
                    clearTitle
            );


            grantReward(
                    winner
            );
        }


        participantUuid =
                null;

        dungeonWorld =
                null;
    }


    /*
     * =========================================================
     * REWARD
     * =========================================================
     *
     * 하나의 0~99 난수를 사용한다.
     *
     * 00 ~ 54 : 셜커 상자 1개 = 55%
     * 55 ~ 64 : 겉날개 1개     = 10%
     * 65 ~ 99 : 보상 없음       = 35%
     *
     * 두 보상이 동시에 지급되지 않는다.
     */
    private void grantReward(
            Player player
    ) {

        int roll =
                ThreadLocalRandom
                        .current()
                        .nextInt(
                                100
                        );


        ItemStack reward =
                null;

        String rewardName =
                null;


        if (roll < 55) {

            reward =
                    new ItemStack(
                            Material.SHULKER_BOX,
                            1
                    );

            rewardName =
                    "셜커 상자 1개";

        } else if (roll < 65) {

            reward =
                    new ItemStack(
                            Material.ELYTRA,
                            1
                    );

            rewardName =
                    "겉날개 1개";
        }


        if (reward == null) {

            player.sendMessage(
                    Component.text(
                            "[던전] 이번에는 보상을 획득하지 못했습니다.",
                            NamedTextColor.GRAY
                    )
            );

            return;
        }


        Map<Integer, ItemStack> leftovers =
                player.getInventory()
                        .addItem(
                                reward
                        );


        if (!leftovers.isEmpty()) {

            for (
                    ItemStack leftover
                    : leftovers.values()
            ) {

                player.getWorld()
                        .dropItemNaturally(
                                player.getLocation(),
                                leftover
                        );
            }
        }


        player.sendMessage(
                Component.text(
                        "[던전] 보상 획득: ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                rewardName,
                                NamedTextColor.YELLOW
                        )
                )
        );
    }


    /*
     * =========================================================
     * PLAYER DEATH = FAIL
     * =========================================================
     */
    public void onParticipantDeath(
            Player player
    ) {

        if (!isParticipant(player)) {
            return;
        }


        pendingExitLocations.put(
                player.getUniqueId(),
                createExitLocation(
                        player.getWorld(),
                        player
                )
        );


        failDungeon();
    }


    /*
     * =========================================================
     * PLAYER QUIT = FAIL
     * =========================================================
     */
    public void onParticipantQuit(
            Player player
    ) {

        if (!isParticipant(player)) {
            return;
        }


        pendingExitLocations.put(
                player.getUniqueId(),
                createExitLocation(
                        player.getWorld(),
                        player
                )
        );


        failDungeon();
    }


    /*
     * =========================================================
     * FAIL
     * =========================================================
     */
    private void failDungeon() {

        if (!active) {
            return;
        }


        active =
                false;

        combatStarted =
                false;


        cancelCountdown();

        removeDungeonEntities();


        Player player =
                getParticipant();


        if (
                player != null
                && player.isOnline()
                && !player.isDead()
        ) {

            player.clearTitle();

            player.teleport(
                    createExitLocation(
                            player.getWorld(),
                            player
                    )
            );


            player.sendMessage(
                    Component.text(
                            "[던전] 셜커 던전에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );


            pendingExitLocations.remove(
                    player.getUniqueId()
            );
        }


        participantUuid =
                null;

        dungeonWorld =
                null;
    }


    /*
     * =========================================================
     * RESPAWN EXIT
     * =========================================================
     */
    public Location consumePendingExitLocation(
            UUID uuid
    ) {

        Location location =
                pendingExitLocations.remove(
                        uuid
                );


        return location == null
                ? null
                : location.clone();
    }


    /*
     * =========================================================
     * RECONNECT EXIT
     * =========================================================
     */
    public void restorePendingExitOnJoin(
            Player player
    ) {

        Location location =
                pendingExitLocations.remove(
                        player.getUniqueId()
                );


        if (location == null) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }


                            player.teleport(
                                    location
                            );


                            player.sendMessage(
                                    Component.text(
                                            "[던전] 이전 셜커 던전은 실패 처리되었습니다.",
                                            NamedTextColor.RED
                                    )
                            );
                        }
                );
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
                && active
                && participantUuid != null
                && participantUuid.equals(
                        player.getUniqueId()
                );
    }


    private Player getParticipant() {

        if (participantUuid == null) {
            return null;
        }


        return plugin.getServer()
                .getPlayer(
                        participantUuid
                );
    }


    /*
     * =========================================================
     * SHULKER TELEPORT LIMIT
     * =========================================================
     */
    public boolean isInsideDungeon(
            Location location
    ) {

        if (
                location == null
                || dungeonWorld == null
                || location.getWorld() == null
                || location.getWorld()
                        != dungeonWorld
        ) {
            return false;
        }


        return location.getX() >= MIN_X
                && location.getX() <= MAX_X
                && location.getY() >= MIN_Y
                && location.getY() <= MAX_Y
                && location.getZ() >= MIN_Z
                && location.getZ() <= MAX_Z;
    }


    /*
     * =========================================================
     * REMOVE ACTIVE DUNGEON ENTITIES
     * =========================================================
     */
    private void removeDungeonEntities() {

        if (dungeonWorld == null) {

            activeDungeonMobs.clear();

            return;
        }


        for (
                UUID uuid
                : new HashSet<>(
                        activeDungeonMobs
                )
        ) {

            Entity entity =
                    dungeonWorld.getEntity(
                            uuid
                    );


            if (entity != null) {

                entity.remove();
            }
        }


        activeDungeonMobs.clear();


        /*
         * 남아 있는 셜커 탄환 제거.
         */
        Location center =
                new Location(
                        dungeonWorld,
                        500900.5D,
                        5.0D,
                        501000.5D
                );


        for (
                Entity entity
                : dungeonWorld.getNearbyEntities(
                        center,
                        18.0D,
                        10.0D,
                        18.0D
                )
        ) {

            if (entity instanceof ShulkerBullet) {

                entity.remove();
            }
        }
    }


    /*
     * =========================================================
     * STALE MOB CLEANUP
     * =========================================================
     */
    private void removeStaleDungeonEntities() {

        if (dungeonWorld == null) {
            return;
        }


        Location center =
                new Location(
                        dungeonWorld,
                        500900.5D,
                        5.0D,
                        501000.5D
                );


        for (
                Entity entity
                : dungeonWorld.getNearbyEntities(
                        center,
                        18.0D,
                        10.0D,
                        18.0D
                )
        ) {

            if (
                    entity instanceof LivingEntity living
                    && isDungeonMob(
                            living
                    )
            ) {

                entity.remove();

                continue;
            }


            if (entity instanceof ShulkerBullet) {

                entity.remove();
            }
        }


        activeDungeonMobs.clear();
    }


    /*
     * =========================================================
     * EXIT LOCATION
     * =========================================================
     */
    private Location createExitLocation(
            World world,
            Player player
    ) {

        return new Location(
                world,
                EXIT_X,
                EXIT_Y,
                EXIT_Z,
                player == null
                        ? 0.0F
                        : player.getLocation()
                                .getYaw(),
                player == null
                        ? 0.0F
                        : player.getLocation()
                                .getPitch()
        );
    }


    /*
     * =========================================================
     * RESET AFTER ENTRY FAILURE
     * =========================================================
     */
    private void resetState() {

        cancelCountdown();

        activeDungeonMobs.clear();

        participantUuid =
                null;

        dungeonWorld =
                null;

        active =
                false;

        combatStarted =
                false;
    }
}
