package com.hcs.rpgcore.dungeon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import java.util.function.BooleanSupplier;


/*
 * =========================================================
 * RED DRAGON METEOR HAZARD
 * =========================================================
 *
 * Drako / MythicMobs 전투 AI와 완전히 독립적으로 실행한다.
 *
 * - 10초마다 플레이어 한 명을 선택
 * - 선택 순간의 위치를 폭격 중심으로 고정
 * - MageMeteorSkill과 동일한 스타일의 메테오 연출
 * - 플레이어에게만 피해
 * - Drako에게 피해 없음
 * - 블록 파괴 없음
 * - 화재 없음
 */
public final class RedDragonMeteorHazard {

    private static final long ATTACK_INTERVAL_TICKS =
            200L;

    private static final double AREA_RADIUS =
            12.0D;

    private static final double IMPACT_RADIUS =
            3.0D;

    private static final int METEOR_COUNT =
            16;

    private static final int MAX_HITS_PER_TARGET =
            3;

    private static final long METEOR_INTERVAL_TICKS =
            4L;

    private static final double FALL_HEIGHT =
            12.0D;

    private static final double FALL_STEP =
            2.0D;


    /*
     * 현재 테스트 피해량.
     *
     * 전투 동작 확인 후 이 값만 밸런싱하면 된다.
     */
    private static final double METEOR_DAMAGE =
            100.0D;


    /*
     * 대형 메테오.
     */
    private static final double MAIN_METEOR_HEIGHT =
            26.0D;

    private static final double MAIN_METEOR_FALL_STEP =
            0.35D;

    private static final double MAIN_METEOR_ROTATION_STEP =
            Math.toRadians(
                    7.0D
            );


    private final JavaPlugin plugin;

    private final Set<UUID> participants;

    private final BooleanSupplier combatActive;


    private BukkitTask attackTask;


    public RedDragonMeteorHazard(
            JavaPlugin plugin,
            Set<UUID> participants,
            BooleanSupplier combatActive
    ) {

        this.plugin =
                plugin;

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
                                this::tryStartMeteorAttack,
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
     * ATTACK
     * =========================================================
     */
    private void tryStartMeteorAttack() {

        if (!combatActive.getAsBoolean()) {
            return;
        }


        Player target =
                selectTarget();

        if (target == null) {
            return;
        }


        /*
         * 공격 시작 순간 위치를 고정한다.
         *
         * 이후 플레이어가 이동해도 폭격 중심은 따라가지 않는다.
         */
        Location center =
                findGround(
                        target.getWorld(),
                        target.getLocation()
                                .getX(),
                        target.getLocation()
                                .getY(),
                        target.getLocation()
                                .getZ()
                );

        if (center == null) {
            return;
        }


        target.getWorld()
                .playSound(
                        target.getLocation(),
                        Sound.ENTITY_ENDER_DRAGON_GROWL,
                        2.0F,
                        0.8F
                );


        startMeteorStorm(
                center
        );
    }


    private Player selectTarget() {

        List<Player> valid =
                new ArrayList<>();


        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );

            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
            ) {
                continue;
            }


            valid.add(
                    player
            );
        }


        if (valid.isEmpty()) {
            return null;
        }


        return valid.get(
                ThreadLocalRandom.current()
                        .nextInt(
                                valid.size()
                        )
        );
    }


    /*
     * =========================================================
     * METEOR STORM
     * =========================================================
     */
    private void startMeteorStorm(
            Location center
    ) {

        World world =
                center.getWorld();

        if (world == null) {
            return;
        }


        Map<UUID, Integer> hitCounts =
                new HashMap<>();


        launchMainMeteor(
                center
        );


        /*
         * 폭격 시작 경고.
         */
        world.spawnParticle(
                Particle.FLAME,
                center.clone()
                        .add(
                                0.0D,
                                0.25D,
                                0.0D
                        ),
                45,
                AREA_RADIUS * 0.45D,
                0.15D,
                AREA_RADIUS * 0.45D,
                0.015D
        );


        world.spawnParticle(
                Particle.CLOUD,
                center.clone()
                        .add(
                                0.0D,
                                0.20D,
                                0.0D
                        ),
                25,
                AREA_RADIUS * 0.35D,
                0.10D,
                AREA_RADIUS * 0.35D,
                0.01D
        );


        world.playSound(
                center,
                Sound.ENTITY_BLAZE_SHOOT,
                1.4F,
                0.55F
        );


        new BukkitRunnable() {

            private int launched =
                    0;


            @Override
            public void run() {

                if (!combatActive.getAsBoolean()) {

                    cancel();

                    return;
                }


                if (launched >= METEOR_COUNT) {

                    cancel();

                    return;
                }


                Location impact =
                        randomImpactLocation(
                                center
                        );


                if (impact != null) {

                    launchFallingMeteor(
                            impact,
                            hitCounts
                    );
                }


                launched++;


                if (launched >= METEOR_COUNT) {

                    cancel();
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                METEOR_INTERVAL_TICKS
        );
    }


    /*
     * =========================================================
     * RANDOM IMPACT
     * =========================================================
     */
    private Location randomImpactLocation(
            Location center
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        double angle =
                random.nextDouble(
                        Math.PI * 2.0D
                );


        double radius =
                Math.sqrt(
                        random.nextDouble()
                ) * AREA_RADIUS;


        double x =
                center.getX()
                        + Math.cos(
                                angle
                        ) * radius;


        double z =
                center.getZ()
                        + Math.sin(
                                angle
                        ) * radius;


        return findGround(
                center.getWorld(),
                x,
                center.getY(),
                z
        );
    }


    /*
     * =========================================================
     * GROUND SEARCH
     * =========================================================
     */
    private Location findGround(
            World world,
            double x,
            double baseY,
            double z
    ) {

        if (world == null) {
            return null;
        }


        int topY =
                (int) Math.floor(
                        baseY + 4.0D
                );


        int bottomY =
                (int) Math.floor(
                        baseY - 7.0D
                );


        for (
                int y = topY;
                y >= bottomY;
                y--
        ) {

            Location floorLocation =
                    new Location(
                            world,
                            x,
                            y,
                            z
                    );


            if (
                    floorLocation
                            .getBlock()
                            .isPassable()
            ) {
                continue;
            }


            Location feetLocation =
                    new Location(
                            world,
                            x,
                            y + 1.0D,
                            z
                    );


            if (
                    !feetLocation
                            .getBlock()
                            .isPassable()
            ) {
                continue;
            }


            if (
                    !feetLocation.clone()
                            .add(
                                    0.0D,
                                    1.0D,
                                    0.0D
                            )
                            .getBlock()
                            .isPassable()
            ) {
                continue;
            }


            return feetLocation;
        }


        return null;
    }


    /*
     * =========================================================
     * SMALL METEOR
     * =========================================================
     */
    private void launchFallingMeteor(
            Location impact,
            Map<UUID, Integer> hitCounts
    ) {

        World world =
                impact.getWorld();

        if (world == null) {
            return;
        }


        Location current =
                impact.clone()
                        .add(
                                0.0D,
                                FALL_HEIGHT,
                                0.0D
                        );


        world.playSound(
                current,
                Sound.ENTITY_BLAZE_SHOOT,
                0.8F,
                0.65F
        );


        new BukkitRunnable() {

            @Override
            public void run() {

                if (!combatActive.getAsBoolean()) {

                    cancel();

                    return;
                }


                for (
                        double columnY = 0.0D;
                        columnY <= 3.0D;
                        columnY += 1.0D
                ) {

                    Location point =
                            current.clone()
                                    .add(
                                            0.0D,
                                            columnY,
                                            0.0D
                                    );


                    world.spawnParticle(
                            Particle.FLAME,
                            point,
                            8,
                            0.30D,
                            0.30D,
                            0.30D,
                            0.025D
                    );


                    world.spawnParticle(
                            Particle.CLOUD,
                            point,
                            2,
                            0.25D,
                            0.20D,
                            0.25D,
                            0.01D
                    );
                }


                world.spawnParticle(
                        Particle.LAVA,
                        current,
                        4,
                        0.30D,
                        0.45D,
                        0.30D,
                        0.0D
                );


                current.subtract(
                        0.0D,
                        FALL_STEP,
                        0.0D
                );


                if (
                        current.getY()
                                <= impact.getY()
                ) {

                    explode(
                            impact,
                            hitCounts
                    );

                    cancel();
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                1L
        );
    }


    private void explode(
            Location impact,
            Map<UUID, Integer> hitCounts
    ) {

        World world =
                impact.getWorld();

        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.FLAME,
                impact.clone()
                        .add(
                                0.0D,
                                0.8D,
                                0.0D
                        ),
                45,
                1.5D,
                0.7D,
                1.5D,
                0.08D
        );


        world.spawnParticle(
                Particle.LAVA,
                impact.clone()
                        .add(
                                0.0D,
                                0.6D,
                                0.0D
                        ),
                15,
                1.6D,
                0.6D,
                1.6D,
                0.0D
        );


        world.spawnParticle(
                Particle.CLOUD,
                impact.clone()
                        .add(
                                0.0D,
                                0.3D,
                                0.0D
                        ),
                30,
                1.6D,
                0.35D,
                1.6D,
                0.04D
        );


        world.spawnParticle(
                Particle.EXPLOSION,
                impact.clone()
                        .add(
                                0.0D,
                                0.8D,
                                0.0D
                        ),
                4,
                1.0D,
                0.4D,
                1.0D,
                0.0D
        );


        world.playSound(
                impact,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.1F,
                0.85F
        );


        for (UUID uuid : participants) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );


            if (
                    player == null
                    || !player.isOnline()
                    || player.isDead()
                    || !player.getWorld()
                            .equals(
                                    world
                            )
            ) {
                continue;
            }


            if (
                    player.getLocation()
                            .distanceSquared(
                                    impact
                            )
                            > IMPACT_RADIUS
                            * IMPACT_RADIUS
            ) {
                continue;
            }


            int previousHits =
                    hitCounts.getOrDefault(
                            uuid,
                            0
                    );


            if (
                    previousHits
                            >= MAX_HITS_PER_TARGET
            ) {
                continue;
            }


            hitCounts.put(
                    uuid,
                    previousHits + 1
            );


            /*
             * source 없는 환경 피해.
             *
             * Drako의 AI / ThreatTable / 공격 상태와 연결하지 않는다.
             */
            player.damage(
                    METEOR_DAMAGE
            );


            world.spawnParticle(
                    Particle.FLAME,
                    player.getLocation()
                            .clone()
                            .add(
                                    0.0D,
                                    1.0D,
                                    0.0D
                            ),
                    12,
                    0.35D,
                    0.5D,
                    0.35D,
                    0.04D
            );
        }
    }


    /*
     * =========================================================
     * LARGE MAIN METEOR
     * =========================================================
     *
     * 시각 효과 전용.
     */
    private void launchMainMeteor(
            Location impact
    ) {

        World world =
                impact.getWorld();

        if (world == null) {
            return;
        }


        Location current =
                impact.clone()
                        .add(
                                0.0D,
                                MAIN_METEOR_HEIGHT,
                                0.0D
                        );


        double[][] parts = {

                { 0.00,  0.00,  0.00, 1.45, 0 },

                { 1.05,  0.05,  0.10, 1.15, 1 },
                {-1.00, -0.10, -0.20, 1.20, 0 },

                { 0.15,  0.15,  1.05, 1.10, 2 },
                {-0.20,  0.00, -1.00, 1.15, 1 },

                { 0.15,  1.00,  0.05, 1.10, 0 },
                {-0.05, -1.00,  0.15, 1.15, 2 },

                { 0.80,  0.80,  0.65, 0.90, 3 },
                {-0.75,  0.65, -0.55, 0.85, 3 },
                { 0.70, -0.75, -0.60, 0.85, 3 },

                {-0.75, -0.65,  0.65, 0.90, 1 },

                { 1.25, -0.35,  0.55, 0.80, 0 },
                {-1.20,  0.35,  0.50, 0.85, 2 },

                { 0.50,  0.40, -1.20, 0.80, 3 },
                {-0.45, -0.45, -1.20, 0.80, 0 }
        };


        Material[] materials = {
                Material.BLACKSTONE,
                Material.BASALT,
                Material.OBSIDIAN,
                Material.MAGMA_BLOCK
        };


        List<BlockDisplay> displays =
                new ArrayList<>();


        for (double[] part : parts) {

            int materialIndex =
                    (int) part[4];


            Location spawnLocation =
                    current.clone()
                            .add(
                                    part[0] * 1.5D,
                                    part[1] * 1.5D,
                                    part[2] * 1.5D
                            );


            BlockDisplay display =
                    world.spawn(
                            spawnLocation,
                            BlockDisplay.class
                    );


            display.setBlock(
                    materials[
                            materialIndex
                    ].createBlockData()
            );


            float scale =
                    (float) (
                            part[3] * 1.5D
                    );


            display.setTransformation(
                    new org.bukkit.util.Transformation(
                            new org.joml.Vector3f(
                                    -scale / 2.0F,
                                    -scale / 2.0F,
                                    -scale / 2.0F
                            ),
                            new org.joml.AxisAngle4f(),
                            new org.joml.Vector3f(
                                    scale,
                                    scale,
                                    scale
                            ),
                            new org.joml.AxisAngle4f()
                    )
            );


            display.setTeleportDuration(
                    1
            );

            display.setInterpolationDuration(
                    1
            );

            display.setPersistent(
                    false
            );


            displays.add(
                    display
            );
        }


        world.playSound(
                current,
                Sound.ENTITY_BLAZE_AMBIENT,
                1.8F,
                0.35F
        );


        new BukkitRunnable() {

            private double rotation =
                    0.0D;


            @Override
            public void run() {

                if (!combatActive.getAsBoolean()) {

                    removeMainMeteor(
                            displays
                    );

                    cancel();

                    return;
                }


                rotation +=
                        MAIN_METEOR_ROTATION_STEP;


                double cos =
                        Math.cos(
                                rotation
                        );

                double sin =
                        Math.sin(
                                rotation
                        );


                for (
                        int i = 0;
                        i < displays.size();
                        i++
                ) {

                    BlockDisplay display =
                            displays.get(
                                    i
                            );

                    if (!display.isValid()) {
                        continue;
                    }


                    double[] part =
                            parts[i];


                    double rotatedX =
                            (
                                    part[0] * cos
                                            - part[2] * sin
                            ) * 1.5D;


                    double rotatedZ =
                            (
                                    part[0] * sin
                                            + part[2] * cos
                            ) * 1.5D;


                    Location next =
                            current.clone()
                                    .add(
                                            rotatedX,
                                            part[1] * 1.5D,
                                            rotatedZ
                                    );


                    display.teleport(
                            next
                    );


                    display.setRotation(
                            (float) Math.toDegrees(
                                    rotation
                            ),
                            (float) (
                                    Math.sin(
                                            rotation * 0.7D
                                    ) * 18.0D
                            )
                    );
                }


                Location trail =
                        current.clone()
                                .add(
                                        0.0D,
                                        1.6D,
                                        0.0D
                                );


                world.spawnParticle(
                        Particle.FLAME,
                        trail,
                        22,
                        1.15D,
                        1.25D,
                        1.15D,
                        0.035D
                );


                world.spawnParticle(
                        Particle.LARGE_SMOKE,
                        trail.clone()
                                .add(
                                        0.0D,
                                        1.2D,
                                        0.0D
                                ),
                        10,
                        1.0D,
                        1.6D,
                        1.0D,
                        0.025D
                );


                world.spawnParticle(
                        Particle.LAVA,
                        current,
                        5,
                        0.85D,
                        0.85D,
                        0.85D,
                        0.0D
                );


                current.subtract(
                        0.0D,
                        MAIN_METEOR_FALL_STEP,
                        0.0D
                );


                if (
                        current.getY()
                                <= impact.getY()
                ) {

                    removeMainMeteor(
                            displays
                    );


                    mainMeteorImpactEffect(
                            impact
                    );


                    cancel();
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                1L
        );
    }


    private void mainMeteorImpactEffect(
            Location impact
    ) {

        World world =
                impact.getWorld();

        if (world == null) {
            return;
        }


        Location center =
                impact.clone()
                        .add(
                                0.0D,
                                0.8D,
                                0.0D
                        );


        world.spawnParticle(
                Particle.EXPLOSION_EMITTER,
                center,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );


        world.spawnParticle(
                Particle.EXPLOSION,
                center,
                12,
                2.8D,
                1.3D,
                2.8D,
                0.0D
        );


        world.spawnParticle(
                Particle.FLAME,
                center,
                110,
                3.2D,
                1.6D,
                3.2D,
                0.10D
        );


        world.spawnParticle(
                Particle.LAVA,
                center,
                45,
                3.0D,
                1.1D,
                3.0D,
                0.0D
        );


        world.spawnParticle(
                Particle.LARGE_SMOKE,
                center.clone()
                        .add(
                                0.0D,
                                1.5D,
                                0.0D
                        ),
                55,
                2.6D,
                2.2D,
                2.6D,
                0.055D
        );


        world.playSound(
                impact,
                Sound.ENTITY_GENERIC_EXPLODE,
                2.4F,
                0.55F
        );
    }


    private void removeMainMeteor(
            List<BlockDisplay> displays
    ) {

        for (BlockDisplay display : displays) {

            if (
                    display != null
                    && display.isValid()
            ) {

                display.remove();
            }
        }
    }
}
