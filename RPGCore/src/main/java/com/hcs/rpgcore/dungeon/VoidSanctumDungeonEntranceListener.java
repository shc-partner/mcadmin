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


/*
 * =========================================================
 * VOID SANCTUM DUNGEON ENTRANCE
 * =========================================================
 *
 * 권장 레벨:
 * Lv.70 ~ 80
 *
 * 던전명:
 * 공허의 성전
 */
public final class VoidSanctumDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * OUTSIDE ENTRANCE
     * =========================================================
     *
     * 20 82 -699
     * ~
     * 17 82 -702
     *
     * X 17 ~ 20
     * Y 82
     * Z -702 ~ -699
     */

    private static final int TRIGGER_MIN_X =
            17;

    private static final int TRIGGER_MAX_X =
            20;

    private static final int TRIGGER_Y =
            82;

    private static final int TRIGGER_MIN_Z =
            -702;

    private static final int TRIGGER_MAX_Z =
            -699;


    /*
     * =========================================================
     * DUNGEON ENTRY LOCATION
     * =========================================================
     */

    private static final double DESTINATION_X =
            -9999573.0D;

    private static final double DESTINATION_Y =
            129.0D;

    private static final double DESTINATION_Z =
            19999105.0D;


    /*
     * 5 tick마다 검사.
     * 총 60 tick = 3초.
     */

    private static final int CHECK_PERIOD_TICKS =
            5;

    private static final int REQUIRED_TICKS =
            60;


    private final JavaPlugin plugin;

    private final VoidSanctumDungeonService dungeonService;


    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();


    private BukkitTask pollingTask;


    public VoidSanctumDungeonEntranceListener(
            JavaPlugin plugin,
            VoidSanctumDungeonService dungeonService
    ) {

        this.plugin =
                plugin;

        this.dungeonService =
                dungeonService;

        startPollingTask();
    }


    /*
     * =========================================================
     * POLLING
     * =========================================================
     */

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
             * 이미 공허의 성전 참가자로 등록된 플레이어는
             * 외부 입장 카운트다운을 다시 실행하지 않는다.
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


    /*
     * =========================================================
     * ENTER DUNGEON
     * =========================================================
     */

    private void enterDungeon(
            Player player
    ) {

        /*
         * 카운트다운 종료 순간에도
         * 반드시 입구 4x4 내부에 있어야 한다.
         */
        if (!isInsideEntrance(
                player.getLocation()
        )) {

            return;
        }


        /*
         * 현재 던전이 사용 중이면
         * 내부로 보내지 않는다.
         *
         * RedDragon과 달리 teleport 후 거절되는 상황을
         * 처음부터 방지한다.
         */
        if (!dungeonService
                .canEnterDungeon()) {

            player.clearTitle();

            player.sendMessage(
                    Component.text(
                            "[던전] 현재 공허의 성전은 공략 중입니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        Location destination =
                new Location(
                        player.getWorld(),
                        DESTINATION_X,
                        DESTINATION_Y,
                        DESTINATION_Z,
                        player.getLocation()
                                .getYaw(),
                        player.getLocation()
                                .getPitch()
                );


        player.clearTitle();


        boolean teleported =
                player.teleport(
                        destination
                );


        if (!teleported) {

            player.sendMessage(
                    Component.text(
                            "[던전] 공허의 성전으로 이동하지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 실제 텔레포트 성공 후에만
         * 참가자 등록 및 던전 시작.
         */
        dungeonService.onPlayerEntered(
                player
        );
    }


    /*
     * =========================================================
     * ENTRANCE AREA
     * =========================================================
     */

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


        return
                x >= TRIGGER_MIN_X
                && x <= TRIGGER_MAX_X

                && y == TRIGGER_Y

                && z >= TRIGGER_MIN_Z
                && z <= TRIGGER_MAX_Z;
    }


    /*
     * =========================================================
     * COUNTDOWN
     * =========================================================
     */

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
