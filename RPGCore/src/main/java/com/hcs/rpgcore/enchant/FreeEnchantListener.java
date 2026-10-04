package com.hcs.rpgcore.enchant;

import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public final class FreeEnchantListener implements Listener {

    /**
     * 인챈트 테이블에 표시되는 각 선택지의 요구 경험치 레벨을 0으로 만듭니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        EnchantmentOffer[] offers = event.getOffers();

        for (EnchantmentOffer offer : offers) {
            if (offer == null) {
                continue;
            }

            offer.setCost(0);
        }
    }

    /**
     * 실제 인챈트 실행 시 소비되는 경험치 레벨을 0으로 만듭니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        event.setExpLevelCost(0);
    }

    /**
     * 모루의 경험치 레벨 비용을 0으로 만듭니다.
     * 재료 아이템 소비량은 바닐라 그대로 유지합니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        event.getView().setRepairCost(0);
    }
}
