package com.hcs.rpgcore.dungeon;

import org.bukkit.entity.LivingEntity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class RedDragonDungeonMobListener
        implements Listener {

    private final RedDragonDungeonService dungeonService;


    public RedDragonDungeonMobListener(
            RedDragonDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * FULL DUNGEON SPAWN CONTROL
     * =========================================================
     *
     * 기반암 내부 전체에서는
     * RPGCore가 직접 승인한 CUSTOM spawn만 허용한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onCreatureSpawn(
            CreatureSpawnEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getLocation()
                )) {

            return;
        }


        if (
                dungeonService
                        .consumeAuthorizedDungeonSpawn(
                                event
                        )
        ) {

            return;
        }


        /*
         * Drako 사망 연출용 MythicMob은
         * 원래 보스 위치에 별도의 HUSK를 CUSTOM spawn한다.
         *
         * 해당 연출 엔티티만 제한적으로 허용한다.
         */
        if (
                dungeonService
                        .isAllowedDrakoDeathVisualSpawn(
                                event
                        )
        ) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onEntityDeath(
            EntityDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();


        if (!dungeonService
                .isDungeonMob(entity)) {

            return;
        }


        /*
         * 바닐라 드롭 / EXP를 레이드 보상으로 사용하지 않는다.
         */
        event.getDrops()
                .clear();

        event.setDroppedExp(
                0
        );


        dungeonService.onDungeonMobDeath(
                entity
        );
    }
}
