package com.hcs.rpgcore.shop;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;


/**
 * 범용 상점 Inventory 이벤트.
 */
public final class ShopListener
        implements Listener {

    private final ShopRegistry
            shopRegistry;

    private final ShopTransactionService
            transactionService;


    public ShopListener(
            ShopRegistry shopRegistry,
            ShopTransactionService transactionService
    ) {

        this.shopRegistry = shopRegistry;
        this.transactionService = transactionService;
    }


    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(
                event.getView()
                        .getTopInventory()
                        .getHolder()
                        instanceof ShopInventoryHolder holder
        )) {
            return;
        }


        /*
         * 상점이 열려 있는 동안에는
         * shift-click 등으로 아이템이 GUI 안으로 들어가는 것도 막는다.
         */
        event.setCancelled(
                true
        );


        if (!(
                event.getWhoClicked()
                        instanceof Player player
        )) {
            return;
        }


        int rawSlot =
                event.getRawSlot();


        /*
         * 플레이어 인벤토리 영역 클릭은 거래하지 않는다.
         */
        if (
                rawSlot < 0
                        || rawSlot >= event.getView()
                                .getTopInventory()
                                .getSize()
        ) {
            return;
        }


        ShopDefinition shop =
                shopRegistry.get(
                        holder.shopId()
                );


        if (shop == null) {
            return;
        }


        ShopItemDefinition definition =
                shop.findBySlot(
                        rawSlot
                );


        if (definition == null) {
            return;
        }


        if (
                shop.type() == ShopType.BUY
                        || shop.type() == ShopType.BOTH
        ) {

            transactionService.buy(
                    player,
                    shop,
                    definition
            );

            return;
        }


        if (
                shop.type() == ShopType.SELL
        ) {

            transactionService.sell(
                    player,
                    shop,
                    definition
            );
        }
    }


    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onDrag(
            InventoryDragEvent event
    ) {

        if (!(
                event.getView()
                        .getTopInventory()
                        .getHolder()
                        instanceof ShopInventoryHolder
        )) {
            return;
        }


        int topSize =
                event.getView()
                        .getTopInventory()
                        .getSize();


        boolean touchesTop =
                event.getRawSlots()
                        .stream()
                        .anyMatch(
                                slot ->
                                        slot < topSize
                        );


        if (touchesTop) {

            event.setCancelled(
                    true
            );
        }
    }
}
