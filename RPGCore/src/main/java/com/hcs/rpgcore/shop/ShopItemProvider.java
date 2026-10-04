package com.hcs.rpgcore.shop;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;

import dev.lone.itemsadder.api.CustomStack;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;


/**
 * 상점 아이템 생성 전담 서비스.
 *
 * 상점 시스템이 개별 아이템 플러그인 API에
 * 직접 의존하지 않도록 생성 경로를 한 곳에 모은다.
 *
 * 지원:
 *
 * VANILLA
 * CRAFTENGINE
 * RPGCORE
 *
 * MYTHICMOBS는 이후 필요할 때 추가한다.
 */
public final class ShopItemProvider {

    private final RPGCorePlugin plugin;

    private final CustomItemFactory
            customItemFactory;


    public ShopItemProvider(
            RPGCorePlugin plugin
    ) {

        this.plugin = plugin;

        this.customItemFactory =
                new CustomItemFactory(
                        plugin
                );

        /*
         * 기존 CustomItemFactory는
         * DB의 RPG 아이템 정의를 읽어야 하므로
         * 생성 시 load한다.
         */
        this.customItemFactory.load();
    }


    /**
     * 상점 정의를 실제 Bukkit ItemStack으로 생성한다.
     *
     * player는 향후 플레이어 컨텍스트가 필요한
     * 커스텀 아이템 생성을 위해 전달받는다.
     */
    public ItemStack create(
            Player player,
            ShopItemDefinition definition
    ) {

        if (definition == null) {

            throw new IllegalArgumentException(
                    "definition must not be null"
            );
        }


        ItemStack item =
                switch (
                        definition.source()
                ) {

                    case VANILLA ->
                            createVanilla(
                                    definition.itemId()
                            );

                    case CRAFTENGINE ->
                            createCraftEngine(
                                    player,
                                    definition.itemId()
                            );

                    case ITEMSADDER ->
                            createItemsAdder(
                                    definition.itemId()
                            );

                    case RPGCORE ->
                            createRpgCore(
                                    definition.itemId()
                            );

                    case ENCHANT_BOOK ->
                            createEnchantBook(
                                    definition.itemId()
                            );

                    case MYTHICMOBS ->
                            throw new UnsupportedOperationException(
                                    "MYTHICMOBS shop item source is not implemented yet: "
                                            + definition.itemId()
                            );
                };


        if (item == null) {

            throw new IllegalStateException(
                    "Shop item creation returned null: "
                            + definition.entryId()
            );
        }


        item.setAmount(
                definition.amount()
        );


        return item;
    }


    /**
     * =========================================================
     * VANILLA
     * =========================================================
     *
     * 지원 예:
     *
     * minecraft:diamond
     * DIAMOND
     */
    private ItemStack createVanilla(
            String itemId
    ) {

        String materialName =
                itemId;


        int separator =
                materialName.indexOf(
                        ':'
                );


        if (separator >= 0) {

            String namespace =
                    materialName.substring(
                            0,
                            separator
                    );

            if (!namespace.equalsIgnoreCase(
                    "minecraft"
            )) {

                throw new IllegalArgumentException(
                        "Unsupported vanilla namespace: "
                                + itemId
                );
            }


            materialName =
                    materialName.substring(
                            separator + 1
                    );
        }


        Material material =
                Material.matchMaterial(
                        materialName.toUpperCase(
                                Locale.ROOT
                        )
                );


        if (
                material == null
                        || material.isAir()
        ) {

            throw new IllegalArgumentException(
                    "Unknown vanilla item: "
                            + itemId
            );
        }


        return new ItemStack(
                material
        );
    }


    /**
     * =========================================================
     * CRAFTENGINE
     * =========================================================
     *
     * CraftEngine 26.9.1 공식 Bukkit API 사용.
     *
     * 예:
     *
     * customcrops:tomato_seeds
     * customcrops:watering_can_1
     */
    private ItemStack createCraftEngine(
            Player player,
            String itemId
    ) {

        BukkitItemDefinition definition =
                CraftEngineItems.byId(
                        itemId
                );


        if (definition == null) {

            throw new IllegalArgumentException(
                    "Unknown CraftEngine item: "
                            + itemId
            );
        }


        ItemStack item;


        /*
         * 플레이어 컨텍스트가 있는 경우
         * player-aware build 경로를 우선 사용한다.
         *
         * 현재 CustomCrops 아이템은
         * 기본 buildBukkitItem()으로도 생성 가능하지만,
         * 향후 동적 프로세서가 붙은 아이템을 고려한다.
         */
        if (player != null) {

            item =
                    definition.buildBukkitItem(
                            player
                    );

        } else {

            item =
                    definition.buildBukkitItem();
        }


        if (item == null) {

            throw new IllegalStateException(
                    "CraftEngine item build failed: "
                            + itemId
            );
        }


        return item;
    }


    /**
     * =========================================================
     * ITEMSADDER
     * =========================================================
     *
     * ItemsAdder 4.0.18 공식 API 사용.
     *
     * 예:
     *
     * nieyels:armchair
     */
    private ItemStack createItemsAdder(
            String itemId
    ) {

        CustomStack customStack =
                CustomStack.getInstance(
                        itemId
                );


        if (customStack == null) {

            throw new IllegalArgumentException(
                    "Unknown ItemsAdder item: "
                            + itemId
            );
        }


        ItemStack item =
                customStack.getItemStack();


        if (
                item == null
                        || item.getType().isAir()
        ) {

            throw new IllegalStateException(
                    "ItemsAdder item build failed: "
                            + itemId
            );
        }


        return item.clone();
    }


    /**
     * =========================================================
     * RPGCORE
     * =========================================================
     */
    private ItemStack createRpgCore(
            String itemId
    ) {

        ItemStack item =
                customItemFactory.create(
                        itemId
                );


        if (item == null) {

            throw new IllegalArgumentException(
                    "Unknown RPGCore item: "
                            + itemId
            );
        }


        return item;
    }


    /**
     * CraftEngine 아이템인지 검사.
     *
     * 이후 판매 상점에서 정확한 커스텀 아이템 판별에 사용한다.
     */
    public boolean isCraftEngineItem(
            ItemStack item
    ) {

        if (
                item == null
                        || item.getType().isAir()
        ) {
            return false;
        }


        return CraftEngineItems.isCustomItem(
                item
        );
    }


    /**
     * CraftEngine custom item ID 조회.
     *
     * 예:
     * customcrops:tomato
     */
    public String getCraftEngineItemId(
            ItemStack item
    ) {

        if (!isCraftEngineItem(item)) {
            return null;
        }


        var key =
                CraftEngineItems.getCustomItemId(
                        item
                );


        if (key == null) {
            return null;
        }


        return key.asString();
    }

    /**
     * 플레이어 인벤토리의 실제 아이템이
     * 상점 정의와 동일한 품목인지 검사한다.
     *
     * CraftEngine 아이템은 display name이나 Material이 아니라
     * custom item ID 자체로 판별한다.
     */
    public boolean matches(
            Player player,
            ItemStack item,
            ShopItemDefinition definition
    ) {

        if (
                item == null
                        || item.getType().isAir()
                        || definition == null
        ) {
            return false;
        }


        if (
                definition.source()
                        == ShopItemSource.CRAFTENGINE
        ) {

            String customId =
                    getCraftEngineItemId(
                            item
                    );


            return customId != null
                    && customId.equals(
                            definition.itemId()
                    );
        }


        if (
                definition.source()
                        == ShopItemSource.MYTHICMOBS
        ) {
            return false;
        }


        try {

            ItemStack target =
                    create(
                            player,
                            definition
                    );


            target.setAmount(
                    item.getAmount()
            );


            return item.isSimilar(
                    target
            );

        } catch (Exception exception) {

            return false;
        }
    }




    /**
     * 실제 인챈트가 저장된 인챈트북 생성.
     *
     * itemId 형식:
     * minecraft:sharpness@5
     * rpgcore:forest_lumberjack@1
     * rpgcore:forest_lumberjack@2
     * rpgcore:forest_lumberjack@3
     */
    private ItemStack createEnchantBook(
            String itemId
    ) {

        int separator =
                itemId.lastIndexOf('@');

        if (
                separator <= 0
                        || separator == itemId.length() - 1
        ) {
            throw new IllegalArgumentException(
                    "Invalid enchant book ID: " + itemId
            );
        }

        String enchantmentId =
                itemId.substring(
                        0,
                        separator
                );

        int level;

        try {
            level = Integer.parseInt(
                    itemId.substring(separator + 1)
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid enchantment level: " + itemId,
                    exception
            );
        }

        NamespacedKey key =
                NamespacedKey.fromString(
                        enchantmentId
                );

        if (key == null) {
            throw new IllegalArgumentException(
                    "Invalid enchantment key: " + enchantmentId
            );
        }

        Enchantment enchantment =
                Registry.ENCHANTMENT.get(key);

        if (enchantment == null) {
            throw new IllegalArgumentException(
                    "Enchantment is not registered: "
                            + enchantmentId
            );
        }

        if (
                level < 1
                        || level > enchantment.getMaxLevel()
        ) {
            throw new IllegalArgumentException(
                    "Unsupported enchantment level: " + itemId
            );
        }

        ItemStack book =
                new ItemStack(
                        Material.ENCHANTED_BOOK
                );

        if (!(
                book.getItemMeta()
                        instanceof EnchantmentStorageMeta meta
        )) {
            throw new IllegalStateException(
                    "Enchanted book metadata is unavailable."
            );
        }

        meta.addStoredEnchant(
                enchantment,
                level,
                false
        );

        // RPGCore 커스텀 인챈트북 전용 아이콘.
        // 바닐라 인챈트북의 모델은 변경하지 않는다.
        String bookModelId =
                switch (enchantmentId) {

                    case "rpgcore:forest_lumberjack" ->
                            "rpgcore:forest_lumberjack_book";

                    case "rpgcore:logging_wave" ->
                            "rpgcore:logging_wave_book";

                    default -> null;
                };

        if (bookModelId != null) {

            NamespacedKey modelKey =
                    NamespacedKey.fromString(bookModelId);

            if (modelKey == null) {
                throw new IllegalStateException(
                        "Invalid enchant book model: "
                                + bookModelId
                );
            }

            meta.setItemModel(modelKey);
        }

        book.setItemMeta(meta);

        return book;
    }
}
