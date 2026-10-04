package com.hcs.rpgcore.furniture.shop;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;
import com.hcs.rpgcore.shop.ShopItemDefinition;
import com.hcs.rpgcore.shop.ShopItemProvider;
import com.hcs.rpgcore.shop.ShopItemSource;

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * NPC 25 가구 상점.
 *
 * 상단 36칸: DB 가구 목록
 * 하단 36: 이전 페이지
 * 하단 40: 구매하기
 * 하단 44: 다음 페이지
 *
 * 가구 아이콘은 실제 공급 플러그인의 아이템을 사용한다.
 */
public final class FurnitureShopNpcListener
        implements Listener {

    private static final int NPC_ID = 25;

    private static final int INVENTORY_SIZE = 45;
    private static final int PAGE_SIZE = 36;

    private static final int PREVIOUS_SLOT = 36;
    private static final int BUY_SLOT = 40;
    private static final int NEXT_SLOT = 44;

    private static final int PREVIOUS_MODEL = 1002;
    private static final int BUY_MODEL = 1005;
    private static final int NEXT_MODEL = 1000;

    private final RPGCorePlugin plugin;
    private final FurnitureShopRepository repository;
    private final FurniturePurchaseService purchaseService;
    private final ShopItemProvider itemProvider;

    private static final class MenuHolder
            implements InventoryHolder {

        private final List<FurnitureShopRepository.FurnitureItem> items;

        private Inventory inventory;
        private int page;
        private String selectedShopItemId;

        private MenuHolder(
                List<FurnitureShopRepository.FurnitureItem> items
        ) {
            this.items = items;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public FurnitureShopNpcListener(
            RPGCorePlugin plugin,
            ShopEconomyService economyService,
            ShopItemProvider itemProvider
    ) {

        this.plugin = plugin;
        this.itemProvider = itemProvider;

        this.repository = new FurnitureShopRepository(
                plugin.getDatabaseManager()
        );

        FurniturePurchaseRepository purchaseRepository =
                new FurniturePurchaseRepository(
                        plugin.getDatabaseManager()
                );

        this.purchaseService = new FurniturePurchaseService(
                plugin,
                repository,
                purchaseRepository,
                economyService,
                itemProvider
        );
    }

    @EventHandler
    public void onNpcRightClick(
            NPCRightClickEvent event
    ) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        open(event.getClicker());
    }

    private void open(
            Player player
    ) {

        final List<FurnitureShopRepository.FurnitureItem> items;

        try {
            items = repository.findEnabled();

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[FurnitureShop] Failed to load products: "
                            + exception.getMessage()
            );

            player.sendMessage(
                    Component.text(
                            "가구 판매 목록을 불러오지 못했습니다.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        MenuHolder holder = new MenuHolder(items);

        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                Component.text("가구 상점")
        );

        holder.inventory = inventory;

        render(player, holder);

        player.openInventory(inventory);
    }

    private void render(
            Player player,
            MenuHolder holder
    ) {

        Inventory inventory = holder.inventory;

        inventory.clear();

        int totalPages = pageCount(holder);
        holder.page = Math.max(
                0,
                Math.min(holder.page, totalPages - 1)
        );

        int start = holder.page * PAGE_SIZE;
        int end = Math.min(
                start + PAGE_SIZE,
                holder.items.size()
        );

        for (int index = start; index < end; index++) {

            FurnitureShopRepository.FurnitureItem product =
                    holder.items.get(index);

            inventory.setItem(
                    index - start,
                    previewItem(player, holder, product)
            );
        }

        inventory.setItem(
                PREVIOUS_SLOT,
                button(
                        "이전 페이지",
                        PREVIOUS_MODEL,
                        holder.page > 0
                                ? "이전 페이지로 이동합니다."
                                : "첫 번째 페이지입니다."
                )
        );

        FurnitureShopRepository.FurnitureItem selected =
                selectedItem(holder);

        if (selected == null) {

            inventory.setItem(
                    BUY_SLOT,
                    button(
                            "구매하기",
                            BUY_MODEL,
                            "먼저 구매할 가구를 선택하세요."
                    )
            );

        } else {

            inventory.setItem(
                    BUY_SLOT,
                    button(
                            "구매하기",
                            BUY_MODEL,
                            selected.displayName()
                                    + " / "
                                    + selected.priceGold()
                                    + "G"
                    )
            );
        }

        inventory.setItem(
                NEXT_SLOT,
                button(
                        "다음 페이지",
                        NEXT_MODEL,
                        holder.page + 1 < totalPages
                                ? "다음 페이지로 이동합니다."
                                : "마지막 페이지입니다."
                )
        );
    }

    private ItemStack previewItem(
            Player player,
            MenuHolder holder,
            FurnitureShopRepository.FurnitureItem product
    ) {

        ItemStack item;

        try {

            if (
                    !"CRAFTENGINE".equalsIgnoreCase(
                            product.provider()
                    )
                            && !"ITEMSADDER".equalsIgnoreCase(
                            product.provider()
                    )
            ) {
                return unavailableItem(
                        product,
                        "현재 지원하지 않는 가구입니다."
                );
            }

            long price = product.priceGold();

            if (price <= 0L || price > Integer.MAX_VALUE) {
                return unavailableItem(
                        product,
                        "상품 가격 설정을 확인해야 합니다."
                );
            }

            ShopItemSource itemSource =
                    ShopItemSource.valueOf(
                            product.provider()
                                    .trim()
                                    .toUpperCase()
                    );

            ShopItemDefinition definition =
                    ShopItemDefinition.fixedBuy(
                            product.shopItemId(),
                            product.displayName(),
                            itemSource,
                            product.providerItemId(),
                            0,
                            1,
                            (int) price
                    );

            item = itemProvider.create(
                    player,
                    definition
            );

            if (item == null || item.getType().isAir()) {
                return unavailableItem(
                        product,
                        "가구 아이템을 생성할 수 없습니다."
                );
            }

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "[FurnitureShop] Preview failed: "
                            + product.shopItemId()
                            + " error="
                            + exception.getMessage()
            );

            return unavailableItem(
                    product,
                    "가구 아이템을 생성할 수 없습니다."
            );
        }

        /*
         * GUI 미리보기 아이템만 수정한다.
         * 실제 구매 아이템은 구매 서비스에서 새로 생성한다.
         */
        ItemStack preview = item.clone();
        preview.setAmount(1);

        ItemMeta meta = preview.getItemMeta();

        if (meta == null) {
            return preview;
        }

        boolean selected =
                product.shopItemId().equals(
                        holder.selectedShopItemId
                );

        meta.displayName(
                Component.text(
                        (selected ? "▶ " : "")
                                + product.displayName(),
                        selected
                                ? NamedTextColor.GREEN
                                : NamedTextColor.YELLOW
                )
        );

        List<Component> lore = new ArrayList<>();

        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }

        lore.add(Component.empty());

        lore.add(
                Component.text(
                        "가격: " + product.priceGold() + "G",
                        NamedTextColor.GOLD
                )
        );

        lore.add(
                Component.text(
                        selected
                                ? "선택된 가구입니다."
                                : "클릭하여 선택합니다.",
                        selected
                                ? NamedTextColor.GREEN
                                : NamedTextColor.GRAY
                )
        );

        meta.lore(lore);

        preview.setItemMeta(meta);

        return preview;
    }

    private ItemStack unavailableItem(
            FurnitureShopRepository.FurnitureItem product,
            String reason
    ) {

        ItemStack item = new ItemStack(Material.BARRIER);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                Component.text(
                        product.displayName(),
                        NamedTextColor.RED
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                reason,
                                NamedTextColor.GRAY
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack button(
            String name,
            int customModelData,
            String description
    ) {

        ItemStack item = new ItemStack(Material.ARROW);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setCustomModelData(customModelData);

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

    private int pageCount(
            MenuHolder holder
    ) {

        return Math.max(
                1,
                (holder.items.size() + PAGE_SIZE - 1)
                        / PAGE_SIZE
        );
    }

    private FurnitureShopRepository.FurnitureItem selectedItem(
            MenuHolder holder
    ) {

        if (holder.selectedShopItemId == null) {
            return null;
        }

        for (FurnitureShopRepository.FurnitureItem item
                : holder.items) {

            if (item.shopItemId().equals(
                    holder.selectedShopItemId
            )) {
                return item;
            }
        }

        return null;
    }

    @EventHandler
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(event.getView()
                .getTopInventory()
                .getHolder() instanceof MenuHolder holder)) {
            return;
        }

        /*
         * 상점이 열린 동안 아이템 이동을 전부 차단한다.
         * Shift 클릭이나 숫자키로 GUI 아이템을 가져갈 수 없다.
         */
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return;
        }

        if (slot < PAGE_SIZE) {

            int index = holder.page * PAGE_SIZE + slot;

            if (index >= holder.items.size()) {
                return;
            }

            holder.selectedShopItemId =
                    holder.items.get(index).shopItemId();

            render(player, holder);

            return;
        }

        if (slot == PREVIOUS_SLOT) {

            if (holder.page > 0) {
                holder.page--;
                render(player, holder);
            }

            return;
        }

        if (slot == NEXT_SLOT) {

            if (holder.page + 1 < pageCount(holder)) {
                holder.page++;
                render(player, holder);
            }

            return;
        }

        if (slot == BUY_SLOT) {

            FurnitureShopRepository.FurnitureItem selected =
                    selectedItem(holder);

            if (selected == null) {

                player.sendMessage(
                        Component.text(
                                "구매할 가구를 먼저 선택하세요.",
                                NamedTextColor.RED
                        )
                );

                return;
            }

            purchaseService.purchase(
                    player,
                    selected.shopItemId()
            );

            if (player.getOpenInventory()
                    .getTopInventory() == holder.inventory) {

                render(player, holder);
            }
        }
    }

    @EventHandler
    public void onDrag(
            InventoryDragEvent event
    ) {

        if (event.getView()
                .getTopInventory()
                .getHolder() instanceof MenuHolder) {

            event.setCancelled(true);
        }
    }
}
