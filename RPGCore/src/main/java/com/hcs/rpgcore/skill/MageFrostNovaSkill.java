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


public final class MageFrostNovaSkill {

    /*
     * =========================================================
     * LEVEL 20 - FROST NOVA
     * =========================================================
     *
     * 플레이어를 중심으로 강력한 냉기 폭발을 발생시킨다.
     *
     * 주변 적에게 ATK + 최대 MP 기반 피해를 주고
     * 일정 시간 이동 속도를 감소시킨다.
     */

    public static final double MANA_COST =
            12.0;

    public static final long COOLDOWN_MILLIS =
            12000L;


    /*
     * 피해 공식:
     * 15 + ATK x 0.35 + 최대 MP x 0.03
     */
    public static final double BASE_DAMAGE =
            15.0;

    public static final double ATTACK_RATIO =
            0.35;

    public static final double MAX_MANA_RATIO =
            0.03;


    /*
     * 플레이어 중심 공격 반경.
     */
    public static final double RANGE =
            5.0;


    /*
     * 둔화 지속시간.
     *
     * 20 tick = 1초
     * 80 tick = 4초
     */
    private static final int SLOW_DURATION_TICKS =
            80;


    /*
     * PotionEffect amplifier:
     *
     * 0 = Slowness I
     * 1 = Slowness II
     * 2 = Slowness III
     */
    private static final int SLOW_AMPLIFIER =
            2;


    private final RPGCorePlugin plugin;

    private final ManaService manaService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public MageFrostNovaSkill(
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


        double maximumMana =
                manaService.getMaximumMana(
                        player.getUniqueId()
                );


        /*
         * ATK + 최대 MP 기반 피해 공식.
         */
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


        double damage =
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
         * 활성 중이면 프로스트 노바 최종 피해 +30%.
         */
        if (offenseBuffService != null) {

            damage =
                    offenseBuffService
                            .applyMageDamageMultiplier(
                                    player.getUniqueId(),
                                    damage
                            );
        }


        Location origin =
                player.getLocation()
                        .clone();


        /*
         * 피해 판정보다 먼저
         * 냉기 폭발 시각 효과를 출력한다.
         */
        spawnNovaEffect(
                player,
                origin
        );


        /*
         * 플레이어 중심에서 냉기 원이
         * 바깥으로 확대되며 퍼져나간다.
         */
        startExpandingFrostRing(
                player
        );


        for (
                Entity entity
                : player.getNearbyEntities(
                        RANGE,
                        RANGE,
                        RANGE
                )
        ) {

            /*
             * 기존 공격 스킬과 동일하게
             * 적대 몬스터만 공격한다.
             *
             * 플레이어 / 동물 / NPC 오폭 방지.
             */
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
                            - origin.getX();

            double dz =
                    targetLocation.getZ()
                            - origin.getZ();


            double horizontalDistanceSquared =
                    dx * dx
                            + dz * dz;


            /*
             * getNearbyEntities()는 직육면체 범위이므로
             * 실제 원형 반경을 다시 검사한다.
             */
            if (
                    horizontalDistanceSquared
                            > RANGE * RANGE
            ) {
                continue;
            }


            /*
             * 다른 층의 몬스터까지 맞는 현상을 방지한다.
             */
            if (
                    Math.abs(
                            targetLocation.getY()
                                    - origin.getY()
                    ) > 3.0
            ) {
                continue;
            }


            /*
             * RPG 스킬 피해.
             */
            target.damage(
                    damage,
                    player
            );


            /*
             * 4초간 Slowness III.
             */
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


        return true;
    }


    /*
     * =========================================================
     * FROST NOVA VISUAL EFFECT
     * =========================================================
     */
    private void spawnNovaEffect(
            Player player,
            Location origin
    ) {

        World world =
                player.getWorld();


        Location center =
                origin.clone()
                        .add(
                                0.0,
                                0.18,
                                0.0
                        );


        Particle.DustOptions deepBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                35,
                                115,
                                255
                        ),
                        1.25f
                );


        Particle.DustOptions iceBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                90,
                                205,
                                255
                        ),
                        1.20f
                );


        Particle.DustOptions frostWhite =
                new Particle.DustOptions(
                        Color.fromRGB(
                                225,
                                250,
                                255
                        ),
                        1.10f
                );


        /*
         * =====================================================
         * 1. CENTRAL FROST EXPLOSION
         * =====================================================
         */
        world.spawnParticle(
                Particle.SNOWFLAKE,
                center.clone()
                        .add(
                                0.0,
                                0.75,
                                0.0
                        ),
                55,
                1.05,
                0.90,
                1.05,
                0.10
        );


        world.spawnParticle(
                Particle.DUST,
                center.clone()
                        .add(
                                0.0,
                                0.85,
                                0.0
                        ),
                30,
                0.60,
                0.70,
                0.60,
                0.0,
                iceBlue
        );


        world.spawnParticle(
                Particle.DUST,
                center.clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        ),
                18,
                0.38,
                0.55,
                0.38,
                0.0,
                frostWhite
        );


        /*
         * =====================================================
         * 2. CENTRAL ICE SPIKES
         * =====================================================
         *
         * 중앙에서 얼음 결정이 위로 솟는 느낌.
         */
        int spikeCount =
                8;


        for (
                int spike = 0;
                spike < spikeCount;
                spike++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * spike
                            / spikeCount;


            double baseRadius =
                    0.35
                            + (
                                    spike % 2
                            ) * 0.18;


            int segments =
                    8;


            for (
                    int i = 0;
                    i < segments;
                    i++
            ) {

                double progress =
                        (double) i
                                / (
                                        segments - 1
                                );


                double radius =
                        baseRadius
                                + progress
                                        * 0.42;


                double y =
                        0.18
                                + progress
                                        * (
                                                1.25
                                                        + (
                                                                spike % 3
                                                        ) * 0.20
                                        );


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
                        i >= segments - 3
                                ? frostWhite
                                : iceBlue
                );


                if (
                        i == segments - 1
                ) {

                    world.spawnParticle(
                            Particle.SNOWFLAKE,
                            point,
                            2,
                            0.04,
                            0.06,
                            0.04,
                            0.015
                    );
                }
            }
        }


        /*
         * =====================================================
         * 3. FROST RINGS
         * =====================================================
         *
         * 아이콘처럼 중앙에서 바깥으로 퍼지는
         * 3중 냉기 파동.
         */
        double[] radii = {
                1.45,
                3.05,
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
                    24
                            + ring * 10;


            for (
                    int i = 0;
                    i < points;
                    i++
            ) {

                double angle =
                        Math.PI
                                * 2.0
                                * i
                                / points;


                double wave =
                        Math.sin(
                                angle * 4.0
                                        + ring
                        ) * 0.06;


                Location point =
                        center.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * radius,
                                        0.10
                                                + ring * 0.035
                                                + wave,
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
                        ring == 0
                                ? frostWhite
                                : (
                                        ring == 1
                                                ? iceBlue
                                                : deepBlue
                                )
                );


                if (
                        i % 3
                                == 0
                ) {

                    world.spawnParticle(
                            Particle.SNOWFLAKE,
                            point,
                            1,
                            0.015,
                            0.03,
                            0.015,
                            0.006
                    );
                }
            }
        }


        /*
         * =====================================================
         * 4. RADIAL ICE BLADES
         * =====================================================
         *
         * 외곽으로 뻗는 얼음 가시.
         */
        int rayCount =
                12;


        for (
                int ray = 0;
                ray < rayCount;
                ray++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * ray
                            / rayCount;


            int segments =
                    10;


            for (
                    int i = 1;
                    i <= segments;
                    i++
            ) {

                double progress =
                        (double) i
                                / segments;


                double radius =
                        0.65
                                + progress
                                        * 4.10;


                double sideWobble =
                        Math.sin(
                                progress
                                        * Math.PI
                                        * 2.0
                                        + ray
                        ) * 0.07;


                double x =
                        Math.cos(
                                angle
                        ) * radius
                                + Math.cos(
                                        angle
                                                + Math.PI / 2.0
                                ) * sideWobble;


                double z =
                        Math.sin(
                                angle
                        ) * radius
                                + Math.sin(
                                        angle
                                                + Math.PI / 2.0
                                ) * sideWobble;


                Location point =
                        center.clone()
                                .add(
                                        x,
                                        0.12
                                                + progress * 0.16,
                                        z
                                );


                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        progress > 0.72
                                ? frostWhite
                                : iceBlue
                );
            }
        }


        /*
         * =====================================================
         * 5. FROST MIST
         * =====================================================
         */
        world.spawnParticle(
                Particle.CLOUD,
                center,
                24,
                1.55,
                0.10,
                1.55,
                0.025
        );


        world.spawnParticle(
                Particle.SNOWFLAKE,
                center.clone()
                        .add(
                                0.0,
                                0.30,
                                0.0
                        ),
                24,
                2.20,
                0.25,
                2.20,
                0.025
        );


        /*
         * =====================================================
         * SOUND
         * =====================================================
         */
        world.playSound(
                origin,
                Sound.BLOCK_GLASS_BREAK,
                1.25f,
                0.72f
        );


        world.playSound(
                origin,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                1.0f,
                1.60f
        );


        world.playSound(
                origin,
                Sound.BLOCK_POWDER_SNOW_BREAK,
                0.9f,
                0.80f
        );
    }


    /*
     * =========================================================
     * EXPANDING FROST RING
     * =========================================================
     *
     * 플레이어를 중심으로 작은 원이 생성되고
     * 약 0.6초 동안 RANGE까지 확대된다.
     */
    private void startExpandingFrostRing(
            Player player
    ) {

        final int durationTicks =
                12;


        new BukkitRunnable() {

            private int tick =
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


                if (
                        tick
                                >= durationTicks
                ) {

                    cancel();
                    return;
                }


                spawnExpandingFrostRingFrame(
                        player,
                        tick,
                        durationTicks
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
     * EXPANDING FROST RING FRAME
     * =========================================================
     */
    private void spawnExpandingFrostRingFrame(
            Player player,
            int tick,
            int durationTicks
    ) {

        World world =
                player.getWorld();


        Location center =
                player.getLocation()
                        .clone()
                        .add(
                                0.0,
                                0.16,
                                0.0
                        );


        double progress =
                (double) (
                        tick + 1
                ) / durationTicks;


        /*
         * 초반에는 빠르게,
         * 후반에는 조금 부드럽게 퍼지는 easing.
         */
        double eased =
                1.0
                        - Math.pow(
                                1.0 - progress,
                                2.0
                        );


        double radius =
                0.45
                        + eased
                                * (
                                        RANGE - 0.45
                                );


        Particle.DustOptions frostWhite =
                new Particle.DustOptions(
                        Color.fromRGB(
                                225,
                                250,
                                255
                        ),
                        1.15f
                );


        Particle.DustOptions iceBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                70,
                                190,
                                255
                        ),
                        1.20f
                );


        Particle.DustOptions deepBlue =
                new Particle.DustOptions(
                        Color.fromRGB(
                                35,
                                105,
                                255
                        ),
                        1.05f
                );


        /*
         * 반경이 커질수록 점 수도 늘린다.
         */
        int points =
                Math.max(
                        20,
                        (int) Math.ceil(
                                radius * 11.0
                        )
                );


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / points;


            /*
             * 완벽하게 평평한 링보다
             * 약간 물결치는 냉기 링.
             */
            double wave =
                    Math.sin(
                            angle * 6.0
                                    + tick * 0.55
                    ) * 0.045;


            Location point =
                    center.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    wave,
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
                    tick < 4
                            ? frostWhite
                            : (
                                    tick < 8
                                            ? iceBlue
                                            : deepBlue
                            )
            );


            /*
             * 일부 위치에는 눈송이를 섞어서
             * 냉기 파동이 더 살아 움직이게 한다.
             */
            if (
                    i % 4
                            == 0
            ) {

                world.spawnParticle(
                        Particle.SNOWFLAKE,
                        point.clone()
                                .add(
                                        0.0,
                                        0.04,
                                        0.0
                                ),
                        1,
                        0.015,
                        0.025,
                        0.015,
                        0.006
                );
            }
        }


        /*
         * 링 안쪽에 얇은 두 번째 원을 하나 더 만들어
         * 단순 선 하나처럼 보이지 않게 한다.
         */
        if (
                radius
                        > 1.0
        ) {

            double innerRadius =
                    radius
                            - 0.22;


            int innerPoints =
                    Math.max(
                            16,
                            points - 8
                    );


            for (
                    int i = 0;
                    i < innerPoints;
                    i += 2
            ) {

                double angle =
                        Math.PI
                                * 2.0
                                * i
                                / innerPoints
                                + 0.08;


                Location point =
                        center.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * innerRadius,
                                        0.025,
                                        Math.sin(
                                                angle
                                        ) * innerRadius
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
            }
        }


        /*
         * 링이 지나간 바닥에 아주 옅은 서리 흔적.
         */
        if (
                tick % 2
                        == 0
        ) {

            world.spawnParticle(
                    Particle.SNOWFLAKE,
                    center,
                    7,
                    radius * 0.55,
                    0.05,
                    radius * 0.55,
                    0.008
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
                                80,
                                195,
                                255
                        ),
                        1.15f
                );


        Particle.DustOptions frostWhite =
                new Particle.DustOptions(
                        Color.fromRGB(
                                225,
                                250,
                                255
                        ),
                        1.05f
                );


        world.spawnParticle(
                Particle.SNOWFLAKE,
                location,
                18,
                0.42,
                0.58,
                0.42,
                0.06
        );


        world.spawnParticle(
                Particle.DUST,
                location,
                10,
                0.35,
                0.42,
                0.35,
                0.0,
                iceBlue
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
                0.20,
                0.28,
                0.20,
                0.0,
                frostWhite
        );


        world.spawnParticle(
                Particle.CLOUD,
                target.getLocation()
                        .clone()
                        .add(
                                0.0,
                                0.15,
                                0.0
                        ),
                6,
                0.28,
                0.08,
                0.28,
                0.018
        );
    }

}