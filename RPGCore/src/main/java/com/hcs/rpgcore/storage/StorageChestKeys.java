package com.hcs.rpgcore.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class StorageChestKeys {

    private final NamespacedKey customItemIdKey;
    private final NamespacedKey instanceUuidKey;
    private final NamespacedKey storageEntityKey;
    private final NamespacedKey storageTypeKey;

    public StorageChestKeys(Plugin plugin) {

        this.customItemIdKey =
                new NamespacedKey(
                        plugin,
                        "custom_item_id"
                );

        this.instanceUuidKey =
                new NamespacedKey(
                        plugin,
                        "storage_chest_instance_uuid"
                );

        this.storageEntityKey =
                new NamespacedKey(
                        plugin,
                        "storage_chest_entity"
                );

        this.storageTypeKey =
                new NamespacedKey(
                        plugin,
                        "storage_chest_type"
                );
    }

    public NamespacedKey customItemIdKey() {
        return customItemIdKey;
    }

    public NamespacedKey instanceUuidKey() {
        return instanceUuidKey;
    }

    public NamespacedKey storageEntityKey() {
        return storageEntityKey;
    }

    public NamespacedKey storageTypeKey() {
        return storageTypeKey;
    }
}
