package com.hcs.rpgcore.craft;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;

/**
 * NPC 15 장비 제작 전용 GUI.
 *
 * 이번 단계:
 * - 종류 / 등급 / 장비 목록
 * - 완성품 및 제작 재료 표시
 * - 보유 재료 수량 표시
 *
 * 재료 차감과 장비 지급은 수행하지 않는다.
 */
public final class EquipmentCraftCreationGui
        implements Listener {

    private static final int PAGE_SIZE = 18;

    private final RPGCorePlugin plugin;
    private final CustomItemFactory itemFactory;
    private final EquipmentCraftListRepository repository;
    private final EquipmentCraftExecutionService executionService;
    private final NamespacedKey customItemIdKey;

    private enum Screen {
        TYPE,
        RARITY,
        LIST,
        DETAIL
    }

    public static final class CraftHolder
            implements InventoryHolder {

        private final Screen screen;
        private final String equipmentType;
        private final String rarity;

        private final List<
                EquipmentCraftListRepository.PreviewItem
                > items;

        private final int page;
        private final int selectedIndex;

        private Inventory inventory;

        private CraftHolder(
                Screen screen,
                String equipmentType,
                String rarity,
                List<EquipmentCraftListRepository.PreviewItem> items,
                int page,
                int selectedIndex
        ) {
            this.screen = screen;
            this.equipmentType = equipmentType;
            this.rarity = rarity;
            this.items = items;
            this.page = page;
            this.selectedIndex = selectedIndex;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public EquipmentCraftCreationGui(
            RPGCorePlugin plugin
    ) {
        this.plugin = plugin;
        this.itemFactory = new CustomItemFactory(plugin);
        this.executionService =
                new EquipmentCraftExecutionService(plugin);

        this.repository = new EquipmentCraftListRepository(
                plugin.getDatabaseManager()
        );

        this.customItemIdKey = new NamespacedKey(
                plugin,
                "custom_item_id"
        );
    }

    public void open(Player player) {
        openTypeMenu(player);
    }

    private Inventory createInventory(
            CraftHolder holder,
            String title
    ) {
        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text(title)
        );

        holder.inventory = inventory;

        return inventory;
    }

    private ItemStack button(
            Material material,
            String name,
            String description
    ) {
        /*
         * GUI 이동 버튼은 미리보기 GUI와 동일한
         * ARROW 커스텀 모델을 사용한다.
         *
         * 제작 재료 오류 등에 사용하는 BARRIER는 유지한다.
         */
        Material buttonMaterial =
                "닫기".equals(name)
                        ? Material.ARROW
                        : material;

        ItemStack item = new ItemStack(buttonMaterial);
        ItemMeta meta = item.getItemMeta();

        if (buttonMaterial == Material.ARROW) {

            switch (name) {

                case "뒤로 가기" ->
                        meta.setCustomModelData(1004);

                case "닫기" ->
                        meta.setCustomModelData(1007);

                case "이전 페이지" ->
                        meta.setCustomModelData(1002);

                case "다음 페이지" ->
                        meta.setCustomModelData(1000);

                case "제작하기" ->
                        meta.setCustomModelData(1005);

                default -> {
                }
            }
        }

        meta.displayName(
                Component.text(name, NamedTextColor.YELLOW)
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

    private ItemStack customButton(
            String itemId,
            String name,
            String description
    ) {
        ItemStack item = itemFactory.create(itemId);

        if (item == null || item.getType().isAir()) {
            return button(
                    Material.BARRIER,
                    name,
                    "아이템을 생성하지 못했습니다: " + itemId
            );
        }

        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.text(name, NamedTextColor.YELLOW)
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

    private void openTypeMenu(Player player) {

        CraftHolder holder = new CraftHolder(
                Screen.TYPE,
                null,
                null,
                List.of(),
                0,
                -1
        );

        Inventory inventory = createInventory(
                holder,
                "장비 제작 - 종류 선택"
        );

        inventory.setItem(
                10,
                button(
                        Material.DIAMOND_SWORD,
                        "무기",
                        "무기 제작 목록을 확인합니다."
                )
        );

        inventory.setItem(
                13,
                button(
                        Material.NETHERITE_CHESTPLATE,
                        "방어구",
                        "방어구와 날개 제작 목록을 확인합니다."
                )
        );

        inventory.setItem(
                16,
                button(
                        Material.SHIELD,
                        "방패",
                        "방패 제작 목록을 확인합니다."
                )
        );

        inventory.setItem(
                22,
                button(
                        Material.ARROW,
                        "닫기",
                        "제작 화면을 닫습니다."
                )
        );

        player.openInventory(inventory);
    }

    private void openRarityMenu(
            Player player,
            String equipmentType
    ) {

        CraftHolder holder = new CraftHolder(
                Screen.RARITY,
                equipmentType,
                null,
                List.of(),
                0,
                -1
        );

        Inventory inventory = createInventory(
                holder,
                "장비 제작 - 등급 선택"
        );

        inventory.setItem(
                10,
                customButton(
                        "hero_ingot",
                        "영웅",
                        "영웅 등급 장비 제작"
                )
        );

        inventory.setItem(
                13,
                customButton(
                        "legendary_ingot",
                        "전설",
                        "전설 등급 장비 제작"
                )
        );

        inventory.setItem(
                16,
                customButton(
                        "mythic_ingot",
                        "신화",
                        "신화 등급 장비 제작"
                )
        );

        inventory.setItem(
                22,
                button(
                        Material.ARROW,
                        "뒤로 가기",
                        "장비 종류 선택으로 돌아갑니다."
                )
        );

        player.openInventory(inventory);
    }

    private void openListMenu(
            Player player,
            String equipmentType,
            String rarity,
            int page
    ) {

        List<EquipmentCraftListRepository.PreviewItem> items;

        try {
            items = repository.findItems(
                    equipmentType,
                    rarity
            );
        } catch (SQLException exception) {

            player.sendMessage(
                    "장비 제작 목록을 불러오지 못했습니다."
            );

            Bukkit.getLogger().warning(
                    "[RPGCore] Equipment craft list: "
                            + exception.getMessage()
            );

            return;
        }

        int maxPage = items.isEmpty()
                ? 0
                : (items.size() - 1) / PAGE_SIZE;

        int safePage = Math.max(
                0,
                Math.min(page, maxPage)
        );

        CraftHolder holder = new CraftHolder(
                Screen.LIST,
                equipmentType,
                rarity,
                items,
                safePage,
                -1
        );

        Inventory inventory = createInventory(
                holder,
                "장비 제작 - " + rarity
        );

        int start = safePage * PAGE_SIZE;
        int end = Math.min(
                start + PAGE_SIZE,
                items.size()
        );

        for (int index = start; index < end; index++) {

            EquipmentCraftListRepository.PreviewItem entry =
                    items.get(index);

            ItemStack item = itemFactory.create(
                    entry.itemId()
            );

            if (item == null || item.getType().isAir()) {
                item = button(
                        Material.BARRIER,
                        entry.displayName(),
                        "아이템 생성 실패: " + entry.itemId()
                );
            }

            inventory.setItem(
                    index - start,
                    item
            );
        }

        if (safePage > 0) {
            inventory.setItem(
                    18,
                    button(
                            Material.ARROW,
                            "이전 페이지",
                            "이전 제작 목록으로 이동합니다."
                    )
            );
        }

        inventory.setItem(
                22,
                button(
                        Material.ARROW,
                        "뒤로 가기",
                        "등급 선택으로 돌아갑니다."
                )
        );

        if (safePage < maxPage) {
            inventory.setItem(
                    26,
                    button(
                            Material.ARROW,
                            "다음 페이지",
                            "다음 제작 목록으로 이동합니다."
                    )
            );
        }

        player.openInventory(inventory);
    }

    private void openDetailMenu(
            Player player,
            CraftHolder previous,
            int selectedIndex
    ) {

        EquipmentCraftListRepository.PreviewItem selected =
                previous.items.get(selectedIndex);

        EquipmentCraftRecipe recipe =
                EquipmentCraftRecipe.of(
                        previous.equipmentType,
                        previous.rarity
                );

        CraftHolder holder = new CraftHolder(
                Screen.DETAIL,
                previous.equipmentType,
                previous.rarity,
                previous.items,
                previous.page,
                selectedIndex
        );

        Inventory inventory = createInventory(
                holder,
                "장비 제작 - 재료 확인"
        );

        ItemStack result = itemFactory.create(
                selected.itemId()
        );

        if (result == null || result.getType().isAir()) {
            player.sendMessage(
                    "선택한 장비를 생성하지 못했습니다."
            );
            return;
        }

        inventory.setItem(
                13,
                result
        );

        inventory.setItem(
                11,
                materialDisplay(
                        player,
                        recipe.ingotItemId(),
                        recipe.ingotAmount()
                )
        );

        inventory.setItem(
                15,
                materialDisplay(
                        player,
                        recipe.recipeItemId(),
                        recipe.recipeAmount()
                )
        );

        inventory.setItem(
                22,
                button(
                        Material.ARROW,
                        "제작하기",
                        "표시된 재료를 사용하여 장비 1개를 제작합니다."
                )
        );

        inventory.setItem(
                18,
                button(
                        Material.ARROW,
                        "뒤로 가기",
                        "장비 목록으로 돌아갑니다."
                )
        );

        inventory.setItem(
                26,
                button(
                        Material.BARRIER,
                        "닫기",
                        "제작 화면을 닫습니다."
                )
        );

        player.openInventory(inventory);
    }

    private ItemStack materialDisplay(
            Player player,
            String itemId,
            int required
    ) {

        ItemStack item = itemFactory.create(itemId);

        if (item == null || item.getType().isAir()) {
            return button(
                    Material.BARRIER,
                    "제작 재료 오류",
                    itemId
            );
        }

        int owned = countMaterial(
                player,
                itemId
        );

        ItemMeta meta = item.getItemMeta();

        meta.lore(
                List.of(
                        Component.text(
                                "필요 수량: " + required,
                                NamedTextColor.GRAY
                        ),
                        Component.text(
                                "보유 수량: " + owned,
                                owned >= required
                                        ? NamedTextColor.GREEN
                                        : NamedTextColor.RED
                        )
                )
        );

        item.setItemMeta(meta);
        item.setAmount(required);

        return item;
    }

    private int countMaterial(
            Player player,
            String itemId
    ) {

        int count = 0;

        for (ItemStack item :
                player.getInventory().getStorageContents()) {

            if (item == null || item.getType().isAir()) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                continue;
            }

            String actualItemId =
                    meta.getPersistentDataContainer().get(
                            customItemIdKey,
                            PersistentDataType.STRING
                    );

            if (itemId.equals(actualItemId)) {
                count += item.getAmount();
            }
        }

        return count;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof CraftHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 27) {
            return;
        }

        switch (holder.screen) {

            case TYPE -> {

                switch (slot) {
                    case 10 ->
                            openRarityMenu(player, "weapon");

                    case 13 ->
                            openRarityMenu(player, "armor");

                    case 16 ->
                            openRarityMenu(player, "shield");

                    case 22 ->
                            player.closeInventory();

                    default -> {
                    }
                }
            }

            case RARITY -> {

                switch (slot) {
                    case 10 ->
                            openListMenu(
                                    player,
                                    holder.equipmentType,
                                    "영웅",
                                    0
                            );

                    case 13 ->
                            openListMenu(
                                    player,
                                    holder.equipmentType,
                                    "전설",
                                    0
                            );

                    case 16 ->
                            openListMenu(
                                    player,
                                    holder.equipmentType,
                                    "신화",
                                    0
                            );

                    case 22 ->
                            openTypeMenu(player);

                    default -> {
                    }
                }
            }

            case LIST -> {

                if (slot < PAGE_SIZE) {

                    int index =
                            holder.page * PAGE_SIZE + slot;

                    if (index < holder.items.size()) {
                        openDetailMenu(
                                player,
                                holder,
                                index
                        );
                    }

                    return;
                }

                if (slot == 18 && holder.page > 0) {

                    openListMenu(
                            player,
                            holder.equipmentType,
                            holder.rarity,
                            holder.page - 1
                    );

                } else if (slot == 22) {

                    openRarityMenu(
                            player,
                            holder.equipmentType
                    );

                } else if (slot == 26) {

                    openListMenu(
                            player,
                            holder.equipmentType,
                            holder.rarity,
                            holder.page + 1
                    );
                }
            }

            case DETAIL -> {

                if (slot == 22) {

                    /*
                     * 클릭 이벤트가 끝난 다음 틱에 제작한다.
                     * 실행 직전에 현재 GUI와 DB 목록을 다시 확인한다.
                     */
                    plugin.getServer().getScheduler().runTask(
                            plugin,
                            () -> executeCraft(player, holder)
                    );

                    return;
                }

                if (slot == 18) {

                    openListMenu(
                            player,
                            holder.equipmentType,
                            holder.rarity,
                            holder.page
                    );

                } else if (slot == 26) {

                    player.closeInventory();
                }
            }
        }
    }

    private void executeCraft(
            Player player,
            CraftHolder holder
    ) {

        if (!player.isOnline()
                || player.getOpenInventory()
                        .getTopInventory()
                        .getHolder() != holder) {
            return;
        }

        if (holder.selectedIndex < 0
                || holder.selectedIndex >= holder.items.size()) {

            player.sendMessage(
                    "제작할 장비를 다시 선택해 주세요."
            );
            return;
        }

        EquipmentCraftListRepository.PreviewItem selected =
                holder.items.get(holder.selectedIndex);

        /*
         * GUI를 열었을 때의 목록만 신뢰하지 않고
         * 현재 DB의 활성 제작 대상 목록에서 다시 확인한다.
         */
        try {

            boolean registered = repository.findItems(
                    holder.equipmentType,
                    holder.rarity
            ).stream().anyMatch(
                    item -> item.itemId().equals(
                            selected.itemId()
                    )
            );

            if (!registered) {

                player.sendMessage(
                        "현재 제작할 수 없는 장비입니다."
                );
                return;
            }

            EquipmentCraftExecutionService.Result result =
                    executionService.craft(
                            player,
                            holder.equipmentType,
                            holder.rarity,
                            selected.itemId()
                    );

            switch (result) {

                case SUCCESS -> {

                    player.closeInventory();

                    player.sendMessage(
                            "장비 제작에 성공했습니다: "
                                    + selected.displayName()
                    );
                }

                case NOT_ENOUGH_INGOTS ->
                        player.sendMessage(
                                "제작 주괴가 부족합니다."
                        );

                case NOT_ENOUGH_RECIPE ->
                        player.sendMessage(
                                "제작서가 부족합니다."
                        );

                case INVENTORY_FULL ->
                        player.sendMessage(
                                "인벤토리 공간이 부족합니다."
                        );

                case INVALID_ITEM ->
                        player.sendMessage(
                                "제작할 수 없는 장비입니다."
                        );

                case FAILED ->
                        player.sendMessage(
                                "장비 제작에 실패했습니다."
                        );
            }

        } catch (SQLException | RuntimeException exception) {

            player.sendMessage(
                    "장비 제작에 실패했습니다."
            );

            plugin.getLogger().warning(
                    "NPC 15 equipment crafting failed: "
                            + selected.itemId()
                            + " / "
                            + exception.getMessage()
            );
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof CraftHolder)) {
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
