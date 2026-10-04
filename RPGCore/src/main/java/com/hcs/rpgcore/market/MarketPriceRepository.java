package com.hcs.rpgcore.market;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.IntSupplier;


/**
 * Minecraft 날짜별 시장 가격 저장소.
 *
 * 동일한 market_day / market_id / item_id 조합의 가격은
 * 서버가 재시작되어도 DB에서 그대로 복원된다.
 */
public final class MarketPriceRepository {

    private final DatabaseManager databaseManager;


    public MarketPriceRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }


    /**
     * 특정 날짜의 저장된 가격을 조회한다.
     *
     * @return 가격, 없으면 null
     */
    public Integer findPrice(
            long marketDay,
            String marketId,
            String itemId
    ) {

        String sql = """
                SELECT price
                FROM rpg_market_prices
                WHERE market_day = ?
                  AND market_id = ?
                  AND item_id = ?
                LIMIT 1
                """;


        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    marketDay
            );

            statement.setString(
                    2,
                    marketId
            );

            statement.setString(
                    3,
                    itemId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getInt(
                        "price"
                );
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "시장 가격 조회 실패: "
                            + marketId
                            + " / "
                            + itemId
                            + " / day="
                            + marketDay,
                    exception
            );
        }
    }


    /**
     * 현재 날짜보다 이전에 존재하는 가장 최근 가격을 조회한다.
     *
     * GUI에서 전일/이전 시세 대비 표시용으로 사용한다.
     *
     * @return 이전 가격, 없으면 null
     */
    public Integer findPreviousPrice(
            long marketDay,
            String marketId,
            String itemId
    ) {

        String sql = """
                SELECT price
                FROM rpg_market_prices
                WHERE market_day < ?
                  AND market_id = ?
                  AND item_id = ?
                ORDER BY market_day DESC
                LIMIT 1
                """;


        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    marketDay
            );

            statement.setString(
                    2,
                    marketId
            );

            statement.setString(
                    3,
                    itemId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getInt(
                        "price"
                );
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "이전 시장 가격 조회 실패: "
                            + marketId
                            + " / "
                            + itemId
                            + " / day="
                            + marketDay,
                    exception
            );
        }
    }


    /**
     * 해당 날짜의 가격이 없으면 생성한다.
     *
     * INSERT IGNORE를 사용하므로 동시에 여러 요청이 들어와도
     * PRIMARY KEY 기준으로 최초 1건만 저장된다.
     */
    public int getOrCreatePrice(
            long marketDay,
            String marketId,
            String itemId,
            IntSupplier priceGenerator
    ) {

        Integer existing =
                findPrice(
                        marketDay,
                        marketId,
                        itemId
                );

        if (existing != null) {
            return existing;
        }


        int generated =
                priceGenerator.getAsInt();


        String sql = """
                INSERT IGNORE INTO rpg_market_prices (
                    market_day,
                    market_id,
                    item_id,
                    price
                )
                VALUES (?, ?, ?, ?)
                """;


        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    marketDay
            );

            statement.setString(
                    2,
                    marketId
            );

            statement.setString(
                    3,
                    itemId
            );

            statement.setInt(
                    4,
                    generated
            );

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "시장 가격 저장 실패: "
                            + marketId
                            + " / "
                            + itemId
                            + " / day="
                            + marketDay,
                    exception
            );
        }


        Integer stored =
                findPrice(
                        marketDay,
                        marketId,
                        itemId
                );

        if (stored == null) {

            throw new IllegalStateException(
                    "시장 가격 저장 후 조회 실패: "
                            + marketId
                            + " / "
                            + itemId
                            + " / day="
                            + marketDay
            );
        }


        return stored;
    }
}
