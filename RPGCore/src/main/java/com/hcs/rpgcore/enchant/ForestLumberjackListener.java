package com.hcs.rpgcore.enchant;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ForestLumberjackListener implements Listener {

    /*
     * =========================================================
     * ENCHANTMENT
     * =========================================================
     */

    private static final NamespacedKey FOREST_LUMBERJACK_KEY =
            NamespacedKey.fromString(
                    "rpgcore:forest_lumberjack"
            );


    /*
     * =========================================================
     * BLOCK BREAK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreak(
            BlockBreakEvent event
    ) {

        Player player = event.getPlayer();

        GameMode gameMode =
                player.getGameMode();

        if (
                gameMode != GameMode.SURVIVAL
                && gameMode != GameMode.ADVENTURE
        ) {
            return;
        }


        /*
         * =====================================================
         * BLOCK CHECK
         * =====================================================
         */

        Material blockType =
                event.getBlock().getType();

        if (!isWoodBlock(blockType)) {
            return;
        }


        /*
         * =====================================================
         * AXE CHECK
         * =====================================================
         */

        ItemStack tool =
                player.getInventory()
                        .getItemInMainHand();

        if (!isAxe(tool.getType())) {
            return;
        }


        /*
         * =====================================================
         * ENCHANTMENT CHECK
         * =====================================================
         */

        if (FOREST_LUMBERJACK_KEY == null) {
            return;
        }

        Enchantment enchantment =
                Registry.ENCHANTMENT.get(
                        FOREST_LUMBERJACK_KEY
                );

        if (enchantment == null) {
            return;
        }

        ItemMeta meta =
                tool.getItemMeta();

        if (meta == null) {
            return;
        }

        int level =
                meta.getEnchantLevel(
                        enchantment
                );

        if (level <= 0) {
            return;
        }

        /*
         * =====================================================
         * DROP AMOUNT
         * =====================================================
         */

        int amount =
                getDropAmount(
                        level
                );


        /*
         * =====================================================
         * REPLACE VANILLA DROP
         * =====================================================
         */

        event.setDropItems(false);

        event.getBlock()
                .getWorld()
                .dropItemNaturally(
                        event.getBlock()
                                .getLocation(),
                        new ItemStack(
                                blockType,
                                amount
                        )
                );
    }


    /*
     * =========================================================
     * DROP TABLE
     * =========================================================
     */

    private int getDropAmount(
            int level
    ) {

        if (level <= 0) {
            return 1;
        }

        if (level == 1) {
            return 2;
        }

        if (level == 2) {
            return randomBetween(
                    2,
                    3
            );
        }

        if (level < 10) {
            return randomBetween(
                    3,
                    4
            );
        }

        int min =
                2
                + (
                        level
                        / 10
                );

        int max =
                3
                + (
                        level
                        / 8
                );

        return randomBetween(
                min,
                max
        );
    }


    /*
     * =========================================================
     * RANDOM
     * =========================================================
     */

    private int randomBetween(
            int min,
            int max
    ) {

        return ThreadLocalRandom
                .current()
                .nextInt(
                        min,
                        max + 1
                );
    }


    /*
     * =========================================================
     * AXE MATERIAL
     * =========================================================
     */

    private boolean isAxe(
            Material material
    ) {

        return material.name()
                .endsWith(
                        "_AXE"
                );
    }


    /*
     * =========================================================
     * WOOD MATERIAL
     * =========================================================
     */

    private boolean isWoodBlock(
            Material material
    ) {

        String name =
                material.name();

        if (
                name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || (
                        name.endsWith("_STEM")
                        && material != Material.MUSHROOM_STEM
                )
                || name.endsWith("_HYPHAE")
        ) {
            return true;
        }

        return material == Material.BAMBOO_BLOCK
                || material == Material.STRIPPED_BAMBOO_BLOCK;
    }
}
