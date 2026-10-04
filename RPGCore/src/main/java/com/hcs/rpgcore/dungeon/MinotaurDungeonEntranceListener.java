package com.hcs.rpgcore.dungeon;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MinotaurDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * ENTRANCE
     * =========================================================
     *
     * 사용자 지정 입장 감지 영역:
     *
     * -94 71 -7
     * ~
     * -91 71 -4
     */

    private static final int TRIGGER_MIN_X = -94;
    private static final int TRIGGER_MAX_X = -91;

    private static final int TRIGGER_Y = 71;

    private static final int TRIGGER_MIN_Z = -7;
    private static final int TRIGGER_MAX_Z = -4;


    /*
     * 던전 내부 입장 좌표.
     *
     * 사용자가 지정한 좌표를 임의로 0.5 보정하지 않는다.
     */
    private static final double DESTINATION_X =
            2000138.0D;

    private static final double DESTINATION_Y =
            193.0D;

    private static final double DESTINATION_Z =
            2000177.0D;


    private static final int CHECK_PERIOD_TICKS =
            5;

    private static final int REQUIRED_TICKS =
            60;


    private final JavaPlugin plugin;

    private final MinotaurDungeonService dungeonService;

    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();

    private BukkitTask pollingTask;


    public MinotaurDungeonEntranceListener(
            JavaPlugin plugin,
            MinotaurDungeonService dungeonService
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

            if (!isInsideEntrance(
                    player.getLocation()
            )) {

                standingTicks.remove(
                        uuid
                );

                lastDisplayedSecond.remove(
                        uuid
                );

                continue;
            }


            /*
             * 이미 미노타우로스 던전 참가 상태라면
             * 같은 입구에서 재카운트하지 않는다.
             */
            if (dungeonService
                    .isParticipant(uuid)) {

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
                            &&
                    (
                            lastSecond == null
                                    ||
                            lastSecond != seconds
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

                standingTicks.remove(
                        uuid
                );

                lastDisplayedSecond.remove(
                        uuid
                );
            }
        }
    }


    private void enterDungeon(
            Player player
    ) {

        if (!isInsideEntrance(
                player.getLocation()
        )) {
            return;
        }


        /*
         * 미노타우로스 던전의 성공/실패 출구는
         * 사용자 지정 고정 좌표를 사용한다.
         */
        Location returnLocation =
                new Location(
                        player.getWorld(),
                        -79.0D,
                        70.0D,
                        -8.0D,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );


        Location destination =
                new Location(
                        player.getWorld(),
                        DESTINATION_X,
                        DESTINATION_Y,
                        DESTINATION_Z,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );


        player.clearTitle();


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

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();

        return x >= TRIGGER_MIN_X
                && x <= TRIGGER_MAX_X
                && y == TRIGGER_Y
                && z >= TRIGGER_MIN_Z
                && z <= TRIGGER_MAX_Z;
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
}
