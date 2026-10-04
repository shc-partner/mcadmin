package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;

import org.bukkit.inventory.Inventory;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import org.bukkit.inventory.ItemStack;


public final class SkillItemProtectionListener
        implements Listener {

    private final RPGCorePlugin plugin;

    private final SkillItemFactory skillItemFactory;


    /*
     * 사망 순간 자신의 귀속 스킬북을 death drops에서 제거하고
     * 리스폰 후 돌려주기 위한 임시 저장소.
     *
     * 정상 리스폰/로그아웃 시 정리된다.
     */
    private final Map<UUID, List<ItemStack>> retainedBooks =
            new ConcurrentHashMap<>();


    public SkillItemProtectionListener(
            RPGCorePlugin plugin,
            SkillItemFactory skillItemFactory
    ) {

        this.plugin =
                plugin;

        this.skillItemFactory =
                skillItemFactory;
    }


    /*
     * =========================================================
     * MANUAL DROP BLOCK
     * =========================================================
     *
     * Q키, Ctrl+Q, 인벤토리 밖으로 버리기 등으로
     * PlayerDropItemEvent가 발생하면 스킬북은 취소한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onDrop(
            PlayerDropItemEvent event
    ) {

        ItemStack item =
                event.getItemDrop()
                        .getItemStack();


        if (!skillItemFactory.isSkillItem(item)) {
            return;
        }


        /*
         * =====================================================
         * CLASS SKILL TOME
         * =====================================================
         *
         * 직업 통합 스킬북은 영구 도감 아이템.
         *
         * 드롭 취소:
         *   - 소멸하지 않음
         *   - 플레이어 인벤토리에 그대로 유지
         */
        if (
                skillItemFactory.isClassSkillTome(
                        item
                )
        ) {

            event.setCancelled(
                    true
            );


            event.getPlayer()
                    .sendActionBar(
                            Component.text(
                                    "직업 스킬북은 버릴 수 없습니다.",
                                    NamedTextColor.RED
                            )
                    );

            return;
        }


        /*
         * =====================================================
         * INDIVIDUAL SKILL ITEM
         * =====================================================
         *
         * 도감에서 꺼낸 개별 스킬은
         * 드롭을 장착 해제로 처리한다.
         *
         * PlayerDropItemEvent는 취소하지 않고
         * 생성된 Item Entity만 즉시 제거한다.
         */
        event.getItemDrop()
                .remove();


        event.getPlayer()
                .sendActionBar(
                        Component.text(
                                "스킬 장착이 해제되었습니다.",
                                NamedTextColor.YELLOW
                        )
                );
    }


    /*
     * =========================================================
     * DEATH PROTECTION
     * =========================================================
     */
    
/*
 * =========================================================
 * EXTERNAL INVENTORY STORAGE BLOCK
 * =========================================================
 *
 * RPGCore 귀속 스킬북은 플레이어 자신의
 * 인벤토리 밖으로 이동할 수 없다.
 *
 * 차단:
 *
 * - 일반 상자
 * - 셜커 상자
 * - 엔더 상자
 * - 배럴
 * - 호퍼
 * - 화로류
 * - 디스펜서
 * - 드로퍼
 * - 기타 외부 인벤토리
 *
 * 플레이어 자신의 인벤토리 / 핫바 내부 이동은 허용.
 */
@EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = false
)
public void onInventoryClick(
        InventoryClickEvent event
) {

    if (
            !(event.getWhoClicked()
                    instanceof Player player)
    ) {
        return;
    }


    Inventory topInventory =
            event.getView()
                    .getTopInventory();


    /*
     * 플레이어 자신의 기본 인벤토리 화면.
     *
     * 이 경우 외부 창고가 아니므로 허용.
     */
    if (
            topInventory.getType()
                    == InventoryType.CRAFTING
    ) {
        return;
    }


    /*
     * =====================================================
     * CURSOR -> EXTERNAL INVENTORY
     * =====================================================
     */
    if (
            event.getClickedInventory()
                    == topInventory
            && skillItemFactory
                    .isSkillItem(
                            event.getCursor()
                    )
    ) {

        blockStorageMove(
                player,
                event
        );

        return;
    }


    /*
     * =====================================================
     * SHIFT CLICK
     * =====================================================
     *
     * 플레이어 인벤토리의 스킬북을
     * Shift+클릭으로 외부 창고에 넣는 것을 차단.
     */
    if (
            event.getAction()
                    == InventoryAction
                            .MOVE_TO_OTHER_INVENTORY

            && event.getClickedInventory()
                    == event.getView()
                            .getBottomInventory()

            && skillItemFactory
                    .isSkillItem(
                            event.getCurrentItem()
                    )
    ) {

        blockStorageMove(
                player,
                event
        );

        return;
    }


    /*
     * =====================================================
     * HOTBAR NUMBER KEY SWAP
     * =====================================================
     *
     * 외부 인벤토리 슬롯 위에서
     * 숫자키 1~9로 핫바 스킬북을 넣는 우회 차단.
     */
    if (
            event.getClickedInventory()
                    == topInventory

            && event.getHotbarButton() >= 0
    ) {

        ItemStack hotbarItem =
                player.getInventory()
                        .getItem(
                                event.getHotbarButton()
                        );


        if (
                skillItemFactory
                        .isSkillItem(
                                hotbarItem
                        )
        ) {

            blockStorageMove(
                    player,
                    event
            );

            return;
        }
    }
}


/*
 * =========================================================
 * INVENTORY DRAG BLOCK
 * =========================================================
 *
 * 드래그로 외부 인벤토리 슬롯에 넣는 우회 차단.
 */
@EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = false
)
public void onInventoryDrag(
        InventoryDragEvent event
) {

    if (
            !(event.getWhoClicked()
                    instanceof Player player)
    ) {
        return;
    }


    if (
            !skillItemFactory
                    .isSkillItem(
                            event.getOldCursor()
                    )
    ) {
        return;
    }


    Inventory topInventory =
            event.getView()
                    .getTopInventory();


    if (
            topInventory.getType()
                    == InventoryType.CRAFTING
    ) {
        return;
    }


    int topSize =
            topInventory.getSize();


    boolean touchesExternalInventory =
            event.getRawSlots()
                    .stream()
                    .anyMatch(
                            rawSlot ->
                                    rawSlot < topSize
                    );


    if (!touchesExternalInventory) {
        return;
    }


    event.setCancelled(
            true
    );


    player.sendActionBar(
            Component.text(
                    "귀속 스킬북은 창고에 보관할 수 없습니다.",
                    NamedTextColor.RED
            )
    );
}


/*
 * =========================================================
 * NON-OWNER PICKUP BLOCK
 * =========================================================
 *
 * 예외적으로 월드에 생성된 스킬북도
 * 원래 소유자가 아니면 획득할 수 없다.
 */
@EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = false
)
public void onPickup(
        EntityPickupItemEvent event
) {

    if (
            !(event.getEntity()
                    instanceof Player player)
    ) {
        return;
    }


    ItemStack item =
            event.getItem()
                    .getItemStack();


    if (
            !skillItemFactory
                    .isSkillItem(
                            item
                    )
    ) {
        return;
    }


    if (
            skillItemFactory
                    .isOwnedBy(
                            item,
                            player.getUniqueId()
                    )
    ) {
        return;
    }


    event.setCancelled(
            true
    );


    player.sendActionBar(
            Component.text(
                    "다른 플레이어에게 귀속된 스킬북입니다.",
                    NamedTextColor.RED
            )
    );
}


/*
 * =========================================================
 * STORAGE BLOCK MESSAGE
 * =========================================================
 */
private void blockStorageMove(
        Player player,
        InventoryClickEvent event
) {

    event.setCancelled(
            true
    );


    player.sendActionBar(
            Component.text(
                    "귀속 스킬북은 창고에 보관할 수 없습니다.",
                    NamedTextColor.RED
            )
    );
}


@EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onDeath(
            PlayerDeathEvent event
    ) {

        Player player =
                event.getEntity();


        UUID uuid =
                player.getUniqueId();


        /*
         * keepInventory=true라면 원래 인벤토리에 남으므로
         * 따로 저장하면 복제된다.
         */
        if (event.getKeepInventory()) {

            retainedBooks.remove(
                    uuid
            );

            return;
        }


        List<ItemStack> restore =
                new ArrayList<>();


        /*
         * death drops에서 모든 RPGCore 스킬북을 제거한다.
         *
         * 자신의 귀속 책:
         *   리스폰 후 복구
         *
         * 남의 책 / 구형 owner_uuid 없는 책:
         *   복구하지 않고 제거
         */
        event.getDrops()
                .removeIf(
                        item -> {

                            if (
                                    !skillItemFactory
                                            .isSkillItem(
                                                    item
                                            )
                            ) {
                                return false;
                            }


                            if (
                                    skillItemFactory
                                            .isOwnedBy(
                                                    item,
                                                    uuid
                                            )
                            ) {

                                restore.add(
                                        item.clone()
                                );
                            }


                            return true;
                        }
                );


        if (restore.isEmpty()) {

            retainedBooks.remove(
                    uuid
            );

            return;
        }


        retainedBooks.put(
                uuid,
                restore
        );
    }


    /*
     * =========================================================
     * RESPAWN RESTORE
     * =========================================================
     */
    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        UUID uuid =
                event.getPlayer()
                        .getUniqueId();


        List<ItemStack> restore =
                retainedBooks.remove(
                        uuid
                );


        if (
                restore == null
                || restore.isEmpty()
        ) {
            return;
        }


        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> restoreBooks(
                                uuid,
                                restore
                        )
                );
    }


    private void restoreBooks(
            UUID uuid,
            List<ItemStack> books
    ) {

        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        if (
                player == null
                || !player.isOnline()
        ) {
            return;
        }


        for (ItemStack book : books) {

            Map<Integer, ItemStack> leftovers =
                    player.getInventory()
                            .addItem(
                                    book
                            );


            /*
             * 보통 사망 후에는 인벤토리에 자리가 있지만,
             * 다른 플러그인/keep 기능 때문에 공간이 없을 경우
             * 본인만 주울 수 있는 Item entity로 생성한다.
             */
            for (
                    ItemStack leftover
                    : leftovers.values()
            ) {

                Item droppedItem =
                        player.getWorld()
                                .dropItemNaturally(
                                        player.getLocation(),
                                        leftover
                                );


                droppedItem.setOwner(
                        uuid
                );
            }
        }


        player.sendMessage(
                Component.text(
                        "귀속 스킬북이 사망 보호로 복구되었습니다.",
                        NamedTextColor.YELLOW
                )
        );
    }


    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        /*
         * 정상적으로 리스폰하지 않고 종료한 경우
         * 메모리에 계속 남지 않도록 정리한다.
         *
         * 일반적인 Minecraft 사망 화면에서는
         * 리스폰 이벤트 후 정상 복구된다.
         */
        retainedBooks.remove(
                event.getPlayer()
                        .getUniqueId()
        );
    }
}
