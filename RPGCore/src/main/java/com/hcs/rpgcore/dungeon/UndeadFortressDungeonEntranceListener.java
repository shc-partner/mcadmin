package com.hcs.rpgcore.dungeon;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


/*
 * =============================================================
 * Lv20~30 UNDEAD FORTRESS ENTRANCE
 * =============================================================
 *
 * ZombieDungeonEntranceListener 규칙을 기준으로 재구성.
 *
 * 흐름:
 * 입장 영역 감지
 * -> 3초 Subtitle 카운트다운
 * -> 던전 내부 좌표 teleport
 * -> teleport 성공 후 참가자 등록
 * -> 클리어/실패 시 지정 퇴장 좌표로 복귀
 *
 * 돔 구조이므로 카운트다운 시작 후 영역 이탈 여부는
 * 다시 검사하지 않는다.
 */
public final class UndeadFortressDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * EXTERNAL ENTRANCE TRIGGER
     * =========================================================
     *
     * 현재 언데드 성채의 기존 입장 영역 값을 유지한다.
     * 새 외부 입구를 확정하면 이 상수만 변경하면 된다.
     */
    private static final double TRIGGER_MIN_X =
            1241.0D;

    private static final double TRIGGER_MAX_X =
            1244.0D;

    private static final double TRIGGER_MIN_Y =
            64.0D;

    private static final double TRIGGER_MAX_Y =
            65.0D;

    private static final double TRIGGER_MIN_Z =
            -1127.0D;

    private static final double TRIGGER_MAX_Z =
            -1124.0D;


    /*
     * =========================================================
     * DUNGEON DESTINATION
     * =========================================================
     *
     * 현재 언데드 성채의 기존 중심 좌표를 사용한다.
     */
    private static final double DESTINATION_X =
            5500965.0D;

    private static final double DESTINATION_Y =
            76.0D;

    private static final double DESTINATION_Z =
            5501185.0D;


    /*
     * =========================================================
     * FIXED EXIT LOCATION
     * =========================================================
     *
     * 현재 언데드 성채의 기존 RETURN 좌표를 유지한다.
     */
    private static final double EXIT_X =
            1240.0D;

    private static final double EXIT_Y =
            64.0D;

    private static final double EXIT_Z =
            -1133.0D;


    private static final int CHECK_PERIOD_TICKS =
            5;

    private static final int REQUIRED_TICKS =
            60;


    private final JavaPlugin plugin;

    private final UndeadFortressDungeonService dungeonService;

    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();

    private final Map<UUID, Boolean> countdownActive =
            new HashMap<>();

    private BukkitTask pollingTask;


    public UndeadFortressDungeonEntranceListener(
            JavaPlugin plugin,
            UndeadFortressDungeonService dungeonService
    ) {

        this.plugin =
                plugin;

        this.dungeonService =
                dungeonService;

        startPollingTask();
    }


    private void startPollingTask() {

        pollingTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::checkPlayers,
                                1L,
                                CHECK_PERIOD_TICKS
                        );
    }


    private void checkPlayers() {

        for (
                Player player
                : plugin.getServer()
                        .getOnlinePlayers()
        ) {

            UUID uuid =
                    player.getUniqueId();

            /*
             * 이미 던전 참가 중이면 입장 카운트다운을 만들지 않는다.
             */
            if (dungeonService.isParticipant(player)) {

                resetCountdownState(
                        uuid
                );

                continue;
            }


            boolean active =
                    countdownActive.getOrDefault(
                            uuid,
                            false
                    );


            /*
             * 카운트다운이 아직 시작되지 않았다면
             * 입장 영역 안에 있을 때만 시작한다.
             */
            if (!active) {

                if (!isInsideEntrance(
                        player.getLocation()
                )) {
                    continue;
                }

                countdownActive.put(
                        uuid,
                        true
                );

                standingTicks.put(
                        uuid,
                        0
                );

                lastDisplayedSecond.remove(
                        uuid
                );
            }


            /*
             * 돔 구조이므로 카운트다운 시작 후에는
             * 입장 영역 이탈 여부를 다시 검사하지 않는다.
             */
            if (!player.isOnline()) {

                resetCountdownState(
                        uuid
                );

                continue;
            }


            int ticks =
                    standingTicks.getOrDefault(
                            uuid,
                            0
                    )
                            + CHECK_PERIOD_TICKS;

            standingTicks.put(
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
                    lastDisplayedSecond.get(
                            uuid
                    );

            if (
                    ticks < REQUIRED_TICKS
                            && (
                            lastSecond == null
                                    || lastSecond != seconds
                    )
            ) {

                lastDisplayedSecond.put(
                        uuid,
                        seconds
                );

                showCountdown(
                        player,
                        seconds
                );
            }


            if (ticks >= REQUIRED_TICKS) {
                enterDungeon(
                        player
                );
            }
        }
    }


    private void enterDungeon(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        resetCountdownState(
                uuid
        );


        Location returnLocation =
                new Location(
                        player.getWorld(),
                        EXIT_X,
                        EXIT_Y,
                        EXIT_Z,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );


        World world =
                player.getWorld();

        Location destination =
                new Location(
                        world,
                        DESTINATION_X,
                        DESTINATION_Y,
                        DESTINATION_Z,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );


        clearCountdown(
                player
        );


        boolean teleported =
                player.teleport(
                        destination
                );

        if (!teleported) {

            player.sendMessage(
                    Component.text(
                            "[던전] 입장 이동에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        player.sendMessage(
                Component.text(
                        "[던전] ",
                        NamedTextColor.DARK_PURPLE
                ).append(
                        Component.text(
                                "언데드 성채에 입장합니다.",
                                NamedTextColor.LIGHT_PURPLE
                        )
                )
        );


        /*
         * 실제 teleport 성공 후에만 참가자로 등록한다.
         */
        dungeonService.onPlayerEntered(
                player,
                returnLocation
        );
    }


    private boolean isInsideEntrance(
            Location location
    ) {

        if (location == null) {
            return false;
        }

        double x =
                location.getX();

        double y =
                location.getY();

        double z =
                location.getZ();

        return x >= TRIGGER_MIN_X
                && x < TRIGGER_MAX_X
                && y >= TRIGGER_MIN_Y
                && y < TRIGGER_MAX_Y
                && z >= TRIGGER_MIN_Z
                && z < TRIGGER_MAX_Z;
    }


    private void showCountdown(
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


    private void clearCountdown(
            Player player
    ) {

        player.clearTitle();
    }


    private void resetCountdownState(
            UUID uuid
    ) {

        standingTicks.remove(
                uuid
        );

        lastDisplayedSecond.remove(
                uuid
        );

        countdownActive.remove(
                uuid
        );
    }


    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        resetCountdownState(
                player.getUniqueId()
        );

        dungeonService.onParticipantQuit(
                player
        );
    }


    @EventHandler
    public void onPlayerDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!dungeonService.isParticipant(
                player
        )) {
            return;
        }

        event.setKeepInventory(
                true
        );

        event.getDrops()
                .clear();

        dungeonService.onParticipantDeath(
                player
        );
    }


    @EventHandler
    public void onPlayerRespawn(
            PlayerRespawnEvent event
    ) {

        Location returnLocation =
                dungeonService
                        .consumePendingExitLocation(
                                event.getPlayer()
                                        .getUniqueId()
                        );

        if (returnLocation == null) {
            return;
        }

        event.setRespawnLocation(
                returnLocation
        );
    }


    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        resetCountdownState(
                event.getPlayer()
                        .getUniqueId()
        );

        dungeonService
                .restorePendingExitOnJoin(
                        event.getPlayer()
                );
    }
}
