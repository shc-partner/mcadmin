package com.hcs.rpgcore.player;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerRepository {

    private final DatabaseManager databaseManager;

    public PlayerRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 플레이어 최초 접속 시 기본 데이터를 생성하고,
     * 이미 존재하는 플레이어라면 닉네임과 마지막 접속 시간을 갱신한다.
     */
    public void createOrUpdatePlayer(
            UUID uuid,
            String playerName
    ) throws SQLException {

        String sql = """
                INSERT INTO rpg_players (
                    player_uuid,
                    player_name,
                    level,
                    experience,
                    player_class,
                    created_at,
                    last_login_at
                )
                VALUES (
                    ?,
                    ?,
                    1,
                    0,
                    'NONE',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    last_login_at = CURRENT_TIMESTAMP
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);

            statement.executeUpdate();
        }
    }

    /**
     * UUID를 기준으로 플레이어 RPG 데이터를 조회한다.
     *
     * @return 플레이어 데이터가 없으면 null
     */
    public PlayerData findPlayer(
            UUID uuid
    ) throws SQLException {

        String sql = """
                SELECT
                    player_uuid,
                    player_name,
                    level,
                    experience,
                    player_class
                FROM rpg_players
                WHERE player_uuid = ?
                LIMIT 1
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, uuid.toString());

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return null;
                }

                return new PlayerData(
                        UUID.fromString(
                                resultSet.getString("player_uuid")
                        ),
                        resultSet.getString("player_name"),
                        resultSet.getInt("level"),
                        resultSet.getLong("experience"),
                        resultSet.getString("player_class")
                );
            }
        }
    }

    /**
     * 화면에 표시할 RPG 닉네임을 조회한다.
     *
     * @return 설정되지 않았으면 null
     */
    public String findDisplayName(
            UUID uuid
    ) throws SQLException {

        String sql = """
                SELECT display_name
                FROM rpg_players
                WHERE player_uuid = ?
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
                    uuid.toString()
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getString(
                        "display_name"
                );
            }
        }
    }


    /**
     * RPG 표시 닉네임을 저장한다.
     */
    public void updateDisplayName(
            UUID uuid,
            String displayName
    ) throws SQLException {

        String sql = """
                UPDATE rpg_players
                SET display_name = ?
                WHERE player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    displayName
            );

            statement.setString(
                    2,
                    uuid.toString()
            );

            statement.executeUpdate();
        }
    }


    /**
     * RPG 표시 닉네임을 해제한다.
     */
    public void clearDisplayName(
            UUID uuid
    ) throws SQLException {

        String sql = """
                UPDATE rpg_players
                SET display_name = NULL
                WHERE player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    uuid.toString()
            );

            statement.executeUpdate();
        }
    }



    /**
     * 플레이어의 레벨과 현재 구간 경험치를 저장한다.
     */
    public void updateLevelAndExperience(
            UUID uuid,
            int level,
            long experience
    ) throws SQLException {

        String sql = """
                UPDATE rpg_players
                SET
                    level = ?,
                    experience = ?
                WHERE player_uuid = ?
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, level);
            statement.setLong(2, experience);
            statement.setString(3, uuid.toString());

            statement.executeUpdate();
        }
    }

    /**
     * 관리 명령 등에서 레벨만 직접 변경할 때 사용한다.
     */
    public void updateLevel(
            UUID uuid,
            int level
    ) throws SQLException {

        String sql = """
                UPDATE rpg_players
                SET level = ?
                WHERE player_uuid = ?
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, level);
            statement.setString(2, uuid.toString());

            statement.executeUpdate();
        }
    }

    /**
     * 관리 명령 등에서 경험치만 직접 변경할 때 사용한다.
     */
    public void updateExperience(
            UUID uuid,
            long experience
    ) throws SQLException {

        String sql = """
                UPDATE rpg_players
                SET experience = ?
                WHERE player_uuid = ?
                """;

        try (
                Connection connection = databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, experience);
            statement.setString(2, uuid.toString());

            statement.executeUpdate();
        }
    }

    /**
     * 직업 선택 시 직업 / 레벨 / 경험치를
     * 하나의 UPDATE로 저장한다.
     */
    public void updateClassLevelAndExperience(
            UUID uuid,
            String playerClass,
            int level,
            long experience
    ) throws SQLException {

        String sql = """
                UPDATE rpg_players
                SET
                    player_class = ?,
                    level = ?,
                    experience = ?
                WHERE player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setString(
                    1,
                    playerClass
            );

            statement.setInt(
                    2,
                    level
            );

            statement.setLong(
                    3,
                    experience
            );

            statement.setString(
                    4,
                    uuid.toString()
            );

            statement.executeUpdate();
        }
    }

}
