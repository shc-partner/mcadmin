package com.hcs.rpgcore.dismantle;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class EquipmentDismantleGui {

    public static final int INPUT_SLOT = 3;
    public static final int DISMANTLE_SLOT = 4;

    public void open(Player player) {

        EquipmentDismantleHolder holder =
                new EquipmentDismantleHolder();

        Inventory inventory = Bukkit.createInventory(
                holder,
                9,
                Component.text("장비 분해")
        );

        holder.setInventory(inventory);

        /*
         * 장비 투입 칸과 분해 버튼을 제외한 슬롯을
         * 검은색 색유리 판으로 채운다.
         */
        ItemStack background =
                new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

        ItemMeta backgroundMeta = background.getItemMeta();

        backgroundMeta.displayName(
                Component.text(" ")
                        .decoration(
                                TextDecoration.ITALIC,
                                false
                        )
        );

        background.setItemMeta(backgroundMeta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {

            if (slot == INPUT_SLOT || slot == DISMANTLE_SLOT) {
                continue;
            }

            inventory.setItem(
                    slot,
                    background.clone()
            );
        }

        inventory.setItem(
                DISMANTLE_SLOT,
                createDismantleButton()
        );

        player.openInventory(inventory);
    }

    private ItemStack createDismantleButton() {

        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();

        meta.displayName(
                Component.text(
                        "장비 분해",
                        NamedTextColor.RED
                ).decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                "왼쪽 칸에 분해할 장비를 올려주세요.",
                                NamedTextColor.GRAY
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),
                        Component.text(
                                "클릭하면 장비를 소비하고 제작서 1개를 지급합니다.",
                                NamedTextColor.YELLOW
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        )
                )
        );

        button.setItemMeta(meta);
        return button;
    }
}
