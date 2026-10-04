package com.hcs.rpgcore.dungeon;

import io.papermc.paper.event.entity.EntityMoveEvent;

import org.bukkit.entity.Entity;
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


public final class AncientDepthsDungeonMobListener
        implements Listener {


    private static final double MAX_VANILLA_HEALTH =
            1024.0D;


    private final AncientDepthsDungeonService dungeonService;


    public AncientDepthsDungeonMobListener(
            AncientDepthsDungeonService dungeonService
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
     * DEATH
     * =========================================================
     */

    @EventHandler
    public void onDeath(
            EntityDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();


        if (
                !dungeonService.isDungeonMob(
                        entity
                )
        ) {

            return;
        }


        event.getDrops()
                .clear();

        event.setDroppedExp(
                0
        );


        dungeonService.onDungeonMobDeath(
                entity
        );
    }


    /*
     * =========================================================
     * EFFECTIVE HEALTH > 1024
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onDamage(
            EntityDamageEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof LivingEntity entity)
        ) {

            return;
        }


        if (
                !dungeonService.isDungeonMob(
                        entity
                )
        ) {

            return;
        }


        double effectiveHealth =
                dungeonService.getEffectiveHealth(
                        entity
                );


        if (
                effectiveHealth
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
     * CUSTOM ATTACK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onAttack(
            EntityDamageByEntityEvent event
    ) {

        LivingEntity attacker =
                resolveAttacker(
                        event.getDamager()
                );


        if (
                attacker == null
                || !dungeonService.isDungeonMob(
                        attacker
                )
        ) {

            return;
        }


        double damage =
                dungeonService.getAttackDamage(
                        attacker
                );


        if (
                damage <= 0.0D
        ) {

            return;
        }


        event.setDamage(
                damage
        );
    }


    private LivingEntity resolveAttacker(
            Entity damager
    ) {

        if (
                damager
                        instanceof LivingEntity living
        ) {

            return living;
        }


        if (
                damager
                        instanceof Projectile projectile
        ) {

            ProjectileSource source =
                    projectile.getShooter();


            if (
                    source
                            instanceof LivingEntity living
            ) {

                return living;
            }
        }


        return null;
    }


    /*
     * =========================================================
     * MOVEMENT BOUNDARY
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onMove(
            EntityMoveEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof LivingEntity entity)
        ) {

            return;
        }


        if (
                !dungeonService.isDungeonMob(
                        entity
                )
        ) {

            return;
        }


        if (
                dungeonService.isInsideBoundary(
                        event.getTo()
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

        if (
                !(event.getEntity()
                        instanceof LivingEntity entity)
        ) {

            return;
        }


        if (
                !dungeonService.isDungeonMob(
                        entity
                )
        ) {

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


        event.setCancelled(
                true
        );
    }
}
