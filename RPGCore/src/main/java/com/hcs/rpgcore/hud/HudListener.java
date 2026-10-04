package com.hcs.rpgcore.hud;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;

import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HudListener
        implements Listener {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final HudService hudService;

    public HudListener(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            HudService hudService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.hudService = hudService;
    }

    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        /*
         * PlayerConnectionListener가
         * DB 데이터를 준비할 시간을 준다.
         */
        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> loadProfile(
                                player
                        ),
                        20L
                );
    }

    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        hudService.remove(
                event.getPlayer()
                        .getUniqueId()
        );
    }

    private void loadProfile(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> {

                            try {

                                PlayerData playerData =
                                        playerRepository
                                                .findPlayer(
                                                        uuid
                                                );

                                if (playerData == null) {

                                    plugin.getLogger()
                                            .warning(
                                                    "HUD 초기화 실패: 플레이어 데이터 없음 - "
                                                            + uuid
                                            );

                                    return;
                                }

                                hudService.updateProfile(
                                        playerData
                                );

                            } catch (SQLException exception) {

                                plugin.getLogger()
                                        .severe(
                                                "HUD 초기화 실패: "
                                                        + exception
                                                        .getMessage()
                                        );

                                exception.printStackTrace();
                            }
                        }
                );
    }
}
