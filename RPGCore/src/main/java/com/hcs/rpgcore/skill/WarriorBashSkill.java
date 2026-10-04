package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.util.Vector;


public final class WarriorBashSkill {

    /*
     * 최초 전사 스킬.
     * MANA를 소모하지 않는다.
     */
    public static final double MANA_COST =
            0.0;

    public static final long COOLDOWN_MILLIS =
            5000L;

    private static final double DAMAGE_MULTIPLIER =
            1.25;

    private static final double RANGE =
            6.0;

    /*
     * 플레이어 정면의 넓은 부채꼴.
     */
    private static final double MIN_DOT =
            0.30;


    private final HudService hudService;

    private final WarriorWeaponAttackService
            weaponAttackService;


    public WarriorBashSkill(
            HudService hudService,
            WarriorWeaponAttackService weaponAttackService
    ) {

        this.hudService =
                hudService;

        this.weaponAttackService =
                weaponAttackService;
    }


    public boolean cast(
            Player player
    ) {

        HudSnapshot snapshot =
                hudService.getProfile(
                        player.getUniqueId()
                );


        if (snapshot == null) {
            return false;
        }


        /*
         * 스킬북 현재 공격력이 아니라
         * 마지막으로 장착한 주 무기를 포함한
         * 전사 총 공격력을 사용한다.
         */
        double totalAttack =
                weaponAttackService
                        .resolveBashAttack(
                                player,
                                snapshot
                        );


        double damage =
                totalAttack
                        * DAMAGE_MULTIPLIER;


        Location origin =
                player.getLocation();


        Vector forward =
                origin.getDirection()
                        .clone();


        forward.setY(
                0.0
        );


        if (
                forward.lengthSquared()
                        <= 0.0001
        ) {

            forward =
                    new Vector(
                            0.0,
                            0.0,
                            1.0
                    );

        } else {

            forward.normalize();
        }


        /*
         * 실제 배쉬 공격 범위와 같은 방향으로
         * 전방 부채꼴 검기 이펙트를 출력한다.
         */
        spawnBashEffect(
                player,
                origin,
                forward
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
                    !(entity instanceof LivingEntity target)
            ) {
                continue;
            }


            if (target.isDead()) {
                continue;
            }


            Vector toTarget =
                    target.getLocation()
                            .toVector()
                            .subtract(
                                    origin.toVector()
                            );


            toTarget.setY(
                    0.0
            );


            double distanceSquared =
                    toTarget.lengthSquared();


            if (
                    distanceSquared <= 0.0001
                    || distanceSquared
                    > RANGE * RANGE
            ) {
                continue;
            }


            toTarget.normalize();


            double dot =
                    forward.dot(
                            toTarget
                    );


            if (dot < MIN_DOT) {
                continue;
            }


            target.damage(
                    damage,
                    player
            );


            spawnHitEffect(
                    target
            );
        }


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        1.4f,
                        0.75f
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_IRON_GOLEM_ATTACK,
                        0.8f,
                        0.85f
                );


        return true;
    }


    /*
     * =========================================================
     * BASH VISUAL EFFECT
     * =========================================================
     */
    private void spawnBashEffect(
            Player player,
            Location origin,
            Vector forward
    ) {

        World world =
                player.getWorld();


        /*
         * 플레이어 오른쪽 방향.
         */
        Vector right =
                new Vector(
                        -forward.getZ(),
                        0.0,
                        forward.getX()
                );


        Location effectOrigin =
                origin.clone()
                        .add(
                                0.0,
                                1.15,
                                0.0
                        );


        /*
         * 중심 + 좌우 SWEEP_ATTACK.
         *
         * 앞으로 갈수록 폭을 넓혀
         * 부채꼴 광역기처럼 보이게 한다.
         */
        double[] distances = {
                1.5,
                3.0,
                4.5,
                6.0
        };


        for (
                int index = 0;
                index < distances.length;
                index++
        ) {

            double distance =
                    distances[index];


            Location center =
                    effectOrigin.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    distance
                                            )
                            );


            double width =
                    0.35
                            + index * 0.30;


            world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    center,
                    1,
                    0.05,
                    0.08,
                    0.05,
                    0.0
            );


            world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    center.clone()
                            .add(
                                    right.clone()
                                            .multiply(
                                                    -width
                                            )
                            ),
                    1,
                    0.05,
                    0.08,
                    0.05,
                    0.0
            );


            world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    center.clone()
                            .add(
                                    right.clone()
                                            .multiply(
                                                    width
                                            )
                            ),
                    1,
                    0.05,
                    0.08,
                    0.05,
                    0.0
            );
        }


        /*
         * CRIT 검기 궤적.
         */
        for (
                double distance = 0.8;
                distance <= RANGE;
                distance += 0.45
        ) {

            double width =
                    0.15
                            + (
                                    distance
                                    / RANGE
                            ) * 2.0;


            Location center =
                    effectOrigin.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    distance
                                            )
                            );


            world.spawnParticle(
                    Particle.CRIT,
                    center,
                    1,
                    0.05,
                    0.08,
                    0.05,
                    0.01
            );


            world.spawnParticle(
                    Particle.CRIT,
                    center.clone()
                            .add(
                                    right.clone()
                                            .multiply(
                                                    -width
                                            )
                            ),
                    1,
                    0.08,
                    0.10,
                    0.08,
                    0.01
            );


            world.spawnParticle(
                    Particle.CRIT,
                    center.clone()
                            .add(
                                    right.clone()
                                            .multiply(
                                                    width
                                            )
                            ),
                    1,
                    0.08,
                    0.10,
                    0.08,
                    0.01
            );
        }


        /*
         * 사거리 끝 압력파.
         */
        Location end =
                effectOrigin.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                RANGE
                                        )
                        );


        world.spawnParticle(
                Particle.CLOUD,
                end,
                10,
                1.25,
                0.20,
                1.25,
                0.02
        );
    }


    /*
     * =========================================================
     * BASH HIT EFFECT
     * =========================================================
     */
    private void spawnHitEffect(
            LivingEntity target
    ) {

        Location hitLocation =
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


        World world =
                target.getWorld();


        world.spawnParticle(
                Particle.CRIT,
                hitLocation,
                12,
                0.35,
                0.45,
                0.35,
                0.15
        );


        world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                hitLocation,
                5,
                0.25,
                0.35,
                0.25,
                0.08
        );
    }
}
