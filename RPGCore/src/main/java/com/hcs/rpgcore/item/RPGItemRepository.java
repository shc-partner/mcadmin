package com.hcs.rpgcore.item;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*
 * =========================================================
 * RPG ITEM REPOSITORY
 * =========================================================
 */
public final class RPGItemRepository {

    private final DatabaseManager
            databaseManager;


    public RPGItemRepository(
            DatabaseManager databaseManager
    ) {

        this.databaseManager =
                databaseManager;
    }


    /*
     * =========================================================
     * FIND ALL ENABLED
     * =========================================================
     */
    public List<RPGItemDefinition>
    findAllEnabled() throws SQLException {

        String sql = """
                SELECT
                    item_id,
                    display_name,
                    rarity,

                    description,
                    description_2,
                    description_3,

                    display_type,
                    special_effect_1,
                    special_effect_2,

                    material,
                    item_model,
                    equipment_model,
                    equipment_slot,

                    unbreakable,
                    max_durability,

                    weapon_attack,
                    weapon_attack_speed,

                    armor_bonus,
                    display_armor,

                    toughness_bonus,
                    display_toughness,

                    display_knockback_resistance,

                    damage_reduction_flat,
                    max_mana,

                    glide,

                    consumable_enabled,
                    consumable_seconds,

                    restore_hp_percent,
                    restore_mp_percent,

                    bound,
                    enabled,

                    enhanceable,
                    enhancement_type,
                    max_enhancement_level

                FROM rpg_items

                WHERE enabled = 1

                ORDER BY item_id
                """;


        List<RPGItemDefinition> result =
                new ArrayList<>();


        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (
                    resultSet.next()
            ) {

                result.add(
                        readDefinition(
                                resultSet
                        )
                );
            }
        }


        return result;
    }


    /*
     * =========================================================
     * FIND BY ID
     * =========================================================
     */
    public RPGItemDefinition findById(
            String itemId
    ) throws SQLException {

        String sql = """
                SELECT
                    item_id,
                    display_name,
                    rarity,

                    description,
                    description_2,
                    description_3,

                    display_type,
                    special_effect_1,
                    special_effect_2,

                    material,
                    item_model,
                    equipment_model,
                    equipment_slot,

                    unbreakable,
                    max_durability,

                    weapon_attack,
                    weapon_attack_speed,

                    armor_bonus,
                    display_armor,

                    toughness_bonus,
                    display_toughness,

                    display_knockback_resistance,

                    damage_reduction_flat,
                    max_mana,

                    glide,

                    consumable_enabled,
                    consumable_seconds,

                    restore_hp_percent,
                    restore_mp_percent,

                    bound,
                    enabled,

                    enhanceable,
                    enhancement_type,
                    max_enhancement_level

                FROM rpg_items

                WHERE item_id = ?

                LIMIT 1
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
                    itemId
            );


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (
                        !resultSet.next()
                ) {

                    return null;
                }


                return readDefinition(
                        resultSet
                );
            }
        }
    }


    /*
     * =========================================================
     * ENCHANTMENTS
     * =========================================================
     */
    public Map<String, Map<String, Integer>>
    findAllEnchantments()
            throws SQLException {

        String sql = """
                SELECT
                    e.item_id,
                    e.enchantment_key,
                    e.level

                FROM rpg_item_enchantments e

                INNER JOIN rpg_items i
                    ON i.item_id = e.item_id

                WHERE i.enabled = 1

                ORDER BY
                    e.item_id,
                    e.enchantment_key
                """;


        Map<String, Map<String, Integer>> result =
                new LinkedHashMap<>();


        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (
                    resultSet.next()
            ) {

                String itemId =
                        resultSet.getString(
                                "item_id"
                        );

                String enchantmentKey =
                        resultSet.getString(
                                "enchantment_key"
                        );

                int level =
                        resultSet.getInt(
                                "level"
                        );


                result
                        .computeIfAbsent(
                                itemId,
                                ignored ->
                                        new LinkedHashMap<>()
                        )
                        .put(
                                enchantmentKey,
                                level
                        );
            }
        }


        return result;
    }


    /*
     * =========================================================
     * COUNT
     * =========================================================
     */
    public long count()
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM rpg_items";


        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            resultSet.next();

            return resultSet.getLong(
                    1
            );
        }
    }


    /*
     * =========================================================
     * READ
     * =========================================================
     */
    private RPGItemDefinition readDefinition(
            ResultSet resultSet
    ) throws SQLException {

        return new RPGItemDefinition(

                resultSet.getString(
                        "item_id"
                ),

                resultSet.getString(
                        "display_name"
                ),

                resultSet.getString(
                        "rarity"
                ),

                resultSet.getString(
                        "description"
                ),

                resultSet.getString(
                        "description_2"
                ),

                resultSet.getString(
                        "description_3"
                ),

                resultSet.getString(
                        "display_type"
                ),

                resultSet.getString(
                        "special_effect_1"
                ),

                resultSet.getString(
                        "special_effect_2"
                ),

                resultSet.getString(
                        "material"
                ),

                resultSet.getString(
                        "item_model"
                ),

                resultSet.getString(
                        "equipment_model"
                ),

                resultSet.getString(
                        "equipment_slot"
                ),

                resultSet.getBoolean(
                        "unbreakable"
                ),

                getNullableInt(
                        resultSet,
                        "max_durability"
                ),

                getNullableDouble(
                        resultSet,
                        "weapon_attack"
                ),

                getNullableDouble(
                        resultSet,
                        "weapon_attack_speed"
                ),

                getNullableDouble(
                        resultSet,
                        "armor_bonus"
                ),

                getNullableDouble(
                        resultSet,
                        "display_armor"
                ),

                getNullableDouble(
                        resultSet,
                        "toughness_bonus"
                ),

                getNullableDouble(
                        resultSet,
                        "display_toughness"
                ),

                getNullableDouble(
                        resultSet,
                        "display_knockback_resistance"
                ),

                getNullableDouble(
                        resultSet,
                        "damage_reduction_flat"
                ),

                getNullableDouble(
                        resultSet,
                        "max_mana"
                ),

                resultSet.getBoolean(
                        "glide"
                ),

                resultSet.getBoolean(
                        "consumable_enabled"
                ),

                getNullableDouble(
                        resultSet,
                        "consumable_seconds"
                ),

                getNullableDouble(
                        resultSet,
                        "restore_hp_percent"
                ),

                getNullableDouble(
                        resultSet,
                        "restore_mp_percent"
                ),

                resultSet.getBoolean(
                        "bound"
                ),

                resultSet.getBoolean(
                        "enabled"
                ),

                resultSet.getBoolean(
                        "enhanceable"
                ),

                resultSet.getString(
                        "enhancement_type"
                ),

                resultSet.getInt(
                        "max_enhancement_level"
                )
        );
    }


    private Integer getNullableInt(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        int value =
                resultSet.getInt(
                        column
                );


        if (
                resultSet.wasNull()
        ) {

            return null;
        }


        return value;
    }


    private Double getNullableDouble(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        double value =
                resultSet.getDouble(
                        column
                );


        if (
                resultSet.wasNull()
        ) {

            return null;
        }


        return value;
    }
}
