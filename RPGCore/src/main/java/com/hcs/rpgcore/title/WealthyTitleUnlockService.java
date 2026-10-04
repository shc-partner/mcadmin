package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 골드 1,000만 이상 보유 시 재벌 칭호 자동 획득.
 *
 * Vault 잔액 조회: 메인 스레드
 * MariaDB 기록 저장: 비동기 스레드
 *
 * 칭호 획득만 수행하며 자동 장착하지 않는다.
 */
public final class WealthyTitleUnlockService {

    private static final String TITLE_ID = "wealthy_10m";

    private static final long REQUIRED_GOLD = 10_000_000L;

    private static final long INITIAL_DELAY_TICKS = 100L;
    private static final long CHECK_INTERVAL_TICKS = 1_200L;

    private final RPGCorePlugin plugin;
    private final ShopEconomyService economyService;
    private final PlayerTitleCollectionRepository titleRepository;

    private final Set<UUID> processing =
            ConcurrentHashMap.newKeySet();

    private final Set<UUID> unlockedThisSession =
            ConcurrentHashMap.newKeySet();

    private BukkitTask task;

    public WealthyTitleUnlockService(
            RPGCorePlugin plugin,
            ShopEconomyService economyService,
            PlayerTitleCollectionRepository titleRepository
    ) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.titleRepository = titleRepository;
    }

    /**
     * 서버 시작 후 온라인 플레이어의 잔액을 주기적으로 확인한다.
     */
    public void start() {

        if (task != null) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::checkOnlinePlayers,
                INITIAL_DELAY_TICKS,
                CHECK_INTERVAL_TICKS
        );

        plugin.getLogger().info(
                "[PlayerTitle] 재벌 칭호 자동 획득 검사 시작"
        );
    }

    private void checkOnlinePlayers() {

        for (Player player : Bukkit.getOnlinePlayers()) {

            UUID uuid = player.getUniqueId();

            /*
             * 이번 접속에서 이미 획득을 확인했다면
             * 이후 불필요한 DB 작업을 하지 않는다.
             */
            if (unlockedThisSession.contains(uuid)) {
                continue;
            }

            if (processing.contains(uuid)) {
                continue;
            }

            final double balance;

            try {
                /*
                 * Vault Economy 접근은 메인 스레드에서 수행.
                 */
                balance = economyService.getBalance(player);

            } catch (RuntimeException exception) {

                plugin.getLogger().log(
                        Level.WARNING,
                        "[PlayerTitle] 골드 잔액 조회 실패: " + uuid,
                        exception
                );

                continue;
            }

            if (!Double.isFinite(balance)
                    || balance < REQUIRED_GOLD) {
                continue;
            }

            if (!processing.add(uuid)) {
                continue;
            }

            Bukkit.getScheduler().runTaskAsynchronously(
                    plugin,
                    () -> unlockWealthyTitle(uuid)
            );
        }
    }

    private void unlockWealthyTitle(UUID uuid) {

        final boolean newlyUnlocked;

        try {

            newlyUnlocked = titleRepository.unlockTitle(
                    uuid,
                    TITLE_ID,
                    "balance_at_least"
            );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 재벌 칭호 획득 기록 저장 실패: "
                            + uuid,
                    exception
            );

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> processing.remove(uuid)
            );

            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    processing.remove(uuid);

                    /*
                     * 새로 지급됐거나 이전에 획득한 칭호라면
                     * 이번 접속 동안 재검사하지 않는다.
                     */
                    if (!newlyUnlocked) {

                        /*
                         * unlockTitle()의 false는 이미 획득했거나
                         * 칭호 정의가 없다는 두 경우를 포함한다.
                         * 정의가 실제로 존재하는지 확인하기 전에는
                         * 획득 완료 캐시에 추가하지 않는다.
                         */
                        Bukkit.getScheduler().runTaskAsynchronously(
                                plugin,
                                () -> verifyExistingUnlock(uuid)
                        );

                        return;
                    }

                    unlockedThisSession.add(uuid);

                    Player player = Bukkit.getPlayer(uuid);

                    if (player == null || !player.isOnline()) {
                        return;
                    }

                    TextColor color =
                            TitleRarity.LEGENDARY.hexColor() == null
                                    ? NamedTextColor.GOLD
                                    : TextColor.fromHexString(
                                            TitleRarity.LEGENDARY.hexColor()
                                    );

                    if (color == null) {
                        color = NamedTextColor.GOLD;
                    }

                    player.sendMessage(
                            Component.text(
                                    "[칭호] ",
                                    NamedTextColor.YELLOW
                            )
                            .append(
                                    Component.text(
                                            "[재벌]",
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
        );
    }

    /**
     * 중복 지급과 칭호 정의 누락을 구분한다.
     */
    private void verifyExistingUnlock(UUID uuid) {

        try {

            boolean alreadyUnlocked =
                    titleRepository.findUnlockedTitles(uuid)
                            .stream()
                            .anyMatch(title ->
                                    TITLE_ID.equals(title.titleId())
                            );

            if (alreadyUnlocked) {
                unlockedThisSession.add(uuid);
            } else {

                plugin.getLogger().warning(
                        "[PlayerTitle] 재벌 칭호 정의 또는 "
                                + "획득 처리 확인 필요: " + uuid
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 재벌 칭호 획득 확인 실패: "
                            + uuid,
                    exception
            );
        }
    }

    /**
     * 서버 종료 시 반복 작업 중지.
     */
    public void shutdown() {

        if (task != null) {
            task.cancel();
            task = null;
        }

        processing.clear();
        unlockedThisSession.clear();
    }
}
