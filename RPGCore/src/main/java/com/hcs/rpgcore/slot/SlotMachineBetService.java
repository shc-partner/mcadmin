package com.hcs.rpgcore.slot;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.bukkit.entity.Player;

/**
 * 슬롯 머신의 결과 추첨, 베팅 차감 및 당첨금 정산.
 *
 * 릴 애니메이션은 GUI에서 담당한다.
 * 실제 결과는 베팅 시작 전에 DB에 저장한다.
 */
public final class SlotMachineBetService {

    public static final long MIN_BET = 100L;
    public static final long MAX_BET = 100_000_000L;
    public static final long BET_STEP = 100L;

    private static final String POTATO = "BAKED_POTATO";
    private static final String APPLE = "APPLE";
    private static final String GOLDEN_APPLE = "GOLDEN_APPLE";

    private static final String[] SYMBOLS = {
            POTATO,
            APPLE,
            GOLDEN_APPLE
    };

    private final RPGCorePlugin plugin;
    private final ShopEconomyService economy;
    private final SlotMachineRepository repository;

    private final com.hcs.rpgcore.title.AchievementTitleUnlockService
            achievementTitleUnlockService;

    public SlotMachineBetService(
            RPGCorePlugin plugin,
            ShopEconomyService economy,
            SlotMachineRepository repository,
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
     * 새 게임을 시작한다.
     *
     * 성공: gameId 반환.
     * 실패: 0 반환.
     *
     * 결과는 골드 차감 전에 확정하여 DB에 보관한다.
     */
    public long placeBet(
            Player player,
            long wager
    ) {

        if (
                wager < MIN_BET
                        || wager > MAX_BET
                        || wager % BET_STEP != 0L
        ) {

            player.sendMessage(
                    "§c[슬롯 머신] 베팅 금액이 올바르지 않습니다."
            );

            return 0L;
        }

        if (!preparePlayer(player)) {
            return 0L;
        }

        if (!economy.has(player, wager)) {

            player.sendMessage(
                    "§c[슬롯 머신] 골드가 부족합니다."
            );

            return 0L;
        }

        Result result = drawResult(wager);

        long gameId = 0L;

        try {

            gameId = repository.createPrepared(
                    player.getUniqueId(),
                    player.getName(),
                    wager,
                    result.reel1(),
                    result.reel2(),
                    result.reel3(),
                    result.payout()
            );

            if (!repository.changeStatus(
                    gameId,
                    "PREPARED",
                    "DEBIT_IN_FLIGHT"
            )) {

                throw new SQLException(
                        "골드 차감 준비 상태 기록 실패"
                );
            }

            /*
             * Vault 거래는 DB 트랜잭션에 포함되지 않는다.
             *
             * 차감 요청 중 서버가 종료되면 차감 여부를
             * 확정할 수 없으므로 자동 재차감하지 않는다.
             */
            boolean withdrawn =
                    economy.withdraw(
                            player,
                            wager
                    );

            if (!withdrawn) {

                if (!repository.changeStatus(
                        gameId,
                        "DEBIT_IN_FLIGHT",
                        "CANCELLED"
                )) {

                    throw new SQLException(
                            "차감 실패 상태 기록 실패"
                    );
                }

                player.sendMessage(
                        "§c[슬롯 머신] 베팅금 차감에 실패했습니다."
                );

                return 0L;
            }

            if (!repository.changeStatus(
                    gameId,
                    "DEBIT_IN_FLIGHT",
                    "DEBIT_CONFIRMED"
            )) {

                throw new SQLException(
                        "골드 차감 완료 상태 기록 실패"
                );
            }

            achievementTitleUnlockService
                    .recordPaidGamblingUse(
                            player.getUniqueId()
                    );

            player.sendMessage(
                    "§6[슬롯 머신] §f"
                            + wager
                            + "G를 베팅했습니다."
            );

            return gameId;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Slot machine bet requires review. game="
                            + gameId,
                    exception
            );

            player.sendMessage(
                    "§c[슬롯 머신] 베팅 처리 확인이 필요합니다. "
                            + "게임 번호: "
                            + gameId
            );

            return 0L;
        }
    }

    /**
     * 3초 애니메이션 종료 시 호출한다.
     *
     * 결과가 확정된 게임만 정산 상태로 변경한다.
     * 실제 입금은 settlePayout()에서 별도로 처리한다.
     */
    public boolean finishGame(
            long gameId
    ) {

        try {

            SlotMachineRepository.GameRecord game =
                    repository.findById(gameId);

            if (
                    game == null
                            || !"DEBIT_CONFIRMED".equals(
                                    game.status()
                            )
            ) {

                return false;
            }

            return repository.recordResult(
                    gameId,
                    game.payout()
            );

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Slot machine result requires review. game="
                            + gameId,
                    exception
            );

            return false;
        }
    }

    /**
     * 확정된 당첨금 지급.
     *
     * 지급 시도 상태를 먼저 기록한다.
     * 입금 실패나 서버 중단 후에는 자동 재지급하지 않는다.
     */
    public boolean settlePayout(
            Player player,
            long gameId
    ) {

        try {

            SlotMachineRepository.GameRecord game =
                    repository.findById(gameId);

            if (
                    game == null
                            || !game.playerUuid().equals(
                                    player.getUniqueId()
                            )
                            || !"PAYOUT_PENDING".equals(
                                    game.status()
                            )
            ) {

                return false;
            }

            if (!repository.changeStatus(
                    gameId,
                    "PAYOUT_PENDING",
                    "PAYOUT_IN_FLIGHT"
            )) {

                return false;
            }

            if (!economy.deposit(
                    player,
                    game.payout()
            )) {

                plugin.getLogger().severe(
                        "Slot machine payout needs manual review: game="
                                + gameId
                                + " player="
                                + player.getUniqueId()
                );

                return false;
            }

            if (!repository.changeStatus(
                    gameId,
                    "PAYOUT_IN_FLIGHT",
                    "PAYOUT_CONFIRMED"
            )) {

                plugin.getLogger().severe(
                        "Slot machine payout succeeded but "
                                + "DB confirmation failed: game="
                                + gameId
                );

                return false;
            }

            player.sendMessage(
                    "§a[슬롯 머신] 당첨금 "
                            + game.payout()
                            + "G가 지급되었습니다!"
            );

            return true;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Slot machine payout requires review. game="
                            + gameId,
                    exception
            );

            return false;
        }
    }

    /**
     * NPC 재이용 또는 재접속 시 미정산 게임을 확인한다.
     *
     * DEBIT_CONFIRMED:
     * 골드 차감은 확정됐으며 결과가 DB에 저장돼 있으므로
     * 저장된 결과대로 게임을 완료한다.
     *
     * DEBIT_IN_FLIGHT / PAYOUT_IN_FLIGHT:
     * 실제 Vault 거래 여부가 불명확하므로 자동 재시도 금지.
     *
     * 이 메서드는 진행 중인 GUI 애니메이션이 없을 때만
     * 호출해야 한다.
     */
    public boolean preparePlayer(
            Player player
    ) {

        try {

            UUID uuid = player.getUniqueId();

            SlotMachineRepository.GameRecord pending =
                    repository.findUnresolvedByPlayer(uuid);

            if (pending == null) {
                return true;
            }

            if ("DEBIT_CONFIRMED".equals(
                    pending.status()
            )) {

                if (!finishGame(pending.gameId())) {

                    player.sendMessage(
                            "§c[슬롯 머신] 이전 게임의 결과 확인이 "
                                    + "필요합니다. 게임 번호: "
                                    + pending.gameId()
                    );

                    return false;
                }

                pending = repository.findById(
                        pending.gameId()
                );

                if (pending == null) {
                    return false;
                }
            }

            if ("PAYOUT_PENDING".equals(
                    pending.status()
            )) {

                if (!settlePayout(
                        player,
                        pending.gameId()
                )) {

                    player.sendMessage(
                            "§c[슬롯 머신] 당첨금 정산 확인이 "
                                    + "필요합니다. 게임 번호: "
                                    + pending.gameId()
                    );

                    return false;
                }

                return true;
            }

            if (
                    "FINISHED_LOSS".equals(pending.status())
                            || "PAYOUT_CONFIRMED".equals(
                                    pending.status()
                            )
            ) {

                return true;
            }

            player.sendMessage(
                    "§c[슬롯 머신] 이전 게임의 거래 확인이 "
                            + "필요합니다. 게임 번호: "
                            + pending.gameId()
            );

            return false;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Slot machine pending settlement lookup failed",
                    exception
            );

            player.sendMessage(
                    "§c[슬롯 머신] 이전 게임 기록을 확인할 수 없습니다."
            );

            return false;
        }
    }

    public SlotMachineRepository.GameRecord findGame(
            long gameId
    ) throws SQLException {

        return repository.findById(gameId);
    }

    public SlotMachineRepository.GameRecord findLatest(
            UUID playerUuid
    ) throws SQLException {

        return repository.findLatestByPlayer(playerUuid);
    }

    /**
     * 최종 결과를 확률에 따라 한 번만 추첨한다.
     *
     * 구운 감자 30%, 사과 15%, 황금 사과 2%, 실패 53%.
     */
    private Result drawResult(
            long wager
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        double roll = random.nextDouble();

        if (roll < 0.30D) {

            return new Result(
                    POTATO,
                    POTATO,
                    POTATO,
                    Math.multiplyExact(wager, 3L) / 2L
            );
        }

        if (roll < 0.45D) {

            return new Result(
                    APPLE,
                    APPLE,
                    APPLE,
                    Math.multiplyExact(wager, 2L)
            );
        }

        if (roll < 0.47D) {

            return new Result(
                    GOLDEN_APPLE,
                    GOLDEN_APPLE,
                    GOLDEN_APPLE,
                    Math.multiplyExact(wager, 5L)
            );
        }

        String reel1;
        String reel2;
        String reel3;

        /*
         * 실패 판정에서는 세 아이콘이 전부 같아지는
         * 조합을 허용하지 않는다.
         */
        do {

            reel1 = randomSymbol(random);
            reel2 = randomSymbol(random);
            reel3 = randomSymbol(random);

        } while (
                reel1.equals(reel2)
                        && reel2.equals(reel3)
        );

        return new Result(
                reel1,
                reel2,
                reel3,
                0L
        );
    }

    private String randomSymbol(
            ThreadLocalRandom random
    ) {

        return SYMBOLS[
                random.nextInt(SYMBOLS.length)
        ];
    }

    private record Result(
            String reel1,
            String reel2,
            String reel3,
            long payout
    ) {
    }
}
