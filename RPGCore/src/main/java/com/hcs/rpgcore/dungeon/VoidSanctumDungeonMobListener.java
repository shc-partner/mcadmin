package com.hcs.rpgcore.dungeon;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDeathEvent;


/*
 * =========================================================
 * VOID SANCTUM DUNGEON MOB LISTENER
 * =========================================================
 *
 * Phase 1:
 * ncr_Faceless
 *
 * Phase 2:
 * ncr_Faceless_phase2
 *
 * 1페이즈 사망 후 MythicMobs가 직접 생성하는
 * 2페이즈 엔티티를 MythicMobSpawnEvent에서 포착한다.
 */
public final class VoidSanctumDungeonMobListener
        implements Listener {

    private final VoidSanctumDungeonService dungeonService;


    public VoidSanctumDungeonMobListener(
            VoidSanctumDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * MYTHIC PHASE 2 SPAWN
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onMythicMobSpawn(
            MythicMobSpawnEvent event
    ) {

        String mythicMobId =
                event.getMobType()
                        .getInternalName();


        Entity entity =
                event.getEntity();


        if (!(entity instanceof LivingEntity boss)) {
            return;
        }


        if (
                !dungeonService
                        .isExpectedPhase2Spawn(
                                mythicMobId,
                                boss.getLocation()
                        )
        ) {

            return;
        }


        dungeonService
                .preparePhase2Boss(
                        boss
                );
    }


    /*
     * =========================================================
     * BOSS DEATH
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


        if (
                !dungeonService
                        .isPhase1Boss(
                                entity
                        )
                        &&
                !dungeonService
                        .isPhase2Boss(
                                entity
                        )
        ) {

            return;
        }


        /*
         * 네크론 보스는 바닐라 드롭 / EXP를
         * 보상으로 사용하지 않는다.
         */
        event.getDrops()
                .clear();

        event.setDroppedExp(
                0
        );


        dungeonService
                .onDungeonMobDeath(
                        entity
                );
    }
}
