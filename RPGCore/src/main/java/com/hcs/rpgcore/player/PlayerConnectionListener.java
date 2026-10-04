package com.hcs.rpgcore.player;

import com.hcs.rpgcore.RPGCorePlugin;
import java.sql.SQLException;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerConnectionListener implements Listener {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final PlayerNameTagService playerNameTagService;

    private final com.hcs.rpgcore.title.PlayerTitleRepository
            titleRepository;

    private final com.hcs.rpgcore.title.PlayerTitleDisplayService
            titleDisplayService;

    public PlayerConnectionListener(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            PlayerNameTagService playerNameTagService,
            com.hcs.rpgcore.title.PlayerTitleRepository titleRepository,
            com.hcs.rpgcore.title.PlayerTitleDisplayService titleDisplayService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.playerNameTagService = playerNameTagService;
        this.titleRepository = titleRepository;
        this.titleDisplayService = titleDisplayService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                playerRepository.createOrUpdatePlayer(uuid, playerName);

                String displayName =
                        playerRepository.findDisplayName(
                                uuid
                        );

                com.hcs.rpgcore.title.PlayerTitleRepository.PlayerTitle title =
                        titleRepository.findTitle(uuid);

                com.hcs.rpgcore.title.SpecialTitleRepository
                        specialTitleRepository =
                        new com.hcs.rpgcore.title.SpecialTitleRepository(
                                plugin.getDatabaseManager()
                        );

                com.hcs.rpgcore.title.SpecialTitleRepository.SpecialTitle
                        activeSpecialTitle =
                        specialTitleRepository.findActive(uuid);

                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {

                            Player onlinePlayer =
                                    Bukkit.getPlayer(
                                            uuid
                                    );

                            if (
                                    onlinePlayer == null
                                            ||
                                    !onlinePlayer.isOnline()
                                            ||
                                    onlinePlayer != player
                            ) {
                                return;
                            }

                            titleDisplayService.setLoaded(
                                    uuid,
                                    displayName,
                                    title
                            );

                            /*
                             * 특수 칭호는 일반 칭호보다 우선 표시한다.
                             * 일반 칭호의 장착 기록은 유지한다.
                             */
                            if (activeSpecialTitle != null) {

                                titleDisplayService.setSpecialTitle(
                                        uuid,
                                        new com.hcs.rpgcore.title
                                                .PlayerTitleRepository.PlayerTitle(
                                                        uuid,
                                                        activeSpecialTitle.titleText(),
                                                        activeSpecialTitle.titleColor()
                                                )
                                );

                            } else {

                                titleDisplayService.clearSpecialTitle(
                                        uuid
                                );
                            }

                            Component nameComponent =
                                    Component.text(
                                            displayName == null
                                                    || displayName.isBlank()
                                                    ? onlinePlayer.getName()
                                                    : displayName
                                    );

                            onlinePlayer.displayName(
                                    nameComponent
                            );

                            onlinePlayer.playerListName(
                                    nameComponent
                            );


                            if (
                                    displayName != null
                                            &&
                                    !displayName.isBlank()
                            ) {

                                playerNameTagService.apply(
                                        onlinePlayer,
                                        displayName
                                );

                            } else {

                                playerNameTagService.clear(
                                        onlinePlayer
                                );
                            }
                        }
                );

                plugin.getLogger().info(
                        "플레이어 DB 동기화 완료: "
                                + playerName
                                + " (" + uuid + ")"
                );
            } catch (SQLException exception) {
                plugin.getLogger().severe(
                        "플레이어 DB 저장 실패: "
                                + playerName
                                + " - "
                                + exception.getMessage()
                );

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player onlinePlayer = Bukkit.getPlayer(uuid);

                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        onlinePlayer.sendMessage(
                                Component.text(
                                        "캐릭터 데이터를 불러오지 못했습니다. "
                                                + "관리자에게 문의해 주세요.",
                                        NamedTextColor.RED
                                )
                        );
                    }
                });
            }
        });
    }
}
