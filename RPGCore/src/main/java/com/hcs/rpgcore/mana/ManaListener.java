package com.hcs.rpgcore.mana;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;

import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class ManaListener implements Listener {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final StatService statService;
    private final ManaService manaService;

    public ManaListener(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            StatService statService,
            ManaService manaService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.statService = statService;
        this.manaService = manaService;
    }

    /*
     * 플레이어 접속.
     *
     * DB의 RPG 레벨을 기준으로
     * 최대 MANA를 계산한다.
     */
    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();

        /*
         * PlayerConnectionListener가
         * DB 정보를 준비할 시간을 준다.
         */
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> initializeMana(player),
                20L
        );
    }

    /*
     * 사망 후 리스폰하면
     * MANA 완전 회복.
     */
    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        UUID uuid =
                event.getPlayer()
                        .getUniqueId();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> manaService.refill(uuid),
                1L
        );
    }

    /*
     * 로그아웃하면
     * 서버 메모리에서 런타임 MANA 제거.
     */
    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        manaService.remove(
                event.getPlayer()
                        .getUniqueId()
        );
    }

    private void initializeMana(
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

                            plugin.getLogger().warning(
                                    "MANA 초기화 실패: 플레이어 데이터 없음 - "
                                            + uuid
                            );

                            return;
                        }

                        PlayerStats stats =
                                statService.calculate(
                                        playerData
                                );

                        manaService.initialize(
                                uuid,
                                stats.maxMana()
                        );

                    } catch (SQLException exception) {

                        plugin.getLogger().severe(
                                "MANA 초기화 실패: "
                                        + exception.getMessage()
                        );

                        exception.printStackTrace();
                    }
                }
        );
    }
}
