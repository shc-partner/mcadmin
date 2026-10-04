package com.hcs.rpgcore.dungeon;

import io.papermc.paper.event.entity.EntityMoveEvent;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;


/*
 * =============================================================
 * Lv20~30 UNDEAD FORTRESS MOB LISTENER
 * =============================================================
 */
public final class UndeadFortressDungeonMobListener
        implements Listener {

    private static final double MAX_VANILLA_HEALTH =
            1024.0D;


    private final UndeadFortressDungeonService
            dungeonService;


    public UndeadFortressDungeonMobListener(
            UndeadFortressDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * NATURAL SPAWN BLOCK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onNaturalSpawn(
            CreatureSpawnEvent event
    ) {

        if (
                event.getSpawnReason()
                        != CreatureSpawnEvent.SpawnReason.NATURAL
        ) {
            return;
        }

        if (
                !dungeonService.isInsideBoundary(
                        event.getLocation()
                )
        ) {
            return;
        }

        event.setCancelled(true);
    }

    /*
     * =========================================================
     * DEATH / VANILLA REWARD BLOCK
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onDeath(
            EntityDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        if (!dungeonService.isDungeonMob(
                entity
        )) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        dungeonService.onDungeonMobDeath(
                entity
        );
    }


    /*
     * =========================================================
     * EFFECTIVE HP
     * =========================================================
     *
     * HP 1024 초과 몬스터는 실제 HP 1024를 사용하고
     * 받는 피해를 비율 보정한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDamage(
            EntityDamageEvent event
    ) {

        if (!(event.getEntity()
                instanceof LivingEntity entity)) {

            return;
        }

        if (!dungeonService.isDungeonMob(
                entity
        )) {
            return;
        }

        double effectiveHealth =
                dungeonService
                        .getEffectiveHealth(
                                entity
                        );

        if (
                effectiveHealth <= 0.0D
                || effectiveHealth
                        <= MAX_VANILLA_HEALTH
        ) {
            return;
        }

        double scale =
                MAX_VANILLA_HEALTH
                        / effectiveHealth;

        event.setDamage(
                event.getDamage()
                        * scale
        );
    }


    /*
     * =========================================================
     * EXACT RPG ATTACK DAMAGE
     * =========================================================
     *
     * 근접몹뿐 아니라 Skeleton/Stray 화살에도
     * 던전 설정 공격력을 적용한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDungeonAttack(
            EntityDamageByEntityEvent event
    ) {

        LivingEntity attacker =
                resolveAttacker(
                        event
                );

        if (attacker == null) {
            return;
        }

        if (!dungeonService.isDungeonMob(
                attacker
        )) {
            return;
        }

        double attack =
                dungeonService
                        .getAttackDamage(
                                attacker
                        );

        if (attack <= 0.0D) {
            return;
        }

        event.setDamage(
                attack
        );
    }


    private LivingEntity resolveAttacker(
            EntityDamageByEntityEvent event
    ) {

        if (
                event.getDamager()
                        instanceof LivingEntity living
        ) {
            return living;
        }

        if (
                event.getDamager()
                        instanceof Projectile projectile
        ) {

            ProjectileSource shooter =
                    projectile.getShooter();

            if (
                    shooter
                            instanceof LivingEntity living
            ) {
                return living;
            }
        }

        return null;
    }


    /*
     * =========================================================
     * WALKING BOUNDARY
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onMove(
            EntityMoveEvent event
    ) {

        if (!(event.getEntity()
                instanceof LivingEntity entity)) {

            return;
        }

        if (!dungeonService.isDungeonMob(
                entity
        )) {
            return;
        }

        if (!event.hasChangedPosition()) {
            return;
        }

        if (dungeonService.isInsideBoundary(
                event.getTo()
        )) {
            return;
        }

        event.setCancelled(true);
    }


    /*
     * =========================================================
     * TELEPORT BOUNDARY
     * =========================================================
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onTeleport(
            EntityTeleportEvent event
    ) {

        if (!(event.getEntity()
                instanceof LivingEntity entity)) {

            return;
        }

        if (!dungeonService.isDungeonMob(
                entity
        )) {
            return;
        }

        if (
                event.getTo() != null
                && dungeonService.isInsideBoundary(
                        event.getTo()
                )
        ) {
            return;
        }

        event.setCancelled(true);
    }
}
