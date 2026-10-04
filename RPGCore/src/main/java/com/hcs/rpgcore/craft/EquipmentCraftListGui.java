package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * NPC 15 장비 목록 GUI.
 *
 * 아이템 선택과 페이지 이동만 처리한다.
 * NPC 18번 장비 변경 및 실제 제작은 수행하지 않는다.
 */
public final class EquipmentCraftListGui {

    private static final int PAGE_SIZE = 18;

    private static final int PREVIOUS_SLOT = 18;
    private static final int STATUS_SLOT = 20;
    private static final int RETURN_SLOT = 22;
    private static final int STOP_SLOT = 23;
    private static final int EXECUTE_SLOT = 24;
    private static final int SET_SLOT = 25;
    private static final int NEXT_SLOT = 26;

    private final EquipmentCraftListRepository repository;
    private final CustomItemFactory itemFactory;
    private final EquipmentCraftPreviewService previewService;
    private final EquipmentCraftSetRepository setRepository;

    public EquipmentCraftListGui(
            RPGCorePlugin plugin,
            EquipmentCraftPreviewService previewService
    ) {
        this.repository = new EquipmentCraftListRepository(
                plugin.getDatabaseManager()
        );
        this.itemFactory = new CustomItemFactory(plugin);
        this.previewService = previewService;
        this.setRepository = new EquipmentCraftSetRepository(
                plugin.getDatabaseManager()
        );
    }

    public static final class ListHolder
            implements InventoryHolder {

        private final String equipmentType;
        private final String typeLabel;
        private final String rarity;
        private final List<EquipmentCraftListRepository.PreviewItem> items;

        private Inventory inventory;
        private int page;
        private int selectedIndex = -1;

        private ListHolder(
                String equipmentType,
                String typeLabel,
                String rarity,
                List<EquipmentCraftListRepository.PreviewItem> items
        ) {
            this.equipmentType = equipmentType;
            this.typeLabel = typeLabel;
            this.rarity = rarity;
            this.items = items;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public String equipmentType() {
            return equipmentType;
        }

        public String rarity() {
            return rarity;
        }
    }

    public void open(
            Player player,
            String equipmentType,
            String typeLabel,
            String rarity
    ) {

        final List<EquipmentCraftListRepository.PreviewItem> items;

        try {
            items = repository.findItems(
                    equipmentType,
                    rarity
            );
        } catch (SQLException exception) {
            player.sendMessage(
                    "장비 목록을 불러오지 못했습니다. 서버 로그를 확인해 주세요."
            );

            Bukkit.getLogger().warning(
                    "[RPGCore] NPC 15 equipment list: "
                            + exception.getMessage()
            );
            return;
        }

        ListHolder holder = new ListHolder(
                equipmentType,
                typeLabel,
                rarity,
                items
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text(
                        typeLabel + " / " + rarity + " - 장비 목록"
                )
        );

        holder.inventory = inventory;

        render(holder);
        player.openInventory(inventory);
    }

    public void handleClick(
            Player player,
            ListHolder holder,
            InventoryClickEvent event,
            Runnable returnToRarity
    ) {

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= holder.inventory.getSize()) {
            return;
        }

        if (slot < PAGE_SIZE) {

            int itemIndex =
                    holder.page * PAGE_SIZE + slot;

            if (itemIndex >= holder.items.size()) {
                return;
            }

            int oldSelected = holder.selectedIndex;
            holder.selectedIndex = itemIndex;

            // 이전 선택 표시를 제거하고 새 선택을 표시한다.
            updateItem(holder, oldSelected);
            updateItem(holder, itemIndex);
            updateControls(holder);

            EquipmentCraftListRepository.PreviewItem selected =
                    holder.items.get(itemIndex);

            player.sendMessage(
                    "선택한 장비: " + selected.displayName()
            );
            return;
        }

        switch (slot) {

            case PREVIOUS_SLOT -> {
                if (holder.page > 0) {
                    holder.page--;
                    render(holder);
                }
            }

            case NEXT_SLOT -> {
                if (holder.page + 1 < pageCount(holder)) {
                    holder.page++;
                    render(holder);
                }
            }

            case RETURN_SLOT ->
                    returnToRarity.run();

            case STOP_SLOT -> {
                previewService.release(player);
                player.sendMessage(
                        "장비 미리보기 종료를 요청했습니다."
                );
            }

            case EXECUTE_SLOT ->
                    executePreview(player, holder);

            case SET_SLOT ->
                    executeSetPreview(player, holder);

            default -> {
            }
        }
    }

    private void executePreview(
            Player player,
            ListHolder holder
    ) {
        if (holder.selectedIndex < 0
                || holder.selectedIndex >= holder.items.size()) {
            player.sendMessage(
                    "먼저 미리보기할 장비를 선택해 주세요."
            );
            return;
        }

        EquipmentCraftListRepository.PreviewItem selected =
                holder.items.get(holder.selectedIndex);

        String dbSlot = selected.equipmentSlot();

        net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot
                npcSlot;

        switch (holder.equipmentType) {
            case "weapon" -> {
                if (dbSlot != null && !dbSlot.equals("HAND")) {
                    player.sendMessage(
                            "이 무기의 장착 슬롯을 확인할 수 없습니다."
                    );
                    return;
                }

                npcSlot =
                        net.citizensnpcs.api.trait.trait.Equipment
                                .EquipmentSlot.HAND;
            }

            case "shield" -> {
                if (!"OFF_HAND".equals(dbSlot)) {
                    player.sendMessage(
                            "방패의 장착 슬롯이 올바르지 않습니다."
                    );
                    return;
                }

                npcSlot =
                        net.citizensnpcs.api.trait.trait.Equipment
                                .EquipmentSlot.OFF_HAND;
            }

            case "armor" -> {
                if (dbSlot == null) {
                    player.sendMessage(
                            "장착 슬롯이 없는 방어구입니다."
                    );
                    return;
                }

                npcSlot = switch (dbSlot) {
                    case "HEAD" ->
                            net.citizensnpcs.api.trait.trait.Equipment
                                    .EquipmentSlot.HELMET;
                    case "CHEST" ->
                            net.citizensnpcs.api.trait.trait.Equipment
                                    .EquipmentSlot.CHESTPLATE;
                    case "LEGS" ->
                            net.citizensnpcs.api.trait.trait.Equipment
                                    .EquipmentSlot.LEGGINGS;
                    case "FEET" ->
                            net.citizensnpcs.api.trait.trait.Equipment
                                    .EquipmentSlot.BOOTS;
                    case "OFF_HAND" ->
                            net.citizensnpcs.api.trait.trait.Equipment
                                    .EquipmentSlot.OFF_HAND;
                    default -> null;
                };

                if (npcSlot == null) {
                    player.sendMessage(
                            "지원하지 않는 방어구 장착 슬롯입니다."
                    );
                    return;
                }
            }

            default -> {
                player.sendMessage(
                        "지원하지 않는 장비 종류입니다."
                );
                return;
            }
        }

        try {
            boolean success = previewService.previewSingle(
                    player,
                    selected.itemId(),
                    npcSlot
            );

            if (!success) {
                return;
            }

            player.closeInventory();
            player.sendMessage(
                    "NPC 18번에 미리보기를 적용했습니다: "
                            + selected.displayName()
            );

        } catch (RuntimeException exception) {
            player.sendMessage(
                    "장비 미리보기에 실패했습니다. 서버 로그를 확인해 주세요."
            );

            Bukkit.getLogger().warning(
                    "[RPGCore] NPC 15 preview "
                            + selected.itemId()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private void executeSetPreview(
            Player player,
            ListHolder holder
    ) {

        if (!"armor".equals(holder.equipmentType)) {
            return;
        }

        if (holder.selectedIndex < 0
                || holder.selectedIndex >= holder.items.size()) {

            player.sendMessage(
                    "먼저 세트에 속한 방어구를 선택해 주세요."
            );
            return;
        }

        EquipmentCraftListRepository.PreviewItem selected =
                holder.items.get(holder.selectedIndex);

        Integer setId = selected.setId();

        if (setId == null || setId <= 0) {
            player.sendMessage(
                    "선택한 방어구에는 전체 세트 정보가 없습니다."
            );
            return;
        }

        try {

            var setItems = setRepository.findPreviewSet(setId, selected.itemId());

            if (setItems.isEmpty()) {
                player.sendMessage(
                        "해당 세트의 미리보기 장비가 없습니다."
                );
                return;
            }

            if (!previewService.preview(player, setItems)) {
                return;
            }

            player.closeInventory();

            player.sendMessage(
                    "미리보기 NPC에 전체 세트를 표시했습니다: "
                            + selected.displayName()
            );

        } catch (SQLException | RuntimeException exception) {

            player.sendMessage(
                    "전체 세트 미리보기에 실패했습니다."
            );

            Bukkit.getLogger().warning(
                    "[RPGCore] NPC 15 set preview "
                            + setId
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private void render(ListHolder holder) {

        holder.inventory.clear();

        int start = holder.page * PAGE_SIZE;
        int end = Math.min(
                start + PAGE_SIZE,
                holder.items.size()
        );

        for (int index = start; index < end; index++) {
            updateItem(holder, index);
        }

        updateControls(holder);
    }

    private void updateItem(
            ListHolder holder,
            int itemIndex
    ) {

        if (itemIndex < 0 || itemIndex >= holder.items.size()) {
            return;
        }

        int visibleSlot =
                itemIndex - holder.page * PAGE_SIZE;

        if (visibleSlot < 0 || visibleSlot >= PAGE_SIZE) {
            return;
        }

        EquipmentCraftListRepository.PreviewItem definition =
                holder.items.get(itemIndex);

        ItemStack item;

        try {
            item = itemFactory.create(
                    definition.itemId()
            );
        } catch (Exception exception) {

            item = new ItemStack(Material.PAPER);

            Bukkit.getLogger().warning(
                    "[RPGCore] NPC 15 display item "
                            + definition.itemId()
                            + ": "
                            + exception.getMessage()
            );
        }

        if (item == null || item.getType().isAir()) {
            item = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = item.getItemMeta();

        List<Component> lore =
                meta.lore() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(meta.lore());

        lore.add(Component.empty());

        if (holder.selectedIndex == itemIndex) {
            lore.add(
                    Component.text(
                            "▶ 선택된 장비",
                            NamedTextColor.GREEN
                    )
            );
        } else {
            lore.add(
                    Component.text(
                            "클릭하여 선택",
                            NamedTextColor.YELLOW
                    )
            );
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        holder.inventory.setItem(
                visibleSlot,
                item
        );
    }

    private void updateControls(ListHolder holder) {

        int totalPages = pageCount(holder);

        holder.inventory.setItem(
                PREVIOUS_SLOT,
                button(
                        "이전 페이지",
                        1002,
                        holder.page > 0
                                ? "이전 페이지로 이동"
                                : "첫 페이지입니다."
                )
        );

        holder.inventory.setItem(
                STATUS_SLOT,
                plainButton(
                        Material.BOOK,
                        "장비 목록",
                        holder.items.size() + "개 / "
                                + (holder.page + 1)
                                + " / "
                                + totalPages
                                + " 페이지"
                )
        );

        holder.inventory.setItem(
                RETURN_SLOT,
                button(
                        "뒤로 가기",
                        1004,
                        "등급 선택 화면으로 돌아갑니다."
                )
        );

        holder.inventory.setItem(
                STOP_SLOT,
                button(
                        "미리보기 종료",
                        1007,
                        "내 미리보기를 종료하고 NPC 18번의 장비를 복원합니다."
                )
        );

        holder.inventory.setItem(
                EXECUTE_SLOT,
                button(
                        "미리보기 실행",
                        1005,
                        holder.selectedIndex >= 0
                                ? "미리보기 NPC에 선택한 장비를 표시합니다."
                                : "먼저 장비를 선택해 주세요."
                )
        );

        // 선택된 방어구에 set_id가 있을 때만 버튼을 표시한다.
        holder.inventory.setItem(SET_SLOT, null);

        if ("armor".equals(holder.equipmentType)
                && holder.selectedIndex >= 0
                && holder.selectedIndex < holder.items.size()) {

            EquipmentCraftListRepository.PreviewItem selected =
                    holder.items.get(holder.selectedIndex);

            if (selected.setId() != null
                    && selected.setId() > 0) {

                holder.inventory.setItem(
                        SET_SLOT,
                        button(
                                "세트 전체 미리보기",
                                1005,
                                "선택한 방어구의 세트 전체를 표시합니다."
                        )
                );
            }
        }

        holder.inventory.setItem(
                NEXT_SLOT,
                button(
                        "다음 페이지",
                        1000,
                        holder.page + 1 < totalPages
                                ? "다음 페이지로 이동"
                                : "마지막 페이지입니다."
                )
        );
    }

    private int pageCount(ListHolder holder) {
        return Math.max(
                1,
                (holder.items.size() + PAGE_SIZE - 1)
                        / PAGE_SIZE
        );
    }

    private ItemStack button(
            String name,
            int customModelData,
            String description
    ) {

        ItemStack item = plainButton(
                Material.ARROW,
                name,
                description
        );

        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack plainButton(
            Material material,
            String name,
            String description
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.text(
                        name,
                        NamedTextColor.YELLOW
                )
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
}
