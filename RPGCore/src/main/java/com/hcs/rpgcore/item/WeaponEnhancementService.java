package com.hcs.rpgcore.item;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.stat.ItemStatKeys;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public final class WeaponEnhancementService {

    public enum Outcome {
        SUCCESS,
        KEEP,
        DOWN
    }


    public record AttemptResult(
            Outcome outcome,
            int oldLevel,
            int newLevel
    ) {
    }


    private static final String ENHANCEMENT_STONE_ID =
            "enhancement_stone";

    private static final String BETTER_ENHANCEMENT_STONE_ID =
            "better_enhancement_stone";


    /*
     * current level 기준.
     *
     * index 0:
     * +0 -> +1
     */
    private static final int[] SUCCESS_CHANCE = {
            100,
            100,
            90,
            80,
            70,
            60,
            45,
            30,
            20,
            10
    };


    private static final int[] KEEP_CHANCE = {
            0,
            0,
            10,
            20,
            30,
            35,
            45,
            50,
            50,
            50
    };


    /*
     * 누적 공격력 증가율.
     */
    private static final double[] ATTACK_BONUS = {
            0.00D,
            0.02D,
            0.04D,
            0.07D,
            0.10D,
            0.14D,
            0.19D,
            0.25D,
            0.32D,
            0.40D,
            0.50D
    };


    private final RPGCorePlugin plugin;

    private final RPGItemRepository itemRepository;

    private final ItemStatKeys itemStatKeys;


    private final NamespacedKey customItemIdKey;

    private final NamespacedKey enhancementLevelKey;

    /*
     * 강화 전 원래 PDC attack 값.
     *
     * 최초 강화 시 저장하며 이후 강화 단계가 오르거나
     * 내려가도 이 값을 기준으로 다시 계산한다.
     */
    private final NamespacedKey enhancementBaseAttackKey;


    private final Map<String, RPGItemDefinition> definitionCache =
            new HashMap<>();


    public WeaponEnhancementService(
            RPGCorePlugin plugin,
            ItemStatKeys itemStatKeys
    ) {

        this.plugin =
                plugin;

        this.itemStatKeys =
                itemStatKeys;

        this.itemRepository =
                new RPGItemRepository(
                        plugin.getDatabaseManager()
                );


        this.customItemIdKey =
                new NamespacedKey(
                        plugin,
                        "custom_item_id"
                );

        this.enhancementLevelKey =
                new NamespacedKey(
                        plugin,
                        "enhancement_level"
                );

        this.enhancementBaseAttackKey =
                new NamespacedKey(
                        plugin,
                        "enhancement_base_attack"
                );
    }


    /*
     * =========================================================
     * ENHANCEMENT STONE
     * =========================================================
     */
    public boolean isEnhancementStone(
            ItemStack item
    ) {

        String itemId =
                getItemId(
                        item
                );


        return ENHANCEMENT_STONE_ID.equals(
                itemId
        );
    }


    public boolean isBetterEnhancementStone(
            ItemStack item
    ) {

        return BETTER_ENHANCEMENT_STONE_ID.equals(
                getItemId(item)
        );
    }


    /*
     * =========================================================
     * WEAPON CHECK
     * =========================================================
     */
    public boolean canEnhance(
            ItemStack item
    ) {

        RPGItemDefinition definition =
                getDefinition(
                        item
                );


        if (definition == null) {
            return false;
        }


        if (!definition.enabled()) {
            return false;
        }


        if (!definition.enhanceable()) {
            return false;
        }


        if (
                definition.enhancementType() == null
                ||
                !definition.enhancementType()
                        .equalsIgnoreCase(
                                "WEAPON"
                        )
        ) {
            return false;
        }


        if (
                definition.weaponAttack() == null
                ||
                definition.weaponAttack() <= 0.0D
        ) {
            return false;
        }


        int maximumLevel =
                Math.min(
                        10,
                        definition.maxEnhancementLevel()
                );


        return maximumLevel > 0
                && getEnhancementLevel(item)
                        < maximumLevel;
    }


    public int getEnhancementLevel(
            ItemStack item
    ) {

        if (
                item == null
                || item.getType().isAir()
        ) {
            return 0;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return 0;
        }


        Integer level =
                meta
                        .getPersistentDataContainer()
                        .get(
                                enhancementLevelKey,
                                PersistentDataType.INTEGER
                        );


        if (level == null) {
            return 0;
        }


        return Math.max(
                0,
                Math.min(
                        10,
                        level
                )
        );
    }


    /*
     * =========================================================
     * CHANCE
     * =========================================================
     */
    public int getSuccessChance(
            int level
    ) {

        if (
                level < 0
                || level >= SUCCESS_CHANCE.length
        ) {
            return 0;
        }


        return SUCCESS_CHANCE[level];
    }


    public int getKeepChance(
            int level
    ) {

        if (
                level < 0
                || level >= KEEP_CHANCE.length
        ) {
            return 0;
        }


        return KEEP_CHANCE[level];
    }


    public int getDownChance(
            int level
    ) {

        if (
                level < 0
                || level >= SUCCESS_CHANCE.length
        ) {
            return 0;
        }


        return 100
                - SUCCESS_CHANCE[level]
                - KEEP_CHANCE[level];
    }


    /*
     * =========================================================
     * ATTEMPT
     * =========================================================
     */
    public AttemptResult attempt(
            ItemStack item
    ) {

        return attempt(item, false);
    }


    public AttemptResult attempt(
            ItemStack item,
            boolean preventLevelDown
    ) {

        if (!canEnhance(item)) {
            return null;
        }


        int oldLevel =
                getEnhancementLevel(
                        item
                );


        int roll =
                ThreadLocalRandom
                        .current()
                        .nextInt(
                                100
                        );


        int successChance =
                getSuccessChance(
                        oldLevel
                );

        int keepChance =
                getKeepChance(
                        oldLevel
                );


        Outcome outcome;

        int newLevel;


        if (
                roll < successChance
        ) {

            outcome =
                    Outcome.SUCCESS;

            newLevel =
                    oldLevel + 1;

        } else if (
                roll
                        < successChance
                        + keepChance
        ) {

            outcome =
                    Outcome.KEEP;

            newLevel =
                    oldLevel;

        } else if (preventLevelDown) {

            outcome =
                    Outcome.KEEP;

            newLevel =
                    oldLevel;

        } else {

            outcome =
                    Outcome.DOWN;

            newLevel =
                    Math.max(
                            0,
                            oldLevel - 1
                    );
        }


        applyEnhancementLevel(
                item,
                newLevel
        );


        return new AttemptResult(
                outcome,
                oldLevel,
                newLevel
        );
    }


    /*
     * =========================================================
     * APPLY LEVEL + ATTACK
     * =========================================================
     */
    private void applyEnhancementLevel(
            ItemStack item,
            int level
    ) {

        RPGItemDefinition definition =
                getDefinition(
                        item
                );


        if (
                definition == null
                || definition.weaponAttack() == null
        ) {
            return;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return;
        }


        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();


        int oldLevel =
                getEnhancementLevel(
                        item
                );


        Double currentAttackPdc =
                pdc.get(
                        itemStatKeys.attackKey(),
                        PersistentDataType.DOUBLE
                );


        if (currentAttackPdc == null) {

            plugin.getLogger().warning(
                    "Enhancement weapon has no attack PDC: "
                            + definition.itemId()
            );

            return;
        }


        Double baseAttackPdc =
                pdc.get(
                        enhancementBaseAttackKey,
                        PersistentDataType.DOUBLE
                );


        /*
         * 기존 강화 아이템인데 base 값이 없는 경우에도
         * 현재 단계의 증가분을 역산해서 복구할 수 있게 한다.
         */
        if (baseAttackPdc == null) {

            baseAttackPdc =
                    currentAttackPdc
                            - (
                                    definition.weaponAttack()
                                    * ATTACK_BONUS[
                                            Math.max(
                                                    0,
                                                    Math.min(
                                                            10,
                                                            oldLevel
                                                    )
                                            )
                                    ]
                            );


            pdc.set(
                    enhancementBaseAttackKey,
                    PersistentDataType.DOUBLE,
                    baseAttackPdc
            );
        }


        double enhancementBonus =
                definition.weaponAttack()
                        * ATTACK_BONUS[level];


        double enhancedAttackPdc =
                baseAttackPdc
                        + enhancementBonus;


        double displayedWeaponAttack =
                definition.weaponAttack()
                        * (
                                1.0D
                                + ATTACK_BONUS[level]
                        );


        pdc.set(
                itemStatKeys.attackKey(),
                PersistentDataType.DOUBLE,
                enhancedAttackPdc
        );


        pdc.set(
                enhancementLevelKey,
                PersistentDataType.INTEGER,
                level
        );


        /*
         * 아이템 이름:
         *
         * +0 : 원래 이름
         * +1 이상 : 원래 이름 +N
         */
        String displayName =
                definition.displayName();


        if (level > 0) {

            displayName +=
                    " +" + level;
        }


        meta.displayName(
                Component.text(
                                displayName
                        )
                        .color(
                                rarityColor(
                                        definition.rarity()
                                )
                        )
                        .decoration(
                                TextDecoration.ITALIC,
                                false
                        )
        );


        updateWeaponAttackLore(
                meta,
                displayedWeaponAttack
        );


        item.setItemMeta(
                meta
        );
    }


    /*
     * =========================================================
     * WEAPON ATTACK LORE
     * =========================================================
     */
    private void updateWeaponAttackLore(
            ItemMeta meta,
            double displayedWeaponAttack
    ) {

        List<Component> lore =
                meta.lore();


        if (
                lore == null
                || lore.isEmpty()
        ) {
            return;
        }


        List<Component> updatedLore =
                new ArrayList<>(
                        lore
                );


        Component replacement =
                Component.text(
                                formatAttack(
                                        displayedWeaponAttack
                                )
                                        + " 공격 피해"
                        )
                        .color(
                                TextColor.color(
                                        0x00AA00
                                )
                        )
                        .decoration(
                                TextDecoration.ITALIC,
                                false
                        );


        for (
                int i = 0;
                i < updatedLore.size();
                i++
        ) {

            Component line =
                    updatedLore.get(i);


            String plain =
                    net.kyori.adventure.text.serializer.plain
                            .PlainTextComponentSerializer
                            .plainText()
                            .serialize(
                                    line
                            );


            if (
                    plain.endsWith(
                            " 공격 피해"
                    )
            ) {

                updatedLore.set(
                        i,
                        replacement
                );

                meta.lore(
                        updatedLore
                );

                return;
            }
        }
    }


    private String formatAttack(
            double value
    ) {

        return Long.toString(
                (long)Math.floor(
                        value
                )
        );
    }


    /*
     * =========================================================
     * ITEM DEFINITION
     * =========================================================
     */
    private RPGItemDefinition getDefinition(
            ItemStack item
    ) {

        String itemId =
                getItemId(
                        item
                );


        if (itemId == null) {
            return null;
        }


        RPGItemDefinition cached =
                definitionCache.get(
                        itemId
                );


        if (cached != null) {
            return cached;
        }


        try {

            RPGItemDefinition definition =
                    itemRepository.findById(
                            itemId
                    );


            if (definition != null) {

                definitionCache.put(
                        itemId,
                        definition
                );
            }


            return definition;

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Failed to load enhancement item definition: "
                            + itemId
            );

            exception.printStackTrace();

            return null;
        }
    }


    private String getItemId(
            ItemStack item
    ) {

        if (
                item == null
                || item.getType().isAir()
        ) {
            return null;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return null;
        }


        return meta
                .getPersistentDataContainer()
                .get(
                        customItemIdKey,
                        PersistentDataType.STRING
                );
    }


    /*
     * =========================================================
     * RARITY COLOR
     * =========================================================
     */
    private TextColor rarityColor(
            String rarity
    ) {

        if (rarity == null) {
            return TextColor.color(
                    0xFFFFFF
            );
        }


        return switch (rarity) {

            case "고급" ->
                    TextColor.color(
                            0x55FF55
                    );

            case "희귀" ->
                    TextColor.color(
                            0x55FFFF
                    );

            case "영웅" ->
                    TextColor.color(
                            0xFF55FF
                    );

            case "전설" ->
                    TextColor.color(
                            0xFFAA00
                    );

            case "신화" ->
                    TextColor.color(
                            0xFDE879
                    );

            default ->
                    TextColor.color(
                            0xFFFFFF
                    );
        };
    }
}
