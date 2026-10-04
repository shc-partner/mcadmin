package com.hcs.rpgcore.storage;

import com.hcs.rpgcore.database.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StorageChestRepository {

    public static final int INVENTORY_SIZE = 54;

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public StorageChestRepository(
            DatabaseManager databaseManager,
            Logger logger
    ) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public String getChestType(
            UUID instanceUuid
    ) {

        String sql = """
                SELECT chest_type
                FROM rpg_storage_chests
                WHERE instance_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    instanceUuid.toString()
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getString(
                        "chest_type"
                );
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 종류 조회 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }


    public boolean exists(UUID instanceUuid) {

        String sql = """
                SELECT 1
                FROM rpg_storage_chests
                WHERE instance_uuid = ?
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
                    instanceUuid.toString()
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 존재 여부 조회 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    public boolean isPlaced(UUID instanceUuid) {

        String sql = """
                SELECT placed
                FROM rpg_storage_chests
                WHERE instance_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    instanceUuid.toString()
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return false;
                }

                return resultSet.getBoolean("placed");
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 설치 상태 조회 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    public void create(
            UUID instanceUuid,
            String chestType,
            UUID ownerUuid
    ) {

        String sql = """
                INSERT INTO rpg_storage_chests (
                    instance_uuid,
                    chest_type,
                    owner_uuid,
                    placed
                )
                VALUES (?, ?, ?, 0)
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    instanceUuid.toString()
            );

            statement.setString(
                    2,
                    chestType
            );

            statement.setString(
                    3,
                    ownerUuid != null
                            ? ownerUuid.toString()
                            : null
            );

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 인스턴스 생성 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    public void markPlaced(
            UUID instanceUuid,
            Location location,
            float yaw
    ) {

        String sql = """
                UPDATE rpg_storage_chests
                SET
                    placed = 1,
                    world_uuid = ?,
                    x = ?,
                    y = ?,
                    z = ?,
                    yaw = ?
                WHERE instance_uuid = ?
                  AND placed = 0
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    location.getWorld()
                            .getUID()
                            .toString()
            );

            statement.setDouble(
                    2,
                    location.getX()
            );

            statement.setDouble(
                    3,
                    location.getY()
            );

            statement.setDouble(
                    4,
                    location.getZ()
            );

            statement.setFloat(
                    5,
                    yaw
            );

            statement.setString(
                    6,
                    instanceUuid.toString()
            );

            int updated =
                    statement.executeUpdate();

            if (updated != 1) {

                throw new IllegalStateException(
                        "이미 설치된 보관함입니다: "
                                + instanceUuid
                );
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 설치 상태 저장 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    public void markUnplaced(
            UUID instanceUuid
    ) {

        String sql = """
                UPDATE rpg_storage_chests
                SET
                    placed = 0,
                    world_uuid = NULL,
                    x = NULL,
                    y = NULL,
                    z = NULL,
                    yaw = NULL
                WHERE instance_uuid = ?
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    instanceUuid.toString()
            );

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 회수 상태 저장 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    public ItemStack[] loadContents(
            UUID instanceUuid
    ) {

        ItemStack[] contents =
                new ItemStack[INVENTORY_SIZE];

        String sql = """
                SELECT
                    slot,
                    item_data
                FROM rpg_storage_chest_items
                WHERE instance_uuid = ?
                ORDER BY slot
                """;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    instanceUuid.toString()
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    int slot =
                            resultSet.getInt(
                                    "slot"
                            );

                    if (
                            slot < 0
                                    || slot >=
                                    INVENTORY_SIZE
                    ) {
                        continue;
                    }

                    String encoded =
                            resultSet.getString(
                                    "item_data"
                            );

                    try {

                        byte[] bytes =
                                Base64
                                        .getDecoder()
                                        .decode(encoded);

                        contents[slot] =
                                ItemStack.deserializeBytes(
                                        bytes
                                );

                    } catch (Exception exception) {

                        logger.log(
                                Level.WARNING,
                                "보관함 아이템 데이터 복원 실패. "
                                        + "instance="
                                        + instanceUuid
                                        + ", slot="
                                        + slot,
                                exception
                        );
                    }
                }
            }

            return contents;

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "보관함 내용물 로드 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    public void saveContents(
            UUID instanceUuid,
            ItemStack[] contents
    ) {

        String deleteSql = """
                DELETE FROM rpg_storage_chest_items
                WHERE instance_uuid = ?
                """;

        String insertSql = """
                INSERT INTO rpg_storage_chest_items (
                    instance_uuid,
                    slot,
                    item_data
                )
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection =
                        databaseManager.getConnection()
        ) {

            connection.setAutoCommit(false);

            try (
                    PreparedStatement deleteStatement =
                            connection.prepareStatement(
                                    deleteSql
                            );

                    PreparedStatement insertStatement =
                            connection.prepareStatement(
                                    insertSql
                            )
            ) {

                deleteStatement.setString(
                        1,
                        instanceUuid.toString()
                );

                deleteStatement.executeUpdate();

                int limit =
                        Math.min(
                                contents.length,
                                INVENTORY_SIZE
                        );

                for (
                        int slot = 0;
                        slot < limit;
                        slot++
                ) {

                    ItemStack item =
                            contents[slot];

                    if (
                            item == null
                                    || item.getType()
                                    .isAir()
                    ) {
                        continue;
                    }

                    String encoded =
                            Base64
                                    .getEncoder()
                                    .encodeToString(
                                            item.serializeAsBytes()
                                    );

                    insertStatement.setString(
                            1,
                            instanceUuid.toString()
                    );

                    insertStatement.setInt(
                            2,
                            slot
                    );

                    insertStatement.setString(
                            3,
                            encoded
                    );

                    insertStatement.addBatch();
                }

                insertStatement.executeBatch();

                connection.commit();

            } catch (Exception exception) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {

                    exception.addSuppressed(
                            rollbackException
                    );
                }

                throw exception;

            } finally {

                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "보관함 내용물 저장 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

    /*
     * =========================================================
     * INITIAL CONTENTS
     * =========================================================
     *
     * 아직 설치되지 않은 보상용 보관함의 초기 내용물을
     * DB에 저장한다.
     */
    public void saveInitialContents(
            UUID instanceUuid,
            ItemStack[] contents
    ) {

        if (instanceUuid == null) {
            throw new IllegalArgumentException(
                    "instanceUuid is null"
            );
        }

        if (contents == null) {
            throw new IllegalArgumentException(
                    "contents is null"
            );
        }

        if (contents.length > INVENTORY_SIZE) {
            throw new IllegalArgumentException(
                    "보관함 최대 슬롯 수 초과: "
                            + contents.length
            );
        }


        String deleteSql = """
                DELETE FROM rpg_storage_chest_items
                WHERE instance_uuid = ?
                """;

        String insertSql = """
                INSERT INTO rpg_storage_chest_items (
                    instance_uuid,
                    slot,
                    item_data
                )
                VALUES (?, ?, ?)
                """;


        try (
                Connection connection =
                        databaseManager.getConnection()
        ) {

            boolean oldAutoCommit =
                    connection.getAutoCommit();

            connection.setAutoCommit(false);

            try {

                try (
                        PreparedStatement deleteStatement =
                                connection.prepareStatement(
                                        deleteSql
                                )
                ) {

                    deleteStatement.setString(
                            1,
                            instanceUuid.toString()
                    );

                    deleteStatement.executeUpdate();
                }


                try (
                        PreparedStatement insertStatement =
                                connection.prepareStatement(
                                        insertSql
                                )
                ) {

                    for (
                            int slot = 0;
                            slot < contents.length;
                            slot++
                    ) {

                        ItemStack item =
                                contents[slot];

                        if (
                                item == null
                                        || item.getType()
                                        .isAir()
                        ) {
                            continue;
                        }

                        String encoded =
                                Base64
                                        .getEncoder()
                                        .encodeToString(
                                                item.serializeAsBytes()
                                        );

                        insertStatement.setString(
                                1,
                                instanceUuid.toString()
                        );

                        insertStatement.setInt(
                                2,
                                slot
                        );

                        insertStatement.setString(
                                3,
                                encoded
                        );

                        insertStatement.addBatch();
                    }

                    insertStatement.executeBatch();
                }


                connection.commit();

            } catch (Exception exception) {

                connection.rollback();

                throw exception;

            } finally {

                connection.setAutoCommit(
                        oldAutoCommit
                );
            }

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "보관함 초기 내용물 저장 실패: "
                            + instanceUuid,
                    exception
            );
        }
    }

}
