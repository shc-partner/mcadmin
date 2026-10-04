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

public final class RedDragonDungeonPlayerListener
        implements Listener {

    private final Plugin plugin;

    private final RedDragonDungeonService dungeonService;


    public RedDragonDungeonPlayerListener(
            Plugin plugin,
            RedDragonDungeonService dungeonService
    ) {

        this.plugin =
                plugin;

        this.dungeonService =
                dungeonService;
    }


    @EventHandler(
            priority = EventPriority.NORMAL
    )
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
                                .getUniqueId()
                )
        ) {
            return;
        }

        event.setKeepInventory(
                true
        );

        event.getDrops()
                .clear();


        dungeonService.onParticipantUnavailable(
                event.getPlayer()
        );
    }


    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        dungeonService.onParticipantUnavailable(
                event.getPlayer()
        );
    }


    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !dungeonService.consumePendingExit(
                        player.getUniqueId()
                )
        ) {

            return;
        }


        Location exit =
                dungeonService.createExitLocation(
                        player.getWorld()
                );


        if (exit != null) {

            event.setRespawnLocation(
                    exit
            );
        }
    }


    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !dungeonService.consumePendingExit(
                        player.getUniqueId()
                )
        ) {

            return;
        }


        Location exit =
                dungeonService.createExitLocation(
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
