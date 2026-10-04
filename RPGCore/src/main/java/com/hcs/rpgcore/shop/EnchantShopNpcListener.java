package com.hcs.rpgcore.shop;

import net.citizensnpcs.api.event.NPCRightClickEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnchantShopNpcListener implements Listener {

    private static final int NPC_ID = 19;
    private static final int PAGE_SIZE = 18;

    private static final int PREVIOUS_SLOT = 18;
    private static final int STATUS_SLOT = 20;
    private static final int BACK_SLOT = 22;
    private static final int CLOSE_SLOT = 24;
    private static final int NEXT_SLOT = 26;

    private static final int WEAPON_SLOT = 10;
    private static final int ARMOR_SLOT = 12;
    private static final int TOOL_SLOT = 14;
    private static final int COMMON_SLOT = 16;

    /*
     * 기존 상품의 테스트 가격은 유지한다.
     * 신규 상품의 기본 가격은 10,000원이다.
     */
    private static final Map<String, Integer> PRICES = Map.ofEntries(
            // 사서 거래 가능 - 기존 등록 상품
            Map.entry("minecraft:sharpness", 4000),
            Map.entry("minecraft:looting", 5000),
            Map.entry("minecraft:fire_aspect", 3000),
            Map.entry("minecraft:protection", 4000),
            Map.entry("minecraft:feather_falling", 4000),
            Map.entry("minecraft:respiration", 3000),
            Map.entry("minecraft:efficiency", 4000),
            Map.entry("minecraft:fortune", 6000),
            Map.entry("minecraft:silk_touch", 5000),
            Map.entry("minecraft:mending", 8000),
            Map.entry("minecraft:unbreaking", 5000),

            // 일반 사서 거래 불가
            Map.entry("minecraft:soul_speed", 15000),
            Map.entry("minecraft:swift_sneak", 25000),
            Map.entry("minecraft:wind_burst", 40000),

            // RPGCore 전용 인챈트: 나무꾼의 풍요
            Map.entry("rpgcore:forest_lumberjack", 24000),
            Map.entry("rpgcore:forest_lumberjack@1", 6000),
            Map.entry("rpgcore:forest_lumberjack@2", 12000),

            // 벌목의 파동
            Map.entry("rpgcore:logging_wave", 30000),
            Map.entry("rpgcore:logging_wave@1", 6000),
            Map.entry("rpgcore:logging_wave@2", 14000)
    );

    private static final Set<String> COMMON_ENCHANTS = Set.of(
            "mending",
            "unbreaking",
            "binding_curse",
            "vanishing_curse"
    );

    private static final Set<String> TOOL_ENCHANTS = Set.of(
            "efficiency",
            "fortune",
            "silk_touch",
            "forest_lumberjack",
            "logging_wave",
            "luck_of_the_sea",
            "lure"
    );

    private static final Set<String> ARMOR_ENCHANTS = Set.of(
            "protection",
            "fire_protection",
            "blast_protection",
            "projectile_protection",
            "feather_falling",
            "respiration",
            "aqua_affinity",
            "thorns",
            "depth_strider",
            "frost_walker",
            "soul_speed",
            "swift_sneak"
    );

    private enum Category {
        WEAPON("무기 인챈트", Material.DIAMOND_SWORD),
        ARMOR("방어구 인챈트", Material.DIAMOND_CHESTPLATE),
        TOOL("도구 인챈트", Material.DIAMOND_PICKAXE),
        COMMON("공통·특수 인챈트", Material.ENCHANTED_BOOK);

        private final String title;
        private final Material icon;

        Category(String title, Material icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private static final class Offer {

        private final Enchantment enchantment;
        private final int level;
        private final int price;

        private Offer(
                Enchantment enchantment,
                int level,
                int price
        ) {
            this.enchantment = enchantment;
            this.level = level;
            this.price = price;
        }
    }

    private static final class MenuHolder
            implements InventoryHolder {

        private final Category category;
        private final List<Offer> offers;
        private int page;
        private Inventory inventory;

        private MenuHolder(
                Category category,
                List<Offer> offers
        ) {
            this.category = category;
            this.offers = offers;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final JavaPlugin plugin;
    private final ShopTransactionService transactionService;
    private final ShopItemProvider itemProvider;

    public EnchantShopNpcListener(
            JavaPlugin plugin,
            ShopTransactionService transactionService,
            ShopItemProvider itemProvider
    ) {
        this.plugin = plugin;
        this.transactionService = transactionService;
        this.itemProvider = itemProvider;
    }

    @EventHandler
    public void onNpcRightClick(
            NPCRightClickEvent event
    ) {
        if (event.getNPC().getId() == NPC_ID) {
            openCategoryMenu(event.getClicker());
        }
    }

    public void openCategoryMenu(Player player) {

        MenuHolder holder = new MenuHolder(
                null,
                List.of()
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text("인챈트북 상점")
        );

        holder.inventory = inventory;

        inventory.setItem(
                WEAPON_SLOT,
                categoryButton(Category.WEAPON)
        );

        inventory.setItem(
                ARMOR_SLOT,
                categoryButton(Category.ARMOR)
        );

        inventory.setItem(
                TOOL_SLOT,
                categoryButton(Category.TOOL)
        );

        inventory.setItem(
                COMMON_SLOT,
                categoryButton(Category.COMMON)
        );

        inventory.setItem(
                BACK_SLOT,
                button(
                        "닫기",
                        1007,
                        "인챈트북 상점을 닫습니다.",
                        NamedTextColor.RED
                )
        );

        player.openInventory(inventory);
    }

    private void openCategory(
            Player player,
            Category category
    ) {

        List<Offer> offers = loadOffers(category);

        MenuHolder holder = new MenuHolder(
                category,
                offers
        );

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text(category.title + " 구매")
        );

        holder.inventory = inventory;

        render(player, holder);
        player.openInventory(inventory);
    }

    /*
     * 서버에 실제 등록된 인챈트를 사용한다.
     * 모든 상품은 해당 인챈트의 최대 레벨이다.
     */
    private List<Offer> loadOffers(
            Category category
    ) {

        List<Offer> offers = new ArrayList<>();

        for (Enchantment enchantment : Registry.ENCHANTMENT) {

            if (enchantment == null) {
                continue;
            }

            NamespacedKey key = enchantment.getKey();

            if (key == null || enchantment.getMaxLevel() < 1) {
                continue;
            }

            if (categoryOf(key) != category) {
                continue;
            }

            int price = PRICES.getOrDefault(
                    key.toString(),
                    4000
            );

            offers.add(
                    new Offer(
                            enchantment,
                            enchantment.getMaxLevel(),
                            price
                    )
            );

            // RPGCore 전용 인챈트는 I·II·III를 모두 판매한다.
            // III는 위의 최대 레벨 상품으로 이미 추가된다.
            if (
                    key.toString().equals("rpgcore:forest_lumberjack")
                    || key.toString().equals("rpgcore:logging_wave")
            ) {

                for (int level = 1; level < 3; level++) {

                    int levelPrice = PRICES.getOrDefault(
                            key + "@" + level,
                            10000
                    );

                    offers.add(
                            new Offer(
                                    enchantment,
                                    level,
                                    levelPrice
                            )
                    );
                }
            }
        }

        offers.sort(
                Comparator
                        .comparing(
                                (Offer offer) ->
                                        offer.enchantment
                                                .getKey()
                                                .toString()
                        )
                        .thenComparingInt(
                                offer -> offer.level
                        )
        );

        return offers;
    }

    private Category categoryOf(
            NamespacedKey key
    ) {

        String name = key.getKey();

        if (COMMON_ENCHANTS.contains(name)) {
            return Category.COMMON;
        }

        if (TOOL_ENCHANTS.contains(name)) {
            return Category.TOOL;
        }

        if (ARMOR_ENCHANTS.contains(name)) {
            return Category.ARMOR;
        }

        /*
         * 알려진 무기 인챈트 및 나머지 등록 인챈트.
         * 신규 인챈트도 판매 목록에서 누락되지 않게 한다.
         */
        if (
                key.getNamespace().equals("minecraft")
                || key.getNamespace().equals("rpgcore")
        ) {
            return Category.WEAPON;
        }

        return Category.COMMON;
    }

    private void render(
            Player player,
            MenuHolder holder
    ) {

        Inventory inventory = holder.inventory;

        int pages = Math.max(
                1,
                (holder.offers.size() + PAGE_SIZE - 1)
                        / PAGE_SIZE
        );

        holder.page = Math.max(
                0,
                Math.min(holder.page, pages - 1)
        );

        inventory.clear();

        int start = holder.page * PAGE_SIZE;

        int end = Math.min(
                start + PAGE_SIZE,
                holder.offers.size()
        );

        for (int index = start; index < end; index++) {

            Offer offer = holder.offers.get(index);

            int slot = index - start;

            ShopItemDefinition definition =
                    definition(offer, slot);

            ItemStack item;

            try {
                item = itemProvider.create(
                        player,
                        definition
                );
            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Enchant shop item failed: "
                                + offer.enchantment.getKey()
                                + " / "
                                + exception.getMessage()
                );
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {

                meta.displayName(
                        offerName(offer)
                );

                meta.lore(
                        List.of(
                                Component.text(
                                        "구매 가격: "
                                                + offer.price
                                                + "원",
                                        NamedTextColor.GOLD
                                ),
                                Component.text(
                                        "클릭하여 구매",
                                        NamedTextColor.YELLOW
                                )
                        )
                );

                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
        }

        inventory.setItem(
                PREVIOUS_SLOT,
                button(
                        "이전 페이지",
                        1002,
                        holder.page > 0
                                ? "이전 페이지로 이동"
                                : "첫 페이지입니다.",
                        NamedTextColor.YELLOW
                )
        );

        inventory.setItem(
                STATUS_SLOT,
                statusButton(
                        holder.offers.size(),
                        holder.page + 1,
                        pages
                )
        );

        inventory.setItem(
                BACK_SLOT,
                button(
                        "뒤로 가기",
                        1004,
                        "인챈트북 종류 선택 화면으로 돌아갑니다.",
                        NamedTextColor.YELLOW
                )
        );

        inventory.setItem(
                CLOSE_SLOT,
                button(
                        "닫기",
                        1007,
                        "인챈트북 상점을 닫습니다.",
                        NamedTextColor.RED
                )
        );

        inventory.setItem(
                NEXT_SLOT,
                button(
                        "다음 페이지",
                        1000,
                        holder.page + 1 < pages
                                ? "다음 페이지로 이동"
                                : "마지막 페이지입니다.",
                        NamedTextColor.YELLOW
                )
        );
    }

    private Component offerName(Offer offer) {

        NamespacedKey key = offer.enchantment.getKey();

        Component name;

        name = switch (key.toString()) {

            case "rpgcore:forest_lumberjack" ->
                    Component.text("나무꾼의 풍요");

            case "rpgcore:logging_wave" ->
                    Component.text("벌목의 파동");

            default -> Component.translatable(
                    "enchantment."
                            + key.getNamespace()
                            + "."
                            + key.getKey()
            );
        };

        boolean showLevel =
                offer.level > 1
                || key.toString().equals("rpgcore:forest_lumberjack")
                || key.toString().equals("rpgcore:logging_wave");

        if (showLevel) {
            name = name.append(
                    Component.text(
                            " " + roman(offer.level)
                    )
            );
        }

        return name.color(NamedTextColor.AQUA);
    }

    private String roman(int level) {

        String[] numbers = {
                "",
                "I", "II", "III", "IV", "V",
                "VI", "VII", "VIII", "IX", "X"
        };

        if (level > 0 && level < numbers.length) {
            return numbers[level];
        }

        return Integer.toString(level);
    }

    private ShopItemDefinition definition(
            Offer offer,
            int slot
    ) {

        String key =
                offer.enchantment.getKey().toString();

        return ShopItemDefinition.fixedBuy(
                key + "@" + offer.level,
                key + " " + roman(offer.level),
                ShopItemSource.ENCHANT_BOOK,
                key + "@" + offer.level,
                slot,
                1,
                offer.price
        );
    }

    private ItemStack categoryButton(
            Category category
    ) {

        ItemStack item = new ItemStack(category.icon);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.text(
                        category.title,
                        NamedTextColor.AQUA
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                "클릭하여 구매 목록을 엽니다.",
                                NamedTextColor.GRAY
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack button(
            String name,
            int model,
            String description,
            NamedTextColor color
    ) {

        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setCustomModelData(model);

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

    private ItemStack statusButton(
            int count,
            int page,
            int pages
    ) {

        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.text(
                        "인챈트북 목록",
                        NamedTextColor.AQUA
                )
        );

        meta.lore(
                List.of(
                        Component.text(
                                count + "개 / "
                                        + page + " / "
                                        + pages + " 페이지",
                                NamedTextColor.GRAY
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    private void nextTick(
            Player player,
            Runnable action
    ) {

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {
                    if (player.isOnline()) {
                        action.run();
                    }
                }
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(
                event.getView()
                        .getTopInventory()
                        .getHolder()
                        instanceof MenuHolder holder
        )) {
            return;
        }

        event.setCancelled(true);

        if (!(
                event.getWhoClicked()
                        instanceof Player player
        )) {
            return;
        }

        int slot = event.getRawSlot();

        if (
                slot < 0
                        || slot >= holder.inventory.getSize()
        ) {
            return;
        }

        if (holder.category == null) {

            switch (slot) {

                case WEAPON_SLOT ->
                        nextTick(
                                player,
                                () -> openCategory(
                                        player,
                                        Category.WEAPON
                                )
                        );

                case ARMOR_SLOT ->
                        nextTick(
                                player,
                                () -> openCategory(
                                        player,
                                        Category.ARMOR
                                )
                        );

                case TOOL_SLOT ->
                        nextTick(
                                player,
                                () -> openCategory(
                                        player,
                                        Category.TOOL
                                )
                        );

                case COMMON_SLOT ->
                        nextTick(
                                player,
                                () -> openCategory(
                                        player,
                                        Category.COMMON
                                )
                        );

                case BACK_SLOT ->
                        nextTick(
                                player,
                                player::closeInventory
                        );

                default -> {
                }
            }

            return;
        }

        if (slot < PAGE_SIZE) {

            int index =
                    holder.page * PAGE_SIZE + slot;

            if (index >= holder.offers.size()) {
                return;
            }

            Offer offer = holder.offers.get(index);

            ShopItemDefinition definition =
                    definition(offer, slot);

            ShopDefinition shop =
                    new ShopDefinition(
                            "npc19_enchant_" +
                                    holder.category.name()
                                            .toLowerCase(),
                            holder.category.title,
                            ShopType.BUY,
                            3,
                            List.of(definition)
                    );

            transactionService.buy(
                    player,
                    shop,
                    definition
            );

            return;
        }

        switch (slot) {

            case PREVIOUS_SLOT -> {
                if (holder.page > 0) {
                    holder.page--;
                    render(player, holder);
                }
            }

            case NEXT_SLOT -> {

                int pages = Math.max(
                        1,
                        (holder.offers.size()
                                + PAGE_SIZE - 1)
                                / PAGE_SIZE
                );

                if (holder.page + 1 < pages) {
                    holder.page++;
                    render(player, holder);
                }
            }

            case BACK_SLOT ->
                    nextTick(
                            player,
                            () -> openCategoryMenu(player)
                    );

            case CLOSE_SLOT ->
                    nextTick(
                            player,
                            player::closeInventory
                    );

            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(
            InventoryDragEvent event
    ) {

        if (!(
                event.getView()
                        .getTopInventory()
                        .getHolder()
                        instanceof MenuHolder holder
        )) {
            return;
        }

        if (
                event.getRawSlots()
                        .stream()
                        .anyMatch(
                                slot ->
                                        slot < holder.inventory.getSize()
                        )
        ) {
            event.setCancelled(true);
        }
    }
}
