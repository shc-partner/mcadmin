package com.hcs.rpgcore.check;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.UUID;

/**
 * 골드 수표 발행 및 환전 기록.
 *
 * 실제 금액과 사용 여부는 아이템 설명이 아닌
 * MariaDB 기록을 기준으로 판정한다.
 */
public final class GoldCheckRepository {

    public static final String ISSUING = "ISSUING";
    public static final String ISSUED = "ISSUED";
    public static final String REDEEMING = "REDEEMING";
    public static final String REDEEMED = "REDEEMED";
    public static final String CANCELLED = "CANCELLED";

    public record GoldCheck(
            UUID checkUuid,
            UUID issuerUuid,
            long amountGold,
            String status,
            UUID redeemedByUuid
    ) {}

    private final DatabaseManager databaseManager;

    public GoldCheckRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 발행 시도 기록.
     *
     * 이 메서드 자체는 골드를 차감하지 않는다.
     */
    public void createIssuing(
            UUID checkUuid,
            UUID issuerUuid,
            long amountGold
    ) throws SQLException {

        if (amountGold <= 0L) {
            throw new IllegalArgumentException(
                    "Check amount must be positive."
            );
        }

        String sql = """
                INSERT INTO rpg_gold_checks
                (
                    check_uuid,
                    issuer_uuid,
                    amount_gold,
                    status
                )
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    checkUuid.toString()
            );

            statement.setString(
                    2,
                    issuerUuid.toString()
            );

            statement.setLong(
                    3,
                    amountGold
            );

            statement.setString(
                    4,
                    ISSUING
            );

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Gold check insert failed: "
                                + checkUuid
                );
            }
        }
    }

    /**
     * 발행 완료.
     *
     * ISSUING 상태일 때만 ISSUED로 변경한다.
     */
    public boolean markIssued(
            UUID checkUuid
    ) throws SQLException {

        String sql = """
                UPDATE rpg_gold_checks
                SET status = ?,
                    issued_at = CURRENT_TIMESTAMP
                WHERE check_uuid = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, ISSUED);
            statement.setString(
                    2,
                    checkUuid.toString()
            );
            statement.setString(3, ISSUING);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 수표 정보 조회.
     */
    public GoldCheck find(
            UUID checkUuid
    ) throws SQLException {

        String sql = """
                SELECT
                    check_uuid,
                    issuer_uuid,
                    amount_gold,
                    status,
                    redeemed_by_uuid
                FROM rpg_gold_checks
                WHERE check_uuid = ?
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
                    checkUuid.toString()
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                String redeemedBy =
                        resultSet.getString(
                                "redeemed_by_uuid"
                        );

                return new GoldCheck(
                        UUID.fromString(
                                resultSet.getString(
                                        "check_uuid"
                                )
                        ),
                        UUID.fromString(
                                resultSet.getString(
                                        "issuer_uuid"
                                )
                        ),
                        resultSet.getLong(
                                "amount_gold"
                        ),
                        resultSet.getString(
                                "status"
                        ),
                        redeemedBy == null
                                ? null
                                : UUID.fromString(redeemedBy)
                );
            }
        }
    }

    /**
     * 환전 권한 선점.
     *
     * ISSUED 상태인 수표만 REDEEMING으로 변경한다.
     * 동일 수표의 중복 환전 시도는 여기서 차단된다.
     */
    public boolean claimRedemption(
            UUID checkUuid,
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                UPDATE rpg_gold_checks
                SET status = ?,
                    redeemed_by_uuid = ?
                WHERE check_uuid = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    REDEEMING
            );

            statement.setString(
                    2,
                    playerUuid.toString()
            );

            statement.setString(
                    3,
                    checkUuid.toString()
            );

            statement.setString(
                    4,
                    ISSUED
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 골드 지급 성공 후 환전 완료 처리.
     */
    public boolean markRedeemed(
            UUID checkUuid,
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                UPDATE rpg_gold_checks
                SET status = ?,
                    redeemed_at = CURRENT_TIMESTAMP
                WHERE check_uuid = ?
                  AND redeemed_by_uuid = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    REDEEMED
            );

            statement.setString(
                    2,
                    checkUuid.toString()
            );

            statement.setString(
                    3,
                    playerUuid.toString()
            );

            statement.setString(
                    4,
                    REDEEMING
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 골드 지급이 명확하게 실패한 경우에만
     * 환전 선점을 취소한다.
     *
     * 지급 성공 여부가 불분명한 예외 상황에서는
     * 이 메서드를 호출하면 안 된다.
     */
    public boolean releaseRedemption(
            UUID checkUuid,
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                UPDATE rpg_gold_checks
                SET status = ?,
                    redeemed_by_uuid = NULL
                WHERE check_uuid = ?
                  AND redeemed_by_uuid = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    ISSUED
            );

            statement.setString(
                    2,
                    checkUuid.toString()
            );

            statement.setString(
                    3,
                    playerUuid.toString()
            );

            statement.setString(
                    4,
                    REDEEMING
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 골드 차감이 명확하게 실패했을 때 발행 시도를 취소한다.
     */
    public boolean cancelIssuing(
            UUID checkUuid
    ) throws SQLException {

        String sql = """
                UPDATE rpg_gold_checks
                SET status = ?
                WHERE check_uuid = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, CANCELLED);
            statement.setString(2, checkUuid.toString());
            statement.setString(3, ISSUING);

            return statement.executeUpdate() == 1;
        }
    }

}
