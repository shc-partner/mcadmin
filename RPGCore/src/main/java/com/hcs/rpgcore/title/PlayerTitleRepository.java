package com.hcs.rpgcore.title;

import com.hcs.rpgcore.database.DatabaseManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * 기존 명령어 및 표시 서비스와의 호환용 Repository.
 *
 * 실제 저장소:
 * - rpg_title_definitions
 * - rpg_player_title_unlocks
 * - rpg_player_title_equipped
 *
 */
public final class PlayerTitleRepository {

    public record PlayerTitle(
            UUID playerUuid,
            String titleText,
            String titleColor
    ) {}

    private final DatabaseManager databaseManager;

    public PlayerTitleRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 관리자 명령어의 대상은 실제 계정명이 아닌 display_name.
     * 같은 표시 이름이 여러 명이면 잘못 지급하지 않도록 중단한다.
     */
    public UUID findPlayerUuidByDisplayName(
            String displayName
    ) throws SQLException {

        String sql = """
                SELECT player_uuid
                FROM rpg_players
                WHERE display_name = ?
                LIMIT 2
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, displayName);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return null;
                }

                UUID playerUuid = UUID.fromString(
                        resultSet.getString("player_uuid")
                );

                if (resultSet.next()) {
                    throw new SQLException(
                            "Duplicate display_name: " + displayName
                    );
                }

                return playerUuid;
            }
        }
    }

    /**
     * 관리자 칭호 지급:
     * 1. 칭호 정의 생성
     * 2. 플레이어 획득 목록에 추가
     * 3. 해당 칭호 장착
     *
     * 세 작업은 하나의 DB 트랜잭션으로 처리한다.
     * 이전에 획득한 다른 칭호는 유지한다.
     */
    public void setTitle(
            UUID playerUuid,
            String titleText,
            String rarityName
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
                    "Invalid title text"
            );
        }

        TitleRarity rarity =
                TitleRarity.require(rarityName);

        String normalizedColor =
                rarity.hexColor();

        /*
         * 관리자 칭호는 문구 + 색상으로 고정 ID 생성.
         * 같은 문구라도 색상이 다르면 별도의 칭호로 보존한다.
         */
        String titleId = createAdminTitleId(
                titleText,
                normalizedColor
        );

        String defineSql = """
                INSERT IGNORE INTO rpg_title_definitions
                    (title_id, title_text, rarity, title_color,
                     unlock_type, unlock_value)
                VALUES (?, ?, ?, ?, 'admin', NULL)
                ON DUPLICATE KEY UPDATE
                    rarity = VALUES(rarity),
                    title_color = VALUES(title_color)
                """;

        String unlockSql = """
                INSERT IGNORE INTO rpg_player_title_unlocks
                    (player_uuid, title_id, unlock_source)
                VALUES (?, ?, 'admin')
                """;

        String equipSql = """
                INSERT INTO rpg_player_title_equipped
                    (player_uuid, title_id)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE
                    title_id = VALUES(title_id),
                    equipped_at = CURRENT_TIMESTAMP
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
                    statement.setString(1, titleId);
                    statement.setString(2, titleText);
                    statement.setString(
                            3,
                            rarity.displayName()
                    );
                    statement.setString(
                            4,
                            normalizedColor
                    );

                    statement.executeUpdate();
                }

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        unlockSql
                                )
                ) {
                    statement.setString(
                            1,
                            playerUuid.toString()
                    );
                    statement.setString(2, titleId);

                    statement.executeUpdate();
                }

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        equipSql
                                )
                ) {
                    statement.setString(
                            1,
                            playerUuid.toString()
                    );
                    statement.setString(2, titleId);

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

    /**
     * 현재 장착한 칭호만 조회한다.
     * 미장착이면 null.
     */
    public PlayerTitle findTitle(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT
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

                TitleRarity rarity =
                        TitleRarity.fromDisplayName(
                                resultSet.getString("rarity")
                        );

                String color = rarity == null
                        ? resultSet.getString("title_color")
                        : rarity.hexColor();

                return new PlayerTitle(
                        playerUuid,
                        resultSet.getString("title_text"),
                        color
                );
            }
        }
    }

    /**
     * 현재 장착한 칭호만 해제한다.
     * 획득한 칭호는 삭제하지 않는다.
     */
    public boolean removeTitle(
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

    private String createAdminTitleId(
            String titleText,
            String titleColor
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    (titleText + "\u0000" + titleColor)
                            .getBytes(StandardCharsets.UTF_8)
            );

            return "admin_"
                    + HexFormat.of()
                            .formatHex(hash)
                            .substring(0, 56);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}
