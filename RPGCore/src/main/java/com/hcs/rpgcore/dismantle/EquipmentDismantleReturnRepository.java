package com.hcs.rpgcore.dismantle;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * NPC 17 분해 GUI에서 반환하지 못한 장비의 영구 보관소.
 *
 * 플레이어당 미수령 장비 1개만 허용한다.
 * 기존 데이터가 있으면 INSERT가 실패하며 덮어쓰지 않는다.
 */
public final class EquipmentDismantleReturnRepository {

    private final DatabaseManager databaseManager;

    public EquipmentDismantleReturnRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    public void createTable() throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS
                    rpg_dismantle_pending_items (
                    player_uuid CHAR(36) NOT NULL,
                    item_data MEDIUMBLOB NOT NULL,
                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    PRIMARY KEY (player_uuid)
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

    public void save(
            UUID playerUuid,
            ItemStack item
    ) throws SQLException {

        if (item == null || item.getType().isAir()
                || item.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "보관할 장비가 없습니다."
            );
        }

        byte[] itemData = item.serializeAsBytes();

        String sql = """
                INSERT INTO rpg_dismantle_pending_items (
                    player_uuid,
                    item_data
                )
                VALUES (?, ?)
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, playerUuid.toString());
            statement.setBytes(2, itemData);
            statement.executeUpdate();
        }
    }

    public ItemStack load(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                SELECT item_data
                FROM rpg_dismantle_pending_items
                WHERE player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, playerUuid.toString());

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                if (!resultSet.next()) {
                    return null;
                }

                byte[] itemData =
                        resultSet.getBytes("item_data");

                if (itemData == null || itemData.length == 0) {
                    throw new SQLException(
                            "보관된 장비 데이터가 비어 있습니다."
                    );
                }

                try {
                    return ItemStack.deserializeBytes(itemData);

                } catch (RuntimeException exception) {
                    throw new SQLException(
                            "보관된 장비 데이터를 복원하지 못했습니다.",
                            exception
                    );
                }
            }
        }
    }

    public int delete(
            UUID playerUuid
    ) throws SQLException {

        String sql = """
                DELETE FROM rpg_dismantle_pending_items
                WHERE player_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, playerUuid.toString());
            return statement.executeUpdate();
        }
    }
}
