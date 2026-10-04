package com.hcs.rpgcore.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;


/**
 * RPGCore 상점 GUI 식별용 InventoryHolder.
 */
public final class ShopInventoryHolder
        implements InventoryHolder {

    private final String shopId;

    private Inventory inventory;


    public ShopInventoryHolder(
            String shopId
    ) {

        if (
                shopId == null
                        || shopId.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "shopId must not be blank"
            );
        }


        this.shopId = shopId;
    }


    public String shopId() {
        return shopId;
    }


    public void setInventory(
            Inventory inventory
    ) {

        this.inventory = inventory;
    }


    @Override
    public @NotNull Inventory getInventory() {

        return inventory;
    }
}
