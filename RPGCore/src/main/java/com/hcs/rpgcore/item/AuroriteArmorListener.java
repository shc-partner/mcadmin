package com.hcs.rpgcore.item;

import com.hcs.rpgcore.dungeon.EndermanDungeonService;

import org.bukkit.NamespacedKey;

import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.Plugin;


/*
 * =============================================================
 * PURPLE AURORITE ARMOR
 * =============================================================
 *
 * Purple Aurorite Helmet:
 *
 * - 일반 Enderman의 자연적인 시선 선공 방지
 * - 플레이어가 먼저 공격한 뒤의 반격은 유지
 * - Enderman Dungeon 커스텀 몹에는 적용하지 않음
 *
 * =============================================================
 */
public final class AuroriteArmorListener
        implements Listener {

    private final NamespacedKey customItemIdKey;

    private final EndermanDungeonService
            endermanDungeonService;


    public AuroriteArmorListener(
            Plugin plugin,
            EndermanDungeonService endermanDungeonService
    ) {

        this.customItemIdKey =
                new NamespacedKey(
                        plugin,
                        "custom_item_id"
                );

        this.endermanDungeonService =
                endermanDungeonService;
    }


    /*
     * =========================================================
     * ENDERMAN PASSIVE EFFECT
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEndermanTarget(
            EntityTargetLivingEntityEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof Enderman enderman)
        ) {
            return;
        }

        if (
                !(event.getTarget()
                        instanceof Player player)
        ) {
            return;
        }


        /*
         * 엔더 던전 커스텀 Enderman / Boss는
         * Purple Aurorite 효과를 무시한다.
         */
        if (
                endermanDungeonService != null
                && endermanDungeonService
                        .isDungeonMob(enderman)
        ) {
            return;
        }


        /*
         * 자연적인 플레이어 인식만 차단한다.
         *
         * 플레이어에게 공격당한 후의 반격까지
         * 차단하지 않는다.
         */
        if (
                event.getReason()
                        != EntityTargetEvent.TargetReason
                                .CLOSEST_PLAYER
        ) {
            return;
        }


        if (!hasPurpleAuroriteHelmet(player)) {
            return;
        }

        event.setCancelled(true);
    }


    private boolean hasPurpleAuroriteHelmet(
            Player player
    ) {

        ItemStack helmet =
                player.getInventory()
                        .getHelmet();

        if (
                helmet == null
                || helmet.getType().isAir()
        ) {
            return false;
        }

        ItemMeta meta =
                helmet.getItemMeta();

        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();

        String itemId =
                pdc.get(
                        customItemIdKey,
                        PersistentDataType.STRING
                );

        return CustomItemIds
                .PURPLE_AURORITE_HELMET
                .equals(itemId);
    }
}
