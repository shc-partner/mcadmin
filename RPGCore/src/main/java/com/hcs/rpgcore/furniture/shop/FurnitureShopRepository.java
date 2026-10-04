package com.hcs.rpgcore.furniture.shop;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 25 가구 상점의 MariaDB 상품 조회.
 *
 * 실제 가구 아이템 생성 및 골드 차감은 담당하지 않는다.
 */
public final class FurnitureShopRepository {

    public record FurnitureItem(
            long id,
            String shopItemId,
            String provider,
            String providerItemId,
            String displayName,
            long priceGold,
            int displayOrder
    ) {
    }

    private final DatabaseManager databaseManager;

    public FurnitureShopRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 판매 중인 가구를 표시 순서대로 조회한다.
     */
    public List<FurnitureItem> findEnabled()
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    shop_item_id,
                    provider,
                    provider_item_id,
                    display_name,
                    price_gold,
                    display_order
                FROM rpg_furniture_shop_items
                WHERE enabled = 1
                ORDER BY display_order, id
                """;

        List<FurnitureItem> items = new ArrayList<>();

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {
                items.add(readItem(resultSet));
            }
        }

        return items;
    }

    /**
     * 구매 직전에 상품을 다시 조회한다.
     *
     * GUI를 연 뒤 가격이나 판매 상태가 변경될 수 있으므로
     * 화면에 저장된 가격만 믿고 결제하지 않는다.
     */
    public FurnitureItem findEnabledByShopItemId(
            String shopItemId
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    shop_item_id,
                    provider,
                    provider_item_id,
                    display_name,
                    price_gold,
                    display_order
                FROM rpg_furniture_shop_items
                WHERE shop_item_id = ?
                  AND enabled = 1
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
                    shopItemId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return readItem(resultSet);
            }
        }
    }

    private FurnitureItem readItem(
            ResultSet resultSet
    ) throws SQLException {

        return new FurnitureItem(
                resultSet.getLong("id"),
                resultSet.getString("shop_item_id"),
                resultSet.getString("provider"),
                resultSet.getString("provider_item_id"),
                resultSet.getString("display_name"),
                resultSet.getLong("price_gold"),
                resultSet.getInt("display_order")
        );
    }
}
