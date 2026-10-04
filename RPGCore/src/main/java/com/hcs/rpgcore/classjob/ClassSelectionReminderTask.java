package com.hcs.rpgcore.classjob;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;

import java.sql.SQLException;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ClassSelectionReminderTask
        implements Runnable {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;

    public ClassSelectionReminderTask(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository
    ) {

        this.plugin =
                plugin;

        this.playerRepository =
                playerRepository;
    }

    @Override
    public void run() {

        /*
         * Bukkit Player 목록은 메인 스레드에서 읽고,
         * DB 조회만 비동기로 넘긴다.
         */
        for (Player player : Bukkit.getOnlinePlayers()) {

            UUID uuid =
                    player.getUniqueId();

            Bukkit.getScheduler()
                    .runTaskAsynchronously(
                            plugin,
                            () -> checkPlayer(
                                    uuid
                            )
                    );
        }
    }

    private void checkPlayer(
            UUID uuid
    ) {

        try {

            PlayerData playerData =
                    playerRepository.findPlayer(
                            uuid
                    );

            if (playerData == null) {
                return;
            }

            /*
             * Lv.10 미만이면 전직 대상이 아니다.
             */
            if (playerData.getLevel() < 10) {
                return;
            }

            /*
             * 이미 직업을 선택했다면
             * 안내하지 않는다.
             */
            if (hasSelectedClass(
                    playerData
            )) {
                return;
            }

            Bukkit.getScheduler()
                    .runTask(
                            plugin,
                            () -> sendReminder(
                                    uuid
                            )
                    );

        } catch (SQLException exception) {

            plugin.getLogger()
                    .warning(
                            "직업 선택 안내 DB 조회 실패: "
                                    + exception.getMessage()
                    );
        }
    }

    private boolean hasSelectedClass(
            PlayerData playerData
    ) {

        String playerClass =
                playerData.getPlayerClass();

        if (playerClass == null) {
            return false;
        }

        playerClass =
                playerClass.trim();

        return !playerClass.isEmpty()
                && !"NONE".equalsIgnoreCase(
                        playerClass
                );
    }

    private void sendReminder(
            UUID uuid
    ) {

        Player player =
                Bukkit.getPlayer(
                        uuid
                );

        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }

        player.sendMessage(
                Component.text(
                        "[전직 안내] ",
                        NamedTextColor.GOLD
                ).append(
                        Component.text(
                                "Lv.10에 도달했습니다. "
                                        + "직업을 선택해야 더 성장할 수 있습니다.",
                                NamedTextColor.YELLOW
                        )
                )
        );

        player.sendMessage(
                Component.text(
                        "전사: /class warrior",
                        NamedTextColor.RED
                )
        );

        player.sendMessage(
                Component.text(
                        "마법사: /class mage",
                        NamedTextColor.AQUA
                )
        );
    }
}
