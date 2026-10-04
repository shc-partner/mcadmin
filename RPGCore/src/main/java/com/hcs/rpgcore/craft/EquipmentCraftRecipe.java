package com.hcs.rpgcore.craft;

import java.util.Map;

/**
 * NPC 15 장비 제작 재료 규칙.
 *
 * 무기:
 *   해당 등급 주괴 2개 + 무기 제작서 1개
 *
 * 방어구 / 방패 / 날개:
 *   해당 등급 주괴 1개 + 방어구 제작서 1개
 *
 * DB 변경 및 실제 재료 차감은 수행하지 않는다.
 */
public final class EquipmentCraftRecipe {

    private static final Map<String, String> INGOTS =
            Map.of(
                    "영웅", "hero_ingot",
                    "전설", "legendary_ingot",
                    "신화", "mythic_ingot"
            );

    private static final Map<String, String> WEAPON_RECIPES =
            Map.of(
                    "영웅", "hero_weapon_recipe",
                    "전설", "legendary_weapon_recipe",
                    "신화", "mythic_weapon_recipe"
            );

    private static final Map<String, String> ARMOR_RECIPES =
            Map.of(
                    "영웅", "hero_armor_recipe",
                    "전설", "legendary_armor_recipe",
                    "신화", "mythic_armor_recipe"
            );

    private final String ingotItemId;
    private final int ingotAmount;

    private final String recipeItemId;
    private final int recipeAmount;

    private EquipmentCraftRecipe(
            String ingotItemId,
            int ingotAmount,
            String recipeItemId,
            int recipeAmount
    ) {

        this.ingotItemId = ingotItemId;
        this.ingotAmount = ingotAmount;

        this.recipeItemId = recipeItemId;
        this.recipeAmount = recipeAmount;
    }

    /**
     * equipmentType:
     * weapon / armor / shield
     *
     * 날개는 DB에서 armor로 분류하므로
     * 방어구 제작 규칙이 적용된다.
     */
    public static EquipmentCraftRecipe of(
            String equipmentType,
            String rarity
    ) {

        if (equipmentType == null || rarity == null) {
            throw new IllegalArgumentException(
                    "Equipment type and rarity are required."
            );
        }

        String ingotId = INGOTS.get(rarity);

        if (ingotId == null) {
            throw new IllegalArgumentException(
                    "Unsupported equipment rarity: " + rarity
            );
        }

        return switch (equipmentType) {

            case "weapon" ->
                    new EquipmentCraftRecipe(
                            ingotId,
                            2,
                            WEAPON_RECIPES.get(rarity),
                            1
                    );

            case "armor", "shield" ->
                    new EquipmentCraftRecipe(
                            ingotId,
                            1,
                            ARMOR_RECIPES.get(rarity),
                            1
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported equipment type: "
                                    + equipmentType
                    );
        };
    }

    public String ingotItemId() {
        return ingotItemId;
    }

    public int ingotAmount() {
        return ingotAmount;
    }

    public String recipeItemId() {
        return recipeItemId;
    }

    public int recipeAmount() {
        return recipeAmount;
    }
}
