package com.hcs.rpgcore.dungeon;

import org.bukkit.entity.LivingEntity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MinotaurDungeonMobListener
        implements Listener {

    private final MinotaurDungeonService dungeonService;


    public MinotaurDungeonMobListener(
            MinotaurDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * DUNGEON SPAWN CONTROL
     * =========================================================
     *
     * 미노타우로스 던전은 구조물이 매우 크고
     * 내부에 어두운 공간이 많다.
     *
     * 따라서 던전 전체 범위에서는
     * RPGCore가 직접 승인한 던전 spawn만 허용한다.
     *
     * NATURAL, CUSTOM 등 나머지 생성은 전부 차단한다.
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
         * 미노타우로스 던전 웨이브/보스는
         * 바닐라 드롭을 보상으로 사용하지 않는다.
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
