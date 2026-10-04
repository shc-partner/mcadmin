package com.hcs.rpgcore.item;

import org.bukkit.NamespacedKey;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.Plugin;


/*
 * =============================================================
 * NETHER LORD ARMOR
 * =============================================================
 *
 * 각 부위:
 *
 * - 화염 피해 감소 10%
 *
 * 1부위  = 10%
 * 2부위  = 20%
 * 3부위  = 30%
 * 4부위  = 40%
 *
 *
 * 네더 군주 방어구를 하나 이상 착용:
 *
 * - 일반 Piglin 자연 선공 방지
 * - Piglin Brute에는 적용하지 않음
 *
 * =============================================================
 */
public final class NetherLordArmorListener
        implements Listener {

    private static final double
            FIRE_REDUCTION_PER_PIECE =
            0.10D;


    private final NamespacedKey
            customItemIdKey;


    public NetherLordArmorListener(
            Plugin plugin
    ) {

        this.customItemIdKey =
                new NamespacedKey(
                        plugin,
                        "custom_item_id"
                );
    }


    /*
     * =========================================================
     * FIRE DAMAGE REDUCTION
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onFireDamage(
            EntityDamageEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof Player player)
        ) {
            return;
        }


        if (!isFireDamage(event)) {
            return;
        }


        int pieces =
                countNetherLordArmor(
                        player
                );

        if (pieces <= 0) {
            return;
        }


        double reduction =
                Math.min(
                        0.40D,
                        pieces
                                * FIRE_REDUCTION_PER_PIECE
                );

        double finalDamage =
                event.getDamage()
                        * (1.0D - reduction);


        event.setDamage(
                Math.max(
                        0.0D,
                        finalDamage
                )
        );
    }


    /*
     * =========================================================
     * PIGLIN NEUTRAL
     * =========================================================
     *
     * 금 갑옷의 기본 중립 효과처럼
     * Piglin의 일반적인 플레이어 선공만 차단한다.
     *
     * 공격 등으로 이미 적대 관계가 형성되는 상황까지
     * 무조건 취소하지 않는다.
     *
     * PiglinBrute는 Piglin 클래스가 아니므로 제외된다.
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPiglinTarget(
            EntityTargetLivingEntityEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof Piglin)
        ) {
            return;
        }


        if (
                !(event.getTarget()
                        instanceof Player player)
        ) {
            return;
        }


        if (
                event.getReason()
                        != EntityTargetEvent.TargetReason
                                .CLOSEST_PLAYER
        ) {
            return;
        }


        if (
                countNetherLordArmor(
                        player
                ) <= 0
        ) {
            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * FIRE DAMAGE TYPE
     * =========================================================
     */
    private boolean isFireDamage(
            EntityDamageEvent event
    ) {

        EntityDamageEvent.DamageCause cause =
                event.getCause();


        if (
                cause
                        == EntityDamageEvent
                                .DamageCause.FIRE
                || cause
                        == EntityDamageEvent
                                .DamageCause.FIRE_TICK
                || cause
                        == EntityDamageEvent
                                .DamageCause.LAVA
                || cause
                        == EntityDamageEvent
                                .DamageCause.HOT_FLOOR
        ) {
            return true;
        }


        /*
         * Ghast / Blaze 등의 화염구 직접 피해.
         */
        if (
                event instanceof
                        EntityDamageByEntityEvent
                                damageByEntityEvent
                && damageByEntityEvent
                        .getDamager()
                        instanceof Fireball
        ) {
            return true;
        }


        return false;
    }


    /*
     * =========================================================
     * EQUIPPED PIECE COUNT
     * =========================================================
     */
    private int countNetherLordArmor(
            Player player
    ) {

        int count =
                0;


        for (
                ItemStack item
                : player.getInventory()
                        .getArmorContents()
        ) {

            if (isNetherLordArmor(item)) {

                count++;
            }
        }


        return count;
    }


    /*
     * =========================================================
     * ITEM CHECK
     * =========================================================
     */
    private boolean isNetherLordArmor(
            ItemStack item
    ) {

        if (
                item == null
                || item.getType().isAir()
        ) {
            return false;
        }


        ItemMeta meta =
                item.getItemMeta();

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

        if (itemId == null) {
            return false;
        }


        return
                CustomItemIds.NETHER_LORD_HELMET
                        .equals(itemId)
                || CustomItemIds
                        .NETHER_LORD_CHESTPLATE
                        .equals(itemId)
                || CustomItemIds
                        .NETHER_LORD_LEGGINGS
                        .equals(itemId)
                || CustomItemIds.NETHER_LORD_BOOTS
                        .equals(itemId);
    }
}
