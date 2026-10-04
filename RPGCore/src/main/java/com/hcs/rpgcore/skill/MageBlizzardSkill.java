package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.mana.ManaService;

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

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;


public final class MageBlizzardSkill {

    /*
     * =========================================================
     * LEVEL 50 - BLIZZARD
     * =========================================================
     *
     * 플레이어가 바라보는 지점에
     * 일정 시간 지속되는 냉기 폭풍을 생성한다.
     */

    public static final double MANA_COST =
            35.0;

    public static final long COOLDOWN_MILLIS =
            20000L;


    /*
     * 1회 피해:
     * 10 + ATK x 0.12 + 최대 MP x 0.012
     *
     * 총 6회 풀히트:
     * 60 + ATK x 0.72 + 최대 MP x 0.072
     */
    public static final double BASE_DAMAGE =
            10.0;

    public static final double ATTACK_RATIO =
            0.12;

    public static final double MAX_MANA_RATIO =
            0.012;


    /*
     * 최대 시전 거리.
     */
    private static final double CAST_RANGE =
            18.0;


    /*
     * 블리자드 공격 반경.
     */
    private static final double AREA_RADIUS =
            5.0;


    /*
     * 6회 공격.
     */
    private static final int HIT_COUNT =
            6;


    /*
     * 1초 = 20 tick.
     */
    private static final long HIT_INTERVAL_TICKS =
            20L;


    /*
     * Slowness II
     * 2초.
     */
    private static final int SLOW_DURATION_TICKS =
            40;

    private static final int SLOW_AMPLIFIER =
            1;


    private final RPGCorePlugin plugin;

    private final ManaService manaService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public MageBlizzardSkill(
            RPGCorePlugin plugin,
            ManaService manaService,
            SkillOffenseBuffService offenseBuffService
    ) {

        this.plugin =
                plugin;

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


        Location center =
                resolveTargetLocation(
                        player
                );


        if (center == null) {
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


        double damagePerHit =
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
         * 시전 시점에 마나 오버로드가 활성 상태라면
         * 블리자드 1회 피해를 +30% 증가시킨다.
         *
         * 이후 6회 지속 타격 모두
         * 이 snapshot 피해값을 사용한다.
         */
        if (offenseBuffService != null) {

            damagePerHit =
                    offenseBuffService
                            .applyMageDamageMultiplier(
                                    player.getUniqueId(),
                                    damagePerHit
                            );
        }


        final double finalDamagePerHit =
                damagePerHit;


        spawnCastEffect(
                player,
                center
        );


        /*
         * 공격 판정과 별개로
         * 블리자드 눈보라 VFX를 지속적으로 실행한다.
         */
        startBlizzardVisual(
                center
        );


        /*
         * =====================================================
         * PERSISTENT AREA
         * =====================================================
         *
         * 0 / 20 / 40 / 60 / 80 / 100 tick
         *
         * 총 6회 공격.
         */
        new BukkitRunnable() {

            private int hitIndex =
                    0;


            @Override
            public void run() {

                if (
                        !player.isOnline()
                        || player.isDead()
                ) {

                    cancel();
                    return;
                }


                performBlizzardTick(
                        player,
                        center,
                        finalDamagePerHit,
                        hitIndex
                );


                hitIndex++;


                if (
                        hitIndex
                                >= HIT_COUNT
                ) {

                    spawnFinishEffect(
                            center
                    );

                    cancel();
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                HIT_INTERVAL_TICKS
        );


        return true;
    }


    /*
     * =========================================================
     * TARGET LOCATION
     * =========================================================
     */
    private Location resolveTargetLocation(
            Player player
    ) {

        Location eye =
                player.getEyeLocation();


        Vector direction =
                eye.getDirection()
                        .clone();


        if (
                direction.lengthSquared()
                        <= 0.0001
        ) {
            return null;
        }


        direction.normalize();


        /*
         * 블록에 시선이 닿으면
         * 해당 지점을 블리자드 중심으로 사용한다.
         */
        RayTraceResult result =
                player.getWorld()
                        .rayTraceBlocks(
                                eye,
                                direction,
                                CAST_RANGE
                        );


        if (
                result != null
                && result.getHitPosition() != null
        ) {

            return result.getHitPosition()
                    .toLocation(
                            player.getWorld()
                    );
        }


        /*
         * 블록에 닿지 않으면
         * 최대 사거리 지점을 사용한다.
         */
        return eye.clone()
                .add(
                        direction.multiply(
                                CAST_RANGE
                        )
                );
    }


    /*
     * =========================================================
     * BLIZZARD TICK
     * =========================================================
     */
    private void performBlizzardTick(
            Player player,
            Location center,
            double damage,
            int hitIndex
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        spawnStormEffect(
                center,
                hitIndex
        );


        for (
                Entity entity
                : world.getNearbyEntities(
                        center,
                        AREA_RADIUS,
                        4.0,
                        AREA_RADIUS
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


            double dx =
                    targetLocation.getX()
                            - center.getX();

            double dz =
                    targetLocation.getZ()
                            - center.getZ();


            double horizontalDistanceSquared =
                    dx * dx
                            + dz * dz;


            if (
                    horizontalDistanceSquared
                            > AREA_RADIUS
                            * AREA_RADIUS
            ) {
                continue;
            }


            if (
                    Math.abs(
                            targetLocation.getY()
                                    - center.getY()
                    ) > 4.0
            ) {
                continue;
            }


            target.damage(
                    damage,
                    player
            );


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


            spawnHitEffect(
                    target
            );
        }


        world.playSound(
                center,
                Sound.BLOCK_POWDER_SNOW_STEP,
                0.7f,
                0.75f
                        + (
                                hitIndex
                                        * 0.03f
                        )
        );
    }


    /*
     * =========================================================
     * CAST EFFECT
     * =========================================================
     */
    private void spawnCastEffect(
            Player player,
            Location center
    ) {

        World world =
                player.getWorld();


        world.spawnParticle(
                Particle.SNOWFLAKE,
                center.clone()
                        .add(
                                0.0,
                                2.0,
                                0.0
                        ),
                55,
                2.0,
                2.0,
                2.0,
                0.08
        );


        world.spawnParticle(
                Particle.CLOUD,
                center,
                20,
                1.2,
                0.2,
                1.2,
                0.04
        );


        world.playSound(
                center,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                1.0f,
                0.75f
        );
    }


    /*
     * =========================================================
     * STORM EFFECT
     * =========================================================
     *
     * 실제 공격 tick에서 발생하는
     * 순간 냉기 폭발.
     *
     * 지속 회전 효과는 startBlizzardVisual()에서 담당한다.
     */
    private void spawnStormEffect(
            Location center,
            int hitIndex
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        Particle.DustOptions iceBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                80,
                                190,
                                255
                        ),
                        1.15f
                );


        Particle.DustOptions frostWhite =
                new Particle.DustOptions(
                        Color.fromRGB(
                                230,
                                250,
                                255
                        ),
                        1.05f
                );


        /*
         * 공격 tick마다 중앙에서
         * 냉기 폭발이 한 번 터진다.
         */
        world.spawnParticle(
                Particle.SNOWFLAKE,
                center.clone()
                        .add(
                                0.0,
                                1.65,
                                0.0
                        ),
                28,
                1.65,
                1.15,
                1.65,
                0.07
        );


        world.spawnParticle(
                Particle.DUST,
                center.clone()
                        .add(
                                0.0,
                                0.50,
                                0.0
                        ),
                14,
                1.25,
                0.25,
                1.25,
                0.0,
                iceBlue
        );


        /*
         * 타격 순간 바닥 충격 링.
         */
        int points =
                30;


        double radius =
                2.15
                        + (
                                hitIndex % 3
                        ) * 0.65;


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / points
                            + hitIndex * 0.25;


            Location point =
                    center.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    0.12,
                                    Math.sin(
                                            angle
                                    ) * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    i % 2 == 0
                            ? frostWhite
                            : iceBlue
            );
        }
    }


    /*
     * =========================================================
     * BLIZZARD CONTINUOUS VISUAL
     * =========================================================
     *
     * 마지막 공격이 100 tick에 발생하므로
     * 약 104 tick 동안 블리자드 VFX를 유지한다.
     */
    private void startBlizzardVisual(
            Location center
    ) {

        final Location fixedCenter =
                center.clone();


        final int durationTicks =
                104;


        new BukkitRunnable() {

            private int tick =
                    0;


            @Override
            public void run() {

                if (
                        tick
                                >= durationTicks
                ) {

                    cancel();
                    return;
                }


                spawnBlizzardFrame(
                        fixedCenter,
                        tick
                );


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
     * BLIZZARD FRAME
     * =========================================================
     */
    private void spawnBlizzardFrame(
            Location center,
            int tick
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        Particle.DustOptions deepBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                30,
                                95,
                                230
                        ),
                        1.10f
                );


        Particle.DustOptions iceBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                70,
                                185,
                                255
                        ),
                        1.15f
                );


        Particle.DustOptions frostWhite =
                new Particle.DustOptions(
                        Color.fromRGB(
                                230,
                                250,
                                255
                        ),
                        1.05f
                );


        /*
         * =====================================================
         * 1. OUTER BLIZZARD
         * =====================================================
         *
         * 반경 4~5블록의 큰 눈보라.
         */
        int outerCount =
                18;


        for (
                int i = 0;
                i < outerCount;
                i++
        ) {

            double angle =
                    Math.toRadians(
                            tick * 15.0
                    )
                            + (
                                    Math.PI
                                            * 2.0
                                            * i
                                    / outerCount
                            );


            double radius =
                    4.10
                            + Math.sin(
                                    angle * 3.0
                                            + tick * 0.12
                            ) * 0.55;


            double y =
                    0.55
                            + (
                                    i % 5
                            ) * 0.58
                            + Math.sin(
                                    angle * 2.0
                                            + tick * 0.18
                            ) * 0.22;


            Location point =
                    center.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    y,
                                    Math.sin(
                                            angle
                                    ) * radius
                            );


            world.spawnParticle(
                    Particle.SNOWFLAKE,
                    point,
                    1,
                    0.025,
                    0.035,
                    0.025,
                    0.008
            );


            if (
                    i % 3
                            == 0
            ) {

                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        deepBlue
                );
            }
        }


        /*
         * =====================================================
         * 2. MIDDLE SPIRAL
         * =====================================================
         *
         * 외곽과 반대 방향으로 회전해
         * 실제 소용돌이 느낌을 만든다.
         */
        int middleCount =
                14;


        for (
                int i = 0;
                i < middleCount;
                i++
        ) {

            double angle =
                    Math.toRadians(
                            tick * -21.0
                    )
                            + (
                                    Math.PI
                                            * 2.0
                                            * i
                                    / middleCount
                            );


            double radius =
                    2.55
                            + (
                                    i % 3
                            ) * 0.22;


            double y =
                    0.75
                            + (
                                    i % 4
                            ) * 0.62;


            Location point =
                    center.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    y,
                                    Math.sin(
                                            angle
                                    ) * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    iceBlue
            );


            if (
                    i % 2
                            == 0
            ) {

                world.spawnParticle(
                        Particle.SNOWFLAKE,
                        point,
                        1,
                        0.02,
                        0.04,
                        0.02,
                        0.01
                );
            }
        }


        /*
         * =====================================================
         * 3. INNER WHITE VORTEX
         * =====================================================
         *
         * 중앙의 밝은 백색 냉기.
         */
        int innerCount =
                9;


        for (
                int i = 0;
                i < innerCount;
                i++
        ) {

            double angle =
                    Math.toRadians(
                            tick * 28.0
                    )
                            + (
                                    Math.PI
                                            * 2.0
                                            * i
                                    / innerCount
                            );


            double radius =
                    1.20
                            + (
                                    i % 2
                            ) * 0.22;


            double y =
                    0.45
                            + (
                                    i % 5
                            ) * 0.52;


            Location point =
                    center.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    y,
                                    Math.sin(
                                            angle
                                    ) * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    frostWhite
            );


            if (
                    i % 3
                            == 0
            ) {

                world.spawnParticle(
                        Particle.END_ROD,
                        point,
                        1,
                        0.015,
                        0.025,
                        0.015,
                        0.0
                );
            }
        }


        /*
         * =====================================================
         * 4. FALLING SNOW
         * =====================================================
         *
         * 블리자드 상공에서 눈이 떨어진다.
         */
        if (
                tick % 2
                        == 0
        ) {

            world.spawnParticle(
                    Particle.SNOWFLAKE,
                    center.clone()
                            .add(
                                    0.0,
                                    4.2,
                                    0.0
                            ),
                    18,
                    3.6,
                    0.35,
                    3.6,
                    0.04
            );
        }


        /*
         * =====================================================
         * 5. GROUND FROST RINGS
         * =====================================================
         */
        if (
                tick % 2
                        == 0
        ) {

            double rotation =
                    Math.toRadians(
                            tick * 6.0
                    );


            double[] radii = {
                    1.75,
                    3.20,
                    4.75
            };


            for (
                    int ring = 0;
                    ring < radii.length;
                    ring++
            ) {

                double radius =
                        radii[ring];


                int points =
                        18
                                + ring * 8;


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
                                    / points
                            )
                                    + rotation
                                            * (
                                                    ring % 2 == 0
                                                            ? 1.0
                                                            : -1.0
                                            );


                    Location point =
                            center.clone()
                                    .add(
                                            Math.cos(
                                                    angle
                                            ) * radius,
                                            0.11
                                                    + ring * 0.025,
                                            Math.sin(
                                                    angle
                                            ) * radius
                                    );


                    world.spawnParticle(
                            Particle.DUST,
                            point,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            ring == 2
                                    ? deepBlue
                                    : iceBlue
                    );
                }
            }
        }


        /*
         * =====================================================
         * 6. ICE SHARD BURSTS
         * =====================================================
         *
         * 일정 간격으로 바깥쪽에
         * 얼음 파편이 튀어나간다.
         */
        if (
                tick % 6
                        == 0
        ) {

            int burstIndex =
                    tick / 6;


            for (
                    int i = 0;
                    i < 5;
                    i++
            ) {

                double angle =
                        Math.toRadians(
                                burstIndex * 37.0
                                        + i * 72.0
                        );


                double radius =
                        3.75
                                + (
                                        i % 2
                                ) * 0.55;


                Location point =
                        center.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * radius,
                                        0.8
                                                + (
                                                        i % 3
                                                ) * 0.65,
                                        Math.sin(
                                                angle
                                        ) * radius
                                );


                world.spawnParticle(
                        Particle.END_ROD,
                        point,
                        2,
                        0.06,
                        0.12,
                        0.06,
                        0.025
                );


                world.spawnParticle(
                        Particle.DUST,
                        point,
                        2,
                        0.08,
                        0.10,
                        0.08,
                        0.0,
                        frostWhite
                );
            }
        }


        /*
         * =====================================================
         * 7. GROUND MIST
         * =====================================================
         */
        if (
                tick % 3
                        == 0
        ) {

            world.spawnParticle(
                    Particle.CLOUD,
                    center.clone()
                            .add(
                                    0.0,
                                    0.16,
                                    0.0
                            ),
                    6,
                    2.7,
                    0.07,
                    2.7,
                    0.012
            );
        }
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


        Particle.DustOptions iceBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                75,
                                190,
                                255
                        ),
                        1.10f
                );


        Particle.DustOptions frostWhite =
                new Particle.DustOptions(
                        Color.fromRGB(
                                230,
                                250,
                                255
                        ),
                        1.05f
                );


        world.spawnParticle(
                Particle.SNOWFLAKE,
                location,
                15,
                0.38,
                0.48,
                0.38,
                0.055
        );


        world.spawnParticle(
                Particle.DUST,
                location,
                8,
                0.30,
                0.35,
                0.30,
                0.0,
                iceBlue
        );


        world.spawnParticle(
                Particle.END_ROD,
                location.clone()
                        .add(
                                0.0,
                                0.10,
                                0.0
                        ),
                4,
                0.18,
                0.25,
                0.18,
                0.02
        );


        world.spawnParticle(
                Particle.DUST,
                location.clone()
                        .add(
                                0.0,
                                0.15,
                                0.0
                        ),
                4,
                0.15,
                0.20,
                0.15,
                0.0,
                frostWhite
        );
    }


    /*
     * =========================================================
     * FINISH EFFECT
     * =========================================================
     */
    private void spawnFinishEffect(
            Location center
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.SNOWFLAKE,
                center.clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        ),
                35,
                1.6,
                1.0,
                1.6,
                0.06
        );


        world.playSound(
                center,
                Sound.BLOCK_GLASS_BREAK,
                0.9f,
                0.7f
        );
    }
}
