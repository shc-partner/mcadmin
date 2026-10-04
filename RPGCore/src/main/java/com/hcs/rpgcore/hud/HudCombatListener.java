package com.hcs.rpgcore.hud;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import org.bukkit.event.player.PlayerRespawnEvent;


public final class HudCombatListener
        implements Listener {

    private final RPGCorePlugin plugin;

    private final HudService hudService;


    public HudCombatListener(
            RPGCorePlugin plugin,
            HudService hudService
    ) {

        this.plugin =
                plugin;

        this.hudService =
                hudService;
    }


    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onDamage(
            EntityDamageEvent event
    ) {

        if (!(
                event.getEntity()
                instanceof Player player
        )) {
            return;
        }

        requestNextTick(
                player
        );
    }


    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onRegainHealth(
            EntityRegainHealthEvent event
    ) {

        if (!(
                event.getEntity()
                instanceof Player player
        )) {
            return;
        }

        requestNextTick(
                player
        );
    }


    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        requestNextTick(
                event.getPlayer()
        );
    }


    private void requestNextTick(
            Player player
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            hudService.requestUpdate(
                                    player
                            );
                        }
                );
    }
}
