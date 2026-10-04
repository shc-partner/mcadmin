package com.hcs.rpgcore.dungeon;

import org.bukkit.Location;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.CreatureSpawnEvent;


public final class DungeonNaturalSpawnBlockListener
        implements Listener {

    /*
     * =========================================================
     * DUNGEON NATURAL SPAWN BLOCK AREA
     * =========================================================
     */

    private static final int MIN_X =
            4000017;

    private static final int MAX_X =
            4000075;

    private static final int MIN_Y =
            189;

    private static final int MAX_Y =
            235;

    private static final int MIN_Z =
            4000051;

    private static final int MAX_Z =
            4000109;


    /*
     * =========================================================
     * NATURAL MOB SPAWN BLOCK
     * =========================================================
     *
     * 미노타우르스 던전에서 사용한 방식과 동일하게
     * NATURAL spawn만 차단한다.
     *
     * RPGCore / MythicMobs 등에서 직접 소환하는
     * CUSTOM 몬스터에는 영향을 주지 않는다.
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onCreatureSpawn(
            CreatureSpawnEvent event
    ) {

        if (
                event.getSpawnReason()
                        != CreatureSpawnEvent.SpawnReason.NATURAL
        ) {
            return;
        }


        if (
                !isInsideDungeon(
                        event.getLocation()
                )
        ) {
            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * AREA CHECK
     * =========================================================
     */

    private boolean isInsideDungeon(
            Location location
    ) {

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();


        return
                x >= MIN_X
                && x <= MAX_X

                && y >= MIN_Y
                && y <= MAX_Y

                && z >= MIN_Z
                && z <= MAX_Z;
    }
}
