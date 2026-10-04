package com.hcs.rpgcore.title;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 다중 칭호 관리.
 *
 * definitions : 칭호 정의
 * unlocks     : 플레이어별 획득 칭호
 * equipped    : 플레이어별 장착 칭호
 *
 * DB 메서드는 메인 스레드에서 반복 호출하지 않는다.
 */
public final class PlayerTitleCollectionRepository {

    public record Title(
            String titleId,
            String titleText,
            String titleColor
    ) {}

    private final DatabaseManager databaseManager;

    public PlayerTitleCollectionRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 서버에 미리 정의된 칭호를 표시 이름으로 조회한다.
     * 같은 이름이 여러 개라면 지급 대상을 확정하지 않는다.
     */
    public Title findDefinitionByText(
            String titleText
    ) throws SQLException {

        String sql = """
                SELECT title_id, title_text, title_color, rarity
                FROM rpg_title_definitions
                WHERE title_text = ?
                LIMIT 2
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, titleText);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return null;
                }

                Title title = readTitle(resultSet);

                if (resultSet.next()) {
                    throw new SQLException(
                            "Duplicate title_text: " + titleText
                    );
                }

                return title;
            }
        }
    }

    /**
     * 칭호 정의 조회.
     */
    public Title findDefinition(
            String titleId
    ) throws SQLException {

        String sql = """
                SELECT title_id, title_text, title_color, rarity
                FROM rpg_title_definitions
                WHERE title_id = ?
                LIMIT 1
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, titleId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return readTitle(resultSet);
            }
        }
    }

    /**
     * 플레이어가 획득한 칭호 전체 조회.
     *
     * NPC 26번의 칭호 목록 GUI에서 사용한다.
     */
    public List<Title> findUnlockedTitles(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT
                    d.title_id,
                    d.title_text,
                    d.title_color,
                    d.rarity
                FROM rpg_player_title_unlocks AS u
                INNER JOIN rpg_title_definitions AS d
                    ON d.title_id = u.title_id
                WHERE u.player_uuid = ?
                ORDER BY u.unlocked_at DESC, d.title_id ASC
                """;

        List<Title> titles = new ArrayList<>();

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
                    titles.add(
                            readTitle(resultSet)
                    );
                }
            }
        }

        return titles;
    }

    /**
     * 칭호 획득.
     *
     * true  : 이번 호출에서 새로 획득
     * false : 이미 획득했거나 존재하지 않는 칭호
     *
     * PRIMARY KEY(player_uuid, title_id)에 의해
     * 중복 획득은 방지된다.
     */
    public boolean unlockTitle(
            UUID playerUuid,
            String titleId,
            String unlockSource
    ) throws SQLException {

        String sql = """
                INSERT IGNORE INTO rpg_player_title_unlocks
                    (player_uuid, title_id, unlock_source)
                SELECT ?, title_id, ?
                FROM rpg_title_definitions
                WHERE title_id = ?
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
                    unlockSource
            );

            statement.setString(
                    3,
                    titleId
            );

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * 현재 장착한 칭호 조회.
     *
     * 획득 기록이 존재하는 칭호만 반환한다.
     * 미장착 상태이면 null.
     */
    public Title findEquippedTitle(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT
                    d.title_id,
                    d.title_text,
                    d.title_color,
                    d.rarity
                FROM rpg_player_title_equipped AS e
                INNER JOIN rpg_player_title_unlocks AS u
                    ON u.player_uuid = e.player_uuid
                    AND u.title_id = e.title_id
                INNER JOIN rpg_title_definitions AS d
                    ON d.title_id = e.title_id
                WHERE e.player_uuid = ?
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

                return readTitle(resultSet);
            }
        }
    }

    /**
     * 획득한 칭호 장착.
     *
     * 획득하지 않은 칭호는 장착할 수 없다.
     * 이미 장착한 칭호를 다시 선택해도 성공으로 처리한다.
     */
    public boolean equipTitle(
            UUID playerUuid,
            String titleId
    ) throws SQLException {

        String sql = """
                INSERT INTO rpg_player_title_equipped
                    (player_uuid, title_id)
                SELECT player_uuid, title_id
                FROM rpg_player_title_unlocks
                WHERE player_uuid = ?
                  AND title_id = ?
                ON DUPLICATE KEY UPDATE
                    title_id = VALUES(title_id),
                    equipped_at = CURRENT_TIMESTAMP
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
                    titleId
            );

            statement.executeUpdate();
        }

        /*
         * UPDATE 결과가 0이어도 이미 같은 칭호를
         * 장착 중일 수 있으므로 실제 장착 상태로 확인한다.
         */
        Title equipped = findEquippedTitle(playerUuid);

        return equipped != null
                && titleId.equals(equipped.titleId());
    }

    /**
     * 칭호 장착 해제.
     *
     * 획득 기록은 삭제하지 않는다.
     */
    public boolean unequipTitle(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                DELETE FROM rpg_player_title_equipped
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

    private Title readTitle(
            ResultSet resultSet
    ) throws SQLException {

        TitleRarity rarity = TitleRarity.fromDisplayName(
                resultSet.getString("rarity")
        );

        String color = rarity == null
                ? resultSet.getString("title_color")
                : rarity.hexColor();

        return new Title(
                resultSet.getString("title_id"),
                resultSet.getString("title_text"),
                color
        );
    }
}
