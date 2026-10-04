package com.hcs.rpgcore.dungeon.reward;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PendingRewardRepository {

    private final DatabaseManager databaseManager;

    public PendingRewardRepository(
            DatabaseManager databaseManager
    ) {

        this.databaseManager =
                databaseManager;
    }

    public void createTable()
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS rpg_pending_rewards (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    player_uuid CHAR(36) NOT NULL,
                    reward_type VARCHAR(64) NOT NULL,
                    amount INT UNSIGNED NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    PRIMARY KEY (id),
                    INDEX idx_pending_rewards_player (
                        player_uuid
                    )
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

    public void insert(
            UUID playerUuid,
            String rewardType,
            int amount
    ) throws SQLException {

        if (amount <= 0) {
            return;
        }

        String sql = """
                INSERT INTO rpg_pending_rewards (
                    player_uuid,
                    reward_type,
                    amount
                )
                VALUES (?, ?, ?)
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

            statement.setString(
                    2,
                    rewardType
            );

            statement.setInt(
                    3,
                    amount
            );

            statement.executeUpdate();
        }
    }

    public List<PendingReward> findByPlayer(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    reward_type,
                    amount
                FROM rpg_pending_rewards
                WHERE player_uuid = ?
                ORDER BY id ASC
                """;

        List<PendingReward> rewards =
                new ArrayList<>();

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

                while (resultSet.next()) {

                    rewards.add(
                            new PendingReward(
                                    resultSet.getLong("id"),
                                    resultSet.getString(
                                            "reward_type"
                                    ),
                                    resultSet.getInt(
                                            "amount"
                                    )
                            )
                    );
                }
            }
        }

        return rewards;
    }

    public void delete(
            long id,
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                DELETE FROM rpg_pending_rewards
                WHERE id = ?
                  AND player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            statement.setString(
                    2,
                    playerUuid.toString()
            );

            statement.executeUpdate();
        }
    }

    public void updateAmount(
            long id,
            UUID playerUuid,
            int amount
    ) throws SQLException {

        if (amount <= 0) {

            delete(
                    id,
                    playerUuid
            );

            return;
        }

        String sql = """
                UPDATE rpg_pending_rewards
                SET amount = ?
                WHERE id = ?
                  AND player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    amount
            );

            statement.setLong(
                    2,
                    id
            );

            statement.setString(
                    3,
                    playerUuid.toString()
            );

            statement.executeUpdate();
        }
    }
}
