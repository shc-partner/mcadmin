package com.hcs.rpgcore.dungeon;

import org.bukkit.Location;

import org.bukkit.entity.LivingEntity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;


public final class ShulkerDungeonMobListener
        implements Listener {

    private final ShulkerDungeonService
            dungeonService;


    public ShulkerDungeonMobListener(
            ShulkerDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * DUNGEON SHULKER DEATH
     * =========================================================
     */
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
         * 던전 셜커 자체의
         * 바닐라 셜커 껍데기 드롭 차단.
         */
        event.getDrops()
                .clear();


        /*
         * 바닐라 녹색 EXP Orb 차단.
         */
        event.setDroppedExp(
                0
        );


        dungeonService
                .onDungeonMobDeath(
                        entity
                );
    }


    /*
     * =========================================================
     * SHULKER TELEPORT GUARD
     * =========================================================
     *
     * 던전 셜커가 외벽 밖의 블록으로
     * 텔레포트하는 것을 플러그인 레벨에서 차단한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityTeleport(
            EntityTeleportEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof LivingEntity living)
        ) {
            return;
        }


        if (!dungeonService
                .isDungeonMob(living)) {

            return;
        }


        Location destination =
                event.getTo();


        if (
                destination == null
                || !dungeonService
                        .isInsideDungeon(
                                destination
                        )
        ) {

            event.setCancelled(
                    true
            );
        }
    }
}
