package com.hcs.rpgcore.storage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class StorageChestListener
        implements Listener {

    private final StorageChestService service;

    public StorageChestListener(
            StorageChestService service
    ) {
        this.service = service;
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPlace(
            PlayerInteractEvent event
    ) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() == null) {
            return;
        }

        ItemStack item =
                event.getItem();

        if (!service.isStorageChestItem(item)) {
            return;
        }

        Block clickedBlock =
                event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        if (event.getBlockFace() != BlockFace.UP) {
            event.setCancelled(true);
            return;
        }

        Block target =
                clickedBlock.getRelative(
                        BlockFace.UP
                );

        Block targetX =
                target.getRelative(
                        BlockFace.EAST
                );

        Block targetZ =
                target.getRelative(
                        BlockFace.SOUTH
                );

        Block targetXZ =
                targetX.getRelative(
                        BlockFace.SOUTH
                );

        if (
                !target.isPassable()
                        || !targetX.isPassable()
                        || !targetZ.isPassable()
                        || !targetXZ.isPassable()
        ) {
            event.setCancelled(true);
            return;
        }

        Location location =
                target
                        .getLocation()
                        .add(
                                1.0,
                                0.0,
                                1.0
                        );

        boolean occupied =
                location
                        .getWorld()
                        .getNearbyEntities(
                                location.clone()
                                        .add(
                                                0.0,
                                                0.5,
                                                0.0
                                        ),
                                0.99,
                                0.8,
                                0.99
                        )
                        .stream()
                        .anyMatch(
                                service::isStorageEntity
                        );

        if (occupied) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        service.place(
                event.getPlayer(),
                item,
                location,
                event.getHand()
        );
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onOpen(
            PlayerInteractEntityEvent event
    ) {

        if (!(
                event.getRightClicked()
                        instanceof Interaction
        )) {
            return;
        }

        if (
                event.getHand()
                        != EquipmentSlot.HAND
        ) {
            return;
        }

        if (!service.isStorageEntity(
                event.getRightClicked()
        )) {
            return;
        }

        UUID instanceUuid =
                service.getEntityInstanceUuid(
                        event.getRightClicked()
                );

        if (instanceUuid == null) {
            return;
        }

        event.setCancelled(true);

        service.open(
                event.getPlayer(),
                instanceUuid
        );
    }

    @EventHandler
    public void onClose(
            InventoryCloseEvent event
    ) {

        if (!(
                event
                        .getInventory()
                        .getHolder()
                        instanceof StorageChestInventoryHolder
        )) {
            return;
        }

        service.save(
                event.getInventory()
        );
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(
                event.getDamager()
                        instanceof Player player
        )) {
            return;
        }

        if (!service.isStorageEntity(
                event.getEntity()
        )) {
            return;
        }

        event.setCancelled(true);

        ItemStack tool =
                player
                        .getInventory()
                        .getItemInMainHand();

        if (!isPickaxe(
                tool.getType()
        )) {
            return;
        }

        service.breakChest(
                player,
                event.getEntity()
        );
    }

    private boolean isPickaxe(
            Material material
    ) {

        return switch (material) {

            case WOODEN_PICKAXE,
                 STONE_PICKAXE,
                 IRON_PICKAXE,
                 GOLDEN_PICKAXE,
                 DIAMOND_PICKAXE,
                 NETHERITE_PICKAXE ->
                    true;

            default ->
                    false;
        };
    }
}
