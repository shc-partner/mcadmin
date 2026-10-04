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


public final class WarriorWhirlwindSkill {

    /*
     * =========================================================
     * LEVEL 20 - WHIRLWIND
     * =========================================================
     *
     * 플레이어를 중심으로 이동하면서
     * 주변 360도 범위의 적을 연속 공격한다.
     */

    public static final double MANA_COST =
            8.0;

    public static final long COOLDOWN_MILLIS =
            10000L;


    /*
     * 1회 공격 피해.
     *
     * 총 5회 적중 시:
     * ATK x 0.40 x 5
     * = ATK x 2.00
     */
    public static final double DAMAGE_MULTIPLIER =
            0.40;


    /*
     * 플레이어 중심 공격 반경.
     */
    public static final double RANGE =
            4.0;


    /*
     * 총 공격 횟수.
     */
    private static final int HIT_COUNT =
            5;


    /*
     * 0.4초 = 8 tick.
     */
    private static final long HIT_INTERVAL_TICKS =
            8L;


    private final RPGCorePlugin plugin;

    private final HudService hudService;

    private final WarriorWeaponAttackService
            weaponAttackService;


    public WarriorWhirlwindSkill(
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
         * 배쉬와 동일하게
         * 현재 손에 든 스킬 아이템이 아니라
         * 마지막 주무기를 포함한 전사 총 공격력을 사용한다.
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


        /*
         * 시전 시작 효과.
         */
        spawnStartEffect(
                player
        );


        /*
         * 공격 판정과 별개로 매 tick 회전하는
         * 휠윈드 전용 VFX를 시작한다.
         */
        startWhirlwindVisual(
                player
        );


        /*
         * 플레이어의 위치를 고정하지 않는다.
         *
         * 매 공격 시점마다 현재 위치를 다시 가져오므로
         * 휠윈드 사용 중에도 자유롭게 이동할 수 있다.
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


                /*
                 * 실제 360도 공격.
                 */
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
     * WHIRLWIND HIT
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
             * 기존 배쉬와 동일하게 적대 몹만 공격.
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


            /*
             * getNearbyEntities()는 직육면체이므로
             * 실제 원형 반경을 다시 검사한다.
             */
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


            if (
                    horizontalDistanceSquared
                            > RANGE * RANGE
            ) {
                continue;
            }


            /*
             * 지나치게 높은 층 / 아래층의 적까지
             * 맞지 않도록 Y 차이도 제한한다.
             */
            if (
                    Math.abs(
                            targetLocation.getY()
                                    - origin.getY()
                    ) > 2.75
            ) {
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


        World world =
                player.getWorld();


        world.playSound(
                origin,
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                0.85f
                        + (
                                hitIndex
                                        * 0.04f
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
                                0.15,
                                0.0
                        );


        World world =
                player.getWorld();


        world.spawnParticle(
                Particle.CLOUD,
                location,
                18,
                0.8,
                0.15,
                0.8,
                0.04
        );


        world.playSound(
                location,
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.2f,
                0.65f
        );
    }


    /*
     * =========================================================
     * WHIRLWIND CONTINUOUS VISUAL
     * =========================================================
     */
    private void startWhirlwindVisual(
            Player player
    ) {

        /*
         * 실제 공격 시점:
         *
         * 0 / 8 / 16 / 24 / 32 tick
         *
         * 마지막 공격 직후까지 VFX를 유지한다.
         */
        final int durationTicks =
                34;


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


                spawnWhirlwindFrame(
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
     * WHIRLWIND FRAME
     * =========================================================
     */
    private void spawnWhirlwindFrame(
            Player player,
            int tick
    ) {

        World world =
                player.getWorld();


        Location origin =
                player.getLocation()
                        .clone();


        /*
         * 플레이어의 허리 부근을 중심으로 회전한다.
         */
        Location center =
                origin.clone()
                        .add(
                                0.0,
                                0.95,
                                0.0
                        );


        /*
         * 매 tick 24도 회전.
         *
         * 약 15 tick마다 한 바퀴 회전한다.
         */
        double baseAngle =
                Math.toRadians(
                        tick * 24.0
                );


        /*
         * 아이콘의 불타는 주황색 검기를 표현한다.
         */
        Particle.DustOptions orangeBlade =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                92,
                                8
                        ),
                        1.35f
                );


        /*
         * 검기 끝부분의 밝은 금색 강조.
         */
        Particle.DustOptions goldBlade =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                185,
                                45
                        ),
                        1.05f
                );


        /*
         * 120도 간격의 검기 3개를 동시에 회전시킨다.
         */
        for (
                int blade = 0;
                blade < 3;
                blade++
        ) {

            double bladeAngle =
                    baseAngle
                            + (
                                    Math.PI
                                            * 2.0
                                            * blade
                                    / 3.0
                            );


            /*
             * 하나의 검기를 짧은 원호 형태로 만든다.
             */
            int segments =
                    12;


            for (
                    int i = 0;
                    i < segments;
                    i++
            ) {

                double trail =
                        Math.toRadians(
                                i * 7.0
                        );


                double angle =
                        bladeAngle
                                - trail;


                double progress =
                        (double) i
                                / (
                                        segments
                                                - 1
                                );


                /*
                 * 검기의 꼬리에서 끝으로 갈수록
                 * 바깥쪽으로 펼쳐진다.
                 */
                double radius =
                        2.15
                                + (
                                        progress
                                                * 1.25
                                );


                double x =
                        Math.cos(
                                angle
                        ) * radius;


                double z =
                        Math.sin(
                                angle
                        ) * radius;


                /*
                 * 완전히 평면인 링보다
                 * 약간 입체적으로 보이도록 높이를 변화시킨다.
                 */
                double y =
                        Math.sin(
                                angle * 2.0
                        ) * 0.16;


                y +=
                        progress
                                * 0.12;


                Location point =
                        center.clone()
                                .add(
                                        x,
                                        y,
                                        z
                                );


                /*
                 * 주황색 메인 검기.
                 */
                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        orangeBlade
                );


                /*
                 * 검기의 바깥쪽 끝부분은
                 * 밝은 금색으로 강조한다.
                 */
                if (
                        i
                                >= segments - 4
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
                            goldBlade
                    );
                }


                /*
                 * 가장 끝에는 CRIT를 추가해서
                 * 날카로운 칼날 느낌을 준다.
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
                            0.04,
                            0.04,
                            0.01
                    );
                }
            }
        }


        /*
         * 발밑 회오리.
         *
         * 시야를 가리지 않도록 CLOUD는 소량만 사용한다.
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
                                    0.20,
                                    0.0
                            ),
                    3,
                    0.75,
                    0.06,
                    0.75,
                    0.015
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
                7,
                0.30,
                0.35,
                0.30,
                0.12
        );


        world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                hitLocation,
                3,
                0.20,
                0.25,
                0.20,
                0.06
        );
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
                                0.8,
                                0.0
                        );


        World world =
                player.getWorld();


        world.spawnParticle(
                Particle.CLOUD,
                location,
                22,
                1.8,
                0.35,
                1.8,
                0.04
        );


        world.playSound(
                location,
                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                1.1f,
                0.75f
        );
    }
}
