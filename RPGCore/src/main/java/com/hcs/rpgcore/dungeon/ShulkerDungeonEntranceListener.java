package com.hcs.rpgcore.dungeon;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.plugin.java.JavaPlugin;


public final class ShulkerDungeonEntranceListener
        implements Listener {

    /*
     * =========================================================
     * ENTRANCE 3 x 3
     * =========================================================
     *
     * 사용자 지정:
     *
     * 1205 64 -1007
     * ~
     * 1207 64 -1005
     */
    private static final int MIN_X =
            1205;

    private static final int MAX_X =
            1207;

    private static final int Y =
            64;

    private static final int MIN_Z =
            -1007;

    private static final int MAX_Z =
            -1005;


    private final JavaPlugin plugin;

    private final ShulkerDungeonService
            dungeonService;


    /*
     * 같은 입장 영역 안에서
     * PlayerMoveEvent가 반복 호출되어도
     * 입장을 여러 번 요청하지 않도록 한다.
     */
    private final Set<UUID> insideEntrance =
            new HashSet<>();


    public ShulkerDungeonEntranceListener(
            JavaPlugin plugin,
            ShulkerDungeonService dungeonService
    ) {

        this.plugin =
                plugin;

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * ENTRANCE DETECTION
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerMove(
            PlayerMoveEvent event
    ) {

        if (event.getTo() == null) {
            return;
        }


        /*
         * 같은 블록 내부에서 고개만 돌리는 경우는 무시.
         */
        if (
                event.getFrom().getBlockX()
                        == event.getTo()
                                .getBlockX()
                && event.getFrom().getBlockY()
                        == event.getTo()
                                .getBlockY()
                && event.getFrom().getBlockZ()
                        == event.getTo()
                                .getBlockZ()
        ) {
            return;
        }


        Player player =
                event.getPlayer();


        UUID uuid =
                player.getUniqueId();


        if (!isInsideEntrance(player)) {

            insideEntrance.remove(
                    uuid
            );

            return;
        }


        /*
         * 이미 같은 3x3 안에서 처리된 플레이어.
         */
        if (!insideEntrance.add(uuid)) {
            return;
        }


        dungeonService.tryEnter(
                player
        );
    }


    /*
     * =========================================================
     * ENTRANCE AREA
     * =========================================================
     *
     * 좌표가 "플레이어가 서 있는 좌표"로 지정된 경우와
     * "발판 블록 좌표"로 지정된 경우 둘 다 대응한다.
     */
    private boolean isInsideEntrance(
            Player player
    ) {

        Location location =
                player.getLocation();


        Block feet =
                location.getBlock();


        Block below =
                location.clone()
                        .subtract(
                                0.0D,
                                1.0D,
                                0.0D
                        )
                        .getBlock();


        return isEntranceBlock(
                feet
        ) || isEntranceBlock(
                below
        );
    }


    private boolean isEntranceBlock(
            Block block
    ) {

        int x =
                block.getX();

        int y =
                block.getY();

        int z =
                block.getZ();


        return x >= MIN_X
                && x <= MAX_X
                && y == Y
                && z >= MIN_Z
                && z <= MAX_Z;
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


        insideEntrance.remove(
                player.getUniqueId()
        );


        dungeonService.onParticipantDeath(
                player
        );
    }


    /*
     * =========================================================
     * PLAYER QUIT
     * =========================================================
     */
    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();


        insideEntrance.remove(
                player.getUniqueId()
        );


        dungeonService.onParticipantQuit(
                player
        );
    }


    /*
     * =========================================================
     * RESPAWN -> EXIT
     * =========================================================
     */
    @EventHandler
    public void onPlayerRespawn(
            PlayerRespawnEvent event
    ) {

        Location exit =
                dungeonService
                        .consumePendingExitLocation(
                                event.getPlayer()
                                        .getUniqueId()
                        );


        if (exit == null) {
            return;
        }


        event.setRespawnLocation(
                exit
        );
    }


    /*
     * =========================================================
     * RECONNECT -> EXIT
     * =========================================================
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
