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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EndermanDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * ENTRANCE AREA
     * =========================================================
     */

    private static final int TRIGGER_MIN_X =
            885;

    private static final int TRIGGER_MAX_X =
            887;

    private static final int TRIGGER_MIN_Z =
            -1568;

    private static final int TRIGGER_MAX_Z =
            -1566;

    /*
     * 외부 엔더맨 던전 입장 게이트.
     *
     * Y=84 한 층만 입장 영역으로 사용한다.
     */
    private static final int TRIGGER_MIN_Y =
            84;

    private static final int TRIGGER_MAX_Y =
            84;


    /*
     * =========================================================
     * DUNGEON ENTRY
     * =========================================================
     *
     * 입장 카운트다운 완료 후 실제 던전 내부로 이동한다.
     */
    private static final double DUNGEON_ENTRY_X =
            -10000360.0D;

    private static final double DUNGEON_ENTRY_Y =
            162.0D;

    private static final double DUNGEON_ENTRY_Z =
            -9999930.0D;


    /*
     * =========================================================
     * COUNTDOWN
     * =========================================================
     */

    private static final int CHECK_PERIOD_TICKS =
            5;

    /*
     * 60 ticks = 3초
     */
    private static final int REQUIRED_TICKS =
            60;


    private final JavaPlugin plugin;

    private final EndermanDungeonService dungeonService;

    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();

    private final Set<UUID> activePlayers =
            new HashSet<>();

    private BukkitTask pollingTask;


    public EndermanDungeonEntranceListener(
            JavaPlugin plugin,
            EndermanDungeonService dungeonService
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

        for (Player player
                : plugin.getServer()
                        .getOnlinePlayers()) {

            UUID uuid =
                    player.getUniqueId();

            /*
             * 이미 던전에 참가한 플레이어라면
             * 입장 카운트다운을 다시 시작하지 않는다.
             */
            if (dungeonService.isParticipant(player)) {

                resetCountdown(
                        player
                );

                continue;
            }

            if (!isInsideEntranceArea(
                    player
            )) {

                resetCountdown(
                        player
                );

                continue;
            }

            activePlayers.add(
                    uuid
            );

            int ticks =
                    standingTicks.getOrDefault(
                            uuid,
                            0
                    ) + CHECK_PERIOD_TICKS;

            standingTicks.put(
                    uuid,
                    ticks
            );

            int seconds;

            if (ticks <= 20) {

                seconds =
                        3;

            } else if (ticks <= 40) {

                seconds =
                        2;

            } else {

                seconds =
                        1;
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


    /*
     * =========================================================
     * ENTER
     * =========================================================
     */

    private void enterDungeon(
            Player player
    ) {

        if (!isInsideEntranceArea(
                player
        )) {

            resetCountdown(
                    player
            );

            return;
        }

        resetCountdown(
                player
        );

        clearCountdown(
                player
        );


        /*
         * =====================================================
         * DUNGEON TELEPORT
         * =====================================================
         *
         * 참가자 등록보다 먼저 실제 던전 내부로 이동한다.
         *
         * 텔레포트에 실패하면 참가자로 등록하지 않는다.
         */
        Location destination =
                new Location(
                        player.getWorld(),
                        DUNGEON_ENTRY_X,
                        DUNGEON_ENTRY_Y,
                        DUNGEON_ENTRY_Z,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );

        boolean teleported =
                player.teleport(
                        destination
                );

        if (!teleported) {

            player.sendMessage(
                    Component.text(
                            "[던전] 던전 입장에 실패했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * 내부 텔레포트가 성공한 플레이어만 참가자로 등록한다.
         */
        dungeonService.onPlayerEntered(
                player
        );
    }


    /*
     * =========================================================
     * ENTRANCE CHECK
     * =========================================================
     */

    private boolean isInsideEntranceArea(
            Player player
    ) {

        Location location =
                player.getLocation();

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();

        return x >= TRIGGER_MIN_X
                && x <= TRIGGER_MAX_X
                && y >= TRIGGER_MIN_Y
                && y <= TRIGGER_MAX_Y
                && z >= TRIGGER_MIN_Z
                && z <= TRIGGER_MAX_Z;
    }


    /*
     * =========================================================
     * PLAYER DEATH
     * =========================================================
     */

    @EventHandler
    public void onPlayerDeath(
            PlayerDeathEvent event
    ) {

        /*
         * =====================================================
         * DUNGEON KEEP INVENTORY
         * =====================================================
         *
         * 던전 참가자가 사망한 경우에는
         * 모든 인벤토리 아이템을 그대로 유지한다.
         */
        if (
                !dungeonService.isParticipant(
                        event.getPlayer()
                )
        ) {
            return;
        }

        event.setKeepInventory(
                true
        );

        event.getDrops()
                .clear();


        Player player =
                event.getPlayer();

        if (!dungeonService
                .isParticipant(player)) {

            return;
        }

        resetCountdown(
                player
        );

        player.sendMessage(
                Component.text(
                        "[던전] 전투 불능 상태가 되었습니다.",
                        NamedTextColor.RED
                )
        );

        dungeonService
                .onParticipantUnavailable(
                        player
                );
    }


    /*
     * =========================================================
     * RESPAWN
     * =========================================================
     */

    @EventHandler
    public void onPlayerRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!dungeonService
                .hasPendingExit(player)) {

            return;
        }

        /*
         * PlayerRespawnEvent 직후에는
         * 서버가 리스폰 위치 처리를 완료한 다음
         * 텔레포트시키는 편이 안정적이다.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> dungeonService
                                .completePendingExit(
                                        player
                                )
                );
    }


    /*
     * =========================================================
     * QUIT
     * =========================================================
     */

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        resetCountdown(
                player
        );

        if (!dungeonService
                .isParticipant(player)) {

            return;
        }

        dungeonService
                .onParticipantUnavailable(
                        player
                );
    }


    /*
     * =========================================================
     * JOIN
     * =========================================================
     *
     * 던전에서 로그아웃한 뒤 재접속한 경우에도
     * 출구로 이동시킨다.
     */

    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!dungeonService
                .hasPendingExit(player)) {

            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> dungeonService
                                .completePendingExit(
                                        player
                                ),
                        1L
                );
    }


    /*
     * =========================================================
     * DUNGEON ENTRANCE COUNTDOWN DISPLAY
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


    private void clearCountdown(
            Player player
    ) {

        player.clearTitle();
    }


    private void resetCountdown(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        standingTicks.remove(
                uuid
        );

        lastDisplayedSecond.remove(
                uuid
        );

        activePlayers.remove(
                uuid
        );
    }
}
