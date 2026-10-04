package com.hcs.rpgcore.dungeon.reward;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;
import com.hcs.rpgcore.item.CustomItemIds;

import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.java.JavaPlugin;

public final class DungeonRewardItemFactory {

    public static final String IRON_INGOT =
            "IRON_INGOT";

    public static final String GOLD_INGOT =
            "GOLD_INGOT";

    public static final String EMERALD =
            "EMERALD";

    public static final String EMERALD_BLOCK =
            "EMERALD_BLOCK";

    public static final String LAPIS_LAZULI =
            "LAPIS_LAZULI";

    public static final String DIAMOND =
            "DIAMOND";

    public static final String DIAMOND_BLOCK =
            "DIAMOND_BLOCK";

    public static final String AMETHYST_SHARD =
            "AMETHYST_SHARD";

    public static final String ECHO_SHARD =
            "ECHO_SHARD";

    public static final String CHORUS_FRUIT =
            "CHORUS_FRUIT";


    public static final String ENDER_PEARL =
            "ENDER_PEARL";

    public static final String GRAVE_GUARDIAN_HELMET =
            "GRAVE_GUARDIAN_HELMET";

    public static final String GRAVE_GUARDIAN_CHESTPLATE =
            "GRAVE_GUARDIAN_CHESTPLATE";

    public static final String GRAVE_GUARDIAN_LEGGINGS =
            "GRAVE_GUARDIAN_LEGGINGS";

    public static final String GRAVE_GUARDIAN_BOOTS =
            "GRAVE_GUARDIAN_BOOTS";

    public static final String PALE_STAFF =
            "PALE_STAFF";

    public static final String GREEN_MAGIC_CATALYST =
            "GREEN_MAGIC_CATALYST";

    public static final String BLUE_WAVE_SWORD =
            "BLUE_WAVE_SWORD";


    /*
     * =========================================================
     * NETHER FORTRESS SPECIAL WEAPONS
     * =========================================================
     *
     * 네더 요새 클리어 시 사용하는 공용 특수 무기 보상 ID.
     * 플레이어 직업과 관계없이 동일한 보상 풀을 사용한다.
     */
    public static final String NETHER_FIRE_SPEAR =
            "NETHER_FIRE_SPEAR";

    public static final String NETHER_BLUE_WAVE_SWORD =
            "NETHER_BLUE_WAVE_SWORD";

    public static final String NETHER_FIRE_STAFF =
            "NETHER_FIRE_STAFF";

    public static final String NETHER_THUNDER_CATALYST =
            "NETHER_THUNDER_CATALYST";


    public static final String FACELESS_GOD_HELMET =
            "FACELESS_GOD_HELMET";

    public static final String FACELESS_GOD_CHESTPLATE =
            "FACELESS_GOD_CHESTPLATE";

    public static final String FACELESS_GOD_LEGGINGS =
            "FACELESS_GOD_LEGGINGS";

    public static final String FACELESS_GOD_BOOTS =
            "FACELESS_GOD_BOOTS";

    public static final String FACELESS_GOD_SPEAR =
            "FACELESS_GOD_SPEAR";



    public static final String NETHER_LORD_SWORD =
            "NETHER_LORD_SWORD";

    public static final String NETHER_LORD_HELMET =
            "NETHER_LORD_HELMET";

    public static final String NETHER_LORD_CHESTPLATE =
            "NETHER_LORD_CHESTPLATE";

    public static final String NETHER_LORD_LEGGINGS =
            "NETHER_LORD_LEGGINGS";

    public static final String NETHER_LORD_BOOTS =
            "NETHER_LORD_BOOTS";


    public static final String NETHER_LORD_LEGEND_DEATHSIDE =
            "nether_lord_legend_deathside";

    public static final String NETHER_LORD_LEGEND_HELMET =
            "nether_lord_legend_helmet";

    public static final String NETHER_LORD_LEGEND_CHESTPLATE =
            "nether_lord_legend_chestplate";

    public static final String NETHER_LORD_LEGEND_LEGGINGS =
            "nether_lord_legend_leggings";

    public static final String NETHER_LORD_LEGEND_BOOTS =
            "nether_lord_legend_boots";


    /*
     * =========================================================
     * ENDERMAN RUINS LEGENDARY EQUIPMENT
     * =========================================================
     */

    public static final String ICE_LORD_HELMET =
            "ICE_LORD_HELMET";

    public static final String ICE_LORD_CHESTPLATE =
            "ICE_LORD_CHESTPLATE";

    public static final String ICE_LORD_LEGGINGS =
            "ICE_LORD_LEGGINGS";

    public static final String ICE_LORD_BOOTS =
            "ICE_LORD_BOOTS";

    public static final String ICE_LORD_SPEAR =
            "ICE_LORD_SPEAR";

    public static final String ICE_LORD_GRIMOIRE =
            "ICE_LORD_GRIMOIRE";


    public static final String FIRE_LORD_HELMET =
            "FIRE_LORD_HELMET";

    public static final String FIRE_LORD_CHESTPLATE =
            "FIRE_LORD_CHESTPLATE";

    public static final String FIRE_LORD_LEGGINGS =
            "FIRE_LORD_LEGGINGS";

    public static final String FIRE_LORD_BOOTS =
            "FIRE_LORD_BOOTS";

    public static final String FIRE_LORD_SWORD =
            "FIRE_LORD_SWORD";

    public static final String FIRE_LORD_DEATH_SCYTHE =
            "FIRE_LORD_DEATH_SCYTHE";


    private final NamespacedKey rewardTypeKey;
    private final NamespacedKey ownerUuidKey;

    private final CustomItemFactory customItemFactory;

    public DungeonRewardItemFactory(
            JavaPlugin plugin
    ) {

        rewardTypeKey =
                new NamespacedKey(
                        plugin,
                        "dungeon_reward_type"
                );

        ownerUuidKey =
                new NamespacedKey(
                        plugin,
                        "owner_uuid"
                );

        if (!(plugin instanceof RPGCorePlugin rpgCorePlugin)) {

            throw new IllegalArgumentException(
                    "DungeonRewardItemFactory requires RPGCorePlugin"
            );
        }

        this.customItemFactory =
                new CustomItemFactory(
                        rpgCorePlugin
                );
    }


    /*
     * =========================================================
     * CUSTOM RPG ITEM
     * =========================================================
     *
     * 보관함 내부에 실제 RPGCore 아이템을 채울 때 사용한다.
     */
    public ItemStack createCustomItem(
            String itemId
    ) {

        return customItemFactory.create(
                itemId
        );
    }


    public ItemStack create(
            String rewardType,
            int amount,
            UUID ownerUuid,
            String ownerName
    ) {

        return switch (rewardType) {

            case IRON_INGOT ->
                    new ItemStack(
                            Material.IRON_INGOT,
                            amount
                    );

            case GOLD_INGOT ->
                    new ItemStack(
                            Material.GOLD_INGOT,
                            amount
                    );

            case EMERALD ->
                    new ItemStack(
                            Material.EMERALD,
                            amount
                    );

            case EMERALD_BLOCK ->
                    new ItemStack(
                            Material.EMERALD_BLOCK,
                            amount
                    );

            case LAPIS_LAZULI ->
                    new ItemStack(
                            Material.LAPIS_LAZULI,
                            amount
                    );

            case DIAMOND ->
                    new ItemStack(
                            Material.DIAMOND,
                            amount
                    );

            case DIAMOND_BLOCK ->
                    new ItemStack(
                            Material.DIAMOND_BLOCK,
                            amount
                    );

            case AMETHYST_SHARD ->
                    new ItemStack(
                            Material.AMETHYST_SHARD,
                            amount
                    );

            case ECHO_SHARD ->
                    new ItemStack(
                            Material.ECHO_SHARD,
                            amount
                    );

            case CHORUS_FRUIT ->
                    new ItemStack(
                            Material.CHORUS_FRUIT,
                            amount
                    );


            case ENDER_PEARL ->
                    new ItemStack(
                            Material.ENDER_PEARL,
                            amount
                    );

            /*
             * =================================================
             * ENDERMAN RUINS LEGENDARY EQUIPMENT
             * =================================================
             */

            case ICE_LORD_HELMET ->
                    customItemFactory.create(
                            "ice_lord_helmet"
                    );

            case ICE_LORD_CHESTPLATE ->
                    customItemFactory.create(
                            "ice_lord_chestplate"
                    );

            case ICE_LORD_LEGGINGS ->
                    customItemFactory.create(
                            "ice_lord_leggings"
                    );

            case ICE_LORD_BOOTS ->
                    customItemFactory.create(
                            "ice_lord_boots"
                    );

            case ICE_LORD_SPEAR ->
                    customItemFactory.create(
                            "ice_lord_spear"
                    );

            case ICE_LORD_GRIMOIRE ->
                    customItemFactory.create(
                            "ice_lord_grimoire"
                    );


            case FIRE_LORD_HELMET ->
                    customItemFactory.create(
                            "fire_lord_helmet"
                    );

            case FIRE_LORD_CHESTPLATE ->
                    customItemFactory.create(
                            "fire_lord_chestplate"
                    );

            case FIRE_LORD_LEGGINGS ->
                    customItemFactory.create(
                            "fire_lord_leggings"
                    );

            case FIRE_LORD_BOOTS ->
                    customItemFactory.create(
                            "fire_lord_boots"
                    );

            case FIRE_LORD_SWORD ->
                    customItemFactory.create(
                            "fire_lord_sword"
                    );

            case FIRE_LORD_DEATH_SCYTHE ->
                    customItemFactory.create(
                            "fire_lord_death_scythe"
                    );


            case GRAVE_GUARDIAN_HELMET ->
                    customItemFactory.create(
                            CustomItemIds.UNDEAD_KNIGHT_HELMET
                    );

            case GRAVE_GUARDIAN_CHESTPLATE ->
                    customItemFactory.create(
                            CustomItemIds.UNDEAD_KNIGHT_CHESTPLATE
                    );

            case GRAVE_GUARDIAN_LEGGINGS ->
                    customItemFactory.create(
                            CustomItemIds.UNDEAD_KNIGHT_LEGGINGS
                    );

            case GRAVE_GUARDIAN_BOOTS ->
                    customItemFactory.create(
                            CustomItemIds.UNDEAD_KNIGHT_BOOTS
                    );

            case PALE_STAFF ->
                    customItemFactory.create(
                            "teyvat_lumidouce_elegy"
                    );

            case GREEN_MAGIC_CATALYST ->
                    customItemFactory.create(
                            "teyvat_jadefalls_splendor"
                    );

            case BLUE_WAVE_SWORD ->
                    customItemFactory.create(
                            "teyvat_freedom_sworn"
                    );


            /*
             * =================================================
             * NETHER FORTRESS SPECIAL WEAPONS
             * =================================================
             */
            case NETHER_FIRE_SPEAR ->
                    customItemFactory.create(
                            "teyvat_dragons_bane"
                    );

            case NETHER_BLUE_WAVE_SWORD ->
                    customItemFactory.create(
                            "teyvat_freedom_sworn"
                    );

            case NETHER_FIRE_STAFF ->
                    customItemFactory.create(
                            "teyvat_staff_of_homa"
                    );

            case NETHER_THUNDER_CATALYST ->
                    customItemFactory.create(
                            "teyvat_memory_of_dust"
                    );


            case FACELESS_GOD_HELMET ->
                    customItemFactory.create(
                            "facelessgod_helmet"
                    );

            case FACELESS_GOD_CHESTPLATE ->
                    customItemFactory.create(
                            "facelessgod_chestplate"
                    );

            case FACELESS_GOD_LEGGINGS ->
                    customItemFactory.create(
                            "facelessgod_leggings"
                    );

            case FACELESS_GOD_BOOTS ->
                    customItemFactory.create(
                            "facelessgod_boots"
                    );

            case FACELESS_GOD_SPEAR ->
                    customItemFactory.create(
                            "teyvat_vortex_vanquisher"
                    );



            case NETHER_LORD_SWORD ->
                    customItemFactory.create(
                            CustomItemIds.NETHER_LORD_SWORD
                    );

            case NETHER_LORD_HELMET ->
                    customItemFactory.create(
                            CustomItemIds.NETHER_LORD_HELMET
                    );

            case NETHER_LORD_CHESTPLATE ->
                    customItemFactory.create(
                            CustomItemIds.NETHER_LORD_CHESTPLATE
                    );

            case NETHER_LORD_LEGGINGS ->
                    customItemFactory.create(
                            CustomItemIds.NETHER_LORD_LEGGINGS
                    );

            case NETHER_LORD_BOOTS ->
                    customItemFactory.create(
                            CustomItemIds.NETHER_LORD_BOOTS
                    );


            case NETHER_LORD_LEGEND_DEATHSIDE ->
                    customItemFactory.create(
                            "nether_lord_legend_deathside"
                    );

            case NETHER_LORD_LEGEND_HELMET ->
                    customItemFactory.create(
                            "nether_lord_legend_helmet"
                    );

            case NETHER_LORD_LEGEND_CHESTPLATE ->
                    customItemFactory.create(
                            "nether_lord_legend_chestplate"
                    );

            case NETHER_LORD_LEGEND_LEGGINGS ->
                    customItemFactory.create(
                            "nether_lord_legend_leggings"
                    );

            case NETHER_LORD_LEGEND_BOOTS ->
                    customItemFactory.create(
                            "nether_lord_legend_boots"
                    );


            default ->
                    throw new IllegalArgumentException(
                            "Unknown dungeon reward type: "
                                    + rewardType
                    );
        };
    }


}
