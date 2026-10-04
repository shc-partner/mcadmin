package com.hcs.rpgcore.horse;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;

import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.entity.Player;

public final class HorseRaceBetService {

    private static final long MIN_BET = 100L;

    /*
     * Vault의 double 금액 정밀도를 고려한 상한.
     * 실제 베팅은 이보다 낮은 운영 상한을 별도로 둘 수 있다.
     */
    private static final long MAX_BET = 100_000_000L;

    private final RPGCorePlugin plugin;
    private final ShopEconomyService economy;
    private final HorseRaceRepository repository;

    private final com.hcs.rpgcore.title.AchievementTitleUnlockService
            achievementTitleUnlockService;

    public HorseRaceBetService(
            RPGCorePlugin plugin,
            ShopEconomyService economy,
            HorseRaceRepository repository,
            com.hcs.rpgcore.title.AchievementTitleUnlockService
                    achievementTitleUnlockService
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.repository = repository;
        this.achievementTitleUnlockService =
                achievementTitleUnlockService;
    }

    /**
     * NPC 이용 또는 재접속 시 미지급 보상을 확인한다.
     * 지급 요청 중 상태는 자동 재시도하지 않는다.
     */

    /**
     * 정상적인 서버 종료로 중단된 경기만 환불 대기로 전환한다.
     *
     * Vault 거래 중단 여부가 불분명한 DEBIT_IN_FLIGHT는
     * 이 메서드에서 변경하지 않는다.
     */
    public boolean markRefundPendingForShutdown(long raceId) {

        try {
            boolean changed = repository.changeStatus(
                    raceId,
                    "DEBIT_CONFIRMED",
                    "REFUND_PENDING"
            );

            if (!changed) {
                plugin.getLogger().severe(
                        "Horse race shutdown refund status "
                                + "requires review: race="
                                + raceId
                );
            }

            return changed;

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race shutdown refund record failed: race="
                            + raceId,
                    exception
            );

            return false;
        }
    }

    /**
     * 정상 종료로 취소된 경기의 베팅금을 반환한다.
     * 지급 요청이 시작된 후 실패하면 자동 재시도하지 않는다.
     */
    public boolean settleRefund(
            Player player,
            long raceId
    ) {

        try {
            HorseRaceRepository.RaceRecord race =
                    repository.findById(raceId);

            if (race == null
                    || !race.playerUuid().equals(
                            player.getUniqueId()
                    )
                    || !"REFUND_PENDING".equals(
                            race.status()
                    )) {
                return false;
            }

            if (!repository.changeStatus(
                    raceId,
                    "REFUND_PENDING",
                    "REFUND_IN_FLIGHT"
            )) {
                return false;
            }

            if (!economy.deposit(
                    player,
                    race.wager()
            )) {

                plugin.getLogger().severe(
                        "Horse race refund deposit "
                                + "requires review: race="
                                + raceId
                );

                return false;
            }

            if (!repository.changeStatus(
                    raceId,
                    "REFUND_IN_FLIGHT",
                    "REFUND_CONFIRMED"
            )) {

                plugin.getLogger().severe(
                        "Horse race refund paid but DB update "
                                + "failed: race="
                                + raceId
                );

                return false;
            }

            player.sendMessage(
                    "§e[경마] 서버 종료로 중단된 경기의 "
                            + race.wager()
                            + "골드가 환불되었습니다."
            );

            return true;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race refund requires review: race="
                            + raceId,
                    exception
            );

            return false;
        }
    }

    public boolean preparePlayer(Player player) {

        try {
            HorseRaceRepository.RaceRecord pending =
                    repository.findUnresolvedByPlayer(
                            player.getUniqueId()
                    );

            if (pending != null
                    && "REFUND_PENDING".equals(
                            pending.status()
                    )) {

                if (!settleRefund(
                        player,
                        pending.raceId()
                )) {

                    player.sendMessage(
                            "§c[경마] 환불 확인이 필요합니다. "
                                    + "경기 번호: "
                                    + pending.raceId()
                    );

                    return false;
                }
            }

            if (pending != null
                    && "PAYOUT_PENDING".equals(
                            pending.status()
                    )) {

                if (!settlePayout(
                        player,
                        pending.raceId()
                )) {

                    player.sendMessage(
                            "§c[경마] 보상 지급 확인이 필요합니다. "
                                    + "경기 번호: "
                                    + pending.raceId()
                    );

                    return false;
                }
            }

            return canStart(player);

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race settlement lookup failed",
                    exception
            );

            player.sendMessage(
                    "§c[경마] 이전 경기 기록을 확인할 수 없습니다."
            );

            return false;
        }
    }

    public boolean canStart(Player player) {

        try {
            HorseRaceRepository.RaceRecord pending =
                    repository.findUnresolvedByPlayer(
                            player.getUniqueId()
                    );

            if (pending == null) {
                return true;
            }

            player.sendMessage(
                    "§c[경마] 이전 경기의 정산 확인이 필요합니다. "
                            + "경기 번호: "
                            + pending.raceId()
            );

            return false;

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race pending lookup failed",
                    exception
            );

            player.sendMessage(
                    "§c[경마] 경기 기록을 확인할 수 없습니다."
            );

            return false;
        }
    }

    /*
     * 성공하면 경기 ID 반환.
     * 실패하면 0 반환.
     */
    public long placeBet(
            Player player,
            String selectedHorse,
            long wager
    ) {

        if (wager < MIN_BET
                || wager > MAX_BET
                || wager % 100L != 0L) {

            player.sendMessage(
                    "§c[경마] 베팅 금액이 올바르지 않습니다."
            );

            return 0L;
        }

        if (!canStart(player)) {
            return 0L;
        }

        if (!economy.has(player, wager)) {

            player.sendMessage(
                    "§c[경마] 골드가 부족합니다."
            );

            return 0L;
        }

        long raceId = 0L;

        try {
            raceId = repository.createPrepared(
                    player.getUniqueId(),
                    player.getName(),
                    selectedHorse,
                    wager
            );

            if (!repository.changeStatus(
                    raceId,
                    "PREPARED",
                    "DEBIT_IN_FLIGHT"
            )) {
                throw new SQLException(
                        "베팅 차감 준비 상태 변경 실패"
                );
            }

            /*
             * 여기서 서버가 중단되면 차감 여부가
             * 불확실하므로 자동으로 다시 차감하지 않는다.
             */
            boolean withdrawn =
                    economy.withdraw(player, wager);

            if (!withdrawn) {

                if (!repository.changeStatus(
                        raceId,
                        "DEBIT_IN_FLIGHT",
                        "CANCELLED"
                )) {
                    throw new SQLException(
                            "베팅 취소 상태 기록 실패"
                    );
                }

                player.sendMessage(
                        "§c[경마] 베팅금 차감에 실패했습니다."
                );

                return 0L;
            }

            if (!repository.changeStatus(
                    raceId,
                    "DEBIT_IN_FLIGHT",
                    "DEBIT_CONFIRMED"
            )) {
                throw new SQLException(
                        "베팅 차감 완료 상태 기록 실패"
                );
            }

            achievementTitleUnlockService
                    .recordPaidGamblingUse(
                            player.getUniqueId()
                    );

            player.sendMessage(
                    "§6[경마] §f"
                            + wager
                            + "골드를 베팅했습니다."
            );

            return raceId;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race bet requires review. race="
                            + raceId,
                    exception
            );

            player.sendMessage(
                    "§c[경마] 베팅 처리 확인이 필요합니다. "
                            + "경기 번호: "
                            + raceId
            );

            return 0L;
        }
    }

    /*
     * 결승선 도착 후 결과를 기록한다.
     * 승리한 경우 지급 대기 상태까지 저장한다.
     * 실제 보상 지급은 별도 정산 단계에서 처리한다.
     */
    public boolean recordFinish(
            long raceId,
            String winnerHorse
    ) {

        try {
            HorseRaceRepository.RaceRecord race =
                    repository.findById(raceId);

            if (race == null
                    || !"DEBIT_CONFIRMED".equals(
                            race.status()
                    )) {
                return false;
            }

            long payout =
                    race.selectedHorse().equals(winnerHorse)
                            ? Math.multiplyExact(
                                    race.wager(),
                                    2L
                            )
                            : 0L;

            return repository.recordResult(
                    raceId,
                    winnerHorse,
                    payout
            );

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race result requires review. race="
                            + raceId,
                    exception
            );

            return false;
        }
    }

    /*
     * 지급 대기 중인 승리 보상을 정산한다.
     * 지급 요청 중 서버가 종료된 경우에는
     * 중복 지급 방지를 위해 자동 재시도하지 않는다.
     */
    public boolean settlePayout(
            Player player,
            long raceId
    ) {

        try {
            HorseRaceRepository.RaceRecord race =
                    repository.findById(raceId);

            if (race == null
                    || !race.playerUuid().equals(
                            player.getUniqueId()
                    )
                    || !"PAYOUT_PENDING".equals(
                            race.status()
                    )) {
                return false;
            }

            if (!repository.changeStatus(
                    raceId,
                    "PAYOUT_PENDING",
                    "PAYOUT_IN_FLIGHT"
            )) {
                return false;
            }

            if (!economy.deposit(
                    player,
                    race.payout()
            )) {
                /*
                 * 지급 실패가 확인되어도 IN_FLIGHT를
                 * 유지하고 관리자 확인 대상으로 남긴다.
                 */
                return false;
            }

            if (!repository.changeStatus(
                    raceId,
                    "PAYOUT_IN_FLIGHT",
                    "PAYOUT_CONFIRMED"
            )) {
                return false;
            }

            player.sendMessage(
                    "§a[경마] 우승 보상 "
                            + race.payout()
                            + "골드가 지급되었습니다!"
            );

            return true;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Horse race payout requires review. race="
                            + raceId,
                    exception
            );

            return false;
        }
    }
}
