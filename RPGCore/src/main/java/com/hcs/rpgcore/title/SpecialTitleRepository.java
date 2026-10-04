package com.hcs.rpgcore.title;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class SpecialTitleRepository {

    public record SpecialTitle(
            long specialTitleId,
            String titleText,
            String titleColor
    ) {}

    private final DatabaseManager databaseManager;

    public SpecialTitleRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /*
     * 특수 칭호를 등록하고 플레이어에게 강제 적용한다.
     *
     * 같은 문구 + 색상은 기존 정의를 재사용한다.
     * 플레이어별 활성 특수 칭호는 한 개만 유지한다.
     */
    public void assign(
            UUID playerUuid,
            String titleText,
            String titleColor,
            String assignedBy
    ) throws SQLException {

        if (playerUuid == null) {
            throw new IllegalArgumentException(
                    "playerUuid must not be null"
            );
        }

        if (titleText == null
                || titleText.isBlank()
                || titleText.length() > 32
                || titleText.contains("\n")
                || titleText.contains("\r")) {
            throw new IllegalArgumentException(
                    "Invalid special title text"
            );
        }

        if (!"#c40000".equals(titleColor)
                && !"#0041d4".equals(titleColor)) {
            throw new IllegalArgumentException(
                    "Unsupported special title color"
            );
        }

        String defineSql = """
                INSERT INTO rpg_special_titles
                    (title_text, title_color, created_by)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    special_title_id = special_title_id
                """;

        String findIdSql = """
                SELECT special_title_id
                FROM rpg_special_titles
                WHERE title_text = ?
                  AND title_color = ?
                LIMIT 1
                """;

        String assignSql = """
                INSERT INTO rpg_player_special_titles
                    (player_uuid, special_title_id, assigned_by)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    special_title_id = VALUES(special_title_id),
                    assigned_by = VALUES(assigned_by),
                    assigned_at = CURRENT_TIMESTAMP
                """;

        try (
                Connection connection =
                        databaseManager.getConnection()
        ) {
            connection.setAutoCommit(false);

            try {
                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        defineSql
                                )
                ) {
                    statement.setString(1, titleText);
                    statement.setString(2, titleColor);
                    statement.setString(3, assignedBy);
                    statement.executeUpdate();
                }

                long specialTitleId;

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        findIdSql
                                )
                ) {
                    statement.setString(1, titleText);
                    statement.setString(2, titleColor);

                    try (
                            ResultSet resultSet =
                                    statement.executeQuery()
                    ) {
                        if (!resultSet.next()) {
                            throw new SQLException(
                                    "특수 칭호 ID 조회 실패"
                            );
                        }

                        specialTitleId =
                                resultSet.getLong(
                                        "special_title_id"
                                );
                    }
                }

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        assignSql
                                )
                ) {
                    statement.setString(
                            1,
                            playerUuid.toString()
                    );
                    statement.setLong(
                            2,
                            specialTitleId
                    );
                    statement.setString(
                            3,
                            assignedBy
                    );
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(
                            rollbackException
                    );
                }

                throw exception;
            }
        }
    }

    /*
     * 현재 강제 적용 중인 특수 칭호 조회.
     */
    public SpecialTitle findActive(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT
                    s.special_title_id,
                    s.title_text,
                    s.title_color
                FROM rpg_player_special_titles AS p
                INNER JOIN rpg_special_titles AS s
                    ON s.special_title_id = p.special_title_id
                WHERE p.player_uuid = ?
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

                return new SpecialTitle(
                        resultSet.getLong(
                                "special_title_id"
                        ),
                        resultSet.getString(
                                "title_text"
                        ),
                        resultSet.getString(
                                "title_color"
                        )
                );
            }
        }
    }

    /*
     * 운영자가 강제 칭호를 해제한다.
     * 일반 칭호의 획득·장착 기록은 변경하지 않는다.
     */
    public boolean clear(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                DELETE FROM rpg_player_special_titles
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
                    playerUuid.toString()
            );

            return statement.executeUpdate() > 0;
        }
    }
}
