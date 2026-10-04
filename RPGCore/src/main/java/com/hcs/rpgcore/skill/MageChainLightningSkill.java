package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.mana.ManaService;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.util.Vector;


public final class MageChainLightningSkill {

    /*
     * =========================================================
     * LEVEL 30 - CHAIN LIGHTNING
     * =========================================================
     *
     * 정면의 적에게 번개를 적중시킨 뒤
     * 주변 적에게 연쇄적으로 전이한다.
     */

    public static final double MANA_COST =
            18.0;

    public static final long COOLDOWN_MILLIS =
            10000L;


    /*
     * 기본 체인 피해:
     * 20 + ATK x 0.45 + 최대 MP x 0.04
     */
    public static final double BASE_DAMAGE =
            20.0;

    public static final double ATTACK_RATIO =
            0.45;

    public static final double MAX_MANA_RATIO =
            0.04;


    /*
     * 처음 공격할 적 탐색 거리.
     */
    private static final double INITIAL_RANGE =
            12.0;


    /*
     * 다음 적으로 번개가 전이되는 거리.
     */
    private static final double CHAIN_RANGE =
            6.0;


    private static final int MAX_TARGETS =
            5;


    /*
     * 연쇄 대상별 피해 배율.
     */
    private static final double[] DAMAGE_RATIOS = {
            1.00,
            0.85,
            0.70,
            0.55,
            0.40
    };


    private final ManaService manaService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public MageChainLightningSkill(
            ManaService manaService,
            SkillOffenseBuffService offenseBuffService
    ) {

        this.manaService =
                manaService;

        this.offenseBuffService =
                offenseBuffService;
    }


    /*
     * =========================================================
     * CAST
     * =========================================================
     */
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


        LivingEntity firstTarget =
                findInitialTarget(
                        player
                );


        if (firstTarget == null) {

            player.getWorld()
                    .playSound(
                            player.getLocation(),
                            Sound.BLOCK_REDSTONE_TORCH_BURNOUT,
                            0.7f,
                            1.4f
                    );

            return false;
        }


        double maximumMana =
                manaService.getMaximumMana(
                        player.getUniqueId()
                );


        AttributeInstance attackAttribute =
                player.getAttribute(
                        Attribute.ATTACK_DAMAGE
                );


        double attack =
                attackAttribute != null
                        ? Math.max(
                                0.0,
                                attackAttribute.getValue()
                        )
                        : 1.0;


        double baseDamage =
                BASE_DAMAGE
                        + attack
                        * ATTACK_RATIO
                        + maximumMana
                        * MAX_MANA_RATIO;


        /*
         * =====================================================
         * MANA OVERLOAD
         * =====================================================
         *
         * 기본 피해에 먼저 +30%를 적용한다.
         *
         * 이후 각 체인 타격에서:
         *
         * 100% / 85% / 70% / 55% / 40%
         *
         * 비율이 적용된다.
         */
        if (offenseBuffService != null) {

            baseDamage =
                    offenseBuffService
                            .applyMageDamageMultiplier(
                                    player.getUniqueId(),
                                    baseDamage
                            );
        }


        Set<UUID> hitTargets =
                new HashSet<>();


        LivingEntity currentTarget =
                firstTarget;


        Location previousLocation =
                player.getEyeLocation()
                        .clone();


        int chainIndex =
                0;


        while (
                currentTarget != null
                && chainIndex < MAX_TARGETS
        ) {

            hitTargets.add(
                    currentTarget.getUniqueId()
            );


            double damage =
                    baseDamage
                            * DAMAGE_RATIOS[
                                    chainIndex
                            ];


            Location currentLocation =
                    getTargetCenter(
                            currentTarget
                    );


            spawnLightningArc(
                    previousLocation,
                    currentLocation,
                    chainIndex
            );


            currentTarget.damage(
                    damage,
                    player
            );


            spawnHitEffect(
                    currentTarget
            );


            LivingEntity nextTarget =
                    findNextTarget(
                            currentTarget,
                            hitTargets
                    );


            previousLocation =
                    currentLocation;

            currentTarget =
                    nextTarget;

            chainIndex++;
        }


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                        0.9f,
                        1.5f
                );


        return true;
    }


    /*
     * =========================================================
     * INITIAL TARGET
     * =========================================================
     */
    private LivingEntity findInitialTarget(
            Player player
    ) {

        Location eye =
                player.getEyeLocation();


        Vector forward =
                eye.getDirection()
                        .clone();


        if (
                forward.lengthSquared()
                        <= 0.0001
        ) {
            return null;
        }


        forward.normalize();


        LivingEntity bestTarget =
                null;


        double bestScore =
                Double.MAX_VALUE;


        for (
                Entity entity
                : player.getNearbyEntities(
                        INITIAL_RANGE,
                        INITIAL_RANGE,
                        INITIAL_RANGE
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


            Location targetCenter =
                    getTargetCenter(
                            target
                    );


            Vector toTarget =
                    targetCenter
                            .toVector()
                            .subtract(
                                    eye.toVector()
                            );


            double distance =
                    toTarget.length();


            if (
                    distance <= 0.0001
                    || distance > INITIAL_RANGE
            ) {
                continue;
            }


            toTarget.normalize();


            double dot =
                    forward.dot(
                            toTarget
                    );


            if (dot < 0.75) {
                continue;
            }


            double score =
                    distance
                            + (
                                    1.0 - dot
                            ) * 6.0;


            if (score < bestScore) {

                bestScore =
                        score;

                bestTarget =
                        target;
            }
        }


        return bestTarget;
    }


    /*
     * =========================================================
     * NEXT CHAIN TARGET
     * =========================================================
     */
    private LivingEntity findNextTarget(
            LivingEntity current,
            Set<UUID> excluded
    ) {

        LivingEntity bestTarget =
                null;


        double bestDistanceSquared =
                Double.MAX_VALUE;


        for (
                Entity entity
                : current.getNearbyEntities(
                        CHAIN_RANGE,
                        CHAIN_RANGE,
                        CHAIN_RANGE
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


            if (
                    excluded.contains(
                            target.getUniqueId()
                    )
            ) {
                continue;
            }


            double distanceSquared =
                    target.getLocation()
                            .distanceSquared(
                                    current.getLocation()
                            );


            if (
                    distanceSquared
                            > CHAIN_RANGE
                            * CHAIN_RANGE
            ) {
                continue;
            }


            if (
                    distanceSquared
                            < bestDistanceSquared
            ) {

                bestDistanceSquared =
                        distanceSquared;

                bestTarget =
                        target;
            }
        }


        return bestTarget;
    }


    /*
     * =========================================================
     * LIGHTNING ARC
     * =========================================================
     *
     * 시전자 -> 첫 대상 -> 다음 대상 순서가
     * 확실하게 보이도록 굵은 지그재그 번개를 만든다.
     */
    private void spawnLightningArc(
            Location start,
            Location end,
            int chainIndex
    ) {

        World world =
                start.getWorld();


        if (
                world == null
                || end.getWorld() != world
        ) {
            return;
        }


        Vector path =
                end.toVector()
                        .subtract(
                                start.toVector()
                        );


        double distance =
                path.length();


        if (
                distance <= 0.0001
        ) {
            return;
        }


        Vector forward =
                path.clone()
                        .normalize();


        /*
         * 번개가 좌우로 흔들릴 수 있는
         * 첫 번째 수직 벡터.
         */
        Vector side =
                forward.clone()
                        .crossProduct(
                                new Vector(
                                        0.0,
                                        1.0,
                                        0.0
                                )
                        );


        if (
                side.lengthSquared()
                        <= 0.0001
        ) {

            side =
                    new Vector(
                            1.0,
                            0.0,
                            0.0
                    );

        } else {

            side.normalize();
        }


        /*
         * 두 번째 흔들림 축.
         *
         * 번개가 평면 한 장처럼 보이지 않고
         * 입체적으로 꺾이도록 사용한다.
         */
        Vector up =
                side.clone()
                        .crossProduct(
                                forward
                        );


        if (
                up.lengthSquared()
                        <= 0.0001
        ) {

            up =
                    new Vector(
                            0.0,
                            1.0,
                            0.0
                    );

        } else {

            up.normalize();
        }


        Particle.DustOptions electricBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                25,
                                95,
                                255
                        ),
                        1.45f
                );


        Particle.DustOptions brightBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                70,
                                180,
                                255
                        ),
                        1.25f
                );


        Particle.DustOptions whiteCore =
                new Particle.DustOptions(
                        Color.fromRGB(
                                235,
                                250,
                                255
                        ),
                        1.10f
                );


        /*
         * =====================================================
         * MAIN BOLT
         * =====================================================
         *
         * 작은 단위로 이어진 선 대신
         * 눈에 확실히 보이는 굵은 지그재그 전류.
         */
        double stepSize =
                0.16;


        for (
                double step = 0.0;
                step <= distance;
                step += stepSize
        ) {

            double progress =
                    step
                            / distance;


            /*
             * 두 종류의 파형을 섞어서
             * 매끄러운 사인파 대신
             * 번개처럼 불규칙하게 꺾이게 한다.
             */
            double wave1 =
                    Math.sin(
                            step * 8.5
                                    + chainIndex * 1.7
                    );


            double wave2 =
                    Math.sin(
                            step * 17.0
                                    + chainIndex * 2.3
                    );


            double sideOffset =
                    wave1 * 0.13
                            + wave2 * 0.055;


            double verticalOffset =
                    Math.cos(
                            step * 11.5
                                    + chainIndex * 0.9
                    ) * 0.09;


            /*
             * 시작점과 끝점에서는 흔들림을 줄여
             * 실제 두 대상에 정확히 연결돼 보이게 한다.
             */
            double edgeFactor =
                    Math.sin(
                            progress * Math.PI
                    );


            sideOffset *=
                    edgeFactor;


            verticalOffset *=
                    edgeFactor;


            Location point =
                    start.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    step
                                            )
                            )
                            .add(
                                    side.clone()
                                            .multiply(
                                                    sideOffset
                                            )
                            )
                            .add(
                                    up.clone()
                                            .multiply(
                                                    verticalOffset
                                            )
                            );


            /*
             * 파란 외곽.
             */
            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    electricBlue
            );


            /*
             * 밝은 청색 중간층.
             */
            if (
                    ((int) (step * 100.0))
                            % 32
                            < 18
            ) {

                world.spawnParticle(
                        Particle.DUST,
                        point.clone()
                                .add(
                                        0.0,
                                        0.018,
                                        0.0
                                ),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        brightBlue
                );
            }


            /*
             * 중앙 백색 번개 코어.
             */
            if (
                    ((int) (step * 100.0))
                            % 24
                            < 15
            ) {

                world.spawnParticle(
                        Particle.DUST,
                        point.clone()
                                .add(
                                        0.0,
                                        0.035,
                                        0.0
                                ),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        whiteCore
                );
            }


            /*
             * 실제 전기 스파크.
             */
            if (
                    ((int) (step * 100.0))
                            % 40
                            < 17
            ) {

                world.spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        point,
                        2,
                        0.035,
                        0.035,
                        0.035,
                        0.015
                );
            }
        }


        /*
         * =====================================================
         * PARALLEL FLASH
         * =====================================================
         *
         * 메인 번개 옆에 순간적인 얇은 보조 전류를 하나 추가해서
         * 번개가 훨씬 굵어 보이도록 한다.
         */
        for (
                double step = 0.15;
                step < distance;
                step += 0.30
        ) {

            double progress =
                    step
                            / distance;


            double offset =
                    Math.sin(
                            step * 13.0
                                    + chainIndex
                    ) * 0.18
                            * Math.sin(
                                    progress * Math.PI
                            );


            Location point =
                    start.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    step
                                            )
                            )
                            .add(
                                    side.clone()
                                            .multiply(
                                                    offset
                                            )
                            )
                            .add(
                                    up.clone()
                                            .multiply(
                                                    -offset * 0.45
                                            )
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    brightBlue
            );


            world.spawnParticle(
                    Particle.END_ROD,
                    point,
                    1,
                    0.015,
                    0.015,
                    0.015,
                    0.0
            );
        }


        /*
         * =====================================================
         * SIDE BRANCHES
         * =====================================================
         *
         * 메인 번개에서 짧은 가지 번개가
         * 좌우로 튀어나오도록 한다.
         */
        int branchCount =
                Math.max(
                        2,
                        (int) Math.floor(
                                distance / 1.8
                        )
                );


        for (
                int branch = 0;
                branch < branchCount;
                branch++
        ) {

            double progress =
                    (
                            branch + 1.0
                    )
                            / (
                                    branchCount + 1.0
                            );


            double baseStep =
                    distance
                            * progress;


            double directionSign =
                    (
                            branch + chainIndex
                    ) % 2 == 0
                            ? 1.0
                            : -1.0;


            Location branchStart =
                    start.clone()
                            .add(
                                    forward.clone()
                                            .multiply(
                                                    baseStep
                                            )
                            );


            double branchLength =
                    0.38
                            + (
                                    branch % 3
                            ) * 0.12;


            int branchSegments =
                    5;


            for (
                    int i = 1;
                    i <= branchSegments;
                    i++
            ) {

                double branchProgress =
                        (double) i
                                / branchSegments;


                Location branchPoint =
                        branchStart.clone()
                                .add(
                                        side.clone()
                                                .multiply(
                                                        directionSign
                                                                * branchLength
                                                                * branchProgress
                                                )
                                )
                                .add(
                                        up.clone()
                                                .multiply(
                                                        0.10
                                                                * Math.sin(
                                                                        branchProgress
                                                                                * Math.PI
                                                                )
                                                )
                                );


                world.spawnParticle(
                        Particle.DUST,
                        branchPoint,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        i >= branchSegments - 1
                                ? brightBlue
                                : electricBlue
                );


                if (
                        i == branchSegments
                ) {

                    world.spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            branchPoint,
                            1,
                            0.025,
                            0.025,
                            0.025,
                            0.01
                    );
                }
            }
        }


        /*
         * 시작점과 도착점을 밝게 찍어
         * 어떤 대상에서 어떤 대상으로 연결됐는지
         * 더욱 명확하게 보여준다.
         */
        world.spawnParticle(
                Particle.DUST,
                start,
                5,
                0.08,
                0.08,
                0.08,
                0.0,
                whiteCore
        );


        world.spawnParticle(
                Particle.DUST,
                end,
                8,
                0.10,
                0.12,
                0.10,
                0.0,
                whiteCore
        );
    }


    /*
     * =========================================================
     * HIT EFFECT
     * =========================================================
     */
    private void spawnHitEffect(
            LivingEntity target
    ) {

        Location location =
                getTargetCenter(
                        target
                );


        World world =
                target.getWorld();


        Particle.DustOptions electricBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                25,
                                105,
                                255
                        ),
                        1.35f
                );


        Particle.DustOptions brightBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                75,
                                195,
                                255
                        ),
                        1.20f
                );


        Particle.DustOptions whiteCore =
                new Particle.DustOptions(
                        Color.fromRGB(
                                240,
                                252,
                                255
                        ),
                        1.15f
                );


        /*
         * 타겟 중심 전기 폭발.
         */
        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                location,
                30,
                0.48,
                0.58,
                0.48,
                0.24
        );


        world.spawnParticle(
                Particle.DUST,
                location,
                15,
                0.36,
                0.44,
                0.36,
                0.0,
                electricBlue
        );


        world.spawnParticle(
                Particle.DUST,
                location.clone()
                        .add(
                                0.0,
                                0.08,
                                0.0
                        ),
                10,
                0.24,
                0.30,
                0.24,
                0.0,
                brightBlue
        );


        world.spawnParticle(
                Particle.DUST,
                location.clone()
                        .add(
                                0.0,
                                0.12,
                                0.0
                        ),
                6,
                0.13,
                0.18,
                0.13,
                0.0,
                whiteCore
        );


        /*
         * 도착점에서 짧은 방사형 전기 가시.
         */
        int rays =
                8;


        for (
                int ray = 0;
                ray < rays;
                ray++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * ray
                            / rays;


            for (
                    int i = 1;
                    i <= 4;
                    i++
            ) {

                double distance =
                        i * 0.16;


                Location point =
                        location.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * distance,
                                        (
                                                i - 2
                                        ) * 0.035,
                                        Math.sin(
                                                angle
                                        ) * distance
                                );


                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        i >= 3
                                ? brightBlue
                                : whiteCore
                );
            }
        }


        world.spawnParticle(
                Particle.END_ROD,
                location,
                8,
                0.25,
                0.35,
                0.25,
                0.035
        );


        world.playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                0.65f,
                1.65f
        );


        world.playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.18f,
                1.9f
        );
    }


    /*
     * =========================================================
     * TARGET CENTER
     * =========================================================
     */
    private Location getTargetCenter(
            LivingEntity target
    ) {

        return target.getLocation()
                .clone()
                .add(
                        0.0,
                        Math.min(
                                1.2,
                                target.getHeight()
                                        * 0.5
                        ),
                        0.0
                );
    }
}
