package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * RPGCore 레벨 달성 칭호.
 *
 * Lv.80: 고인물
 * Lv.90: 썩은물
 *
 * DB 저장은 비동기 작업으로 처리한다.
 * 칭호는 획득만 하며 자동 장착하지 않는다.
 */
public final class LevelTitleUnlockService {

    private final RPGCorePlugin plugin;

    private final PlayerTitleCollectionRepository repository;

    public LevelTitleUnlockService(
            RPGCorePlugin plugin
    ) {
        this.plugin = plugin;

        this.repository =
                new PlayerTitleCollectionRepository(
                        plugin.getDatabaseManager()
                );
    }

    /**
     * 플레이어의 RPGCore 레벨이 확인되었을 때 호출.
     *
     * 접속 시와 레벨 상승 시 모두 호출할 수 있다.
     * 중복 호출은 DB의 획득 기록으로 방지한다.
     */
    public void checkLevel(
            UUID playerUuid,
            int level
    ) {

        if (playerUuid == null || level < 80) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> {

                    if (level >= 80) {
                        unlock(
                                playerUuid,
                                "level_80",
                                "고인물",
                                TitleRarity.LEGENDARY
                        );
                    }

                    if (level >= 90) {
                        unlock(
                                playerUuid,
                                "level_90",
                                "썩은물",
                                TitleRarity.MYTHIC
                        );
                    }
                }
        );
    }

    private void unlock(
            UUID playerUuid,
            String titleId,
            String titleText,
            TitleRarity rarity
    ) {

        final boolean newlyUnlocked;

        try {

            newlyUnlocked =
                    repository.unlockTitle(
                            playerUuid,
                            titleId,
                            "level_at_least"
                    );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 레벨 칭호 지급 실패: "
                            + playerUuid
                            + " / "
                            + titleId,
                    exception
            );

            return;
        }

        if (!newlyUnlocked) {
            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    Player player =
                            Bukkit.getPlayer(playerUuid);

                    if (player == null || !player.isOnline()) {
                        return;
                    }

                    TextColor titleColor =
                            TextColor.fromHexString(
                                    rarity.hexColor()
                            );

                    if (titleColor == null) {
                        titleColor = NamedTextColor.WHITE;
                    }

                    player.sendMessage(
                            Component.text(
                                    "[칭호] ",
                                    NamedTextColor.YELLOW
                            )
                            .append(
                                    Component.text(
                                            "[" + titleText + "]",
                                            titleColor
                                    )
                            )
                            .append(
                                    Component.text(
                                            " 칭호를 획득했습니다! "
                                                    + "칭호 관리 NPC에게서 장착할 수 있습니다.",
                                            NamedTextColor.WHITE
                                    )
                            )
                    );
                }
        );
    }
}
