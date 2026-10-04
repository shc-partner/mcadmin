package com.hcs.rpgcore.stat;

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
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerStatListener
        implements Listener {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final StatService statService;
    private final PlayerStatApplier statApplier;

    public PlayerStatListener(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            StatService statService,
            PlayerStatApplier statApplier
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.statService = statService;
        this.statApplier = statApplier;
    }

    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> loadAndApply(player),
                10L
        );
    }

    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> loadAndApply(player),
                1L
        );
    }

    private void loadAndApply(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> {

                    try {

                        PlayerData playerData =
                                playerRepository.findPlayer(
                                        uuid
                                );

                        if (playerData == null) {
                            return;
                        }


                        Bukkit.getScheduler().runTask(
                                plugin,
                                () -> {

                                    Player onlinePlayer =
                                            Bukkit.getPlayer(
                                                    uuid
                                            );

                                    if (onlinePlayer == null
                                            || !onlinePlayer.isOnline()) {
                                        return;
                                    }

                                    PlayerStats finalStats =
                                        statService.calculate(
                                                playerData,
                                                onlinePlayer
                                        );

                                statApplier.apply(
                                        onlinePlayer,
                                        finalStats
                                );
                                }
                        );

                    } catch (SQLException exception) {

                        plugin.getLogger().severe(
                                "플레이어 스탯 로딩 실패: "
                                        + exception.getMessage()
                        );

                        exception.printStackTrace();
                    }
                }
        );
    }
}
