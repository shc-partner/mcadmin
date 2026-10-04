package com.hcs.rpgcore.horse;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * 경마 베팅 및 정산 기록.
 *
 * Vault 경제 거래는 MariaDB 트랜잭션에 포함되지 않으므로
 * 불확실한 차감/지급 상태를 임의로 재실행하지 않는다.
 */
public final class HorseRaceRepository {

    private final DatabaseManager databaseManager;

    public HorseRaceRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    public void createTable() throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS rpg_horse_races (
                    race_id BIGINT NOT NULL AUTO_INCREMENT,
                    player_uuid CHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    selected_horse VARCHAR(16) NOT NULL,
                    wager BIGINT NOT NULL,
                    winner_horse VARCHAR(16) NULL,
                    payout BIGINT NOT NULL DEFAULT 0,
                    status VARCHAR(24) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

                    PRIMARY KEY (race_id),
                    INDEX idx_horse_races_player
                        (player_uuid, status),
                    INDEX idx_horse_races_status
                        (status)
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

    /**
     * 골드 차감 전 경기 기록을 생성한다.
     */
    public long createPrepared(
            UUID playerUuid,
            String playerName,
            String selectedHorse,
            long wager
    ) throws SQLException {

        if (wager < 100L || wager % 100L != 0L) {
            throw new IllegalArgumentException(
                    "베팅 금액은 100골드 단위여야 합니다."
            );
        }

        if (!"GOLD".equals(selectedHorse)
                && !"DIAMOND".equals(selectedHorse)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 경주마입니다."
            );
        }

        String sql = """
                INSERT INTO rpg_horse_races (
                    player_uuid,
                    player_name,
                    selected_horse,
                    wager,
                    status
                )
                VALUES (?, ?, ?, ?, 'PREPARED')
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
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerName);
            statement.setString(3, selectedHorse);
            statement.setLong(4, wager);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "경마 기록 생성에 실패했습니다."
                );
            }

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {
                if (!keys.next()) {
                    throw new SQLException(
                            "생성된 경마 기록 ID를 받지 못했습니다."
                    );
                }

                return keys.getLong(1);
            }
        }
    }

    /**
     * 예상한 상태에서만 다음 상태로 변경한다.
     * 반환값 0이면 이미 변경됐거나 대상이 없는 것이다.
     */
    public boolean changeStatus(
            long raceId,
            String expectedStatus,
            String nextStatus
    ) throws SQLException {

        String sql = """
                UPDATE rpg_horse_races
                SET status = ?
                WHERE race_id = ?
                    AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, nextStatus);
            statement.setLong(2, raceId);
            statement.setString(3, expectedStatus);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * DB에 저장된 경기 기록.
     */
    public record RaceRecord(
            long raceId,
            UUID playerUuid,
            String playerName,
            String selectedHorse,
            long wager,
            String winnerHorse,
            long payout,
            String status
    ) {
    }


    /**
     * 경기 ID로 기록을 조회한다.
     */
    public RaceRecord findById(
            long raceId
    ) throws SQLException {

        String sql = """
                SELECT race_id,
                       player_uuid,
                       player_name,
                       selected_horse,
                       wager,
                       winner_horse,
                       payout,
                       status
                FROM rpg_horse_races
                WHERE race_id = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, raceId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return null;
                }

                return readRecord(resultSet);
            }
        }
    }


    /**
     * 플레이어의 미완료 경기 조회.
     *
     * 미완료 경기가 있으면 새 경기를 시작하지 않는다.
     * 불확실한 경제 거래는 자동으로 재실행하지 않는다.
     */
    public RaceRecord findUnresolvedByPlayer(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT race_id,
                       player_uuid,
                       player_name,
                       selected_horse,
                       wager,
                       winner_horse,
                       payout,
                       status
                FROM rpg_horse_races
                WHERE player_uuid = ?
                  AND status NOT IN (
                      'FINISHED_LOSS',
                      'PAYOUT_CONFIRMED',
                      'REFUND_CONFIRMED',
                      'CANCELLED'
                  )
                ORDER BY race_id DESC
                LIMIT 1
                """;

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
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return null;
                }

                return readRecord(resultSet);
            }
        }
    }


    private RaceRecord readRecord(
            ResultSet resultSet
    ) throws SQLException {

        return new RaceRecord(
                resultSet.getLong("race_id"),
                UUID.fromString(
                        resultSet.getString("player_uuid")
                ),
                resultSet.getString("player_name"),
                resultSet.getString("selected_horse"),
                resultSet.getLong("wager"),
                resultSet.getString("winner_horse"),
                resultSet.getLong("payout"),
                resultSet.getString("status")
        );
    }


    /**
     * 경기 종료 결과를 기록한다.
     * 지급이 필요한 승리 기록은 PAYOUT_PENDING으로 남긴다.
     */
    public boolean recordResult(
            long raceId,
            String winnerHorse,
            long payout
    ) throws SQLException {

        if (!"GOLD".equals(winnerHorse)
                && !"DIAMOND".equals(winnerHorse)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 승리 경주마입니다."
            );
        }

        if (payout < 0L) {
            throw new IllegalArgumentException(
                    "지급 금액이 올바르지 않습니다."
            );
        }

        String nextStatus =
                payout > 0L
                        ? "PAYOUT_PENDING"
                        : "FINISHED_LOSS";

        String sql = """
                UPDATE rpg_horse_races
                SET winner_horse = ?,
                    payout = ?,
                    status = ?
                WHERE race_id = ?
                    AND status = 'DEBIT_CONFIRMED'
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, winnerHorse);
            statement.setLong(2, payout);
            statement.setString(3, nextStatus);
            statement.setLong(4, raceId);

            return statement.executeUpdate() == 1;
        }
    }
}
