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

public final class RedDragonDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * OUTSIDE ENTRANCE
     * =========================================================
     *
     * 2540 184 -1134
     * ~
     * 2538 184 -1136
     */

    private static final int TRIGGER_MIN_X =
            2538;

    private static final int TRIGGER_MAX_X =
            2540;

    private static final int TRIGGER_Y =
            184;

    private static final int TRIGGER_MIN_Z =
            -1136;

    private static final int TRIGGER_MAX_Z =
            -1134;


    /*
     * 드래곤 성채 첫 진입 위치.
     */
    private static final double DESTINATION_X =
            -9999735.0D;

    private static final double DESTINATION_Y =
            261.0D;

    private static final double DESTINATION_Z =
            1000001.0D;


    private static final int CHECK_PERIOD_TICKS =
            5;

    private static final int REQUIRED_TICKS =
            60;


    private final JavaPlugin plugin;

    private final RedDragonDungeonService dungeonService;


    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();


    private BukkitTask pollingTask;


    public RedDragonDungeonEntranceListener(
            JavaPlugin plugin,
            RedDragonDungeonService dungeonService
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
             * 이미 참가자가 된 플레이어는
             * 외부 입구 카운트다운을 다시 실행하지 않는다.
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
                            "[던전] 드래곤의 성채로 이동하지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 실제 텔레포트 성공 이후에만
         * 던전 참가자로 등록한다.
         */
        dungeonService.onPlayerEntered(
                player
        );
    }


    private boolean isInsideEntrance(
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
                                "드래곤의 성채로 이동까지 "
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
