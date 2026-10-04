package com.hcs.rpgcore.level.reward;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class LevelRewardRepository {

    public static final String STURDY_SHIELD = "level_10_sturdy_shield";

    private final DatabaseManager databaseManager;

    public LevelRewardRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void createTable() throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS rpg_level_rewards (
                    player_uuid CHAR(36) NOT NULL,
                    reward_id VARCHAR(64) NOT NULL,
                    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_uuid, reward_id)
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;

        try (
                Connection connection = databaseManager.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(sql);
        }
    }

    public void registerShieldReward(UUID playerUuid)
            throws SQLException {

        String sql = """
                INSERT IGNORE INTO rpg_level_rewards (
                    player_uuid, reward_id, status
                ) VALUES (?, ?, 'PENDING')
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, STURDY_SHIELD);
            statement.executeUpdate();
        }
    }

    public String findShieldStatus(UUID playerUuid)
            throws SQLException {

        String sql = """
                SELECT status
                FROM rpg_level_rewards
                WHERE player_uuid = ?
                  AND reward_id = ?
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, STURDY_SHIELD);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? result.getString("status")
                        : null;
            }
        }
    }

    public boolean changeShieldStatus(
            UUID playerUuid,
            String expectedStatus,
            String nextStatus
    ) throws SQLException {

        String sql = """
                UPDATE rpg_level_rewards
                SET status = ?
                WHERE player_uuid = ?
                  AND reward_id = ?
                  AND status = ?
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, nextStatus);
            statement.setString(2, playerUuid.toString());
            statement.setString(3, STURDY_SHIELD);
            statement.setString(4, expectedStatus);

            return statement.executeUpdate() == 1;
        }
    }
}
