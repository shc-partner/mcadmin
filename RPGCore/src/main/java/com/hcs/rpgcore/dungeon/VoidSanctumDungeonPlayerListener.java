package com.hcs.rpgcore.dungeon;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.plugin.Plugin;


/*
 * =========================================================
 * VOID SANCTUM DUNGEON PLAYER LISTENER
 * =========================================================
 */
public final class VoidSanctumDungeonPlayerListener
        implements Listener {

    private final Plugin plugin;

    private final VoidSanctumDungeonService dungeonService;


    public VoidSanctumDungeonPlayerListener(
            Plugin plugin,
            VoidSanctumDungeonService dungeonService
    ) {

        this.plugin =
                plugin;

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * PLAYER DEATH
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerDeath(
            PlayerDeathEvent event
    ) {

        if (
                !dungeonService
                        .isParticipant(
                                event.getPlayer()
                                        .getUniqueId()
                        )
        ) {

            return;
        }


        /*
         * 던전 사망 시 인벤토리 유지.
         */
        event.setKeepInventory(
                true
        );

        event.getDrops()
                .clear();


        dungeonService
                .onParticipantUnavailable(
                        event.getPlayer()
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
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        dungeonService
                .onParticipantUnavailable(
                        event.getPlayer()
                );
    }


    /*
     * =========================================================
     * PLAYER RESPAWN
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !dungeonService
                        .consumePendingExit(
                                player.getUniqueId()
                        )
        ) {

            return;
        }


        Location exit =
                dungeonService
                        .createExitLocation(
                                player.getWorld()
                        );


        if (exit != null) {

            event.setRespawnLocation(
                    exit
            );
        }
    }


    /*
     * =========================================================
     * PLAYER REJOIN
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !dungeonService
                        .consumePendingExit(
                                player.getUniqueId()
                        )
        ) {

            return;
        }


        Location exit =
                dungeonService
                        .createExitLocation(
                                player.getWorld()
                        );


        if (exit == null) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> player.teleport(
                                exit
                        )
                );
    }
}
