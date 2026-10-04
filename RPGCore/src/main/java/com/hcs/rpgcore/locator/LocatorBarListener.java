package com.hcs.rpgcore.locator;

import com.hcs.rpgcore.RPGCorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class LocatorBarListener
        implements Listener {

    private final RPGCorePlugin plugin;
    private final LocatorBarService service;


    public LocatorBarListener(
            RPGCorePlugin plugin,
            LocatorBarService service
    ) {

        this.plugin = plugin;
        this.service = service;
    }


    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        /*
         * 접속 직후에는 클라이언트 월드 초기화가
         * 진행 중일 수 있으므로 20 ticks 뒤 생성한다.
         */
        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    event.getPlayer()
                                            .isOnline()
                            ) {

                                service.ensureDummy(
                                        event.getPlayer()
                                );
                            }
                        },
                        20L
                );
    }


    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        service.removeDummy(
                event.getPlayer()
        );
    }


    @EventHandler
    public void onWorldChange(
            PlayerChangedWorldEvent event
    ) {

        service.removeDummy(
                event.getPlayer()
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    event.getPlayer()
                                            .isOnline()
                            ) {

                                service.ensureDummy(
                                        event.getPlayer()
                                );
                            }
                        },
                        1L
                );
    }


    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        service.removeDummy(
                event.getPlayer()
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    event.getPlayer()
                                            .isOnline()
                            ) {

                                service.ensureDummy(
                                        event.getPlayer()
                                );
                            }
                        },
                        1L
                );
    }
}
