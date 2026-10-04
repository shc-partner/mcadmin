package com.hcs.rpgcore.combat;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public final class DragonProximityDamageTask implements Runnable {

    private static final double DAMAGE = 20.0;
    private static final double RANGE = 3.0;

    @Override
    public void run() {

        for (World world : Bukkit.getWorlds()) {

            for (EnderDragon dragon :
                    world.getEntitiesByClass(EnderDragon.class)) {

                if (dragon.isDead() || !dragon.isValid()) {
                    continue;
                }

                BoundingBox dangerZone =
                        dragon.getBoundingBox()
                                .clone()
                                .expand(RANGE);

                for (Player player : world.getPlayers()) {

                    if (player.isDead()) {
                        continue;
                    }

                    if (!dangerZone.contains(
                            player.getLocation().toVector()
                    )) {
                        continue;
                    }

                    applyDamage(
                            player,
                            dragon
                    );
                }
            }
        }
    }

    private void applyDamage(
            Player player,
            EnderDragon dragon
    ) {
        player.damage(
                DAMAGE,
                dragon
        );
    }
}

