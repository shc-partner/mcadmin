package com.hcs.rpgcore.command;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.level.LevelService;
import com.hcs.rpgcore.mana.ManaService;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;

import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public final class StatsCommand
        implements CommandExecutor {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final LevelService levelService;
    private final StatService statService;
    private final ManaService manaService;

    public StatsCommand(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            LevelService levelService,
            StatService statService,
            ManaService manaService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.levelService = levelService;
        this.statService = statService;
        this.manaService = manaService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    Component.text(
                            "이 명령어는 플레이어만 사용할 수 있습니다.",
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        UUID uuid =
                player.getUniqueId();

        Bukkit.getScheduler()
                .runTaskAsynchronously(
                        plugin,
                        () -> loadStats(
                                player,
                                uuid
                        )
                );

        return true;
    }

    private void loadStats(
            Player player,
            UUID uuid
    ) {

        try {

            PlayerData playerData =
                    playerRepository.findPlayer(
                            uuid
                    );

            if (playerData == null) {

                sendSync(
                        player,
                        Component.text(
                                "RPG 캐릭터 데이터를 찾을 수 없습니다.",
                                NamedTextColor.RED
                        )
                );

                return;
            }

            PlayerStats stats =
                    statService.calculate(
                            playerData,
                            player
                    );

            /*
             * ManaService가 초기화되어 있다면
             * 실제 런타임 MANA 사용.
             *
             * 아직 초기화되지 않은 매우 짧은 접속 직후라면
             * 계산상 최대 MANA를 임시 사용.
             */
            double currentMana;
            double maximumMana;

            if (manaService.isInitialized(uuid)) {

                currentMana =
                        manaService.getCurrentMana(
                                uuid
                        );

                maximumMana =
                        manaService.getMaximumMana(
                                uuid
                        );

            } else {

                currentMana =
                        stats.maxMana();

                maximumMana =
                        stats.maxMana();
            }

            sendStats(
                    player,
                    playerData,
                    stats,
                    currentMana,
                    maximumMana
            );

        } catch (SQLException exception) {

            plugin.getLogger()
                    .severe(
                            "/stats 조회 실패: "
                                    + exception.getMessage()
                    );

            exception.printStackTrace();

            sendSync(
                    player,
                    Component.text(
                            "캐릭터 정보를 불러오지 못했습니다.",
                            NamedTextColor.RED
                    )
            );
        }
    }

    private void sendStats(
            Player player,
            PlayerData playerData,
            PlayerStats stats,
            double currentMana,
            double maximumMana
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {

                                return;
                            }

                            player.sendMessage(
                                    Component.text(
                                            "────── RPG CHARACTER ──────",
                                            NamedTextColor.GOLD
                                    )
                            );

                            player.sendMessage(
                                    Component.text(
                                            "Lv."
                                                    + playerData
                                                    .getLevel(),
                                            NamedTextColor.YELLOW
                                    )
                            );

                            String playerClass =
                                    playerData
                                            .getPlayerClass();

                            if (playerClass == null
                                    || playerClass.isBlank()) {

                                playerClass =
                                        "NONE";
                            }

                            player.sendMessage(
                                    Component.text(
                                            "직업: "
                                                    + playerClass,
                                            NamedTextColor.AQUA
                                    )
                            );

                            if (playerData.getLevel()
                                    >= levelService
                                    .getMaximumLevel()) {

                                player.sendMessage(
                                        Component.text(
                                                "EXP: MAX",
                                                NamedTextColor.GOLD
                                        )
                                );

                            } else {

                                long requiredExperience =
                                        levelService
                                                .getRequiredExperience(
                                                        playerData
                                                                .getLevel()
                                                );

                                double percentage =
                                        requiredExperience <= 0L
                                                ? 0.0
                                                : (
                                                (double)
                                                        playerData
                                                                .getExperience()
                                                        / requiredExperience
                                        ) * 100.0;

                                player.sendMessage(
                                        Component.text(
                                                "EXP: "
                                                        + playerData
                                                        .getExperience()
                                                        + " / "
                                                        + requiredExperience
                                                        + "  ("
                                                        + String.format(
                                                                Locale.US,
                                                                "%.1f",
                                                                percentage
                                                        )
                                                        + "%)",
                                                NamedTextColor.GREEN
                                        )
                                );
                            }

                            player.sendMessage(
                                    Component.text(
                                            "----------------------------",
                                            NamedTextColor.DARK_GRAY
                                    )
                            );

                            player.sendMessage(
                                    Component.text(
                                            "HP   : "
                                                    + format(
                                                    stats.maxHealth()
                                            ),
                                            NamedTextColor.RED
                                    )
                            );

                            player.sendMessage(
                                    Component.text(
                                            "ATK  : "
                                                    + format(
                                                    stats.attack()
                                            ),
                                            NamedTextColor.YELLOW
                                    )
                            );

                            player.sendMessage(
                                    Component.text(
                                            "DEF  : "
                                                    + format(
                                                    stats.defense()
                                            ),
                                            NamedTextColor.AQUA
                                    )
                            );

                            player.sendMessage(
                                    Component.text(
                                            "MANA : "
                                                    + format(
                                                    currentMana
                                            )
                                                    + " / "
                                                    + format(
                                                    maximumMana
                                            ),
                                            NamedTextColor.BLUE
                                    )
                            );

                            player.sendMessage(
                                    Component.text(
                                            "────────────────────────────",
                                            NamedTextColor.GOLD
                                    )
                            );
                        }
                );
    }

    private String format(
            double value
    ) {

        if (value == Math.floor(value)) {

            return Long.toString(
                    (long) value
            );
        }

        return String.format(
                Locale.US,
                "%.1f",
                value
        );
    }

    private void sendSync(
            Player player,
            Component component
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (player.isOnline()) {

                                player.sendMessage(
                                        component
                                );
                            }
                        }
                );
    }
}
