package com.hcs.rpgcore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.configuration.file.FileConfiguration;

public final class DatabaseManager implements AutoCloseable {

    private HikariDataSource dataSource;

    public void connect(FileConfiguration config) {
        String host = config.getString("database.host", "127.0.0.1");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.name", "rpg_server");
        String username = config.getString("database.username", "rpgcore");
        String password = config.getString("database.password", "");

        if (password.isBlank() || password.equals("CHANGE_ME")) {
            throw new IllegalStateException(
                    "database.yml에 실제 MariaDB 비밀번호가 설정되지 않았습니다."
            );
        }

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setPoolName("RPGCore-HikariPool");
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");

        hikariConfig.setJdbcUrl(
                "jdbc:mariadb://" + host + ":" + port + "/" + database
                        + "?useUnicode=true"
                        + "&characterEncoding=utf8"
                        + "&useServerPrepStmts=true"
        );

        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikariConfig.setMaximumPoolSize(
                config.getInt("database.pool.maximum-size", 10)
        );

        hikariConfig.setMinimumIdle(
                config.getInt("database.pool.minimum-idle", 2)
        );

        hikariConfig.setConnectionTimeout(
                config.getLong(
                        "database.pool.connection-timeout-ms",
                        10000L
                )
        );

        hikariConfig.setValidationTimeout(
                config.getLong(
                        "database.pool.validation-timeout-ms",
                        5000L
                )
        );

        hikariConfig.setIdleTimeout(
                config.getLong(
                        "database.pool.idle-timeout-ms",
                        600000L
                )
        );

        hikariConfig.setMaxLifetime(
                config.getLong(
                        "database.pool.max-lifetime-ms",
                        1800000L
                )
        );

        hikariConfig.setConnectionTestQuery("SELECT 1");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public void testConnection() throws SQLException {
        ensureConnected();

        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute("SELECT 1");
        }
    }

    public void createTables() throws SQLException {
        ensureConnected();

        String sql = """
                CREATE TABLE IF NOT EXISTS rpg_players (
                    player_uuid CHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    level SMALLINT UNSIGNED NOT NULL DEFAULT 1,
                    experience BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    player_class VARCHAR(32) NOT NULL DEFAULT 'NONE',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

                    PRIMARY KEY (player_uuid),
                    INDEX idx_rpg_players_name (player_name),
                    INDEX idx_rpg_players_level (level)
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;

        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(sql);
        }

        createItemTables();
        createStorageChestTables();
        createMarketPriceTables();
    }


    /*
     * =========================================================
     * RPG ITEM TABLES
     * =========================================================
     */
    private void createItemTables() throws SQLException {

        String itemSql = """
                CREATE TABLE IF NOT EXISTS rpg_items (
                    item_id VARCHAR(100) NOT NULL,
                    display_name VARCHAR(255) NOT NULL,

                    bound TINYINT(1) NOT NULL DEFAULT 0,
                    enabled TINYINT(1) NOT NULL DEFAULT 1,

                    enhanceable TINYINT(1) NOT NULL DEFAULT 0,
                    enhancement_type VARCHAR(32) DEFAULT NULL,
                    max_enhancement_level SMALLINT UNSIGNED
                        NOT NULL DEFAULT 0,

                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

                    PRIMARY KEY (item_id),

                    INDEX idx_rpg_items_enabled (
                        enabled
                    ),

                    INDEX idx_rpg_items_enhanceable (
                        enhanceable
                    )
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;



        String enchantmentSql = """
                CREATE TABLE IF NOT EXISTS rpg_item_enchantments (
                    item_id VARCHAR(100) NOT NULL,
                    enchantment_key VARCHAR(100) NOT NULL,
                    level INT NOT NULL,

                    PRIMARY KEY (
                        item_id,
                        enchantment_key
                    ),

                    CONSTRAINT fk_rpg_item_enchantments_item
                        FOREIGN KEY (item_id)
                        REFERENCES rpg_items(item_id)
                        ON DELETE CASCADE
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;


        String setSql = """
                CREATE TABLE IF NOT EXISTS rpg_item_sets (
                    set_id VARCHAR(100) NOT NULL,
                    display_name VARCHAR(255) NOT NULL,
                    enabled TINYINT(1) NOT NULL DEFAULT 1,

                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

                    PRIMARY KEY (set_id)
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;


        String setMemberSql = """
                CREATE TABLE IF NOT EXISTS rpg_item_set_members (
                    set_id VARCHAR(100) NOT NULL,
                    item_id VARCHAR(100) NOT NULL,
                    sort_order INT NOT NULL DEFAULT 0,

                    PRIMARY KEY (
                        set_id,
                        item_id
                    ),

                    INDEX idx_rpg_item_set_members_item (
                        item_id
                    ),

                    CONSTRAINT fk_rpg_item_set_members_set
                        FOREIGN KEY (set_id)
                        REFERENCES rpg_item_sets(set_id)
                        ON DELETE CASCADE,

                    CONSTRAINT fk_rpg_item_set_members_item
                        FOREIGN KEY (item_id)
                        REFERENCES rpg_items(item_id)
                        ON DELETE CASCADE
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;


        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(
                    itemSql
            );


            statement.executeUpdate(
                    enchantmentSql
            );

            statement.executeUpdate(
                    setSql
            );

            statement.executeUpdate(
                    setMemberSql
            );
        }
    }

    /*
     * =========================================================
     * STORAGE CHEST TABLES
     * =========================================================
     */
    private void createStorageChestTables()
            throws SQLException {

        String chestSql = """
                CREATE TABLE IF NOT EXISTS rpg_storage_chests (
                    instance_uuid CHAR(36) NOT NULL,
                    chest_type VARCHAR(100) NOT NULL,
                    owner_uuid CHAR(36) DEFAULT NULL,

                    placed TINYINT(1) NOT NULL DEFAULT 0,

                    world_uuid CHAR(36) DEFAULT NULL,
                    x DOUBLE DEFAULT NULL,
                    y DOUBLE DEFAULT NULL,
                    z DOUBLE DEFAULT NULL,
                    yaw FLOAT DEFAULT NULL,

                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

                    PRIMARY KEY (instance_uuid),

                    INDEX idx_storage_chest_type (
                        chest_type
                    ),

                    INDEX idx_storage_chest_owner (
                        owner_uuid
                    ),

                    INDEX idx_storage_chest_world (
                        world_uuid
                    )
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;

        String itemSql = """
                CREATE TABLE IF NOT EXISTS rpg_storage_chest_items (
                    instance_uuid CHAR(36) NOT NULL,
                    slot SMALLINT UNSIGNED NOT NULL,
                    item_data MEDIUMTEXT NOT NULL,

                    PRIMARY KEY (
                        instance_uuid,
                        slot
                    ),

                    CONSTRAINT fk_storage_chest_items_chest
                        FOREIGN KEY (instance_uuid)
                        REFERENCES rpg_storage_chests(
                            instance_uuid
                        )
                        ON DELETE CASCADE
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(
                    chestSql
            );

            statement.executeUpdate(
                    itemSql
            );
        }
    }


    public Connection getConnection() throws SQLException {
        ensureConnected();
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "MariaDB 연결 풀이 초기화되지 않았습니다."
            );
        }
    }

    /*
     * =========================================================
     * MARKET PRICE TABLES
     * =========================================================
     */
    private void createMarketPriceTables()
            throws SQLException {

        String sql = """
                CREATE TABLE IF NOT EXISTS rpg_market_prices (
                    market_day BIGINT NOT NULL,
                    market_id VARCHAR(64) NOT NULL,
                    item_id VARCHAR(100) NOT NULL,
                    price INT UNSIGNED NOT NULL,

                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    PRIMARY KEY (
                        market_day,
                        market_id,
                        item_id
                    ),

                    INDEX idx_market_item (
                        market_id,
                        item_id,
                        market_day
                    )
                )
                ENGINE=InnoDB
                DEFAULT CHARACTER SET utf8mb4
                COLLATE utf8mb4_unicode_ci
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(
                    sql
            );
        }
    }



    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
