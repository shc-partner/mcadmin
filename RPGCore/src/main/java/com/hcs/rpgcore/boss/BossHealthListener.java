package com.hcs.rpgcore.boss;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class BossHealthListener implements Listener {

    private static final double ENDER_DRAGON_HEALTH = 35_000.0;
    private static final double WITHER_HEALTH = 30_000.0;
    private static final double WARDEN_HEALTH = 50_000.0;

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {

        Entity entity = event.getEntity();

        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (entity instanceof EnderDragon) {

            applyHealth(
                    livingEntity,
                    ENDER_DRAGON_HEALTH
            );

            return;
        }

        if (entity instanceof Wither) {

            applyHealth(
                    livingEntity,
                    WITHER_HEALTH
            );

            return;
        }

        if (entity instanceof Warden) {

            applyHealth(
                    livingEntity,
                    WARDEN_HEALTH
            );
        }
    }

    public static void applyHealth(
            LivingEntity entity,
            double health
    ) {

        AttributeInstance attribute =
                entity.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (attribute == null) {
            return;
        }

        attribute.setBaseValue(
                health
        );

        entity.setHealth(
                health
        );
    }
}

