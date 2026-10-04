package com.hcs.rpgcore.shop;

import net.citizensnpcs.api.event.NPCRightClickEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


/**
 * Citizens 상점 NPC 우클릭 처리.
 *
 * NPC 2 -> farm_buy
 * NPC 3 -> farm_sell
 * NPC 8 -> farm_fertilizer
 * NPC 9 -> farm_tools
 */
public final class ShopNpcListener
        implements Listener {

    private final ShopGuiService
            shopGuiService;

    private final com.hcs.rpgcore.dismantle.EquipmentDismantleListener
            dismantleListener;

    private final com.hcs.rpgcore.dismantle.EquipmentDismantleGui
            dismantleGui =
            new com.hcs.rpgcore.dismantle.EquipmentDismantleGui();


    public ShopNpcListener(
            ShopGuiService shopGuiService,
            com.hcs.rpgcore.dismantle.EquipmentDismantleListener
                    dismantleListener
    ) {

        this.shopGuiService = shopGuiService;
        this.dismantleListener = dismantleListener;
    }


    @EventHandler
    public void onNpcRightClick(
            NPCRightClickEvent event
    ) {

        int npcId =
                event.getNPC()
                        .getId();


        if (npcId == 17) {

            dismantleListener.openOrReturn(
                    event.getClicker()
            );

            return;
        }

        String shopId =
                switch (npcId) {

                    case 2 ->
                            ShopRegistry.FARM_BUY;

                    case 3 ->
                            ShopRegistry.FARM_SELL;

                    case 8 ->
                            ShopRegistry.FARM_FERTILIZER;

                    case 9 ->
                            ShopRegistry.FARM_TOOLS;

                    case 11 ->
                            ShopRegistry.EQUIPMENT_MATERIALS;

                    default ->
                            null;
                };


        if (shopId == null) {
            return;
        }


        Player player =
                event.getClicker();


        shopGuiService.open(
                player,
                shopId
        );
    }
}
