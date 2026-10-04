package com.hcs.rpgcore.craft;

import net.citizensnpcs.api.event.NPCRightClickEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * NPC 18번 장비 전체 초기화 GUI.
 *
 * NPC 우클릭 시 GUI만 열며,
 * 초기화 버튼 클릭 시에만 장비를 전부 해제한다.
 */
public final class EquipmentPreviewResetListener
        implements Listener {

    private static final int NPC_ID = 18;

    private static final int RESET_SLOT = 11;
    private static final int EXIT_SLOT = 15;

    private final EquipmentCraftPreviewService previewService;

    public EquipmentPreviewResetListener(
            EquipmentCraftPreviewService previewService
    ) {
        this.previewService = previewService;
    }

    private static final class ResetHolder
            implements InventoryHolder {

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    @EventHandler
    public void onNpcRightClick(
            NPCRightClickEvent event
    ) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        openResetMenu(event.getClicker());
    }

    private void openResetMenu(Player player) {

        ResetHolder holder = new ResetHolder();

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text("미리보기 NPC - 장비 초기화")
        );

        holder.setInventory(inventory);

        inventory.setItem(
                RESET_SLOT,
                createButton(
                        Material.REDSTONE_BLOCK,
                        "장비 전체 초기화",
                        NamedTextColor.RED,
                        "미리보기 NPC의 착용 장비를 모두 해제합니다.",
                        null
                )
        );

        inventory.setItem(
                EXIT_SLOT,
                createButton(
                        Material.ARROW,
                        "닫기",
                        NamedTextColor.YELLOW,
                        "장비를 변경하지 않고 창을 닫습니다.",
                        1007
                )
        );

        player.openInventory(inventory);
    }

    private ItemStack createButton(
            Material material,
            String name,
            NamedTextColor color,
            String description,
            Integer customModelData
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        meta.displayName(
                Component.text(name, color)
        );

        meta.lore(
                List.of(
                        Component.text(
                                description,
                                NamedTextColor.GRAY
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof ResetHolder)) {
            return;
        }

        // GUI 버튼을 가져가거나 교체하지 못하게 한다.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0
                || slot >= event.getView()
                        .getTopInventory()
                        .getSize()) {
            return;
        }

        switch (slot) {

            case RESET_SLOT -> {

                if (!previewService.resetEquipment()) {
                    player.sendMessage(
                            "미리보기 NPC를 찾을 수 없어 초기화하지 못했습니다."
                    );
                    return;
                }

                player.closeInventory();

                player.sendMessage(
                        "미리보기 NPC의 장비를 모두 해제했습니다."
                );
            }

            case EXIT_SLOT ->
                    player.closeInventory();

            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof ResetHolder)) {
            return;
        }

        int topSize = event.getView()
                .getTopInventory()
                .getSize();

        for (int slot : event.getRawSlots()) {

            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
