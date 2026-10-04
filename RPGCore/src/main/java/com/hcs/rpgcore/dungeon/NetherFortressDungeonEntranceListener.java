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
 * Lv40~50 NETHER FORTRESS ENTRANCE
 * =============================================================
 *
 * 흐름:
 * 외부 게이트 영역 감지
 * -> 3초 Subtitle 카운트다운
 * -> 네더 요새 내부 좌표 teleport
 * -> teleport 성공 후 참가자 등록
 * -> 기존 네더 요새 Wave 시스템 시작
 *
 * 게이트 구조 안으로 들어오면 카운트다운이 시작되며,
 * 시작 후에는 영역 이탈 여부를 다시 검사하지 않는다.
 */
public final class NetherFortressDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * EXTERNAL ENTRANCE TRIGGER
     * =========================================================
     *
     * 사용자 지정:
     * 1260 69 -1668
     * ~
     * 1262 69 -1670
     *
     * 반열린 범위로 표현한다.
     */
    private static final double TRIGGER_MIN_X =
            1260.0D;

    private static final double TRIGGER_MAX_X =
            1263.0D;

    private static final double TRIGGER_MIN_Y =
            69.0D;

    private static final double TRIGGER_MAX_Y =
            70.0D;

    private static final double TRIGGER_MIN_Z =
            -1670.0D;

    private static final double TRIGGER_MAX_Z =
            -1667.0D;


    /*
     * =========================================================
     * DUNGEON DESTINATION
     * =========================================================
     */
    private static final double DESTINATION_X =
            7999565.0D;

    private static final double DESTINATION_Y =
            229.0D;

    private static final double DESTINATION_Z =
            80002041.0D;


    /*
     * =========================================================
     * FIXED EXIT
     * =========================================================
     */
    private static final double EXIT_X =
            1257.0D;

    private static final double EXIT_Y =
            69.0D;

    private static final double EXIT_Z =
            -1661.0D;


    private static final int CHECK_PERIOD_TICKS =
            5;

    private static final int REQUIRED_TICKS =
            60;


    private final JavaPlugin plugin;

    private final NetherFortressDungeonService dungeonService;

    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();

    private final Map<UUID, Boolean> countdownActive =
            new HashMap<>();

    private BukkitTask pollingTask;


    public NetherFortressDungeonEntranceListener(
            JavaPlugin plugin,
            NetherFortressDungeonService dungeonService
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
             * 이미 네더 요새 참가 중이면
             * 새 입장 카운트다운을 만들지 않는다.
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
             * 아직 카운트다운이 시작되지 않았다면
             * 게이트 영역 안에 있을 때만 시작한다.
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
             * 게이트에 진입해 카운트다운이 시작된 뒤에는
             * 영역 이탈 여부를 다시 검사하지 않는다.
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
                                "네더 요새에 입장합니다.",
                                NamedTextColor.LIGHT_PURPLE
                        )
                )
        );

        /*
         * 실제 teleport 성공 후에만 기존 던전 시스템을 시작한다.
         *
         * NetherFortressDungeonService는 현재 고정 RETURN 좌표를
         * 사용하므로 returnLocation은 여기서 좌표 일치 확인용이다.
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
                        .consumePendingReturnLocation(
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

        Player player =
                event.getPlayer();

        resetCountdownState(
                player.getUniqueId()
        );

        Location returnLocation =
                dungeonService
                        .consumePendingReturnLocation(
                                player.getUniqueId()
                        );

        if (returnLocation != null) {
            player.teleport(
                    returnLocation
            );
        }
    }
}
