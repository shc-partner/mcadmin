package com.hcs.rpgcore.dungeon;

import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.block.Block;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scheduler.BukkitTask;


/*
 * =========================================================
 * RED DRAGON AREA BURST HAZARD
 * =========================================================
 *
 * Drako / MythicMobs AI와 완전히 독립적으로 실행한다.
 *
 * 4초마다:
 *
 * 1. Drako 발밑 바닥 위치 고정
 * 2. SOUL_FIRE_FLAME + FLAME 원형 경고
 * 3. 낮은 폭발음
 * 4. 14 ticks 후 폭발
 * 5. 반경 8블록 플레이어 피해
 *
 * 넉백 없음.
 * 블록 파괴 없음.
 * 실제 폭발 생성 없음.
 */
public final class RedDragonAreaBurstHazard {

    private static final long ATTACK_INTERVAL_TICKS =
            80L;

    private static final long WARNING_DELAY_TICKS =
            14L;


    private static final double ATTACK_RADIUS =
            8.0D;

    private static final double DAMAGE =
            70.0D;


    /*
     * 원형 경고 파티클 점 개수.
     */
    private static final int WARNING_RING_POINTS =
            56;


    private final JavaPlugin plugin;

    private final LivingEntity boss;

    private final Set<UUID> participants;

    private final BooleanSupplier combatActive;


    private BukkitTask attackTask;


    public RedDragonAreaBurstHazard(
            JavaPlugin plugin,
            LivingEntity boss,
            Set<UUID> participants,
            BooleanSupplier combatActive
    ) {

        this.plugin =
                plugin;

        this.boss =
                boss;

        this.participants =
                participants;

        this.combatActive =
                combatActive;
    }


    /*
     * =========================================================
     * START / STOP
     * =========================================================
     */
    public void start() {

        stop();


        attackTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::beginAttack,
                                ATTACK_INTERVAL_TICKS,
                                ATTACK_INTERVAL_TICKS
                        );
    }


    public void stop() {

        if (attackTask != null) {

            attackTask.cancel();

            attackTask =
                    null;
        }
    }


    /*
     * =========================================================
     * ATTACK START
     * =========================================================
     */
    private void beginAttack() {

        if (!isAttackAvailable()) {
            return;
        }


        /*
         * 공격 시작 순간 Drako 발밑 바닥 위치를 고정한다.
         *
         * 이후 Drako가 움직여도 공격 위치는 이동하지 않는다.
         */
        Location center =
                findGroundBelowBoss();

        if (center == null) {
            return;
        }


        showWarning(
                center
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!combatActive.getAsBoolean()) {
                                return;
                            }


                            performBurst(
                                    center
                            );
                        },
                        WARNING_DELAY_TICKS
                );
    }


    private boolean isAttackAvailable() {

        return combatActive.getAsBoolean()
                &&
                boss != null
                &&
                boss.isValid()
                &&
                !boss.isDead();
    }


    /*
     * =========================================================
     * FIND GROUND BELOW BOSS
     * =========================================================
     *
     * 보스가 비행 중이어도 X/Z 바로 아래의 실제 바닥을 찾는다.
     */
    private Location findGroundBelowBoss() {

        World world =
                boss.getWorld();

        Location bossLocation =
                boss.getLocation();


        int startY =
                Math.min(
                        world.getMaxHeight() - 2,
                        bossLocation.getBlockY()
                );


        int minimumY =
                Math.max(
                        world.getMinHeight(),
                        startY - 48
                );


        int x =
                bossLocation.getBlockX();

        int z =
                bossLocation.getBlockZ();


        for (
                int y = startY;
                y >= minimumY;
                y--
        ) {

            Block floor =
                    world.getBlockAt(
                            x,
                            y,
                            z
                    );


            if (floor.isPassable()) {
                continue;
            }


            Block feet =
                    world.getBlockAt(
                            x,
                            y + 1,
                            z
                    );


            if (!feet.isPassable()) {
                continue;
            }


            return new Location(
                    world,
                    x + 0.5D,
                    y + 1.05D,
                    z + 0.5D
            );
        }


        return null;
    }


    /*
     * =========================================================
     * WARNING
     * =========================================================
     */
    private void showWarning(
            Location center
    ) {

        World world =
                center.getWorld();

        if (world == null) {
            return;
        }


        /*
         * 반경 8블록 원형 표시.
         */
        for (
                int i = 0;
                i < WARNING_RING_POINTS;
                i++
        ) {

            double angle =
                    Math.PI
                            * 2.0D
                            * i
                            / WARNING_RING_POINTS;


            double x =
                    Math.cos(
                            angle
                    ) * ATTACK_RADIUS;

            double z =
                    Math.sin(
                            angle
                    ) * ATTACK_RADIUS;


            Location point =
                    center.clone()
                            .add(
                                    x,
                                    0.08D,
                                    z
                            );


            world.spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    point,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );


            world.spawnParticle(
                    Particle.FLAME,
                    point,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }


        /*
         * 중심부 경고.
         */
        world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                center.clone()
                        .add(
                                0.0D,
                                0.15D,
                                0.0D
                        ),
                35,
                2.0D,
                0.12D,
                2.0D,
                0.015D
        );


        world.spawnParticle(
                Particle.FLAME,
                center.clone()
                        .add(
                                0.0D,
                                0.15D,
                                0.0D
                        ),
                25,
                1.7D,
                0.12D,
                1.7D,
                0.015D
        );


        /*
         * 낮고 무거운 사전 폭발음.
         */
        world.playSound(
                center,
                Sound.ENTITY_GENERIC_EXPLODE,
                0.75F,
                0.55F
        );
    }


    /*
     * =========================================================
     * IMPACT
     * =========================================================
     */
    private void performBurst(
            Location center
    ) {

        World world =
                center.getWorld();

        if (world == null) {
            return;
        }


        /*
         * 실제 createExplosion()은 사용하지 않는다.
         *
         * 따라서 지형 파괴 / 화재 / 폭발 넉백 없음.
         */
        world.spawnParticle(
                Particle.EXPLOSION,
                center.clone()
                        .add(
                                0.0D,
                                0.7D,
                                0.0D
                        ),
                14,
                3.2D,
                1.0D,
                3.2D,
                0.0D
        );


        world.spawnParticle(
                Particle.LARGE_SMOKE,
                center.clone()
                        .add(
                                0.0D,
                                0.8D,
                                0.0D
                        ),
                80,
                4.0D,
                1.3D,
                4.0D,
                0.055D
        );


        world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                center.clone()
                        .add(
                                0.0D,
                                0.3D,
                                0.0D
                        ),
                90,
                3.8D,
                0.65D,
                3.8D,
                0.045D
        );


        world.playSound(
                center,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.65F,
                0.70F
        );


        /*
         * activeParticipants에게만 피해.
         *
         * velocity는 변경하지 않으므로
         * 의도적인 넉백은 발생하지 않는다.
         */
        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                    ||
                    !player.isOnline()
                    ||
                    player.isDead()
                    ||
                    player.getWorld()
                            != world
            ) {
                continue;
            }


            /*
             * X/Z 기준 원형 범위.
             *
             * 플레이어가 점프 중이어도 정상적으로
             * 바닥 장판 판정을 받도록 수평 거리만 사용한다.
             */
            double dx =
                    player.getLocation()
                            .getX()
                            - center.getX();

            double dz =
                    player.getLocation()
                            .getZ()
                            - center.getZ();


            double distanceSquared =
                    dx * dx
                            + dz * dz;


            if (
                    distanceSquared
                            > ATTACK_RADIUS
                            * ATTACK_RADIUS
            ) {
                continue;
            }


            player.damage(
                    DAMAGE
            );
        }
    }
}
