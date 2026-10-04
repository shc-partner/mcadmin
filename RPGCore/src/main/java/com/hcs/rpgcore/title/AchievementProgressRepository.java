package com.hcs.rpgcore.title;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.UUID;

public final class AchievementProgressRepository {

    private final DatabaseManager databaseManager;

    public AchievementProgressRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 진행도를 1 증가시키고 증가 후 값을 반환한다.
     */
    public long increment(
            UUID playerUuid,
            String progressType,
            String progressTarget
    ) throws SQLException {

        String upsertSql = """
                INSERT INTO rpg_player_achievement_progress
                    (
                        player_uuid,
                        progress_type,
                        progress_target,
                        progress_value
                    )
                VALUES (?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE
                    progress_value = progress_value + 1,
                    updated_at = CURRENT_TIMESTAMP
                """;

        String selectSql = """
                SELECT progress_value
                FROM rpg_player_achievement_progress
                WHERE player_uuid = ?
                  AND progress_type = ?
                  AND progress_target = ?
                LIMIT 1
                """;

        try (
                Connection connection =
                        databaseManager.getConnection()
        ) {

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(upsertSql)
            ) {

                statement.setString(
                        1,
                        playerUuid.toString()
                );

                statement.setString(
                        2,
                        progressType
                );

                statement.setString(
                        3,
                        progressTarget
                );

                statement.executeUpdate();
            }

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(selectSql)
            ) {

                statement.setString(
                        1,
                        playerUuid.toString()
                );

                statement.setString(
                        2,
                        progressType
                );

                statement.setString(
                        3,
                        progressTarget
                );

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    if (!resultSet.next()) {
                        throw new SQLException(
                                "Achievement progress row missing after increment"
                        );
                    }

                    return resultSet.getLong(
                            "progress_value"
                    );
                }
            }
        }
    }

    public long getProgress(
            UUID playerUuid,
            String progressType,
            String progressTarget
    ) throws SQLException {

        String sql = """
                SELECT progress_value
                FROM rpg_player_achievement_progress
                WHERE player_uuid = ?
                  AND progress_type = ?
                  AND progress_target = ?
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

            statement.setString(
                    2,
                    progressType
            );

            statement.setString(
                    3,
                    progressTarget
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return 0L;
                }

                return resultSet.getLong(
                        "progress_value"
                );
            }
        }
    }
}
