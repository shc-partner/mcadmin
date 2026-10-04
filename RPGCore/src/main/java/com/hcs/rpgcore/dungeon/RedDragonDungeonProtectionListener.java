package com.hcs.rpgcore.dungeon;

import org.bukkit.Material;

import org.bukkit.block.Block;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;


/*
 * ============================================================
 * RED DRAGON DUNGEON TERRAIN PROTECTION
 * ============================================================
 *
 * 던전 내부 구조물이 전투 중 훼손되지 않도록 보호한다.
 *
 * - 엔티티 폭발의 블록 파괴 차단
 * - 블록 폭발의 블록 파괴 차단
 * - 블록 연소 차단
 * - 화염 점화 / 확산 차단
 * - 엔티티의 블록 변경 차단
 * - 몬스터의 문 파괴 차단
 *
 * 폭발 이벤트 자체는 취소하지 않는다.
 * 따라서 전투 피해 / 넉백 등은 유지하고
 * blockList만 제거한다.
 */
public final class RedDragonDungeonProtectionListener
        implements Listener {

    private final RedDragonDungeonService dungeonService;


    public RedDragonDungeonProtectionListener(
            RedDragonDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * PLAYER BLOCK BREAK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreak(
            BlockBreakEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * PLAYER BLOCK PLACE
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockPlace(
            BlockPlaceEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * ENTITY EXPLOSION
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityExplode(
            EntityExplodeEvent event
    ) {

        /*
         * 폭발 중심이 던전 밖이더라도
         * 던전 안쪽 블록이 폭발 범위에 걸릴 수 있으므로
         * blockList에서 보호 영역 블록만 제거한다.
         */
        event.blockList()
                .removeIf(
                        block ->
                                dungeonService
                                        .isInsideDungeonBoundary(
                                                block.getLocation()
                                        )
                );
    }


    /*
     * =========================================================
     * BLOCK EXPLOSION
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockExplode(
            BlockExplodeEvent event
    ) {

        event.blockList()
                .removeIf(
                        block ->
                                dungeonService
                                        .isInsideDungeonBoundary(
                                                block.getLocation()
                                        )
                );
    }


    /*
     * =========================================================
     * BLOCK BURN
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBurn(
            BlockBurnEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * FIRE IGNITE
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockIgnite(
            BlockIgniteEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * FIRE SPREAD
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockSpread(
            BlockSpreadEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        Block source =
                event.getSource();


        Material type =
                source.getType();


        if (
                type != Material.FIRE
                        &&
                type != Material.SOUL_FIRE
        ) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * ENTITY BLOCK CHANGE
     * =========================================================
     *
     * 엔더맨 블록 이동,
     * 몹의 블록 변경 등 엔티티 기반 지형 변화를 차단한다.
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityChangeBlock(
            EntityChangeBlockEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * ENTITY BREAK DOOR
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityBreakDoor(
            EntityBreakDoorEvent event
    ) {

        if (!dungeonService
                .isInsideDungeonBoundary(
                        event.getBlock()
                                .getLocation()
                )) {

            return;
        }


        event.setCancelled(
                true
        );
    }
}
