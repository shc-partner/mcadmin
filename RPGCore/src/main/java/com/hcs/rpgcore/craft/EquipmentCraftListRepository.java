package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 15 장비 미리보기 목록 조회.
 *
 * rpg_items.equipment_type:
 * weapon / armor / shield
 *
 * DB 데이터를 수정하지 않는다.
 */
public final class EquipmentCraftListRepository {

    public record PreviewItem(
            String itemId,
            String displayName,
            String equipmentSlot,
            Integer setId
    ) {
    }

    private final DatabaseManager databaseManager;

    public EquipmentCraftListRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    public List<PreviewItem> findItems(
            String equipmentType,
            String rarity
    ) throws SQLException {

        if (!List.of("weapon", "armor", "shield")
                .contains(equipmentType)) {
            throw new IllegalArgumentException(
                    "Unsupported equipment type: " + equipmentType
            );
        }

        if (!List.of("영웅", "전설", "신화")
                .contains(rarity)) {
            throw new IllegalArgumentException(
                    "Unsupported rarity: " + rarity
            );
        }

        String sql = """
                SELECT
                    item_id,
                    display_name,
                    equipment_slot,
                    set_id
                FROM rpg_items
                WHERE enabled = 1
                  AND rarity = ?
                  AND equipment_type = ?
                  AND item_id NOT LIKE
                      'mal!_nyun!_%' ESCAPE '!'
                  AND (
                      (
                          equipment_type = 'weapon'
                          AND (
                              equipment_slot IS NULL
                              OR equipment_slot = 'HAND'
                          )
                      )
                      OR
                      (
                          equipment_type = 'armor'
                          AND equipment_slot IN (
                              'HEAD',
                              'CHEST',
                              'LEGS',
                              'FEET',
                              'OFF_HAND'
                          )
                      )
                      OR
                      (
                          equipment_type = 'shield'
                          AND equipment_slot = 'OFF_HAND'
                      )
                  )
                ORDER BY display_name, item_id
                """;

        List<PreviewItem> items = new ArrayList<>();

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, rarity);
            statement.setString(2, equipmentType);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    int rawSetId =
                            resultSet.getInt("set_id");

                    Integer setId =
                            resultSet.wasNull()
                                    ? null
                                    : rawSetId;

                    items.add(
                            new PreviewItem(
                                    resultSet.getString("item_id"),
                                    resultSet.getString("display_name"),
                                    resultSet.getString("equipment_slot"),
                                    setId
                            )
                    );
                }
            }
        }

        return items;
    }
}
