package com.hcs.rpgcore.furniture.shop;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.UUID;

/**
 * NPC 25 가구 상점 구매 기록.
 *
 * 구매마다 고유 transaction_id를 사용한다.
 * 상태 변경은 예상 상태가 일치할 때만 수행한다.
 */
public final class FurniturePurchaseRepository {

    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDED = "REFUNDED";
    public static final String REFUND_FAILED = "REFUND_FAILED";

    private final DatabaseManager databaseManager;

    public FurniturePurchaseRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 결제 전에 구매 시도를 기록한다.
     *
     * transactionId는 구매 요청마다 새로 생성한다.
     */
    public void createPending(
            UUID transactionId,
            UUID playerUuid,
            FurnitureShopRepository.FurnitureItem item
    ) throws SQLException {

        String sql = """
                INSERT INTO rpg_furniture_shop_purchases
                (
                    transaction_id,
                    player_uuid,
                    shop_item_id,
                    provider,
                    provider_item_id,
                    display_name,
                    price_gold,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    transactionId.toString()
            );

            statement.setString(
                    2,
                    playerUuid.toString()
            );

            statement.setString(
                    3,
                    item.shopItemId()
            );

            statement.setString(
                    4,
                    item.provider()
            );

            statement.setString(
                    5,
                    item.providerItemId()
            );

            statement.setString(
                    6,
                    item.displayName()
            );

            statement.setLong(
                    7,
                    item.priceGold()
            );

            statement.setString(
                    8,
                    PENDING
            );

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Furniture purchase insert failed: "
                                + transactionId
                );
            }
        }
    }

    /**
     * 현재 상태가 expected인 경우에만 next로 변경한다.
     *
     * 중복 처리나 예상하지 못한 상태 변경을 방지한다.
     */
    public boolean transition(
            UUID transactionId,
            String expected,
            String next
    ) throws SQLException {

        String sql = """
                UPDATE rpg_furniture_shop_purchases
                SET status = ?
                WHERE transaction_id = ?
                  AND status = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, next);
            statement.setString(
                    2,
                    transactionId.toString()
            );
            statement.setString(3, expected);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 오류 발생 시 구매 기록 상태를 확인한다.
     */
    public String findStatus(
            UUID transactionId
    ) throws SQLException {

        String sql = """
                SELECT status
                FROM rpg_furniture_shop_purchases
                WHERE transaction_id = ?
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
                    transactionId.toString()
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getString("status");
            }
        }
    }
}
