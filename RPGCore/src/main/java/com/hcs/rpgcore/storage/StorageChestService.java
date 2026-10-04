package com.hcs.rpgcore.storage;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StorageChestService {

    private static final Set<String>
            STORAGE_CHEST_ITEM_IDS =
            Set.of(
                    "ancient_dragon_chest",
                    "dragon_lord_chest",
                    "sky_guardian_chest"
            );

    private final Plugin plugin;

    private final StorageChestRepository repository;
    private final StorageChestKeys keys;

    private final Map<UUID, Inventory>
            openInventories =
            new HashMap<>();

    public StorageChestService(
            Plugin plugin,
            StorageChestRepository repository
    ) {

        this.plugin = plugin;
        this.repository = repository;
        this.keys =
                new StorageChestKeys(plugin);
    }

    public StorageChestKeys keys() {
        return keys;
    }

    public boolean isStorageChestItem(
            ItemStack item
    ) {

        if (
                item == null
                        || item.getType().isAir()
        ) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        String customItemId =
                meta
                        .getPersistentDataContainer()
                        .get(
                                keys.customItemIdKey(),
                                PersistentDataType.STRING
                        );

        return customItemId != null
                && STORAGE_CHEST_ITEM_IDS.contains(
                        customItemId
                );
    }

    public String getStorageChestType(
            ItemStack item
    ) {

        if (item == null) {
            return null;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return null;
        }

        String customItemId =
                meta
                        .getPersistentDataContainer()
                        .get(
                                keys.customItemIdKey(),
                                PersistentDataType.STRING
                        );

        if (
                customItemId == null
                        || !STORAGE_CHEST_ITEM_IDS
                        .contains(customItemId)
        ) {
            return null;
        }

        return customItemId;
    }


    public UUID getInstanceUuid(
            ItemStack item
    ) {

        if (item == null) {
            return null;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return null;
        }

        String value =
                meta
                        .getPersistentDataContainer()
                        .get(
                                keys.instanceUuidKey(),
                                PersistentDataType.STRING
                        );

        if (value == null) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (
                IllegalArgumentException exception
        ) {
            return null;
        }
    }

    private void setInstanceUuid(
            ItemStack item,
            UUID instanceUuid
    ) {

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta
                .getPersistentDataContainer()
                .set(
                        keys.instanceUuidKey(),
                        PersistentDataType.STRING,
                        instanceUuid.toString()
                );

        item.setItemMeta(meta);
    }


    /*
     * =========================================================
     * PRELOADED STORAGE CHEST
     * =========================================================
     *
     * 던전 보상처럼 설치 전에 이미 내용물이 들어 있어야 하는
     * 보관함을 생성한다.
     */
    public ItemStack createPreloadedChest(
            ItemStack chestItem,
            UUID ownerUuid,
            ItemStack[] contents
    ) {

        if (!isStorageChestItem(chestItem)) {

            throw new IllegalArgumentException(
                    "보관함 아이템이 아닙니다."
            );
        }


        String chestType =
                getStorageChestType(
                        chestItem
                );

        if (chestType == null) {

            throw new IllegalArgumentException(
                    "보관함 종류를 확인할 수 없습니다."
            );
        }


        if (
                contents == null
                        || contents.length
                        > StorageChestRepository
                        .INVENTORY_SIZE
        ) {

            throw new IllegalArgumentException(
                    "잘못된 보관함 내용물입니다."
            );
        }


        ItemStack result =
                chestItem.clone();

        result.setAmount(1);


        UUID instanceUuid =
                UUID.randomUUID();


        repository.create(
                instanceUuid,
                chestType,
                ownerUuid
        );


        repository.saveInitialContents(
                instanceUuid,
                contents
        );


        setInstanceUuid(
                result,
                instanceUuid
        );


        return result;
    }


    public boolean place(
            Player player,
            ItemStack handItem,
            Location location,
            EquipmentSlot hand
    ) {

        if (!isStorageChestItem(handItem)) {
            return false;
        }

        String chestType =
                getStorageChestType(
                        handItem
                );

        if (chestType == null) {
            return false;
        }

        UUID instanceUuid =
                getInstanceUuid(handItem);

        boolean newInstance =
                instanceUuid == null;

        if (newInstance) {

            instanceUuid =
                    UUID.randomUUID();

            repository.create(
                    instanceUuid,
                    chestType,
                    player.getUniqueId()
            );

        } else if (
                repository.isPlaced(
                        instanceUuid
                )
        ) {

            player.sendMessage(
                    Component.text(
                            "이미 설치된 보관함입니다."
                    )
            );

            return false;
        }

        float yaw =
                snapYaw(
                        player.getLocation()
                                .getYaw()
                );

        try {

            repository.markPlaced(
                    instanceUuid,
                    location,
                    yaw
            );

        } catch (
                RuntimeException exception
        ) {

            plugin
                    .getLogger()
                    .warning(
                            exception.getMessage()
                    );

            player.sendMessage(
                    Component.text(
                            "보관함을 설치할 수 없습니다."
                    )
            );

            return false;
        }

        /*
         * 설치 당시 아이템 한 개를 복사한다.
         *
         * 이 ItemStack은 ItemDisplay 내부에도 보관되므로
         * 나중에 도끼로 회수할 때 같은 아이템을 그대로
         * 플레이어에게 돌려줄 수 있다.
         */
        ItemStack displayItem =
                handItem.clone();

        displayItem.setAmount(1);

        setInstanceUuid(
                displayItem,
                instanceUuid
        );

        spawnEntities(
                instanceUuid,
                chestType,
                location,
                yaw,
                displayItem
        );

        consumeOne(
                player,
                hand
        );

        return true;
    }

    private void spawnEntities(
            UUID instanceUuid,
            String chestType,
            Location location,
            float yaw,
            ItemStack displayItem
    ) {

        Location displayLocation =
                location.clone();

        displayLocation.setYaw(yaw);
        displayLocation.setPitch(0.0F);

        ItemDisplay itemDisplay =
                location.getWorld()
                        .spawn(
                                displayLocation,
                                ItemDisplay.class,
                                entity -> {

                                    entity.setPersistent(
                                            true
                                    );

                                    entity.setInvulnerable(
                                            true
                                    );

                                    entity.setGravity(
                                            false
                                    );

                                    entity.setBillboard(
                                            Display.Billboard.FIXED
                                    );

                                    entity.setItemDisplayTransform(
                                            ItemDisplay
                                                    .ItemDisplayTransform
                                                    .FIXED
                                    );

                                    entity.setItemStack(
                                            displayItem
                                    );

                                    /*
                                     * Divine Zephyr chest 모델은
                                     * ItemDisplay FIXED 기준으로
                                     * 원본 크기가 매우 크게 보이므로
                                     * 설치된 보관함에만 축소 스케일을 적용한다.
                                     */
                                    Transformation transformation =
                                            entity.getTransformation();

                                    transformation
                                            .getScale()
                                            .set(
                                                    0.30F,
                                                    0.30F,
                                                    0.30F
                                            );

                                    entity.setTransformation(
                                            transformation
                                    );

                                    PersistentDataContainer
                                            pdc =
                                            entity
                                                    .getPersistentDataContainer();

                                    pdc.set(
                                            keys.instanceUuidKey(),
                                            PersistentDataType.STRING,
                                            instanceUuid.toString()
                                    );

                                    pdc.set(
                                            keys.storageEntityKey(),
                                            PersistentDataType.STRING,
                                            "display"
                                    );

                                    pdc.set(
                                            keys.storageTypeKey(),
                                            PersistentDataType.STRING,
                                            chestType
                                    );
                                }
                        );

        /*
         * Interaction은 클릭 판정 전용이다.
         *
         * ItemDisplay보다 약간 위에 배치하여
         * 실제 보관함 몸체를 클릭하는 느낌으로 만든다.
         */
        Location interactionLocation =
                location.clone()
                        .add(
                                0.0,
                                0.55,
                                0.0
                        );

        interactionLocation.setYaw(yaw);

        location.getWorld()
                .spawn(
                        interactionLocation,
                        Interaction.class,
                        entity -> {

                            entity.setPersistent(
                                    true
                            );

                            entity.setInteractionWidth(
                                    1.35F
                            );

                            entity.setInteractionHeight(
                                    1.10F
                            );

                            entity.setResponsive(
                                    true
                            );

                            PersistentDataContainer
                                    pdc =
                                    entity
                                            .getPersistentDataContainer();

                            pdc.set(
                                    keys.instanceUuidKey(),
                                    PersistentDataType.STRING,
                                    instanceUuid.toString()
                            );

                            pdc.set(
                                    keys.storageEntityKey(),
                                    PersistentDataType.STRING,
                                    "interaction"
                            );

                            pdc.set(
                                    keys.storageTypeKey(),
                                    PersistentDataType.STRING,
                                    chestType
                            );
                        }
                );
    }

    public boolean isStorageEntity(
            org.bukkit.entity.Entity entity
    ) {

        return entity
                .getPersistentDataContainer()
                .has(
                        keys.instanceUuidKey(),
                        PersistentDataType.STRING
                );
    }

    public UUID getEntityInstanceUuid(
            org.bukkit.entity.Entity entity
    ) {

        String value =
                entity
                        .getPersistentDataContainer()
                        .get(
                                keys.instanceUuidKey(),
                                PersistentDataType.STRING
                        );

        if (value == null) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (
                IllegalArgumentException exception
        ) {
            return null;
        }
    }

    public void open(
            Player player,
            UUID instanceUuid
    ) {

        Inventory inventory =
                openInventories.get(
                        instanceUuid
                );

        if (inventory == null) {

            StorageChestInventoryHolder
                    holder =
                    new StorageChestInventoryHolder(
                            instanceUuid
                    );

            String chestType =
                    repository.getChestType(
                            instanceUuid
                    );

            String title =
                    switch (
                            chestType != null
                                    ? chestType
                                    : ""
                    ) {

                        case "dragon_lord_chest" ->
                                "마룡 군주의 보관함";

                        case "sky_guardian_chest" ->
                                "천공 수호자의 보관함";

                        default ->
                                "고대 용기사의 보관함";
                    };

            inventory =
                    Bukkit.createInventory(
                            holder,
                            StorageChestRepository
                                    .INVENTORY_SIZE,
                            Component.text(
                                    title
                            )
                    );

            holder.setInventory(
                    inventory
            );

            ItemStack[] contents =
                    repository.loadContents(
                            instanceUuid
                    );

            inventory.setContents(
                    contents
            );

            openInventories.put(
                    instanceUuid,
                    inventory
            );
        }

        player.openInventory(
                inventory
        );
    }

    public void save(
            Inventory inventory
    ) {

        if (!(
                inventory.getHolder()
                        instanceof
                        StorageChestInventoryHolder holder
        )) {
            return;
        }

        repository.saveContents(
                holder.instanceUuid(),
                inventory.getContents()
        );
    }

    public boolean breakChest(
            Player player,
            org.bukkit.entity.Entity hitEntity
    ) {

        UUID instanceUuid =
                getEntityInstanceUuid(
                        hitEntity
                );

        if (instanceUuid == null) {
            return false;
        }

        ItemDisplay display =
                findDisplay(
                        hitEntity.getLocation(),
                        instanceUuid
                );

        if (display == null) {

            player.sendMessage(
                    Component.text(
                            "보관함 표시 엔티티를 찾을 수 없습니다."
                    )
            );

            return false;
        }

        Inventory inventory =
                openInventories.get(
                        instanceUuid
                );

        if (inventory != null) {

            /*
             * 먼저 현재 내용물을 저장한다.
             */
            save(inventory);

            /*
             * 다른 플레이어가 열고 있더라도
             * 회수 전에 모두 닫는다.
             */
            for (
                    org.bukkit.entity.HumanEntity
                            viewer :
                    java.util.List.copyOf(
                            inventory.getViewers()
                    )
            ) {

                viewer.closeInventory();
            }

            openInventories.remove(
                    instanceUuid
            );
        }

        ItemStack returnItem =
                display.getItemStack()
                        .clone();

        returnItem.setAmount(1);

        /*
         * 동일 UUID가 ItemStack에 반드시 남도록 보장.
         */
        setInstanceUuid(
                returnItem,
                instanceUuid
        );

        removeChestEntities(
                hitEntity.getLocation(),
                instanceUuid
        );

        repository.markUnplaced(
                instanceUuid
        );

        Map<Integer, ItemStack> overflow =
                player
                        .getInventory()
                        .addItem(
                                returnItem
                        );

        for (
                ItemStack overflowItem :
                overflow.values()
        ) {

            player
                    .getWorld()
                    .dropItemNaturally(
                            player.getLocation(),
                            overflowItem
                    );
        }

        return true;
    }

    private ItemDisplay findDisplay(
            Location center,
            UUID instanceUuid
    ) {

        for (
                org.bukkit.entity.Entity entity :
                center
                        .getWorld()
                        .getNearbyEntities(
                                center,
                                2.0,
                                2.0,
                                2.0
                        )
        ) {

            if (!(
                    entity instanceof
                            ItemDisplay display
            )) {
                continue;
            }

            UUID candidate =
                    getEntityInstanceUuid(
                            display
                    );

            if (
                    instanceUuid.equals(
                            candidate
                    )
            ) {
                return display;
            }
        }

        return null;
    }

    private void removeChestEntities(
            Location center,
            UUID instanceUuid
    ) {

        for (
                org.bukkit.entity.Entity entity :
                center
                        .getWorld()
                        .getNearbyEntities(
                                center,
                                2.0,
                                2.0,
                                2.0
                        )
        ) {

            UUID candidate =
                    getEntityInstanceUuid(
                            entity
                    );

            if (
                    instanceUuid.equals(
                            candidate
                    )
            ) {
                entity.remove();
            }
        }
    }

    private void consumeOne(
            Player player,
            EquipmentSlot hand
    ) {

        ItemStack item;

        if (hand == EquipmentSlot.OFF_HAND) {

            item =
                    player
                            .getInventory()
                            .getItemInOffHand();

        } else {

            item =
                    player
                            .getInventory()
                            .getItemInMainHand();
        }

        if (item.getAmount() <= 1) {

            if (hand == EquipmentSlot.OFF_HAND) {

                player
                        .getInventory()
                        .setItemInOffHand(
                                new ItemStack(
                                        Material.AIR
                                )
                        );

            } else {

                player
                        .getInventory()
                        .setItemInMainHand(
                                new ItemStack(
                                        Material.AIR
                                )
                        );
            }

            return;
        }

        item.setAmount(
                item.getAmount() - 1
        );
    }

    private float snapYaw(
            float yaw
    ) {

        return Math.round(
                yaw / 90.0F
        ) * 90.0F;
    }
}
