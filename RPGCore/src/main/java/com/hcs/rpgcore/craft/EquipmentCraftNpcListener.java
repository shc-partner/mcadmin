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
 * NPC 15 장비 제작소.
 *
 * 현재 단계:
 * - 첫 화면 표시
 * - GUI 아이템 이동 방지
 *
 * 장비 미리보기와 제작의 상세 화면은 다음 단계에서 연결한다.
 */
public final class EquipmentCraftNpcListener
        implements Listener {

    private final EquipmentCraftListGui listGui;
    private final EquipmentCraftCreationGui creationGui;
    private final EquipmentCraftPreviewService previewService;
    private final com.hcs.rpgcore.item.CustomItemFactory rarityItemFactory;

    public EquipmentCraftNpcListener(
            com.hcs.rpgcore.RPGCorePlugin plugin,
            EquipmentCraftPreviewService previewService
    ) {
        this.previewService = previewService;
        this.creationGui = new EquipmentCraftCreationGui(plugin);
        this.rarityItemFactory =
                new com.hcs.rpgcore.item.CustomItemFactory(plugin);
        this.listGui = new EquipmentCraftListGui(
                plugin,
                previewService
        );
    }

    public EquipmentCraftCreationGui getCreationGui() {
        return creationGui;
    }

    private static final int NPC_ID = 15;

    private static final int PREVIEW_SLOT = 11;
    private static final int CRAFT_SLOT = 15;
    private static final int EXIT_SLOT = 22;

    private static final int WEAPON_SLOT = 10;
    private static final int ARMOR_SLOT = 13;
    private static final int SHIELD_SLOT = 16;

    private enum Screen {
        MAIN,
        TYPE,
        RARITY
    }

    private enum EquipmentType {
        WEAPON("무기"),
        ARMOR("방어구"),
        SHIELD("방패");

        private final String label;

        EquipmentType(String label) {
            this.label = label;
        }
    }

    private static final class MenuHolder
            implements InventoryHolder {

        private final Screen screen;
        private final EquipmentType equipmentType;
        private Inventory inventory;

        private MenuHolder(Screen screen) {
            this(screen, null);
        }

        private MenuHolder(
                Screen screen,
                EquipmentType equipmentType
        ) {
            this.screen = screen;
            this.equipmentType = equipmentType;
        }

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

        openMainMenu(event.getClicker());
    }

    private void openMainMenu(Player player) {

        MenuHolder holder = new MenuHolder(Screen.MAIN);

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text("장비 제작소")
        );

        holder.setInventory(inventory);

        inventory.setItem(
                PREVIEW_SLOT,
                createButton(
                        Material.NETHER_STAR,
                        "장비 미리보기",
                        NamedTextColor.AQUA,
                        "장비를 선택하고 NPC 18번에서 확인합니다."
                )
        );

        inventory.setItem(
                CRAFT_SLOT,
                createButton(
                        Material.END_CRYSTAL,
                        "장비 제작",
                        NamedTextColor.GOLD,
                        "재료를 사용하여 장비를 제작합니다."
                )
        );

        inventory.setItem(
                EXIT_SLOT,
                createButton(
                        Material.ARROW,
                        "닫기",
                        NamedTextColor.RED,
                        "장비 제작소를 닫습니다."
                )
        );

        player.openInventory(inventory);
    }

    private void openTypeMenu(Player player) {

        MenuHolder holder = new MenuHolder(Screen.TYPE);

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text("장비 미리보기 - 종류 선택")
        );

        holder.setInventory(inventory);

        inventory.setItem(
                WEAPON_SLOT,
                createButton(
                        Material.DIAMOND_SWORD,
                        "무기",
                        NamedTextColor.AQUA,
                        "미리보기할 무기를 선택합니다."
                )
        );

        inventory.setItem(
                ARMOR_SLOT,
                createButton(
                        Material.NETHERITE_CHESTPLATE,
                        "방어구",
                        NamedTextColor.AQUA,
                        "방어구와 날개를 선택합니다."
                )
        );

        inventory.setItem(
                SHIELD_SLOT,
                createButton(
                        Material.SHIELD,
                        "방패",
                        NamedTextColor.AQUA,
                        "미리보기할 방패를 선택합니다."
                )
        );

        inventory.setItem(
                EXIT_SLOT,
                createButton(
                        Material.ARROW,
                        "뒤로 가기",
                        NamedTextColor.YELLOW,
                        "장비 제작소 첫 화면으로 돌아갑니다."
                )
        );

        player.openInventory(inventory);
    }

    private void openRarityMenu(
            Player player,
            EquipmentType equipmentType
    ) {

        MenuHolder holder = new MenuHolder(
                Screen.RARITY,
                equipmentType
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text(
                        equipmentType.label + " - 등급 선택"
                )
        );

        holder.setInventory(inventory);

        inventory.setItem(
                WEAPON_SLOT,
                createRarityButton(
                        "hero_ingot",
                        "영웅",
                        NamedTextColor.LIGHT_PURPLE,
                        equipmentType.label + " / 영웅 등급"
                )
        );

        inventory.setItem(
                ARMOR_SLOT,
                createRarityButton(
                        "legendary_ingot",
                        "전설",
                        NamedTextColor.GOLD,
                        equipmentType.label + " / 전설 등급"
                )
        );

        inventory.setItem(
                SHIELD_SLOT,
                createRarityButton(
                        "mythic_ingot",
                        "신화",
                        NamedTextColor.AQUA,
                        equipmentType.label + " / 신화 등급"
                )
        );

        inventory.setItem(
                EXIT_SLOT,
                createButton(
                        Material.ARROW,
                        "뒤로 가기",
                        NamedTextColor.YELLOW,
                        "장비 종류 선택 화면으로 돌아갑니다."
                )
        );

        player.openInventory(inventory);
    }

    private void openListMenu(
            Player player,
            EquipmentType equipmentType,
            String rarity
    ) {

        listGui.open(
                player,
                equipmentType.name().toLowerCase(
                        java.util.Locale.ROOT
                ),
                equipmentType.label,
                rarity
        );
    }

    private ItemStack createRarityButton(
            String itemId,
            String name,
            NamedTextColor color,
            String description
    ) {

        ItemStack item = rarityItemFactory.create(itemId);

        if (item == null || item.getType().isAir()) {
            throw new IllegalStateException(
                    "등급 선택 아이콘을 생성하지 못했습니다: " + itemId
            );
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException(
                    "등급 선택 아이콘의 ItemMeta가 없습니다: " + itemId
            );
        }

        // 커스텀 모델은 유지하고 GUI 표시 이름과 설명만 변경한다.
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

    private ItemStack createButton(
            Material material,
            String name,
            NamedTextColor color,
            String description
    ) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (material == Material.ARROW) {

            if ("뒤로 가기".equals(name)) {
                meta.setCustomModelData(1004);
            } else if ("닫기".equals(name)) {
                meta.setCustomModelData(1007);
            }
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

        if (event.getView()
                .getTopInventory()
                .getHolder()
                instanceof EquipmentCraftListGui.ListHolder listHolder) {

            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player player) {

                EquipmentType type = EquipmentType.valueOf(
                        listHolder.equipmentType().toUpperCase(
                                java.util.Locale.ROOT
                        )
                );

                listGui.handleClick(
                        player,
                        listHolder,
                        event,
                        () -> openRarityMenu(player, type)
                );
            }

            return;
        }

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof MenuHolder holder)) {
            return;
        }

        // GUI에 표시한 버튼을 가져가거나 교체하지 못하게 한다.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot >= event.getView()
                .getTopInventory()
                .getSize()) {
            return;
        }

        if (holder.screen == Screen.RARITY) {

            if (holder.equipmentType == null) {
                player.closeInventory();
                return;
            }

            switch (slot) {

                case WEAPON_SLOT ->
                        openListMenu(
                                player,
                                holder.equipmentType,
                                "영웅"
                        );

                case ARMOR_SLOT ->
                        openListMenu(
                                player,
                                holder.equipmentType,
                                "전설"
                        );

                case SHIELD_SLOT ->
                        openListMenu(
                                player,
                                holder.equipmentType,
                                "신화"
                        );

                case EXIT_SLOT ->
                        openTypeMenu(player);

                default -> {
                }
            }

            return;
        }

        if (holder.screen == Screen.TYPE) {

            switch (slot) {

                case WEAPON_SLOT ->
                        openRarityMenu(
                                player,
                                EquipmentType.WEAPON
                        );

                case ARMOR_SLOT ->
                        openRarityMenu(
                                player,
                                EquipmentType.ARMOR
                        );

                case SHIELD_SLOT ->
                        openRarityMenu(
                                player,
                                EquipmentType.SHIELD
                        );

                case EXIT_SLOT ->
                        openMainMenu(player);

                default -> {
                }
            }

            return;
        }

        switch (slot) {

            case PREVIEW_SLOT ->
                    openTypeMenu(player);

            case CRAFT_SLOT ->
                    creationGui.open(player);

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

        if (event.getView()
                .getTopInventory()
                .getHolder()
                instanceof EquipmentCraftListGui.ListHolder) {

            int topSize = event.getView()
                    .getTopInventory()
                    .getSize();

            for (int slot : event.getRawSlots()) {

                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }

            return;
        }

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof MenuHolder)) {
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
