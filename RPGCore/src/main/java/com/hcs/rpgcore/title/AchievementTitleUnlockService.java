package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class AchievementTitleUnlockService {

    private static final String GAMBLING_TITLE_ID =
            "gambling_addiction";

    private static final String GAMBLING_PROGRESS_TYPE =
            "paid_gambling_count";

    private static final String GAMBLING_PROGRESS_TARGET =
            "total";

    private static final long GAMBLING_REQUIRED_COUNT =
            100L;

    private final RPGCorePlugin plugin;

    private final AchievementProgressRepository
            progressRepository;

    private final PlayerTitleCollectionRepository
            titleRepository;

    /*
     * 이미 칭호 보유가 확인된 플레이어는
     * 같은 접속 세션에서 불필요한 해금 검사를 줄인다.
     *
     * 진행도 자체는 계속 증가시킨다.
     */
    private final Set<UUID> gamblingUnlockedThisSession =
            ConcurrentHashMap.newKeySet();

    public AchievementTitleUnlockService(
            RPGCorePlugin plugin
    ) {

        this.plugin = plugin;

        this.progressRepository =
                new AchievementProgressRepository(
                        plugin.getDatabaseManager()
                );

        this.titleRepository =
                new PlayerTitleCollectionRepository(
                        plugin.getDatabaseManager()
                );
    }

    /**
     * 경마장 또는 슬롯머신의 실제 유료 이용이
     * 확정된 경우 1회 호출한다.
     */
    public void recordPaidGamblingUse(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> processPaidGamblingUse(playerUuid)
        );
    }

    private void processPaidGamblingUse(
            UUID playerUuid
    ) {

        final long progress;

        try {

            progress =
                    progressRepository.increment(
                            playerUuid,
                            GAMBLING_PROGRESS_TYPE,
                            GAMBLING_PROGRESS_TARGET
                    );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[Achievement] 유료 도박 이용 횟수 저장 실패: "
                            + playerUuid,
                    exception
            );

            return;
        }

        /*
         * 진행도는 100회 이후에도 계속 기록한다.
         * 이미 보유 확인된 경우에는 해금 DB 검사만 생략한다.
         */
        if (progress < GAMBLING_REQUIRED_COUNT
                || gamblingUnlockedThisSession.contains(
                        playerUuid
                )) {

            return;
        }

        final PlayerTitleCollectionRepository.Title
                definition;

        try {

            definition =
                    titleRepository.findDefinition(
                            GAMBLING_TITLE_ID
                    );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[Achievement] 도박중독 칭호 정의 조회 실패",
                    exception
            );

            return;
        }

        if (definition == null) {

            plugin.getLogger().warning(
                    "[Achievement] 칭호 정의가 없습니다: "
                            + GAMBLING_TITLE_ID
            );

            return;
        }

        final boolean newlyUnlocked;

        try {

            newlyUnlocked =
                    titleRepository.unlockTitle(
                            playerUuid,
                            GAMBLING_TITLE_ID,
                            GAMBLING_PROGRESS_TYPE
                    );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[Achievement] 도박중독 칭호 지급 실패: "
                            + playerUuid,
                    exception
            );

            return;
        }

        /*
         * 정의가 존재하는 상태에서 false라면
         * 이미 획득한 칭호로 판단할 수 있다.
         */
        gamblingUnlockedThisSession.add(
                playerUuid
        );

        if (!newlyUnlocked) {
            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> notifyUnlock(
                        playerUuid,
                        definition
                )
        );
    }

    private void notifyUnlock(
            UUID playerUuid,
            PlayerTitleCollectionRepository.Title title
    ) {

        Player player =
                Bukkit.getPlayer(playerUuid);

        if (player == null || !player.isOnline()) {
            return;
        }

        TextColor color =
                TextColor.fromHexString(
                        title.titleColor()
                );

        if (color == null) {
            color = NamedTextColor.WHITE;
        }

        player.sendMessage(
                Component.text(
                        "[칭호] ",
                        NamedTextColor.YELLOW
                )
                .append(
                        Component.text(
                                "[" + title.titleText() + "]",
                                color
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
