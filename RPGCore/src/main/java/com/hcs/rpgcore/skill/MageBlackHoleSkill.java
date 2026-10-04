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

import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;


public final class MageBlackHoleSkill {

    /*
     * =========================================================
     * LEVEL 60 - BLACK HOLE
     * =========================================================
     *
     * 바라보는 위치에 블랙홀을 생성한다.
     *
     * 일정 시간 동안 범위 내 적을
     * 중심으로 끌어당기면서 지속 피해를 준다.
     */

    public static final double MANA_COST =
            45.0;

    public static final long COOLDOWN_MILLIS =
            25000L;


    /*
     * 1회 피해:
     * 6 + ATK x 0.10 + 최대 MP x 0.008
     *
     * 총 10회 풀히트:
     * 60 + ATK x 1.00 + 최대 MP x 0.08
     */
    public static final double BASE_DAMAGE =
            6.0;

    public static final double ATTACK_RATIO =
            0.10;

    public static final double MAX_MANA_RATIO =
            0.008;


    /*
     * 최대 시전 거리.
     */
    private static final double CAST_RANGE =
            18.0;


    /*
     * 블랙홀 효과 반경.
     */
    private static final double AREA_RADIUS =
            6.0;


    /*
     * 5초 동안 0.5초 간격.
     *
     * 총 10회.
     */
    private static final int HIT_COUNT =
            10;

    private static final long HIT_INTERVAL_TICKS =
            10L;


    /*
     * 적을 중심으로 끌어당기는 힘.
     */
    private static final double PULL_STRENGTH =
            0.38;


    /*
     * 과도하게 위아래로 당겨지는 것을 방지.
     */
    private static final double MAX_VERTICAL_PULL =
            0.14;


    private final RPGCorePlugin plugin;

    private final ManaService manaService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public MageBlackHoleSkill(
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
         * 블랙홀 1회 피해를 +30% 증가시킨다.
         *
         * 이후 10회 지속 타격 모두
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
         * 공격 판정과 별도로
         * 블랙홀 자체와 흡입 소용돌이를 매 tick 표현한다.
         */
        startBlackHoleVisual(
                center
        );


        /*
         * =====================================================
         * BLACK HOLE LOOP
         * =====================================================
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


                performBlackHoleTick(
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

            return result
                    .getHitPosition()
                    .toLocation(
                            player.getWorld()
                    );
        }


        /*
         * 블록을 바라보지 않는 경우
         * 최대 사거리 지점에 생성한다.
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
     * BLACK HOLE TICK
     * =========================================================
     */
    private void performBlackHoleTick(
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


        spawnBlackHoleEffect(
                center,
                hitIndex
        );


        for (
                Entity entity
                : world.getNearbyEntities(
                        center,
                        AREA_RADIUS,
                        4.5,
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
                    ) > 4.5
            ) {
                continue;
            }


            /*
             * =================================================
             * PULL
             * =================================================
             */
            Vector pull =
                    center.toVector()
                            .subtract(
                                    targetLocation.toVector()
                            );


            if (
                    pull.lengthSquared()
                            > 0.04
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
                 * 기존 속도를 크게 줄인 뒤
                 * 블랙홀 방향 힘을 더한다.
                 */
                Vector velocity =
                        target.getVelocity()
                                .clone()
                                .multiply(
                                        0.30
                                )
                                .add(
                                        pull
                                );


                target.setVelocity(
                        velocity
                );
            }


            /*
             * RPG 지속 피해.
             */
            target.damage(
                    damage,
                    player
            );


            spawnTargetEffect(
                    target
            );
        }


        world.playSound(
                center,
                Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,
                0.30f,
                0.55f
                        + (
                                hitIndex
                                        * 0.015f
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
                Particle.PORTAL,
                center.clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        ),
                60,
                1.5,
                1.2,
                1.5,
                0.20
        );


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                center.clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        ),
                35,
                1.2,
                1.0,
                1.2,
                0.15
        );


        world.playSound(
                center,
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                1.0f,
                0.55f
        );


        world.playSound(
                center,
                Sound.ENTITY_ENDERMAN_TELEPORT,
                0.75f,
                0.55f
        );
    }


    /*
     * =========================================================
     * BLACK HOLE VISUAL
     * =========================================================
     *
     * 실제 피해 tick마다 중심부를 한 번 강하게 맥동시킨다.
     *
     * 지속적인 구체와 흡입 애니메이션은
     * startBlackHoleVisual()에서 별도로 처리한다.
     */
    private void spawnBlackHoleEffect(
            Location center,
            int hitIndex
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        Location core =
                center.clone()
                        .add(
                                0.0,
                                1.20,
                                0.0
                        );


        Particle.DustOptions violet =
                new Particle.DustOptions(
                        Color.fromRGB(
                                145,
                                40,
                                255
                        ),
                        1.25f
                );


        Particle.DustOptions brightViolet =
                new Particle.DustOptions(
                        Color.fromRGB(
                                210,
                                90,
                                255
                        ),
                        1.10f
                );


        /*
         * 피해가 발생할 때마다
         * 사건의 지평선 주변이 순간적으로 밝아진다.
         */
        double pulseRadius =
                0.95
                        + (
                                hitIndex % 2
                        ) * 0.16;


        int points =
                28;


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
                            + hitIndex * 0.28;


            Location point =
                    core.clone()
                            .add(
                                    Math.cos(angle)
                                            * pulseRadius,
                                    Math.sin(
                                            angle * 2.0
                                    ) * 0.08,
                                    Math.sin(angle)
                                            * pulseRadius
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
                            ? violet
                            : brightViolet
            );
        }


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                core,
                16,
                0.65,
                0.45,
                0.65,
                0.08
        );
    }


    /*
     * =========================================================
     * BLACK HOLE CONTINUOUS VISUAL
     * =========================================================
     */
    private void startBlackHoleVisual(
            Location center
    ) {

        final Location fixedCenter =
                center.clone();


        /*
         * 실제 공격:
         *
         * 0 / 10 / 20 / ... / 90 tick
         *
         * 마지막 타격 직후까지 VFX 유지.
         */
        final int durationTicks =
                94;


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


                spawnBlackHoleFrame(
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
                                0.0,
                                1.20,
                                0.0
                        );


        Particle.DustOptions black =
                new Particle.DustOptions(
                        Color.fromRGB(
                                2,
                                2,
                                4
                        ),
                        1.55f
                );


        Particle.DustOptions darkPurple =
                new Particle.DustOptions(
                        Color.fromRGB(
                                45,
                                5,
                                75
                        ),
                        1.35f
                );


        Particle.DustOptions purple =
                new Particle.DustOptions(
                        Color.fromRGB(
                                115,
                                20,
                                220
                        ),
                        1.20f
                );


        Particle.DustOptions violet =
                new Particle.DustOptions(
                        Color.fromRGB(
                                190,
                                55,
                                255
                        ),
                        1.10f
                );


        /*
         * =====================================================
         * 1. BLACK SPHERE
         * =====================================================
         *
         * 구형 표면에 검은 입자를 배치해서
         * 중심에 실제 검은 구체가 있는 것처럼 보이게 한다.
         */
        if (
                tick % 2
                        == 0
        ) {

            double sphereRadius =
                    0.72;


            int latitudeCount =
                    7;


            int longitudeCount =
                    12;


            for (
                    int lat = 1;
                    lat < latitudeCount;
                    lat++
            ) {

                double phi =
                        Math.PI
                                * lat
                                / latitudeCount;


                double y =
                        Math.cos(phi)
                                * sphereRadius;


                double ringRadius =
                        Math.sin(phi)
                                * sphereRadius;


                for (
                        int lon = 0;
                        lon < longitudeCount;
                        lon++
                ) {

                    double angle =
                            Math.PI
                                    * 2.0
                                    * lon
                                    / longitudeCount
                                    + tick * 0.025;


                    Location point =
                            core.clone()
                                    .add(
                                            Math.cos(angle)
                                                    * ringRadius,
                                            y,
                                            Math.sin(angle)
                                                    * ringRadius
                                    );


                    world.spawnParticle(
                            Particle.DUST,
                            point,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            black
                    );
                }
            }


            /*
             * 검은 구체 가장자리의
             * 아주 어두운 보라색 윤곽.
             */
            int edgePoints =
                    24;


            for (
                    int i = 0;
                    i < edgePoints;
                    i++
            ) {

                double angle =
                        Math.PI
                                * 2.0
                                * i
                                / edgePoints
                                + tick * 0.04;


                Location point =
                        core.clone()
                                .add(
                                        Math.cos(angle)
                                                * 0.80,
                                        Math.sin(
                                                angle * 2.0
                                        ) * 0.08,
                                        Math.sin(angle)
                                                * 0.80
                                );


                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        darkPurple
                );
            }
        }


        /*
         * =====================================================
         * 2. ACCRETION DISC
         * =====================================================
         *
         * 검은 구체 주위를 빠르게 도는
         * 밝은 보라색 강착원반.
         */
        int discPoints =
                30;


        for (
                int i = 0;
                i < discPoints;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / discPoints
                            + Math.toRadians(
                                    tick * 13.0
                            );


            /*
             * 완벽한 원이 아니라
             * 약간 불규칙한 원반.
             */
            double radius =
                    0.98
                            + Math.sin(
                                    angle * 3.0
                                            + tick * 0.15
                            ) * 0.12;


            Location point =
                    core.clone()
                            .add(
                                    Math.cos(angle)
                                            * radius,
                                    Math.sin(
                                            angle * 2.0
                                    ) * 0.065,
                                    Math.sin(angle)
                                            * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    i % 3 == 0
                            ? violet
                            : purple
            );
        }


        /*
         * 두 번째 얇은 원반은 반대 방향으로 회전.
         */
        for (
                int i = 0;
                i < 18;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / 18.0
                            - Math.toRadians(
                                    tick * 18.0
                            );


            double radius =
                    1.25;


            Location point =
                    core.clone()
                            .add(
                                    Math.cos(angle)
                                            * radius,
                                    Math.sin(angle)
                                            * 0.16,
                                    Math.sin(angle)
                                            * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    darkPurple
            );
        }


        /*
         * =====================================================
         * 3. INWARD SPIRAL STREAMS
         * =====================================================
         *
         * 핵심 효과:
         *
         * 바깥에 있던 입자가 시간이 지날수록
         * 회전하면서 검은 구체 안쪽으로 빨려 들어간다.
         */
        int streams =
                14;


        int particlesPerStream =
                4;


        for (
                int stream = 0;
                stream < streams;
                stream++
        ) {

            double streamOffset =
                    (double) stream
                            / streams;


            for (
                    int p = 0;
                    p < particlesPerStream;
                    p++
            ) {

                /*
                 * 0 -> 바깥
                 * 1 -> 블랙홀 중심
                 *
                 * 시간이 흐르면서 progress가 반복된다.
                 */
                double progress =
                        (
                                tick * 0.055
                                        + streamOffset
                                        + p * 0.18
                        ) % 1.0;


                /*
                 * 바깥쪽에서는 천천히,
                 * 중심에 가까워질수록 빠르게 수축.
                 */
                double radius =
                        0.82
                                + AREA_RADIUS
                                        * Math.pow(
                                                1.0 - progress,
                                                1.35
                                        );


                /*
                 * 중심으로 갈수록 더 많이 회전해서
                 * 빨려드는 느낌을 강화한다.
                 */
                double angle =
                        Math.PI
                                * 2.0
                                * streamOffset
                                + tick * 0.10
                                + progress
                                        * progress
                                        * 12.0;


                /*
                 * 상하에서도 중심으로 수렴.
                 */
                double initialHeight =
                        (
                                stream % 5
                                        - 2
                        ) * 0.58;


                double y =
                        initialHeight
                                * (
                                        1.0 - progress
                                )
                                + Math.sin(
                                        angle * 1.7
                                ) * 0.12;


                Location point =
                        core.clone()
                                .add(
                                        Math.cos(angle)
                                                * radius,
                                        y,
                                        Math.sin(angle)
                                                * radius
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
                                ? violet
                                : (
                                        progress > 0.40
                                                ? purple
                                                : darkPurple
                                )
                );


                /*
                 * 일부 입자는 포탈 효과를 섞어
                 * 실제로 안쪽으로 빨려드는 듯한 움직임을 강조.
                 */
                if (
                        p == 0
                        && stream % 2 == 0
                ) {

                    world.spawnParticle(
                            Particle.REVERSE_PORTAL,
                            point,
                            1,
                            0.015,
                            0.015,
                            0.015,
                            0.025
                    );
                }
            }
        }


        /*
         * =====================================================
         * 4. UPPER / LOWER INFALL
         * =====================================================
         *
         * 평면 소용돌이로만 보이지 않도록
         * 위아래 입자도 중심으로 수축시킨다.
         */
        int verticalStreams =
                8;


        for (
                int i = 0;
                i < verticalStreams;
                i++
        ) {

            double progress =
                    (
                            tick * 0.045
                                    + i
                                            / (
                                                    double
                                            ) verticalStreams
                    ) % 1.0;


            double radius =
                    3.8
                            * (
                                    1.0 - progress
                            )
                            + 0.75;


            double angle =
                    Math.PI
                            * 2.0
                            * i
                            / verticalStreams
                            - progress * 7.0;


            double y =
                    (
                            i % 2 == 0
                                    ? 1.0
                                    : -1.0
                    )
                            * (
                                    2.3
                                            * (
                                                    1.0
                                                            - progress
                                            )
                            );


            Location point =
                    core.clone()
                            .add(
                                    Math.cos(angle)
                                            * radius,
                                    y,
                                    Math.sin(angle)
                                            * radius
                            );


            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    purple
            );
        }


        /*
         * =====================================================
         * 5. EVENT HORIZON
         * =====================================================
         *
         * 입자가 사라지는 경계.
         */
        if (
                tick % 2
                        == 0
        ) {

            world.spawnParticle(
                    Particle.REVERSE_PORTAL,
                    core,
                    8,
                    0.74,
                    0.50,
                    0.74,
                    0.055
            );
        }


        /*
         * =====================================================
         * 6. OUTER DISTORTION
         * =====================================================
         *
         * 블랙홀 전체 범위 가장자리에도
         * 소량의 암흑 입자를 둔다.
         */
        if (
                tick % 4
                        == 0
        ) {

            int boundaryPoints =
                    20;


            for (
                    int i = 0;
                    i < boundaryPoints;
                    i++
            ) {

                double angle =
                        Math.PI
                                * 2.0
                                * i
                                / boundaryPoints
                                - tick * 0.035;


                Location point =
                        center.clone()
                                .add(
                                        Math.cos(angle)
                                                * AREA_RADIUS,
                                        0.18,
                                        Math.sin(angle)
                                                * AREA_RADIUS
                                );


                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        darkPurple
                );
            }
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


        World world =
                target.getWorld();


        Particle.DustOptions purple =
                new Particle.DustOptions(
                        Color.fromRGB(
                                125,
                                25,
                                220
                        ),
                        1.10f
                );


        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                location,
                8,
                0.30,
                0.40,
                0.30,
                0.06
        );


        world.spawnParticle(
                Particle.DUST,
                location,
                5,
                0.26,
                0.32,
                0.26,
                0.0,
                purple
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


        Location core =
                center.clone()
                        .add(
                                0.0,
                                1.20,
                                0.0
                        );


        Particle.DustOptions black =
                new Particle.DustOptions(
                        Color.fromRGB(
                                2,
                                2,
                                4
                        ),
                        1.50f
                );


        Particle.DustOptions violet =
                new Particle.DustOptions(
                        Color.fromRGB(
                                185,
                                45,
                                255
                        ),
                        1.20f
                );


        /*
         * 마지막 순간 모든 것이 중심으로
         * 수축한 듯한 암흑 폭발.
         */
        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                core,
                70,
                1.15,
                1.00,
                1.15,
                0.24
        );


        world.spawnParticle(
                Particle.DUST,
                core,
                35,
                0.55,
                0.55,
                0.55,
                0.0,
                black
        );


        world.spawnParticle(
                Particle.DUST,
                core,
                18,
                0.85,
                0.55,
                0.85,
                0.0,
                violet
        );


        world.spawnParticle(
                Particle.CLOUD,
                core,
                10,
                0.45,
                0.40,
                0.45,
                0.025
        );


        world.playSound(
                center,
                Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                0.9f,
                0.55f
        );
    }

}