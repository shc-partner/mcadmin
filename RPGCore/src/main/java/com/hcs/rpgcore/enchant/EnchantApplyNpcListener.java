package com.hcs.rpgcore.enchant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class EnchantApplyNpcListener implements Listener {

    public EnchantApplyNpcListener(
            org.bukkit.plugin.java.JavaPlugin plugin
    ) {
        // NPC 20번은 아이템 원본을 플레이어 인벤토리에 유지한다.
        // 현재 구현에서는 플러그인 인스턴스를 별도로 보관하지 않는다.
    }

    private static final int NPC_ID = 20;

    private static final int EQUIPMENT_SLOT = 11;
    private static final int PREVIEW_SLOT = 13;
    private static final int BOOK_SLOT = 15;
    private static final int APPLY_SLOT = 22;

    private static final String REMOVED_ENCHANT =
            "rpgcore:auto_seed";

    /*
     * 선택한 원본의 인벤토리 위치와 당시 상태를 기억한다.
     * 실제 아이템은 GUI로 이동하지 않는다.
     */
    private record Selection(
            int slot,
            ItemStack snapshot
    ) {}

    private static final class ApplyHolder
            implements InventoryHolder {

        private final UUID owner;

        private Inventory inventory;
        private Selection equipment;
        private Selection book;

        private ApplyHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onNpcClick(NPCRightClickEvent event) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        Player player = event.getClicker();

        ApplyHolder holder =
                new ApplyHolder(player.getUniqueId());

        Inventory inventory = Bukkit.createInventory(
                holder,
                27,
                Component.text("마법 부여")
        );

        holder.inventory = inventory;

        inventory.setItem(
                APPLY_SLOT,
                button(
                        Material.ANVIL,
                        "마법 부여 적용",
                        NamedTextColor.GREEN
                )
        );

        refresh(holder, player);
        player.openInventory(inventory);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onClick(InventoryClickEvent event) {

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof ApplyHolder holder)) {
            return;
        }

        /*
         * GUI와 플레이어 인벤토리의 모든 기본 이동을 차단한다.
         * 따라서 미리보기 아이템을 꺼낼 수 없다.
         */
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.owner.equals(player.getUniqueId())) {
            return;
        }

        if (event.getClickedInventory() == top) {

            if (event.getRawSlot() == APPLY_SLOT
                    && event.getClick() == ClickType.LEFT) {
                apply(holder, player);
            }

            return;
        }

        if (event.getClickedInventory()
                != player.getInventory()) {
            return;
        }

        if (event.getClick() != ClickType.LEFT) {
            return;
        }

        /*
         * 하단 인벤토리에서 원본을 직접 선택한다.
         * 0~35번 일반 인벤토리 슬롯만 허용한다.
         */
        int slot = event.getSlot();

        if (slot < 0 || slot >= 36) {
            return;
        }

        ItemStack clicked =
                player.getInventory().getItem(slot);

        if (empty(clicked)) {
            return;
        }

        if (clicked.getType() == Material.ENCHANTED_BOOK) {

            if (!(clicked.getItemMeta()
                    instanceof EnchantmentStorageMeta bookMeta)) {
                return;
            }

            if (bookMeta.getStoredEnchants().isEmpty()) {
                player.sendMessage("§c인챈트가 없는 책입니다.");
                return;
            }

            for (Enchantment enchantment
                    : bookMeta.getStoredEnchants().keySet()) {

                if (REMOVED_ENCHANT.equals(
                        enchantment.getKey().toString()
                )) {
                    player.sendMessage(
                            "§c제거된 자동 파종 인챈트는 적용할 수 없습니다."
                    );
                    return;
                }
            }

            holder.book = new Selection(
                    slot,
                    clicked.clone()
            );

        } else {

            /*
             * 장비는 1개짜리 아이템만 받는다.
             * 미리보기 아이템 자체를 투입하지 않는다.
             */
            if (clicked.getAmount() != 1
                    || clicked.getType().isAir()
                    || clicked.getItemMeta() == null) {
                return;
            }

            holder.equipment = new Selection(
                    slot,
                    clicked.clone()
            );
        }

        refresh(holder, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder()
                instanceof ApplyHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClose(InventoryCloseEvent event) {

        /*
         * 입력 아이템은 원래 플레이어 인벤토리에 있으므로
         * 반환하거나 바닥에 떨어뜨릴 필요가 없다.
         */
        if (event.getView().getTopInventory().getHolder()
                instanceof ApplyHolder holder) {

            holder.equipment = null;
            holder.book = null;
        }
    }

    private void refresh(
            ApplyHolder holder,
            Player player
    ) {

        Inventory inventory = holder.inventory;

        ItemStack equipment =
                selectedItem(player, holder.equipment);

        ItemStack book =
                selectedItem(player, holder.book);

        inventory.setItem(
                EQUIPMENT_SLOT,
                equipment == null
                        ? button(
                                Material.GRAY_STAINED_GLASS_PANE,
                                "인벤토리에서 장비 선택",
                                NamedTextColor.GRAY
                        )
                        : equipment.clone()
        );

        inventory.setItem(
                BOOK_SLOT,
                book == null
                        ? button(
                                Material.GRAY_STAINED_GLASS_PANE,
                                "인벤토리에서 인챈트북 선택",
                                NamedTextColor.GRAY
                        )
                        : book.clone()
        );

        if (equipment == null || book == null) {

            inventory.setItem(
                    PREVIEW_SLOT,
                    button(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "장비와 책을 선택하세요",
                            NamedTextColor.GRAY
                    )
            );

            return;
        }

        ItemStack result = createResult(
                equipment,
                book
        );

        inventory.setItem(
                PREVIEW_SLOT,
                result == null
                        ? button(
                                Material.BARRIER,
                                "마법 부여 불가",
                                NamedTextColor.RED
                        )
                        : result.clone()
        );
    }

    private ItemStack selectedItem(
            Player player,
            Selection selection
    ) {

        if (selection == null) {
            return null;
        }

        ItemStack current =
                player.getInventory().getItem(selection.slot());

        if (empty(current)
                || current.getAmount()
                        != selection.snapshot().getAmount()
                || !current.isSimilar(selection.snapshot())) {
            return null;
        }

        return current;
    }

    private ItemStack createResult(
            ItemStack equipment,
            ItemStack book
    ) {

        if (equipment == null
                || book == null
                || equipment.getAmount() != 1
                || book.getType() != Material.ENCHANTED_BOOK
                || !(book.getItemMeta()
                        instanceof EnchantmentStorageMeta bookMeta)) {
            return null;
        }

        Map<Enchantment, Integer> stored =
                bookMeta.getStoredEnchants();

        if (stored.isEmpty()) {
            return null;
        }

        ItemStack result = equipment.clone();
        ItemMeta meta = result.getItemMeta();

        if (meta == null) {
            return null;
        }

        Map<Enchantment, Integer> levels =
                new HashMap<>(meta.getEnchants());

        boolean changed = false;

        for (Map.Entry<Enchantment, Integer> entry
                : stored.entrySet()) {

            Enchantment enchantment = entry.getKey();
            int bookLevel = entry.getValue();

            if (REMOVED_ENCHANT.equals(
                    enchantment.getKey().toString()
            )) {
                return null;
            }

            if (bookLevel < 1
                    || bookLevel > enchantment.getMaxLevel()
                    || !enchantment.canEnchantItem(result)) {
                return null;
            }

            for (Enchantment existing : levels.keySet()) {

                if (!existing.equals(enchantment)
                        && existing.conflictsWith(enchantment)) {
                    return null;
                }
            }

            int current =
                    levels.getOrDefault(enchantment, 0);

            int next = current == bookLevel
                    ? Math.min(
                            enchantment.getMaxLevel(),
                            current + 1
                    )
                    : Math.max(current, bookLevel);

            if (next > current) {
                levels.put(enchantment, next);
                changed = true;
            }
        }

        if (!changed) {
            return null;
        }

        /*
         * 책에 여러 인챈트가 들어 있을 때
         * 새 인챈트끼리의 충돌도 검증한다.
         */
        for (Enchantment first : levels.keySet()) {

            for (Enchantment second : levels.keySet()) {

                if (!first.equals(second)
                        && first.conflictsWith(second)) {
                    return null;
                }
            }
        }

        for (Map.Entry<Enchantment, Integer> entry
                : levels.entrySet()) {

            if (!meta.addEnchant(
                    entry.getKey(),
                    entry.getValue(),
                    false
            ) && meta.getEnchantLevel(entry.getKey())
                    != entry.getValue()) {
                return null;
            }
        }

        result.setItemMeta(meta);

        return result;
    }

    private void apply(
            ApplyHolder holder,
            Player player
    ) {

        ItemStack equipment =
                selectedItem(player, holder.equipment);

        ItemStack book =
                selectedItem(player, holder.book);

        if (equipment == null || book == null) {

            player.sendMessage(
                    "§c장비 또는 인챈트북이 변경되었습니다. 다시 선택해주세요."
            );

            refresh(holder, player);
            return;
        }

        ItemStack result = createResult(
                equipment,
                book
        );

        if (result == null) {

            player.sendMessage(
                    "§c적용할 수 없는 인챈트이거나 레벨이 이미 최대입니다."
            );

            refresh(holder, player);
            return;
        }

        int equipmentSlot = holder.equipment.slot();
        int bookSlot = holder.book.slot();

        if (equipmentSlot == bookSlot) {
            return;
        }

        /*
         * 결과 장비는 기존 장비 슬롯을 교체한다.
         * 인챈트북은 같은 슬롯에서 정확히 1개만 소비한다.
         */
        player.getInventory().setItem(
                equipmentSlot,
                result
        );

        if (book.getAmount() == 1) {

            player.getInventory().setItem(
                    bookSlot,
                    null
            );

        } else {

            ItemStack remaining = book.clone();
            remaining.setAmount(book.getAmount() - 1);

            player.getInventory().setItem(
                    bookSlot,
                    remaining
            );
        }

        holder.equipment = null;
        holder.book = null;

        refresh(holder, player);

        player.sendMessage(
                "§a마법 부여가 완료되었습니다."
        );
    }

    private ItemStack button(
            Material material,
            String name,
            NamedTextColor color
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name, color));
            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean empty(ItemStack item) {

        return item == null
                || item.getType().isAir()
                || item.getAmount() < 1;
    }
}
