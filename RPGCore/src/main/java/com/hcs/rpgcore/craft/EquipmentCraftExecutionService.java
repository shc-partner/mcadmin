package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * NPC 15 장비 제작 실행 서비스.
 *
 * 인벤토리 복사본에서 재료 차감과 완성품 지급을 먼저 검증한다.
 * 모든 조건을 만족한 경우에만 플레이어 인벤토리에 적용한다.
 *
 * 반드시 서버 메인 스레드에서 실행한다.
 */
public final class EquipmentCraftExecutionService {

    public enum Result {
        SUCCESS,
        NOT_ENOUGH_INGOTS,
        NOT_ENOUGH_RECIPE,
        INVENTORY_FULL,
        INVALID_ITEM,
        FAILED
    }

    private final CustomItemFactory itemFactory;
    private final NamespacedKey customItemIdKey;

    public EquipmentCraftExecutionService(
            RPGCorePlugin plugin
    ) {

        this.itemFactory = new CustomItemFactory(plugin);

        this.customItemIdKey = new NamespacedKey(
                plugin,
                "custom_item_id"
        );
    }

    public Result craft(
            Player player,
            String equipmentType,
            String rarity,
            String resultItemId
    ) {

        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Equipment crafting must run on the server main thread."
            );
        }

        if (player == null
                || equipmentType == null
                || rarity == null
                || resultItemId == null
                || resultItemId.isBlank()
                || resultItemId.startsWith("mal_nyun_")) {

            return Result.INVALID_ITEM;
        }

        final EquipmentCraftRecipe recipe;

        try {

            recipe = EquipmentCraftRecipe.of(
                    equipmentType,
                    rarity
            );

        } catch (IllegalArgumentException exception) {

            return Result.INVALID_ITEM;
        }

        /*
         * 완성품을 먼저 생성한다.
         * 생성 실패 시 재료를 차감하지 않는다.
         */
        ItemStack result;

        try {

            result = itemFactory.create(resultItemId);

        } catch (RuntimeException exception) {

            return Result.FAILED;
        }

        if (result == null
                || result.getType().isAir()
                || !resultItemId.equals(getCustomItemId(result))) {

            return Result.INVALID_ITEM;
        }

        result.setAmount(1);

        /*
         * 실제 인벤토리는 건드리지 않는다.
         * 일반 보관 슬롯 36칸을 복사한다.
         */
        ItemStack[] original =
                player.getInventory().getStorageContents();

        ItemStack[] working =
                new ItemStack[original.length];

        for (int slot = 0; slot < original.length; slot++) {

            working[slot] =
                    original[slot] == null
                            ? null
                            : original[slot].clone();
        }

        int ingots = count(
                working,
                recipe.ingotItemId()
        );

        if (ingots < recipe.ingotAmount()) {
            return Result.NOT_ENOUGH_INGOTS;
        }

        int recipeCount = count(
                working,
                recipe.recipeItemId()
        );

        if (recipeCount < recipe.recipeAmount()) {
            return Result.NOT_ENOUGH_RECIPE;
        }

        /*
         * 복사본에서만 재료를 차감한다.
         */
        if (!consume(
                working,
                recipe.ingotItemId(),
                recipe.ingotAmount()
        )) {
            return Result.FAILED;
        }

        if (!consume(
                working,
                recipe.recipeItemId(),
                recipe.recipeAmount()
        )) {
            return Result.FAILED;
        }

        /*
         * 재료 차감 이후의 인벤토리 공간에
         * 완성품을 지급할 수 있는지 확인한다.
         */
        Inventory simulated = Bukkit.createInventory(
                null,
                36
        );

        if (working.length != 36) {
            return Result.FAILED;
        }

        simulated.setContents(working);

        Map<Integer, ItemStack> leftovers =
                simulated.addItem(result.clone());

        if (!leftovers.isEmpty()) {
            return Result.INVENTORY_FULL;
        }

        /*
         * 모든 검증을 통과했으므로
         * 최종 인벤토리를 한 번에 적용한다.
         */
        ItemStack[] completed =
                simulated.getContents();

        player.getInventory().setStorageContents(
                completed
        );

        return Result.SUCCESS;
    }

    private int count(
            ItemStack[] contents,
            String itemId
    ) {

        int total = 0;

        for (ItemStack item : contents) {

            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (itemId.equals(getCustomItemId(item))) {
                total += item.getAmount();
            }
        }

        return total;
    }

    private boolean consume(
            ItemStack[] contents,
            String itemId,
            int amount
    ) {

        int remaining = amount;

        for (int slot = 0; slot < contents.length; slot++) {

            ItemStack item = contents[slot];

            if (item == null
                    || item.getType().isAir()
                    || !itemId.equals(getCustomItemId(item))) {
                continue;
            }

            int consumed = Math.min(
                    item.getAmount(),
                    remaining
            );

            int newAmount =
                    item.getAmount() - consumed;

            if (newAmount == 0) {

                contents[slot] = null;

            } else {

                item.setAmount(newAmount);
            }

            remaining -= consumed;

            if (remaining == 0) {
                return true;
            }
        }

        return false;
    }

    private String getCustomItemId(
            ItemStack item
    ) {

        if (item == null || item.getType().isAir()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(
                customItemIdKey,
                PersistentDataType.STRING
        );
    }
}
