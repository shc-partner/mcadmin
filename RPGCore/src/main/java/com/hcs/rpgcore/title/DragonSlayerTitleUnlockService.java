package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 레드 드래곤 던전 클리어 시 용살자 칭호 획득.
 *
 * 참가자 목록은 메인 스레드에서 복사한다.
 * DB 저장은 비동기로 수행한다.
 * 신규 획득 메시지는 메인 스레드에서 전송한다.
 *
 * 칭호는 자동 장착하지 않는다.
 */
public final class DragonSlayerTitleUnlockService {

    private static final String TITLE_ID = "dragon_slayer";

    private final RPGCorePlugin plugin;

    private final PlayerTitleCollectionRepository repository;

    public DragonSlayerTitleUnlockService(
            RPGCorePlugin plugin
    ) {
        this.plugin = plugin;

        this.repository =
                new PlayerTitleCollectionRepository(
                        plugin.getDatabaseManager()
                );
    }

    /**
     * 던전 클리어가 확정된 시점에 호출한다.
     * participants 원본을 비동기 작업에 전달하지 않는다.
     */
    public void grantToParticipants(
            Collection<UUID> participants
    ) {

        if (participants == null || participants.isEmpty()) {
            return;
        }

        final Set<UUID> recipients =
                new HashSet<>(participants);

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> {

                    for (UUID uuid : recipients) {

                        final boolean newlyUnlocked;

                        try {

                            newlyUnlocked =
                                    repository.unlockTitle(
                                            uuid,
                                            TITLE_ID,
                                            "dungeon_clear"
                                    );

                        } catch (SQLException exception) {

                            plugin.getLogger().log(
                                    Level.SEVERE,
                                    "[PlayerTitle] 용살자 칭호 지급 실패: "
                                            + uuid,
                                    exception
                            );

                            continue;
                        }

                        /*
                         * 이미 획득한 플레이어에게는
                         * 메시지를 반복 전송하지 않는다.
                         */
                        if (!newlyUnlocked) {
                            continue;
                        }

                        Bukkit.getScheduler().runTask(
                                plugin,
                                () -> notifyNewUnlock(uuid)
                        );
                    }
                }
        );
    }

    private void notifyNewUnlock(
            UUID uuid
    ) {

        Player player = Bukkit.getPlayer(uuid);

        if (player == null || !player.isOnline()) {
            return;
        }

        TextColor mythicColor =
                TextColor.fromHexString(
                        TitleRarity.MYTHIC.hexColor()
                );

        if (mythicColor == null) {
            mythicColor = NamedTextColor.GOLD;
        }

        player.sendMessage(
                Component.text(
                        "[칭호] ",
                        NamedTextColor.YELLOW
                )
                .append(
                        Component.text(
                                "[용살자]",
                                mythicColor
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
}
