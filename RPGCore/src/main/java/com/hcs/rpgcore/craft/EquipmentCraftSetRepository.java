package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.database.DatabaseManager;

import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.EnumMap;
import java.util.Map;

/**
 * rpg_items.set_id 기반 전체 세트 미리보기 조회.
 *
 * HEAD     -> HELMET
 * CHEST    -> CHESTPLATE
 * LEGS     -> LEGGINGS
 * FEET     -> BOOTS
 * OFF_HAND -> OFF_HAND
 *
 * 기본 투구는 *_hat(장식된 투구)이다.
 * *_helmet을 명시적으로 선택하면 해당 투명한 투구를 사용한다.
 *
 * NPC 생성이나 장비 변경은 수행하지 않는다.
 */
public final class EquipmentCraftSetRepository {

    private final DatabaseManager databaseManager;

    public EquipmentCraftSetRepository(
            DatabaseManager databaseManager
    ) {
        this.databaseManager = databaseManager;
    }

    /**
     * 관리자 명령 등에서 사용하는 기존 메서드.
     * 장식된 투구를 기본으로 선택한다.
     */
    public Map<EquipmentSlot, String> findPreviewSet(
            int setId
    ) throws SQLException {

        return findPreviewSet(setId, null);
    }

    /**
     * NPC 15 GUI에서 선택한 아이템을 반영한 세트 조회.
     *
     * 선택한 아이템이 *_helmet이면 해당 투명한 투구를 사용한다.
     * 나머지 아이템을 선택한 경우에는 장식된 투구가 기본이다.
     */
    public Map<EquipmentSlot, String> findPreviewSet(
            int setId,
            String selectedItemId
    ) throws SQLException {

        if (setId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid equipment set ID: " + setId
            );
        }

        String sql = """
                SELECT
                    item_id,
                    equipment_slot
                FROM rpg_items
                WHERE set_id = ?
                  AND enabled = 1
                  AND equipment_slot IN (
                      'HEAD',
                      'CHEST',
                      'LEGS',
                      'FEET',
                      'OFF_HAND'
                  )
                  AND item_id NOT LIKE
                      'mal!_nyun!_%' ESCAPE '!'
                ORDER BY item_id
                """;

        Map<EquipmentSlot, String> items =
                new EnumMap<>(EquipmentSlot.class);

        String decoratedHelmet = null;
        String transparentHelmet = null;
        String otherHelmet = null;

        try (
                Connection connection =
                        databaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, setId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    String itemId =
                            resultSet.getString("item_id");

                    String dbSlot =
                            resultSet.getString("equipment_slot");

                    EquipmentSlot npcSlot =
                            toNpcSlot(dbSlot);

                    /*
                     * 장식된 투구와 투명한 투구는
                     * 같은 HEAD 슬롯을 사용하는 정상적인 선택지다.
                     */
                    if (npcSlot == EquipmentSlot.HELMET) {

                        if (itemId.endsWith("_hat")) {

                            if (decoratedHelmet != null) {
                                throw duplicateSlot(
                                        setId,
                                        npcSlot,
                                        decoratedHelmet,
                                        itemId
                                );
                            }

                            decoratedHelmet = itemId;

                        } else if (itemId.endsWith("_helmet")) {

                            if (transparentHelmet != null) {
                                throw duplicateSlot(
                                        setId,
                                        npcSlot,
                                        transparentHelmet,
                                        itemId
                                );
                            }

                            transparentHelmet = itemId;

                        } else {

                            if (otherHelmet != null) {
                                throw duplicateSlot(
                                        setId,
                                        npcSlot,
                                        otherHelmet,
                                        itemId
                                );
                            }

                            otherHelmet = itemId;
                        }

                        continue;
                    }

                    String existing =
                            items.putIfAbsent(
                                    npcSlot,
                                    itemId
                            );

                    if (existing != null) {
                        throw duplicateSlot(
                                setId,
                                npcSlot,
                                existing,
                                itemId
                        );
                    }
                }
            }
        }

        String chosenHelmet;

        if (selectedItemId != null
                && selectedItemId.endsWith("_helmet")) {

            /*
             * 요청한 투명한 투구가 실제로 이 세트에 속하는지
             * 조회 결과와 정확히 대조한다.
             */
            if (!selectedItemId.equals(transparentHelmet)) {
                throw new IllegalArgumentException(
                        "Selected transparent helmet "
                                + "does not belong to set_id="
                                + setId
                                + ": "
                                + selectedItemId
                );
            }

            chosenHelmet = transparentHelmet;

        } else {

            /*
             * 일반 장비를 선택했다면 장식된 투구가 우선이다.
             * 장식된 투구가 없는 세트는 기존 투구를 사용한다.
             */
            chosenHelmet =
                    decoratedHelmet != null
                            ? decoratedHelmet
                            : transparentHelmet != null
                                    ? transparentHelmet
                                    : otherHelmet;
        }

        if (chosenHelmet != null) {
            items.put(
                    EquipmentSlot.HELMET,
                    chosenHelmet
            );
        }

        return items;
    }

    private IllegalStateException duplicateSlot(
            int setId,
            EquipmentSlot slot,
            String first,
            String second
    ) {

        return new IllegalStateException(
                "Duplicate equipment slot "
                        + slot
                        + " in set_id="
                        + setId
                        + ": "
                        + first
                        + ", "
                        + second
        );
    }

    private EquipmentSlot toNpcSlot(
            String dbSlot
    ) {

        return switch (dbSlot) {

            case "HEAD" ->
                    EquipmentSlot.HELMET;

            case "CHEST" ->
                    EquipmentSlot.CHESTPLATE;

            case "LEGS" ->
                    EquipmentSlot.LEGGINGS;

            case "FEET" ->
                    EquipmentSlot.BOOTS;

            case "OFF_HAND" ->
                    EquipmentSlot.OFF_HAND;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported preview slot: "
                                    + dbSlot
                    );
        };
    }
}
