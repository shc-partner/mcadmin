package com.hcs.rpgcore.stat;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.NamespacedKey;

public final class ItemStatKeys {

    private final NamespacedKey attackKey;

    private final NamespacedKey defenseKey;

    private final NamespacedKey maxManaKey;

    private final NamespacedKey damageReductionFlatKey;


    public ItemStatKeys(
            RPGCorePlugin plugin
    ) {

        this.attackKey =
                new NamespacedKey(
                        plugin,
                        "attack"
                );

        this.defenseKey =
                new NamespacedKey(
                        plugin,
                        "defense"
                );

        this.maxManaKey =
                new NamespacedKey(
                        plugin,
                        "max_mana"
                );

        this.damageReductionFlatKey =
                new NamespacedKey(
                        plugin,
                        "damage_reduction_flat"
                );
    }


    public NamespacedKey attackKey() {

        return attackKey;
    }


    public NamespacedKey defenseKey() {

        return defenseKey;
    }


    public NamespacedKey maxManaKey() {

        return maxManaKey;
    }


    public NamespacedKey damageReductionFlatKey() {

        return damageReductionFlatKey;
    }
}
