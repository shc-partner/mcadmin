package com.hcs.rpgcore.dismantle;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;
import com.hcs.rpgcore.item.RPGItemDefinition;
import com.hcs.rpgcore.item.RPGItemRepository;

import java.sql.SQLException;
import java.util.Locale;

import org.bukkit.inventory.ItemStack;

public final class EquipmentDismantleService {

    public record DismantlePreview(
            String equipmentId,
            String recipeId,
            ItemStack recipe
    ) {
    }

    private final RPGItemRepository itemRepository;
    private final CustomItemFactory itemFactory;


    public EquipmentDismantleService(
            RPGCorePlugin plugin
    ) {

        this.itemRepository =
                new RPGItemRepository(
                        plugin.getDatabaseManager()
                );

        this.itemFactory =
                new CustomItemFactory(plugin);
    }


    /*
     * 장비를 소비하지 않고 분해 결과만 검증한다.
     *
     * 반환값이 null이면 분해할 수 없는 아이템이다.
     */
    public DismantlePreview preview(
            ItemStack equipment
    ) throws SQLException {

        String itemId =
                itemFactory.getItemId(equipment);

        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        /*
         * 신규 장비도 DB에서 직접 조회하므로
         * 장비 ID별 하드코딩이 필요하지 않다.
         */
        RPGItemDefinition definition =
                itemRepository.findById(itemId);

        if (definition == null || !definition.enabled()) {
            return null;
        }

        String type = equipmentType(definition);
        String rarity = rarityPrefix(definition.rarity());

        if (type == null || rarity == null) {
            return null;
        }

        String recipeId =
                rarity + "_" + type + "_recipe";

        /*
         * 제작서를 정상 생성할 수 있는지 먼저 확인한다.
         * 아직 장비를 소비하거나 제작서를 지급하지 않는다.
         */
        ItemStack recipe =
                itemFactory.create(recipeId);

        if (recipe == null
                || recipe.getType().isAir()
                || !recipeId.equals(
                        itemFactory.getItemId(recipe)
                )) {

            return null;
        }

        recipe.setAmount(1);

        return new DismantlePreview(
                itemId,
                recipeId,
                recipe
        );
    }


    private String equipmentType(
            RPGItemDefinition definition
    ) {

        if ("WEAPON".equalsIgnoreCase(
                definition.enhancementType()
        )) {

            return "weapon";
        }

        String slot = definition.equipmentSlot();

        if (slot == null) {
            return null;
        }

        return switch (
                slot.toUpperCase(Locale.ROOT)
        ) {

            case "HEAD", "CHEST", "LEGS", "FEET" ->
                    "armor";

            default ->
                    null;
        };
    }


    private String rarityPrefix(
            String rarity
    ) {

        if (rarity == null) {
            return null;
        }

        return switch (rarity) {

            case "영웅" ->
                    "hero";

            case "전설" ->
                    "legendary";

            case "신화" ->
                    "mythic";

            default ->
                    null;
        };
    }
}
