package com.hcs.rpgcore.dungeon;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;


/*
 * ============================================================
 * MINOTAUR EFFECTIVE HP
 * ============================================================
 *
 * 실제 Entity HP:
 * 1024
 *
 * RPG 실질 HP:
 * 1600
 *
 * 모든 피해를
 *
 * 1024 / 1600
 *
 * 비율로 보정한다.
 */
public final class MinotaurDamageListener
        implements Listener {

    private static final double DAMAGE_SCALE =
            MinotaurDungeonService.BOSS_VANILLA_HEALTH
                    / MinotaurDungeonService.BOSS_EFFECTIVE_HEALTH;


    private final MinotaurDungeonService dungeonService;


    public MinotaurDamageListener(
            MinotaurDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBossDamage(
            EntityDamageEvent event
    ) {

        if (!(event.getEntity()
                instanceof LivingEntity entity)) {

            return;
        }


        if (!dungeonService
                .isMinotaur(entity)) {

            return;
        }


        event.setDamage(
                event.getDamage()
                        * DAMAGE_SCALE
        );
    }
}
