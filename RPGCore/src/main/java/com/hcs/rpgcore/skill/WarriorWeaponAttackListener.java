package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;


public final class WarriorWeaponAttackListener
        implements Listener {

    private final RPGCorePlugin plugin;

    private final WarriorWeaponAttackService
            weaponAttackService;


    private final Set<UUID> pendingPlayers =
            ConcurrentHashMap.newKeySet();


    public WarriorWeaponAttackListener(
            RPGCorePlugin plugin,
            WarriorWeaponAttackService weaponAttackService
    ) {

        this.plugin =
                plugin;

        this.weaponAttackService =
                weaponAttackService;
    }


    @EventHandler
    public void onItemHeld(
            PlayerItemHeldEvent event
    ) {

        scheduleCapture(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onSwapHand(
            PlayerSwapHandItemsEvent event
    ) {

        scheduleCapture(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (
                event.getWhoClicked()
                        instanceof Player player
        ) {

            scheduleCapture(
                    player,
                    1L
            );
        }
    }


    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (
                event.getWhoClicked()
                        instanceof Player player
        ) {

            scheduleCapture(
                    player,
                    1L
            );
        }
    }


    @EventHandler
    public void onDrop(
            PlayerDropItemEvent event
    ) {

        scheduleCapture(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onBreak(
            PlayerItemBreakEvent event
    ) {

        scheduleCapture(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        scheduleCapture(
                event.getPlayer(),
                30L
        );
    }


    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        scheduleCapture(
                event.getPlayer(),
                5L
        );
    }


    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        UUID uuid =
                event.getPlayer()
                        .getUniqueId();


        pendingPlayers.remove(
                uuid
        );


        weaponAttackService.remove(
                uuid
        );
    }


    private void scheduleCapture(
            Player player,
            long delay
    ) {

        UUID uuid =
                player.getUniqueId();


        if (!pendingPlayers.add(uuid)) {
            return;
        }


        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            pendingPlayers.remove(
                                    uuid
                            );


                            Player onlinePlayer =
                                    Bukkit.getPlayer(
                                            uuid
                                    );


                            if (
                                    onlinePlayer == null
                                    || !onlinePlayer.isOnline()
                            ) {
                                return;
                            }


                            weaponAttackService.capture(
                                    onlinePlayer
                            );
                        },
                        delay
                );
    }
}
