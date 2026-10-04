package com.hcs.rpgcore.skill;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.util.Vector;


public final class WarriorBattleCrySkill {

    /*
     * =========================================================
     * LEVEL 60 - BATTLE CRY
     * =========================================================
     *
     * 주변 적에게 강력한 위압을 가해
     * 플레이어 방향으로 끌어당기고 둔화시킨다.
     */

    public static final double MANA_COST =
            18.0;

    public static final long COOLDOWN_MILLIS =
            20000L;


    /*
     * 효과 범위.
     */
    private static final double RANGE =
            10.0;


    /*
     * 당기는 힘.
     */
    private static final double PULL_STRENGTH =
            0.85;


    /*
     * 너무 강하게 위아래로 끌리지 않도록
     * 수직 성분은 별도로 제한한다.
     */
    private static final double MAX_VERTICAL_PULL =
            0.20;


    /*
     * Slowness III
     * 4초.
     */
    private static final int SLOW_DURATION_TICKS =
            80;

    private static final int SLOW_AMPLIFIER =
            2;


    public boolean cast(
            Player player
    ) {

        if (
                player == null
                || !player.isOnline()
                || player.isDead()
        ) {
            return false;
        }


        Location origin =
                player.getLocation()
                        .clone();


        spawnActivationEffect(
                player,
                origin
        );


        for (
                Entity entity
                : player.getNearbyEntities(
                        RANGE,
                        RANGE,
                        RANGE
                )
        ) {

            if (!(entity instanceof Enemy)) {
                continue;
            }


            if (
                    !(entity
                            instanceof LivingEntity target)
            ) {
                continue;
            }


            if (target.isDead()) {
                continue;
            }


            Location targetLocation =
                    target.getLocation();


            double distanceSquared =
                    targetLocation.distanceSquared(
                            origin
                    );


            if (
                    distanceSquared
                            > RANGE * RANGE
            ) {
                continue;
            }


            /*
             * 플레이어 중심 방향 벡터.
             */
            Vector pull =
                    origin.toVector()
                            .subtract(
                                    targetLocation.toVector()
                            );


            if (
                    pull.lengthSquared()
                            > 0.0001
            ) {

                pull.normalize()
                        .multiply(
                                PULL_STRENGTH
                        );


                double safeY =
                        Math.max(
                                -MAX_VERTICAL_PULL,
                                Math.min(
                                        MAX_VERTICAL_PULL,
                                        pull.getY()
                                )
                        );


                pull.setY(
                        safeY
                );


                /*
                 * 기존 속도 일부 보존.
                 */
                Vector velocity =
                        target.getVelocity()
                                .clone()
                                .multiply(
                                        0.25
                                )
                                .add(
                                        pull
                                );


                target.setVelocity(
                        velocity
                );
            }


            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            SLOW_DURATION_TICKS,
                            SLOW_AMPLIFIER,
                            false,
                            true,
                            true
                    )
            );


            spawnTargetEffect(
                    target
            );
        }


        player.getWorld()
                .playSound(
                        origin,
                        Sound.ENTITY_ENDER_DRAGON_GROWL,
                        0.65f,
                        0.75f
                );


        player.getWorld()
                .playSound(
                        origin,
                        Sound.ENTITY_RAVAGER_ROAR,
                        0.8f,
                        0.85f
                );


        return true;
    }


    /*
     * =========================================================
     * ACTIVATION EFFECT
     * =========================================================
     */
    private void spawnActivationEffect(
            Player player,
            Location origin
    ) {

        World world =
                player.getWorld();


        Location center =
                origin.clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        );


        world.spawnParticle(
                Particle.CLOUD,
                center,
                35,
                1.4,
                0.8,
                1.4,
                0.10
        );


        world.spawnParticle(
                Particle.CRIT,
                center,
                28,
                1.2,
                0.8,
                1.2,
                0.12
        );


        /*
         * 범위 링.
         */
        int points =
                48;


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double angle =
                    (
                            Math.PI
                                    * 2.0
                                    * i
                    )
                            / points;


            double x =
                    Math.cos(
                            angle
                    ) * RANGE;


            double z =
                    Math.sin(
                            angle
                    ) * RANGE;


            Location point =
                    origin.clone()
                            .add(
                                    x,
                                    0.20,
                                    z
                            );


            world.spawnParticle(
                    Particle.CRIT,
                    point,
                    1,
                    0.03,
                    0.05,
                    0.03,
                    0.01
            );
        }
    }


    /*
     * =========================================================
     * TARGET EFFECT
     * =========================================================
     */
    private void spawnTargetEffect(
            LivingEntity target
    ) {

        Location location =
                target.getLocation()
                        .clone()
                        .add(
                                0.0,
                                Math.min(
                                        1.0,
                                        target.getHeight()
                                                * 0.5
                                ),
                                0.0
                        );


        target.getWorld()
                .spawnParticle(
                        Particle.CLOUD,
                        location,
                        8,
                        0.30,
                        0.40,
                        0.30,
                        0.04
                );


        target.getWorld()
                .spawnParticle(
                        Particle.CRIT,
                        location,
                        7,
                        0.30,
                        0.40,
                        0.30,
                        0.08
                );
    }
}
