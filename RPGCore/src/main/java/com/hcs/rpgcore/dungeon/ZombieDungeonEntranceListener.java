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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ZombieDungeonEntranceListener
        implements Listener {

    private static final int TRIGGER_MIN_X = 1183;
    private static final int TRIGGER_MAX_X = 1185;

    /*
     * 새 좀비 던전 입장 게이트 블록 Y = 63
     * X 1183~1185 / Z -991~-989 의 3x3 영역
     */
    private static final int TRIGGER_BLOCK_Y = 63;

    private static final int TRIGGER_MIN_Z = -991;
    private static final int TRIGGER_MAX_Z = -989;

    /*
     * 현재 정상 작동이 확인된 입장 위치 유지.
     */
    private static final double DESTINATION_X =
            4000032.5D;

    private static final double DESTINATION_Y =
            205.0D;

    private static final double DESTINATION_Z =
            4000080.5D;

    private static final int CHECK_PERIOD_TICKS = 5;
    private static final int REQUIRED_TICKS = 60;

    private final JavaPlugin plugin;

    private final ZombieDungeonService dungeonService;

    private final Map<UUID, Integer> standingTicks =
            new HashMap<>();

    private final Map<UUID, Integer> lastDisplayedSecond =
            new HashMap<>();

    private final Set<UUID> activePlayers =
            new HashSet<>();

    private BukkitTask pollingTask;

    public ZombieDungeonEntranceListener(
            JavaPlugin plugin,
            ZombieDungeonService dungeonService
    ) {

        this.plugin = plugin;
        this.dungeonService = dungeonService;

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

        for (Player player
                : plugin.getServer()
                        .getOnlinePlayers()) {

            UUID uuid =
                    player.getUniqueId();

            if (!isStandingOnDungeonEntrance(
                    player
            )) {

                if (activePlayers.remove(uuid)) {

                    standingTicks.remove(uuid);
                    lastDisplayedSecond.remove(uuid);

                    clearCountdown(player);
                }

                continue;
            }

            activePlayers.add(uuid);

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
                seconds = 3;
            } else if (ticks <= 40) {
                seconds = 2;
            } else {
                seconds = 1;
            }

            Integer lastSecond =
                    lastDisplayedSecond.get(uuid);

            if (ticks < REQUIRED_TICKS
                    && (
                    lastSecond == null
                            || lastSecond != seconds
            )) {

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
                enterDungeon(player);
            }
        }
    }

    private void enterDungeon(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        if (!isStandingOnDungeonEntrance(
                player
        )) {

            reset(uuid);
            return;
        }

        reset(uuid);

    /*
     * 던전 성공/실패 후 복귀할
     * 고정 출구 위치를 저장한다.
     */
    Location returnLocation =
	new Location(
		player.getWorld(),
		1182.5,
                64.0,
                -982.5,
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

        clearCountdown(player);

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
                                "폐허가 된 지하묘지에 입장합니다.",
                                NamedTextColor.LIGHT_PURPLE
                        )
                )
        );

        /*
         * 텔레포트가 성공한 경우에만
         * 던전 참가 및 Wave 시스템 시작.
         */
        dungeonService.onPlayerEntered(
            player,
            returnLocation
    );
    }

    private boolean isStandingOnDungeonEntrance(
            Player player
    ) {

        Location location =
                player.getLocation();

        Block below =
                location.clone()
                        .subtract(
                                0.0D,
                                1.0D,
                                0.0D
                        )
                        .getBlock();

        int x = below.getX();
        int y = below.getY();
        int z = below.getZ();

        if (x < TRIGGER_MIN_X
                || x > TRIGGER_MAX_X) {
            return false;
        }

        if (z < TRIGGER_MIN_Z
                || z > TRIGGER_MAX_Z) {
            return false;
        }

        if (y != TRIGGER_BLOCK_Y) {
            return false;
        }

        return below.getType()
                == Material.CRYING_OBSIDIAN;
    }


    /*
     * =========================================================
     * DUNGEON COUNTDOWN DISPLAY
     * =========================================================
     *
     * RPG HUD는 ActionBar를 사용하므로
     * 던전 입장 카운트다운은 ActionBar를 사용하지 않는다.
     *
     * Subtitle 채널을 사용해서 HUD와 완전히 분리한다.
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
                                Duration.ofMillis(1100L),
                                Duration.ZERO
                        )
                );

        player.showTitle(title);
    }

    private void clearCountdown(
            Player player
    ) {

        player.clearTitle();
    }

    private void reset(UUID uuid) {

        standingTicks.remove(uuid);
        lastDisplayedSecond.remove(uuid);
        activePlayers.remove(uuid);
    }

    @EventHandler
public void onPlayerQuit(
        PlayerQuitEvent event
) {

    Player player =
            event.getPlayer();


    /*
     * 입장 발판 카운트다운 상태 정리.
     */
    reset(
            player.getUniqueId()
    );


    /*
     * 실제 던전 참가 상태도 전달.
     */
    dungeonService.onParticipantQuit(
            player
    );
}


/*
 * =========================================================
 * DUNGEON PLAYER DEATH
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


    dungeonService.onParticipantDeath(
            event.getPlayer()
    );
}


/*
 * =========================================================
 * DUNGEON RESPAWN RETURN
 * =========================================================
 *
 * 던전에서 사망한 플레이어는
 * 일반 리스폰 위치가 아니라
 * 던전 입장 직전 위치로 복귀한다.
 */
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


/*
 * =========================================================
 * DUNGEON RECONNECT RETURN
 * =========================================================
 *
 * 던전에서 로그아웃한 플레이어가
 * 내부에 남은 상태로 재접속하는 것을 방지한다.
 */
@EventHandler
public void onPlayerJoin(
        PlayerJoinEvent event
) {

    dungeonService
            .restorePendingExitOnJoin(
                    event.getPlayer()
            );
}

}
