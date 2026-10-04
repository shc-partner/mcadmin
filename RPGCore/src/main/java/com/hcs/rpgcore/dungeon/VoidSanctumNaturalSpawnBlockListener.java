package com.hcs.rpgcore.dungeon;

import org.bukkit.Location;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.CreatureSpawnEvent;


/*
 * =========================================================
 * VOID SANCTUM NATURAL SPAWN BLOCK
 * =========================================================
 *
 * 공허의 성전에서는 NATURAL spawn만 차단한다.
 *
 * MythicMobs의 CUSTOM / PLUGIN 계열 소환,
 * 1페이즈 → 2페이즈 변환 및 VFX 엔티티에는
 * 영향을 주지 않는다.
 */
public final class VoidSanctumNaturalSpawnBlockListener
        implements Listener {

    private static final int MIN_X =
            -9999678;

    private static final int MAX_X =
            -9999523;

    private static final int MIN_Y =
            51;

    private static final int MAX_Y =
            213;

    private static final int MIN_Z =
            19999044;

    private static final int MAX_Z =
            19999165;


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


        if (!isInsideDungeon(
                event.getLocation()
        )) {
            return;
        }


        event.setCancelled(
                true
        );
    }


    private boolean isInsideDungeon(
            Location location
    ) {

        if (location == null) {
            return false;
        }


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
