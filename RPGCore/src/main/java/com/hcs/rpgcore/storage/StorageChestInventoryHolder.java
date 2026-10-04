package com.hcs.rpgcore.storage;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class StorageChestInventoryHolder
        implements InventoryHolder {

    private final UUID instanceUuid;

    private Inventory inventory;

    public StorageChestInventoryHolder(
            UUID instanceUuid
    ) {
        this.instanceUuid = instanceUuid;
    }

    public UUID instanceUuid() {
        return instanceUuid;
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
