package com.hcs.rpgcore.combat;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


/*
 * =========================================================
 * ENDER DRAGON BLACK HOLE TASK
 * =========================================================
 *
 * 살아 있는 엔더 드래곤 주변에
 * 5초마다 낙하형 블랙홀 5개를 생성한다.
 *
 * 블랙홀 하나는 같은 플레이어에게
 * 최대 한 번만 피해를 준다.
 */
public final class EnderDragonBlackHoleTask
        implements Runnable {

    private static final int BLACK_HOLE_COUNT =
            5;

    private static final double MIN_RADIUS =
            6.0D;

    private static final double MAX_RADIUS =
            16.0D;

    private static final double SPAWN_HEIGHT =
            12.0D;

    private static final double FALL_SPEED =
            0.12D;

    private static final double DAMAGE =
            100.0D;

    private static final double DAMAGE_RADIUS =
            2.5D;

    /*
     * 10초.
     */
    private static final int MAX_LIFETIME_TICKS =
            200;


    private final RPGCorePlugin plugin;


    public EnderDragonBlackHoleTask(
            RPGCorePlugin plugin
    ) {

        this.plugin =
                plugin;
    }


    /*
     * =========================================================
     * MAIN
     * =========================================================
     */
    @Override
    public void run() {

        for (
                World world
                : plugin.getServer().getWorlds()
        ) {

            for (
                    EnderDragon dragon
                    : world.getEntitiesByClass(
                            EnderDragon.class
                    )
            ) {

                if (
                        dragon.isDead()
                        || !dragon.isValid()
                ) {
                    continue;
                }


                spawnWave(
                        dragon
                );
            }
        }
    }


    /*
     * =========================================================
     * SPAWN WAVE
     * =========================================================
     */
    private void spawnWave(
            EnderDragon dragon
    ) {

        Location dragonLocation =
                dragon.getLocation();


        for (
                int i = 0;
                i < BLACK_HOLE_COUNT;
                i++
        ) {

            double angle =
                    ThreadLocalRandom
                            .current()
                            .nextDouble(
                                    0.0D,
                                    Math.PI * 2.0D
                            );


            double radius =
                    ThreadLocalRandom
                            .current()
                            .nextDouble(
                                    MIN_RADIUS,
                                    MAX_RADIUS
                            );


            double x =
                    dragonLocation.getX()
                            + Math.cos(angle)
                            * radius;

            double z =
                    dragonLocation.getZ()
                            + Math.sin(angle)
                            * radius;


            World world =
                    dragon.getWorld();


            int groundY =
                    world.getHighestBlockYAt(
                            (int) Math.floor(x),
                            (int) Math.floor(z)
                    );


            Location spawnLocation =
                    new Location(
                            world,
                            x,
                            groundY
                                    + SPAWN_HEIGHT,
                            z
                    );


            startFallingBlackHole(
                    dragon,
                    spawnLocation
            );
        }
    }


    /*
     * =========================================================
     * FALLING BLACK HOLE
     * =========================================================
     */
    private void startFallingBlackHole(
            EnderDragon dragon,
            Location start
    ) {

        final Location center =
                start.clone();


        final Set<UUID> damagedPlayers =
                new HashSet<>();


        spawnAppearEffect(
                center
        );


        new BukkitRunnable() {

            private int tick =
                    0;


            @Override
            public void run() {

                if (
                        dragon.isDead()
                        || !dragon.isValid()
                ) {

                    spawnDisappearEffect(
                            center
                    );

                    cancel();
                    return;
                }


                if (
                        tick
                                >= MAX_LIFETIME_TICKS
                ) {

                    spawnImpactEffect(
                            center
                    );

                    damageNearbyPlayers(
                            dragon,
                            center,
                            damagedPlayers
                    );

                    cancel();
                    return;
                }


                /*
                 * 매 tick 천천히 아래로 이동.
                 */
                center.subtract(
                        0.0D,
                        FALL_SPEED,
                        0.0D
                );


                spawnBlackHoleFrame(
                        center,
                        tick
                );


                damageNearbyPlayers(
                        dragon,
                        center,
                        damagedPlayers
                );


                /*
                 * 아래 블록에 닿으면 종료.
                 */
                Location below =
                        center.clone()
                                .subtract(
                                        0.0D,
                                        0.35D,
                                        0.0D
                                );


                if (
                        !below.getBlock()
                                .isPassable()
                ) {

                    spawnImpactEffect(
                            center
                    );

                    damageNearbyPlayers(
                            dragon,
                            center,
                            damagedPlayers
                    );

                    cancel();
                    return;
                }


                tick++;
            }

        }.runTaskTimer(
                plugin,
                0L,
                1L
        );
    }


    /*
     * =========================================================
     * DAMAGE
     * =========================================================
     */
    private void damageNearbyPlayers(
            EnderDragon dragon,
            Location center,
            Set<UUID> damagedPlayers
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        double radiusSquared =
                DAMAGE_RADIUS
                        * DAMAGE_RADIUS;


        for (
                Player player
                : world.getPlayers()
        ) {

            if (
                    player.isDead()
                    || !player.isOnline()
            ) {
                continue;
            }


            UUID playerUuid =
                    player.getUniqueId();


            if (
                    damagedPlayers.contains(
                            playerUuid
                    )
            ) {
                continue;
            }


            Location playerLocation =
                    player.getLocation();


            double dx =
                    playerLocation.getX()
                            - center.getX();

            double dy =
                    (
                            playerLocation.getY()
                                    + 1.0D
                    )
                            - (
                                    center.getY()
                                            + 1.0D
                            );

            double dz =
                    playerLocation.getZ()
                            - center.getZ();


            double distanceSquared =
                    dx * dx
                            + dy * dy
                            + dz * dz;


            if (
                    distanceSquared
                            > radiusSquared
            ) {
                continue;
            }


            damagedPlayers.add(
                    playerUuid
            );


            player.damage(
                    DAMAGE,
                    dragon
            );


            spawnHitEffect(
                    player
            );
        }
    }


    /*
     * =========================================================
     * APPEAR EFFECT
     * =========================================================
     */
    private void spawnAppearEffect(
            Location center
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.PORTAL,
                center.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                50,
                1.2D,
                1.0D,
                1.2D,
                0.16D
        );


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                center.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                35,
                1.0D,
                0.8D,
                1.0D,
                0.12D
        );


        world.playSound(
                center,
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                0.8F,
                0.55F
        );
    }


    /*
     * =========================================================
     * BLACK HOLE FRAME
     * =========================================================
     */
    private void spawnBlackHoleFrame(
            Location center,
            int tick
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        Location core =
                center.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        );


        Particle.DustOptions violet =
                new Particle.DustOptions(
                        Color.fromRGB(
                                145,
                                40,
                                255
                        ),
                        1.25F
                );


        Particle.DustOptions brightViolet =
                new Particle.DustOptions(
                        Color.fromRGB(
                                210,
                                90,
                                255
                        ),
                        1.0F
                );


        /*
         * 검은 중심을 둘러싸는 회전 링.
         */
        double rotation =
                tick * 0.14D;


        int points =
                24;


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0D
                            * i
                            / points
                            + rotation;


            double radius =
                    1.05D;


            Location point =
                    core.clone()
                            .add(
                                    Math.cos(angle)
                                            * radius,
                                    Math.sin(
                                            angle * 2.0D
                                    ) * 0.10D,
                                    Math.sin(angle)
                                            * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    i % 2 == 0
                            ? violet
                            : brightViolet
            );
        }


        /*
         * 중심 소용돌이.
         */
        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                core,
                10,
                0.45D,
                0.35D,
                0.45D,
                0.05D
        );


        world.spawnParticle(
                Particle.PORTAL,
                core,
                6,
                0.30D,
                0.25D,
                0.30D,
                0.03D
        );


        /*
         * 낙하 궤적.
         */
        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                center.clone()
                        .add(
                                0.0D,
                                1.8D,
                                0.0D
                        ),
                3,
                0.18D,
                0.45D,
                0.18D,
                0.01D
        );


        /*
         * 사운드는 매 tick 재생하지 않는다.
         */
        if (
                tick % 20
                        == 0
        ) {

            world.playSound(
                    center,
                    Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,
                    0.22F,
                    0.50F
            );
        }
    }


    /*
     * =========================================================
     * IMPACT EFFECT
     * =========================================================
     */
    private void spawnImpactEffect(
            Location center
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        Location effect =
                center.clone()
                        .add(
                                0.0D,
                                0.8D,
                                0.0D
                        );


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                effect,
                60,
                1.6D,
                0.8D,
                1.6D,
                0.18D
        );


        world.spawnParticle(
                Particle.PORTAL,
                effect,
                40,
                1.3D,
                0.6D,
                1.3D,
                0.12D
        );


        world.playSound(
                center,
                Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                0.9F,
                0.50F
        );
    }


    /*
     * =========================================================
     * DISAPPEAR EFFECT
     * =========================================================
     */
    private void spawnDisappearEffect(
            Location center
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                center.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                25,
                0.8D,
                0.8D,
                0.8D,
                0.10D
        );
    }


    /*
     * =========================================================
     * HIT EFFECT
     * =========================================================
     */
    private void spawnHitEffect(
            Player player
    ) {

        World world =
                player.getWorld();


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                player.getLocation()
                        .clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                30,
                0.55D,
                0.8D,
                0.55D,
                0.12D
        );


        world.playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_HURT,
                0.65F,
                0.55F
        );
    }
}
