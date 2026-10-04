package com.hcs.rpgcore.dungeon;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class ZombieDungeonMobListener
        implements Listener {

    private final ZombieDungeonService dungeonService;

    public ZombieDungeonMobListener(
            ZombieDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
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
         * =====================================================
         * DUNGEON VANILLA REWARD BLOCK
         * =====================================================
         *
         * 썩은 살점
         * 감자
         * 당근
         * 철괴
         * 착용 장비
         * 기타 모든 바닐라 아이템
         *
         * 전부 제거.
         */
        event.getDrops().clear();

        /*
         * 바닐라 녹색 EXP Orb 차단.
         *
         * RPGCore의 자체 RPG EXP 시스템은
         * 여기에서 수정하지 않는다.
         */
        event.setDroppedExp(0);

        dungeonService.onDungeonMobDeath(
                entity
        );
    }
}
