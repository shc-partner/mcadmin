package com.hcs.rpgcore.starter;

import java.util.List;
import java.util.UUID;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

public final class GuideNpcListener implements Listener {

    private static final int NPC_ID = 21;

    private static final int WELCOME_SLOT = 12;
    private static final int INFORMATION_SLOT = 14;
    private static final int ADVENTURE_SLOT = 30;
    private static final int GUIDE_BOOK_SLOT = 32;

    private static final class GuideHolder
            implements InventoryHolder {

        private final UUID owner;
        private Inventory inventory;

        private GuideHolder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {

        if (event.getNPC().getId() != NPC_ID) {
            return;
        }

        Player player = event.getClicker();

        GuideHolder holder =
                new GuideHolder(player.getUniqueId());

        Inventory inventory = Bukkit.createInventory(
                holder,
                45,
                Component.text(
                        "마을회관 안내",
                        NamedTextColor.DARK_AQUA
                )
        );

        holder.inventory = inventory;

        ItemStack background = new ItemStack(
                Material.BLACK_STAINED_GLASS_PANE
        );

        ItemMeta backgroundMeta = background.getItemMeta();

        if (backgroundMeta != null) {
            backgroundMeta.displayName(Component.text(" "));
            background.setItemMeta(backgroundMeta);
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, background.clone());
        }

        inventory.setItem(
                WELCOME_SLOT,
                sign(
                        "모험가 월드에 오신 것을 환영합니다!",
                        List.of(
                                "이곳은 모험의 시작점인",
                                "모험가 월드 마을회관입니다."
                        )
                )
        );

        inventory.setItem(
                INFORMATION_SLOT,
                sign(
                        "마을회관 이용 안내",
                        List.of(
                                "먼저 모험가 가이드를 읽고,",
                                "마을회관의 상점과 제작 NPC를",
                                "둘러보세요."
                        )
                )
        );

        inventory.setItem(
                ADVENTURE_SLOT,
                sign(
                        "모험을 시작할 준비가 되셨나요?",
                        List.of(
                                "준비를 마쳤다면",
                                "자신만의 모험을 시작해 보세요!"
                        )
                )
        );

        ItemStack guideBook =
                StarterKitListener.createGuideBook();

        ItemMeta guideMeta = guideBook.getItemMeta();

        if (guideMeta != null) {
            guideMeta.lore(
                    List.of(
                            Component.text(
                                    "가이드북을 잃어버렸거나",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "다시 읽고 싶다면 클릭해 받으세요.",
                                    NamedTextColor.GRAY
                            ),
                            Component.text(
                                    "클릭 시 가이드북 1권 지급",
                                    NamedTextColor.YELLOW
                            )
                    )
            );

            guideBook.setItemMeta(guideMeta);
        }

        inventory.setItem(GUIDE_BOOK_SLOT, guideBook);

        player.openInventory(inventory);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onInventoryClick(InventoryClickEvent event) {

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof GuideHolder holder)) {
            return;
        }

        // 안내 GUI가 열린 동안 아이템 이동을 전부 차단한다.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.owner.equals(player.getUniqueId())) {
            return;
        }

        if (event.getClickedInventory() != top) {
            return;
        }

        if (event.getRawSlot() != GUIDE_BOOK_SLOT) {
            return;
        }

        if (!event.isLeftClick() && !event.isRightClick()) {
            return;
        }

        giveGuideBook(player);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder()
                instanceof GuideHolder) {
            event.setCancelled(true);
        }
    }

    private void giveGuideBook(Player player) {

        int emptySlot = -1;

        // 일반 인벤토리와 핫바의 36칸만 검사한다.
        for (int slot = 0; slot < 36; slot++) {

            ItemStack item =
                    player.getInventory().getItem(slot);

            if (item == null || item.getType().isAir()) {
                emptySlot = slot;
                break;
            }
        }

        if (emptySlot < 0) {

            player.sendMessage(
                    "§c[가이드] 인벤토리 공간이 부족합니다. "
                    + "빈칸을 확보한 뒤 다시 시도해주세요."
            );

            return;
        }

        ItemStack guideBook =
                StarterKitListener.createGuideBook();

        player.getInventory().setItem(
                emptySlot,
                guideBook
        );

        player.sendMessage(
                "§a[가이드] §f모험가 가이드 1권을 지급했습니다."
        );
    }

    private ItemStack sign(
            String title,
            List<String> description
    ) {

        ItemStack item = new ItemStack(
                Material.PALE_OAK_HANGING_SIGN
        );

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.displayName(
                    Component.text(
                            title,
                            NamedTextColor.YELLOW
                    )
            );

            meta.lore(
                    description.stream()
                            .map(line -> Component.text(
                                    line,
                                    NamedTextColor.WHITE
                            ))
                            .toList()
            );

            item.setItemMeta(meta);
        }

        return item;
    }
}
