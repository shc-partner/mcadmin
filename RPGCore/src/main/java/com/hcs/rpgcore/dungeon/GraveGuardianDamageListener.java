package com.hcs.rpgcore.dungeon;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class GraveGuardianDamageListener
        implements Listener {

    private static final double DAMAGE_SCALE =
            ZombieDungeonService.BOSS_VANILLA_HEALTH
                    / ZombieDungeonService.BOSS_EFFECTIVE_HEALTH;

    private final ZombieDungeonService dungeonService;

    public GraveGuardianDamageListener(
            ZombieDungeonService dungeonService
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
                .isGraveGuardian(entity)) {

            return;
        }

        /*
         * 실제 Entity HP: 600
         * 목표 RPG HP: 600
         *
         * 체력이 동일하므로 피해 보정 비율은 1:1이다.
         *
         * 일반적인 피격/넉백/사망 이벤트 구조는 유지된다.
         */
        event.setDamage(
                event.getDamage()
                        * DAMAGE_SCALE
        );
    }
}
