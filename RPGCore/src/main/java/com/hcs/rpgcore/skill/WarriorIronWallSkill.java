package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;


public final class WarriorIronWallSkill {

    /*
     * =========================================================
     * LEVEL 40 - IRON WALL
     * =========================================================
     *
     * 일정 시간 받는 최종 피해를 40% 감소시킨다.
     *
     * 실제 피해 감소 계산은
     * SkillDefenseBuffService에서 처리한다.
     */

    public static final double MANA_COST =
            15.0;

    public static final long COOLDOWN_MILLIS =
            30000L;


    /*
     * 6초.
     */
    public static final long DURATION_MILLIS =
            6000L;


    private final RPGCorePlugin plugin;


    private final SkillDefenseBuffService
            defenseBuffService;


    public WarriorIronWallSkill(
            RPGCorePlugin plugin,
            SkillDefenseBuffService defenseBuffService
    ) {

        this.plugin =
                plugin;

        this.defenseBuffService =
                defenseBuffService;
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


        defenseBuffService.activateIronWall(
                player,
                DURATION_MILLIS
        );


        spawnActivationEffect(
                player
        );


        startIronWallVisual(
                player
        );


        return true;
    }


    /*
     * =========================================================
     * ACTIVATION EFFECT
     * =========================================================
     */
    private void spawnActivationEffect(
            Player player
    ) {

        Location center =
                player.getLocation()
                        .clone()
                        .add(
                                0.0,
                                1.0,
                                0.0
                        );


        /*
         * 아이콘의 금빛 테두리.
         */
        Particle.DustOptions gold =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                180,
                                35
                        ),
                        1.35f
                );


        /*
         * 강철 방패의 밝은 은색.
         */
        Particle.DustOptions steel =
                new Particle.DustOptions(
                        Color.fromRGB(
                                200,
                                210,
                                220
                        ),
                        1.20f
                );


        /*
         * 발동 순간 원형 충격파.
         */
        for (
                int i = 0;
                i < 28;
                i++
        ) {

            double angle =
                    (
                            Math.PI
                                    * 2.0
                                    * i
                            / 28.0
                    );


            double x =
                    Math.cos(
                            angle
                    ) * 1.25;


            double z =
                    Math.sin(
                            angle
                    ) * 1.25;


            Location point =
                    center.clone()
                            .add(
                                    x,
                                    -0.55,
                                    z
                            );


            player.getWorld()
                    .spawnParticle(
                            Particle.DUST,
                            point,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            gold
                    );
        }


        /*
         * 중앙의 강철성 파편.
         */
        player.getWorld()
                .spawnParticle(
                        Particle.DUST,
                        center,
                        24,
                        0.55,
                        0.75,
                        0.55,
                        0.0,
                        steel
                );


        player.getWorld()
                .spawnParticle(
                        Particle.CRIT,
                        center,
                        18,
                        0.65,
                        0.85,
                        0.65,
                        0.08
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ITEM_SHIELD_BLOCK,
                        1.3f,
                        0.60f
                );


        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.BLOCK_ANVIL_LAND,
                        0.50f,
                        0.72f
                );
    }


    /*
     * =========================================================
     * IRON WALL CONTINUOUS VISUAL
     * =========================================================
     *
     * 6초 동안 플레이어를 따라다니는
     * 금빛 + 은빛 보호막.
     */
    private void startIronWallVisual(
            Player player
    ) {

        final int durationTicks =
                (int) Math.ceil(
                        DURATION_MILLIS
                                / 50.0
                );


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


                spawnIronWallFrame(
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
     * IRON WALL FRAME
     * =========================================================
     */
    private void spawnIronWallFrame(
            Player player,
            int tick
    ) {

        /*
         * 매 tick 모든 파티클을 찍으면 너무 빽빽하므로
         * 2 tick마다 주요 보호막을 갱신한다.
         */
        if (
                tick % 2
                        != 0
        ) {
            return;
        }


        Location origin =
                player.getLocation()
                        .clone();


        Particle.DustOptions gold =
                new Particle.DustOptions(
                        Color.fromRGB(
                                255,
                                175,
                                28
                        ),
                        1.10f
                );


        Particle.DustOptions steel =
                new Particle.DustOptions(
                        Color.fromRGB(
                                185,
                                198,
                                210
                        ),
                        1.00f
                );


        /*
         * 보호막이 너무 기계적으로 고정되어 보이지 않도록
         * 천천히 회전시킨다.
         */
        double rotation =
                Math.toRadians(
                        tick * 4.0
                );


        /*
         * 3단 링:
         * 발목 / 허리 / 어깨.
         */
        double[] heights = {
                0.35,
                1.05,
                1.75
        };


        double[] radii = {
                0.90,
                1.08,
                0.90
        };


        for (
                int layer = 0;
                layer < heights.length;
                layer++
        ) {

            int points =
                    12;


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
                                + (
                                        layer
                                                * 0.35
                                );


                double x =
                        Math.cos(
                                angle
                        ) * radii[layer];


                double z =
                        Math.sin(
                                angle
                        ) * radii[layer];


                Location point =
                        origin.clone()
                                .add(
                                        x,
                                        heights[layer],
                                        z
                                );


                /*
                 * 은색 강철 보호막 본체.
                 */
                player.getWorld()
                        .spawnParticle(
                                Particle.DUST,
                                point,
                                1,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                steel
                        );


                /*
                 * 일부 지점은 금빛 테두리로 강조.
                 */
                if (
                        i % 3
                                == 0
                ) {

                    player.getWorld()
                            .spawnParticle(
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
                                    gold
                            );
                }
            }
        }


        /*
         * 플레이어 정면에 방패 중앙부처럼 보이는
         * 세로형 강철 라인.
         */
        Location center =
                origin.clone()
                        .add(
                                0.0,
                                1.05,
                                0.0
                        );


        for (
                int i = -3;
                i <= 3;
                i++
        ) {

            player.getWorld()
                    .spawnParticle(
                            Particle.DUST,
                            center.clone()
                                    .add(
                                            0.0,
                                            i * 0.16,
                                            0.0
                                    ),
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            steel
                    );
        }


        /*
         * 방어막 존재감을 조금만 추가.
         */
        if (
                tick % 8
                        == 0
        ) {

            player.getWorld()
                    .spawnParticle(
                            Particle.CRIT,
                            origin.clone()
                                    .add(
                                            0.0,
                                            1.0,
                                            0.0
                                    ),
                            4,
                            0.75,
                            0.80,
                            0.75,
                            0.02
                    );
        }
    }

}