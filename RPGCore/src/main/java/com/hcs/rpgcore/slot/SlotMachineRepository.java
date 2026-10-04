package com.hcs.rpgcore.slot;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * 슬롯 머신 베팅 및 정산 기록.
 *
 * Vault 골드 거래는 DB 트랜잭션에 포함되지 않는다.
 * 따라서 차감/지급 중단 상태를 임의로 재실행하지 않는다.
 */
public final class SlotMachineRepository {

    private static final long MIN_BET = 100L;
    private static final long MAX_BET = 100_000_000L;

    private final DatabaseManager databaseManager;

    public SlotMachineRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    public void createTable() throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS rpg_slot_games (
                    game_id BIGINT NOT NULL AUTO_INCREMENT,
                    player_uuid CHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    wager BIGINT NOT NULL,

                    reel_1 VARCHAR(24) NOT NULL,
                    reel_2 VARCHAR(24) NOT NULL,
                    reel_3 VARCHAR(24) NOT NULL,

                    payout BIGINT NOT NULL DEFAULT 0,
                    status VARCHAR(24) NOT NULL,

                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

                    PRIMARY KEY (game_id),
                    INDEX idx_slot_player_status
                        (player_uuid, status),
                    INDEX idx_slot_status (status),
                    INDEX idx_slot_player_latest
                        (player_uuid, game_id)
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {
            statement.executeUpdate(sql);
        }
    }

    public record GameRecord(
            long gameId,
            UUID playerUuid,
            String playerName,
            long wager,
            String reel1,
            String reel2,
            String reel3,
            long payout,
            String status
    ) {
    }

    /**
     * 베팅 차감 전에 게임 ID와 최종 결과를 기록한다.
     * 최종 결과는 게임 중 변경하지 않는다.
     */
    public long createPrepared(
            UUID playerUuid,
            String playerName,
            long wager,
            String reel1,
            String reel2,
            String reel3,
            long payout
    ) throws SQLException {

        validateGame(
                wager,
                reel1,
                reel2,
                reel3,
                payout
        );

        String sql = """
                INSERT INTO rpg_slot_games (
                    player_uuid,
                    player_name,
                    wager,
                    reel_1,
                    reel_2,
                    reel_3,
                    payout,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PREPARED')
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setString(
                    1,
                    playerUuid.toString()
            );
            statement.setString(2, playerName);
            statement.setLong(3, wager);
            statement.setString(4, reel1);
            statement.setString(5, reel2);
            statement.setString(6, reel3);
            statement.setLong(7, payout);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "슬롯 머신 기록 생성 실패"
                );
            }

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {
                if (!keys.next()) {
                    throw new SQLException(
                            "슬롯 머신 게임 ID 조회 실패"
                    );
                }

                return keys.getLong(1);
            }
        }
    }

    /**
     * 예상한 상태에서만 다음 상태로 전환한다.
     * 같은 게임의 중복 정산을 방지하는 데 사용한다.
     */
    public boolean changeStatus(
            long gameId,
            String expectedStatus,
            String nextStatus
    ) throws SQLException {

        String sql = """
                UPDATE rpg_slot_games
                SET status = ?
                WHERE game_id = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, nextStatus);
            statement.setLong(2, gameId);
            statement.setString(3, expectedStatus);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 차감이 확정된 게임의 결과를 정산 대기 상태로 전환한다.
     * 당첨금이 0이면 패배 완료 상태로 전환한다.
     */
    public boolean recordResult(
            long gameId,
            long payout
    ) throws SQLException {

        String nextStatus = payout > 0L
                ? "PAYOUT_PENDING"
                : "FINISHED_LOSS";

        String sql = """
                UPDATE rpg_slot_games
                SET status = ?
                WHERE game_id = ?
                  AND payout = ?
                  AND status = 'DEBIT_CONFIRMED'
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, nextStatus);
            statement.setLong(2, gameId);
            statement.setLong(3, payout);

            return statement.executeUpdate() == 1;
        }
    }

    public GameRecord findById(
            long gameId
    ) throws SQLException {

        String sql = """
                SELECT game_id, player_uuid, player_name,
                       wager, reel_1, reel_2, reel_3,
                       payout, status
                FROM rpg_slot_games
                WHERE game_id = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, gameId);

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {
                return result.next()
                        ? readRecord(result)
                        : null;
            }
        }
    }

    /**
     * 플레이어의 미정산 게임 조회.
     * 미정산 게임이 있으면 새 베팅을 허용하지 않는다.
     */
    public GameRecord findUnresolvedByPlayer(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT game_id, player_uuid, player_name,
                       wager, reel_1, reel_2, reel_3,
                       payout, status
                FROM rpg_slot_games
                WHERE player_uuid = ?
                  AND status NOT IN (
                      'FINISHED_LOSS',
                      'PAYOUT_CONFIRMED',
                      'REFUND_CONFIRMED',
                      'CANCELLED'
                  )
                ORDER BY game_id DESC
                LIMIT 1
                """;

        return findPlayerRecord(playerUuid, sql);
    }

    /**
     * GUI를 다시 열 때 마지막 게임 결과를 표시하기 위한 조회.
     */
    public GameRecord findLatestByPlayer(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT game_id, player_uuid, player_name,
                       wager, reel_1, reel_2, reel_3,
                       payout, status
                FROM rpg_slot_games
                WHERE player_uuid = ?
                ORDER BY game_id DESC
                LIMIT 1
                """;

        return findPlayerRecord(playerUuid, sql);
    }

    private GameRecord findPlayerRecord(
            UUID playerUuid,
            String sql
    ) throws SQLException {

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(
                    1,
                    playerUuid.toString()
            );

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {
                return result.next()
                        ? readRecord(result)
                        : null;
            }
        }
    }

    private GameRecord readRecord(
            ResultSet result
    ) throws SQLException {

        return new GameRecord(
                result.getLong("game_id"),
                UUID.fromString(
                        result.getString("player_uuid")
                ),
                result.getString("player_name"),
                result.getLong("wager"),
                result.getString("reel_1"),
                result.getString("reel_2"),
                result.getString("reel_3"),
                result.getLong("payout"),
                result.getString("status")
        );
    }

    private void validateGame(
            long wager,
            String reel1,
            String reel2,
            String reel3,
            long payout
    ) {

        if (
                wager < MIN_BET
                        || wager > MAX_BET
                        || wager % 100L != 0L
        ) {
            throw new IllegalArgumentException(
                    "슬롯 머신 베팅 금액이 올바르지 않습니다."
            );
        }

        if (
                !validReel(reel1)
                        || !validReel(reel2)
                        || !validReel(reel3)
        ) {
            throw new IllegalArgumentException(
                    "슬롯 머신 결과 아이콘이 올바르지 않습니다."
            );
        }

        long expectedPayout = 0L;

        if (
                reel1.equals(reel2)
                        && reel2.equals(reel3)
        ) {
            expectedPayout = switch (reel1) {
                case "BAKED_POTATO" ->
                        Math.multiplyExact(wager, 3L) / 2L;
                case "APPLE" ->
                        Math.multiplyExact(wager, 2L);
                case "GOLDEN_APPLE" ->
                        Math.multiplyExact(wager, 5L);
                default ->
                        throw new IllegalArgumentException(
                                "알 수 없는 당첨 조합"
                        );
            };
        }

        if (payout != expectedPayout) {
            throw new IllegalArgumentException(
                    "슬롯 머신 결과와 지급액이 일치하지 않습니다."
            );
        }
    }

    private boolean validReel(String reel) {

        return "BAKED_POTATO".equals(reel)
                || "APPLE".equals(reel)
                || "GOLDEN_APPLE".equals(reel);
    }
}
