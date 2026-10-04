package com.hcs.rpgcore.stat;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public final class ItemStatWriter {

    private final ItemStatKeys keys;


    public ItemStatWriter(
            ItemStatKeys keys
    ) {

        this.keys =
                keys;
    }


    /*
     * =========================================================
     * ATTACK
     * =========================================================
     */
    public boolean setAttack(
            ItemStack item,
            double value
    ) {

        if (!isValidItem(item)) {
            return false;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return false;
        }


        meta.getPersistentDataContainer()
                .set(
                        keys.attackKey(),
                        PersistentDataType.DOUBLE,
                        value
                );


        item.setItemMeta(
                meta
        );


        return true;
    }


    /*
     * =========================================================
     * DEFENSE
     * =========================================================
     */
    public boolean setDefense(
            ItemStack item,
            double value
    ) {

        if (!isValidItem(item)) {
            return false;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return false;
        }


        meta.getPersistentDataContainer()
                .set(
                        keys.defenseKey(),
                        PersistentDataType.DOUBLE,
                        value
                );


        item.setItemMeta(
                meta
        );


        return true;
    }


    /*
     * =========================================================
     * READ
     * =========================================================
     */
    public double getAttack(
            ItemStack item
    ) {

        if (!isValidItem(item)) {
            return 0.0;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return 0.0;
        }


        Double value =
                meta.getPersistentDataContainer()
                        .get(
                                keys.attackKey(),
                                PersistentDataType.DOUBLE
                        );


        return value == null
                ? 0.0
                : value;
    }


    public double getDefense(
            ItemStack item
    ) {

        if (!isValidItem(item)) {
            return 0.0;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return 0.0;
        }


        Double value =
                meta.getPersistentDataContainer()
                        .get(
                                keys.defenseKey(),
                                PersistentDataType.DOUBLE
                        );


        return value == null
                ? 0.0
                : value;
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     *
     * RPGCore attack / defense PDC만 제거한다.
     *
     * 인챈트, 이름, Lore, 내구도 등에는 손대지 않는다.
     */
    public boolean clear(
            ItemStack item
    ) {

        if (!isValidItem(item)) {
            return false;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return false;
        }


        PersistentDataContainer container =
                meta.getPersistentDataContainer();


        container.remove(
                keys.attackKey()
        );


        container.remove(
                keys.defenseKey()
        );


        item.setItemMeta(
                meta
        );


        return true;
    }


    private boolean isValidItem(
            ItemStack item
    ) {

        return item != null
                && !item.getType().isAir();
    }
}
