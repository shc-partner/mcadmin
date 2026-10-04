package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.util.Vector;


public final class WarriorBladeStormSkill {

    /*
     * =========================================================
     * LEVEL 80 - BLADE STORM
     * =========================================================
     *
     * 플레이어를 중심으로 거대한 칼날 폭풍을 일으킨다.
     *
     * 이동하면서 주변 360도 범위의 적을
     * 연속으로 공격할 수 있다.
     *
     * 휠윈드의 상위 광역 지속 공격 스킬.
     */

    public static final double MANA_COST =
            35.0;

    public static final long COOLDOWN_MILLIS =
            30000L;


    /*
     * 1회 공격:
     *
     * 총 ATK x 0.375
     *
     * 총 8회:
     *
     * ATK x 0.375 x 8
     * = ATK x 3.00
     */
    public static final double DAMAGE_MULTIPLIER =
            0.375;


    /*
     * 플레이어 중심 5블록.
     */
    public static final double RANGE =
            5.0;


    /*
     * 총 8회 공격.
     */
    private static final int HIT_COUNT =
            8;


    /*
     * 0.5초 간격.
     *
     * 10 tick = 0.5초
     */
    private static final long HIT_INTERVAL_TICKS =
            10L;


    private final RPGCorePlugin plugin;

    private final HudService hudService;

    private final WarriorWeaponAttackService
            weaponAttackService;


    public WarriorBladeStormSkill(
            RPGCorePlugin plugin,
            HudService hudService,
            WarriorWeaponAttackService weaponAttackService
    ) {

        this.plugin =
                plugin;

        this.hudService =
                hudService;

        this.weaponAttackService =
                weaponAttackService;
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


        HudSnapshot snapshot =
                hudService.getProfile(
                        player.getUniqueId()
                );


        if (snapshot == null) {
            return false;
        }


        /*
         * 휠윈드 / 배쉬와 동일한 방식으로
         * 현재 스킬 아이템이 아니라
         * 마지막 주무기를 포함한 전사 총 ATK를 사용한다.
         */
        final double totalAttack =
                weaponAttackService
                        .resolveBashAttack(
                                player,
                                snapshot
                        );


        final double damage =
                totalAttack
                        * DAMAGE_MULTIPLIER;


        spawnStartEffect(
                player
        );


        /*
         * 공격 판정과 별도로
         * 매 tick 움직이는 Blade Storm VFX를 시작한다.
         */
        startBladeStormVisual(
                player
        );


        /*
         * 플레이어 위치를 고정하지 않는다.
         *
         * 매 타격마다 현재 위치를 다시 조회하므로
         * 블레이드 스톰 도중에도 이동할 수 있다.
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


                performHit(
                        player,
                        damage,
                        hitIndex
                );


                hitIndex++;


                if (
                        hitIndex
                                >= HIT_COUNT
                ) {

                    spawnFinishEffect(
                            player
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
     * BLADE STORM HIT
     * =========================================================
     */
    private void performHit(
            Player player,
            double damage,
            int hitIndex
    ) {

        Location origin =
                player.getLocation()
                        .clone();


        for (
                Entity entity
                : player.getNearbyEntities(
                        RANGE,
                        RANGE,
                        RANGE
                )
        ) {

            /*
             * RPG 공격 스킬 공통 정책:
             *
             * 적대 몬스터만 공격.
             *
             * 플레이어 / 동물 / NPC에는 피해 없음.
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
             * getNearbyEntities()의 직육면체 판정을
             * 실제 원형 공격 범위로 제한한다.
             */
            if (
                    horizontalDistanceSquared
                            > RANGE * RANGE
            ) {
                continue;
            }


            /*
             * 다른 층의 적까지 공격되는 것을 방지.
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
             * 실제 RPG 스킬 피해.
             *
             * 버서커 레이지 공격 배율은
             * CombatListener에서 공통 적용된다.
             */
            target.damage(
                    damage,
                    player
            );


            spawnHitEffect(
                    target,
                    hitIndex
            );
        }


        World world =
                player.getWorld();


        world.playSound(
                origin,
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.15f,
                0.72f
                        + (
                                hitIndex
                                        * 0.045f
                        )
        );
    }


    /*
     * =========================================================
     * START EFFECT
     * =========================================================
     */
    private void spawnStartEffect(
            Player player
    ) {

        Location location =
                player.getLocation()
                        .clone()
                        .add(
                                0.0,
                                0.6,
                                0.0
                        );


        World world =
                player.getWorld();


        world.spawnParticle(
                Particle.CLOUD,
                location,
                30,
                1.4,
                0.35,
                1.4,
                0.08
        );


        world.spawnParticle(
                Particle.CRIT,
                location.clone()
                        .add(
                                0.0,
                                0.5,
                                0.0
                        ),
                35,
                1.2,
                0.8,
                1.2,
                0.18
        );


        world.playSound(
                location,
                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                1.3f,
                0.62f
        );
    }


    /*
     * =========================================================
     * BLADE STORM CONTINUOUS VISUAL
     * =========================================================
     *
     * 공격 판정:
     *
     * 0 / 10 / 20 / 30 / 40 / 50 / 60 / 70 tick
     *
     * VFX는 별도로 매 tick 실행한다.
     */
    private void startBladeStormVisual(
            Player player
    ) {

        final int durationTicks =
                72;


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


                spawnBladeStormFrame(
                        player,
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
     * BLADE STORM FRAME
     * =========================================================
     */
    private void spawnBladeStormFrame(
            Player player,
            int tick
    ) {

        World world =
                player.getWorld();


        Location origin =
                player.getLocation()
                        .clone();


        /*
         * =====================================================
         * COLOR
         * =====================================================
         */

        Particle.DustOptions darkRed =
                new Particle.DustOptions(
                        Color.fromRGB(
                                125,
                                8,
                                8
                        ),
                        1.20f
                );


        Particle.DustOptions red =
                new Particle.DustOptions(
                        Color.fromRGB(
                                230,
                                24,
                                12
                        ),
                        1.35f
                );


        Particle.DustOptions orange =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                92,
                                12
                        ),
                        1.25f
                );


        Particle.DustOptions bladeCore =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                235,
                                210
                        ),
                        1.10f
                );


        /*
         * =====================================================
         * 5 MAIN BLADES
         * =====================================================
         *
         * 휠윈드보다 훨씬 큰 5개의 칼날을 만든다.
         */
        int bladeCount =
                5;


        for (
                int blade = 0;
                blade < bladeCount;
                blade++
        ) {

            /*
             * 일부 칼날은 반대 방향으로 회전한다.
             */
            double spinDirection =
                    blade % 2 == 0
                            ? 1.0
                            : -1.0;


            double baseAngle =
                    Math.toRadians(
                            tick
                                    * (
                                            28.0
                                                    + blade
                                                    * 2.5
                                    )
                                    * spinDirection
                    )
                            + (
                                    Math.PI
                                            * 2.0
                                            * blade
                                    / bladeCount
                            );


            /*
             * 각각 약간 다른 반경을 사용한다.
             */
            double baseRadius =
                    2.25
                            + (
                                    blade % 3
                            ) * 0.52;


            /*
             * 칼날 높이도 서로 다르게 해서
             * 평면 링이 아니라 폭풍처럼 만든다.
             */
            double baseHeight =
                    0.65
                            + (
                                    blade % 3
                            ) * 0.48;


            int segments =
                    14;


            for (
                    int i = 0;
                    i < segments;
                    i++
            ) {

                double progress =
                        (double) i
                                / (
                                        segments
                                                - 1
                                );


                /*
                 * 약 95도 길이의 검기 꼬리.
                 */
                double trail =
                        Math.toRadians(
                                i * 7.3
                        );


                double angle =
                        baseAngle
                                - (
                                        trail
                                                * spinDirection
                                );


                /*
                 * 칼날 꼬리에서 끝으로 갈수록
                 * 바깥으로 펼쳐진다.
                 */
                double radius =
                        baseRadius
                                + progress
                                        * 1.35;


                /*
                 * 칼날 전체가 살짝 위아래로 요동한다.
                 */
                double verticalWave =
                        Math.sin(
                                angle * 2.2
                                        + tick * 0.24
                                        + blade
                        ) * 0.17;


                /*
                 * 칼날 끝을 약간 올린다.
                 */
                double y =
                        baseHeight
                                + verticalWave
                                + progress
                                        * 0.18;


                Location point =
                        origin.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * radius,
                                        y,
                                        Math.sin(
                                                angle
                                        ) * radius
                                );


                /*
                 * -------------------------------------------------
                 * OUTER RED BLADE
                 * -------------------------------------------------
                 */
                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        i < 4
                                ? darkRed
                                : red
                );


                /*
                 * -------------------------------------------------
                 * ORANGE INNER EDGE
                 * -------------------------------------------------
                 */
                if (
                        i
                                >= 4
                ) {

                    Location innerPoint =
                            point.clone()
                                    .add(
                                            0.0,
                                            0.035,
                                            0.0
                                    );


                    world.spawnParticle(
                            Particle.DUST,
                            innerPoint,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            orange
                    );
                }


                /*
                 * -------------------------------------------------
                 * WHITE BLADE CORE
                 * -------------------------------------------------
                 *
                 * 마지막 부분은 밝은 칼날 중심.
                 */
                if (
                        i
                                >= segments - 5
                ) {

                    Location corePoint =
                            point.clone()
                                    .add(
                                            0.0,
                                            0.065,
                                            0.0
                                    );


                    world.spawnParticle(
                            Particle.DUST,
                            corePoint,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            bladeCore
                    );
                }


                /*
                 * 칼날 최종 끝점.
                 */
                if (
                        i
                                == segments - 1
                ) {

                    world.spawnParticle(
                            Particle.CRIT,
                            point,
                            2,
                            0.04,
                            0.05,
                            0.04,
                            0.018
                    );
                }
            }
        }


        /*
         * =====================================================
         * INNER STORM
         * =====================================================
         *
         * 플레이어 가까이에 작은 고속 검기 링을 추가한다.
         */
        int innerBlades =
                4;


        for (
                int i = 0;
                i < innerBlades;
                i++
        ) {

            double angle =
                    Math.toRadians(
                            tick * -36.0
                    )
                            + (
                                    Math.PI
                                            * 2.0
                                            * i
                                    / innerBlades
                            );


            double radius =
                    1.45;


            Location point =
                    origin.clone()
                            .add(
                                    Math.cos(
                                            angle
                                    ) * radius,
                                    1.05
                                            + Math.sin(
                                                    angle * 2.0
                                            ) * 0.14,
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
                    orange
            );


            if (
                    tick % 3
                            == 0
            ) {

                world.spawnParticle(
                        Particle.CRIT,
                        point,
                        1,
                        0.03,
                        0.04,
                        0.03,
                        0.01
                );
            }
        }


        /*
         * =====================================================
         * OUTWARD FLYING SLASHES
         * =====================================================
         *
         * 일정 간격으로 칼날 일부가
         * 폭풍 바깥으로 튀어나가는 느낌.
         */
        if (
                tick % 5
                        == 0
        ) {

            int burstIndex =
                    tick / 5;


            for (
                    int i = 0;
                    i < 3;
                    i++
            ) {

                double angle =
                        Math.toRadians(
                                burstIndex * 51.0
                                        + i * 120.0
                        );


                double radius =
                        4.15
                                + (
                                        i * 0.22
                                );


                Location burst =
                        origin.clone()
                                .add(
                                        Math.cos(
                                                angle
                                        ) * radius,
                                        0.85
                                                + i * 0.32,
                                        Math.sin(
                                                angle
                                        ) * radius
                                );


                world.spawnParticle(
                        Particle.SWEEP_ATTACK,
                        burst,
                        1,
                        0.04,
                        0.08,
                        0.04,
                        0.0
                );


                world.spawnParticle(
                        Particle.DUST,
                        burst,
                        2,
                        0.10,
                        0.08,
                        0.10,
                        0.0,
                        red
                );
            }
        }


        /*
         * =====================================================
         * GROUND STORM
         * =====================================================
         *
         * 발밑은 짙은 회오리 바람.
         */
        if (
                tick % 2
                        == 0
        ) {

            world.spawnParticle(
                    Particle.CLOUD,
                    origin.clone()
                            .add(
                                    0.0,
                                    0.18,
                                    0.0
                            ),
                    5,
                    1.35,
                    0.07,
                    1.35,
                    0.018
            );
        }


        /*
         * 폭풍 중심의 작은 불꽃성 파편.
         */
        if (
                tick % 4
                        == 0
        ) {

            world.spawnParticle(
                    Particle.CRIT,
                    origin.clone()
                            .add(
                                    0.0,
                                    1.05,
                                    0.0
                            ),
                    5,
                    1.20,
                    0.70,
                    1.20,
                    0.035
            );
        }
    }


    /*
     * =========================================================
     * HIT EFFECT
     * =========================================================
     */
    private void spawnHitEffect(
            LivingEntity target,
            int hitIndex
    ) {

        Location hitLocation =
                target.getLocation()
                        .clone()
                        .add(
                                0.0,
                                Math.min(
                                        1.1,
                                        target.getHeight()
                                                * 0.5
                                ),
                                0.0
                        );


        World world =
                target.getWorld();


        Particle.DustOptions red =
                new Particle.DustOptions(
                        Color.fromRGB(
                                235,
                                25,
                                12
                        ),
                        1.20f
                );


        Particle.DustOptions orange =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                105,
                                18
                        ),
                        1.05f
                );


        /*
         * 검날 타격 파편.
         */
        world.spawnParticle(
                Particle.CRIT,
                hitLocation,
                12,
                0.38,
                0.45,
                0.38,
                0.16
        );


        /*
         * 붉은 칼날 파편.
         */
        world.spawnParticle(
                Particle.DUST,
                hitLocation,
                8,
                0.34,
                0.38,
                0.34,
                0.0,
                red
        );


        world.spawnParticle(
                Particle.DUST,
                hitLocation,
                5,
                0.25,
                0.30,
                0.25,
                0.0,
                orange
        );


        world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                hitLocation,
                5,
                0.25,
                0.32,
                0.25,
                0.08
        );


        /*
         * 짝수 타격에는
         * 눈에 보이는 큰 검기 한 번.
         */
        if (
                hitIndex % 2
                        == 0
        ) {

            world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    hitLocation,
                    1,
                    0.08,
                    0.10,
                    0.08,
                    0.0
            );
        }
    }


    /*
     * =========================================================
     * FINISH EFFECT
     * =========================================================
     */
    private void spawnFinishEffect(
            Player player
    ) {

        Location location =
                player.getLocation()
                        .clone()
                        .add(
                                0.0,
                                0.9,
                                0.0
                        );


        World world =
                player.getWorld();


        world.spawnParticle(
                Particle.CLOUD,
                location,
                38,
                2.4,
                0.55,
                2.4,
                0.07
        );


        world.spawnParticle(
                Particle.CRIT,
                location,
                28,
                1.7,
                0.8,
                1.7,
                0.16
        );


        world.playSound(
                location,
                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                1.25f,
                0.82f
        );
    }
}
