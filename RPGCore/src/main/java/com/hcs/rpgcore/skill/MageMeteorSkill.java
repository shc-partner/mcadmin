package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;
import com.hcs.rpgcore.stat.PlayerStats;
import com.hcs.rpgcore.stat.StatService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.block.Block;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;

import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;


public final class MageMeteorSkill {

    public static final double MANA_COST =
            150.0;

    public static final long COOLDOWN_MILLIS =
            45000L;

    /*
     * 전체 폭격 반경.
     */
    public static final double AREA_RADIUS =
            12.0;

    /*
     * 개별 메테오 착탄 반경.
     */
    public static final double IMPACT_RADIUS =
            3.0;

    /*
     * 총 메테오 수.
     */
    public static final int METEOR_COUNT =
            16;

    /*
     * 동일 대상 최대 피해 횟수.
     */
    public static final int MAX_HITS_PER_TARGET =
            3;


    /*
     * 플레이어 정면의 폭격 중심 거리.
     */
    private static final double CENTER_DISTANCE =
            10.0;

    /*
     * 4 tick = 약 0.2초.
     *
     * 16발이 약 3초에 걸쳐 생성된다.
     */
    private static final long METEOR_INTERVAL_TICKS =
            4L;

    /*
     * 불기둥 낙하 시작 높이.
     */
    private static final double FALL_HEIGHT =
            12.0;

    /*
     * 1 tick당 하강 거리.
     */
    private static final double FALL_STEP =
            2.0;

    /*
     * 개별 착탄 기본 피해:
     * 80 + ATK x 1.00 + 최대 MP x 0.10
     *
     * 착탄까지 시간이 필요한
     * 하이리스크 / 하이리턴 최상위 공격 스킬.
     */
    private static final double BASE_DAMAGE =
            80.0;

    private static final double ATTACK_MULTIPLIER =
            1.00;

    private static final double MAX_MANA_MULTIPLIER =
            0.10;


    /*
     * =========================================================
     * MAIN METEOR VISUAL
     * =========================================================
     *
     * 기존 불 비와 동시에 생성되지만
     * 훨씬 느리게 내려와 마지막에 착탄한다.
     *
     * 현재는 시각 효과 전용이며
     * 별도 추가 피해는 주지 않는다.
     */
    private static final double MAIN_METEOR_HEIGHT =
            26.0;

    private static final double MAIN_METEOR_FALL_STEP =
            0.35;

    private static final double MAIN_METEOR_ROTATION_STEP =
            Math.toRadians(
                    7.0
            );


    private final RPGCorePlugin plugin;

    private final HudService hudService;

    private final StatService statService;

    private final SkillOffenseBuffService
            offenseBuffService;


    public MageMeteorSkill(
            RPGCorePlugin plugin,
            HudService hudService,
            StatService statService,
            SkillOffenseBuffService offenseBuffService
    ) {

        this.plugin =
                plugin;

        this.hudService =
                hudService;

        this.statService =
                statService;

        this.offenseBuffService =
                offenseBuffService;
    }


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


        PlayerStats stats =
                statService.calculate(
                        snapshot.level(),
                        snapshot.playerClass(),
                        player
                );


        final double baseDamage =
                BASE_DAMAGE
                        + stats.attack()
                        * ATTACK_MULTIPLIER
                        + stats.maxMana()
                        * MAX_MANA_MULTIPLIER;


        /*
         * =====================================================
         * MANA OVERLOAD
         * =====================================================
         *
         * 시전 시점의 메테오 기본 피해를 snapshot 한다.
         *
         * 마나 오버로드 활성:
         *
         * (80 + ATK * 1.00 + 최대 MP * 0.10) * 1.30
         *
         * 이후 메테오의 모든 낙하 피해와
         * 중심 폭발 거리 배율은 이 값을 기준으로 계산한다.
         */
        final double damage =
                offenseBuffService != null
                        ? offenseBuffService
                                .applyMageDamageMultiplier(
                                        player.getUniqueId(),
                                        baseDamage
                                )
                        : baseDamage;


        Location center =
                resolveCenter(
                        player
                );


        if (center == null) {
            return false;
        }


        World world =
                center.getWorld();


        /*
         * 한 번의 메테오 시전 전체에서 공유.
         *
         * 같은 대상은 최대 3회까지만
         * 실제 피해를 받을 수 있다.
         */
        Map<UUID, Integer> hitCounts =
                new HashMap<>();


        /*
         * =====================================================
         * MAIN METEOR
         * =====================================================
         *
         * 기존 불 비와 같은 시점에 시작한다.
         *
         * 중앙 바닥을 기준으로
         * 대형 운석 1개가 천천히 하강한다.
         */
        Location mainImpact =
                findGround(
                        world,
                        center.getX(),
                        center.getY(),
                        center.getZ()
                );

        if (mainImpact != null) {

            launchMainMeteor(
                    player,
                    mainImpact,
                    damage
            );
        }


        /*
         * 폭격 시작 경고 효과.
         */
        world.spawnParticle(
                Particle.FLAME,
                center.clone()
                        .add(
                                0.0,
                                0.25,
                                0.0
                        ),
                45,
                AREA_RADIUS * 0.45,
                0.15,
                AREA_RADIUS * 0.45,
                0.015
        );


        world.spawnParticle(
                Particle.CLOUD,
                center.clone()
                        .add(
                                0.0,
                                0.20,
                                0.0
                        ),
                25,
                AREA_RADIUS * 0.35,
                0.10,
                AREA_RADIUS * 0.35,
                0.01
        );


        world.playSound(
                center,
                Sound.ENTITY_BLAZE_SHOOT,
                1.4f,
                0.55f
        );


        new BukkitRunnable() {

            private int launched =
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
                        launched
                                >= METEOR_COUNT
                ) {

                    cancel();
                    return;
                }


                Location impact =
                        randomImpactLocation(
                                center
                        );


                if (impact != null) {

                    launchFallingMeteor(
                            player,
                            impact,
                            damage,
                            hitCounts
                    );
                }


                launched++;


                if (
                        launched
                                >= METEOR_COUNT
                ) {

                    cancel();
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                METEOR_INTERVAL_TICKS
        );


        return true;
    }


    /*
     * =========================================================
     * TARGET CENTER
     * =========================================================
     *
     * 플레이어 정면 최대 10블록을
     * 폭격 중심으로 설정한다.
     *
     * 벽이 있으면 벽 뒤로 중심점이 넘어가지 않는다.
     */
    private Location resolveCenter(
            Player player
    ) {

        Location origin =
                player.getLocation();


        Vector direction =
                player.getEyeLocation()
                        .getDirection()
                        .clone();


        /*
         * 폭격 위치는 수평 방향 기준.
         */
        direction.setY(
                0.0
        );


        if (
                direction.lengthSquared()
                        <= 0.0001
        ) {

            return origin.clone();
        }


        direction.normalize();


        double distance =
                CENTER_DISTANCE;


        RayTraceResult hit =
                player.getWorld()
                        .rayTraceBlocks(
                                player.getEyeLocation(),
                                direction,
                                CENTER_DISTANCE,
                                FluidCollisionMode.NEVER,
                                true
                        );


        if (
                hit != null
                && hit.getHitPosition() != null
        ) {

            double hitDistance =
                    hit.getHitPosition()
                            .distance(
                                    player.getEyeLocation()
                                            .toVector()
                            );


            distance =
                    Math.max(
                            0.0,
                            hitDistance - 1.0
                    );
        }


        return origin.clone()
                .add(
                        direction.multiply(
                                distance
                        )
                );
    }


    /*
     * =========================================================
     * RANDOM IMPACT
     * =========================================================
     *
     * 반경 12블록의 원 안에서
     * 랜덤 착탄 위치를 생성한다.
     */
    private Location randomImpactLocation(
            Location center
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        double angle =
                random.nextDouble(
                        Math.PI * 2.0
                );


        /*
         * sqrt를 사용하여
         * 원 전체에 비교적 균일하게 분포시킨다.
         */
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
     *
     * 폭격 중심과 비슷한 높이의 실제 바닥을 찾는다.
     *
     * 던전/건물에서 지나치게 높은 지붕을
     * 착탄 위치로 잡는 것을 줄이기 위해
     * Y +4 ~ Y -7 범위만 탐색한다.
     */
    private Location findGround(
            World world,
            double x,
            double baseY,
            double z
    ) {

        int topY =
                (int) Math.floor(
                        baseY + 4.0
                );


        int bottomY =
                (int) Math.floor(
                        baseY - 7.0
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


            Block floor =
                    floorLocation
                            .getBlock();


            if (floor.isPassable()) {
                continue;
            }


            Location feetLocation =
                    new Location(
                            world,
                            x,
                            y + 1.0,
                            z
                    );


            /*
             * 발 공간.
             */
            if (
                    !feetLocation
                            .getBlock()
                            .isPassable()
            ) {
                continue;
            }


            /*
             * 머리 공간.
             */
            if (
                    !feetLocation.clone()
                            .add(
                                    0.0,
                                    1.0,
                                    0.0
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
     * LARGE MAIN METEOR
     * =========================================================
     *
     * BLACKSTONE / BASALT / OBSIDIAN을 외곽 암석으로,
     * MAGMA_BLOCK을 내부의 뜨거운 균열처럼 배치한다.
     *
     * 실제 블록은 설치하지 않고
     * BlockDisplay만 사용한다.
     */
    private void launchMainMeteor(
            Player caster,
            Location impact,
            double damage
    ) {

        World world =
                impact.getWorld();


        Location current =
                impact.clone()
                        .add(
                                0.0,
                                MAIN_METEOR_HEIGHT,
                                0.0
                        );


        /*
         * 각 배열:
         *
         * x, y, z, scale, material index
         *
         * material:
         * 0 = BLACKSTONE
         * 1 = BASALT
         * 2 = OBSIDIAN
         * 3 = MAGMA_BLOCK
         *
         * 완전한 구형이 아니라
         * 울퉁불퉁한 비대칭 암석 형태로 만든다.
         */
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
                                    part[0] * 1.5,
                                    part[1] * 1.5,
                                    part[2] * 1.5
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


            /*
             * Display 자체를 확대/축소한다.
             *
             * setTransformationMatrix를 쓰지 않아
             * 회전 로직과 위치 이동을 단순하게 유지한다.
             */
            float scale =
                    (float) (
                            part[3] * 1.5
                    );

            display.setTransformation(
                    new org.bukkit.util.Transformation(
                            new org.joml.Vector3f(
                                    -scale / 2.0f,
                                    -scale / 2.0f,
                                    -scale / 2.0f
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


            /*
             * 매 tick teleport 시
             * 클라이언트에서 조금 더 부드럽게 보이도록 한다.
             */
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


        /*
         * 등장 순간:
         * 낮고 무거운 소리로 대형 물체가
         * 하늘에 나타났다는 느낌을 준다.
         */
        world.playSound(
                current,
                Sound.ENTITY_BLAZE_AMBIENT,
                1.8f,
                0.35f
        );


        new BukkitRunnable() {

            private double rotation =
                    0.0;


            @Override
            public void run() {

                if (
                        !caster.isOnline()
                        || caster.isDead()
                ) {

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


                /*
                 * 복합 운석 전체를 Y축 중심으로 회전시키면서
                 * 동시에 아래로 이동한다.
                 */
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
                            ) * 1.5;

                    double rotatedZ =
                            (
                                    part[0] * sin
                                            + part[2] * cos
                            ) * 1.5;


                    Location next =
                            current.clone()
                                    .add(
                                            rotatedX,
                                            part[1] * 1.5,
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
                                            rotation * 0.7
                                    ) * 18.0
                            )
                    );
                }


                /*
                 * 대형 운석 꼬리.
                 *
                 * 중심보다 위쪽으로 길게 퍼뜨려
                 * 하강 방향의 반대편에 불/연기가 남게 한다.
                 */
                Location trail =
                        current.clone()
                                .add(
                                        0.0,
                                        1.6,
                                        0.0
                                );


                world.spawnParticle(
                        Particle.FLAME,
                        trail,
                        22,
                        1.15,
                        1.25,
                        1.15,
                        0.035
                );


                world.spawnParticle(
                        Particle.LARGE_SMOKE,
                        trail.clone()
                                .add(
                                        0.0,
                                        1.2,
                                        0.0
                                ),
                        10,
                        1.0,
                        1.6,
                        1.0,
                        0.025
                );


                world.spawnParticle(
                        Particle.LAVA,
                        current,
                        5,
                        0.85,
                        0.85,
                        0.85,
                        0.0
                );


                current.subtract(
                        0.0,
                        MAIN_METEOR_FALL_STEP,
                        0.0
                );


                if (
                        current.getY()
                                <= impact.getY()
                ) {

                    removeMainMeteor(
                            displays
                    );


                    mainMeteorImpactEffect(
                            caster,
                            impact,
                            damage
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


    /*
     * =========================================================
     * MAIN METEOR IMPACT VISUAL
     * =========================================================
     *
     * 현재 단계에서는 추가 피해 없음.
     */
    private void mainMeteorImpactEffect(
            Player caster,
            Location impact,
            double meteorDamage
    ) {

        World world =
                impact.getWorld();


        Location center =
                impact.clone()
                        .add(
                                0.0,
                                0.8,
                                0.0
                        );


        world.spawnParticle(
                Particle.EXPLOSION_EMITTER,
                center,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );


        world.spawnParticle(
                Particle.EXPLOSION,
                center,
                12,
                2.8,
                1.3,
                2.8,
                0.0
        );


        world.spawnParticle(
                Particle.FLAME,
                center,
                110,
                3.2,
                1.6,
                3.2,
                0.10
        );


        world.spawnParticle(
                Particle.LAVA,
                center,
                45,
                3.0,
                1.1,
                3.0,
                0.0
        );


        world.spawnParticle(
                Particle.LARGE_SMOKE,
                center.clone()
                        .add(
                                0.0,
                                1.5,
                                0.0
                        ),
                55,
                2.6,
                2.2,
                2.6,
                0.055
        );


        world.playSound(
                impact,
                Sound.ENTITY_GENERIC_EXPLODE,
                2.4f,
                0.55f
        );


        /*
         * =====================================================
         * MAIN METEOR DAMAGE
         * =====================================================
         *
         * 기존 불 비의 hitCounts와 별도로 처리한다.
         *
         * 따라서 불 비를 이미 최대 횟수만큼 맞은 대상도
         * 대형 운석 폭발 피해를 추가로 받을 수 있다.
         *
         * 0 ~ 2 blocks : x2.50
         * 2 ~ 4 blocks : x2.00
         * 4 ~ 6 blocks : x1.25
         */
        double explosionRadius =
                6.0;


        for (
                Entity nearby
                : world.getNearbyEntities(
                        impact,
                        explosionRadius,
                        explosionRadius,
                        explosionRadius
                )
        ) {

            if (
                    !(nearby instanceof LivingEntity target)
            ) {
                continue;
            }


            /*
             * 플레이어에게는 피해 없음.
             */
            if (
                    target == caster
                    || target instanceof Player
                    || target.isDead()
            ) {
                continue;
            }


            /*
             * 적대 몬스터 및 주요 보스만 공격.
             */
            if (
                    !(target instanceof Enemy)
                    && !(target instanceof EnderDragon)
                    && !(target instanceof Wither)
            ) {
                continue;
            }


            double distanceSquared =
                    target.getLocation()
                            .distanceSquared(
                                    impact
                            );


            if (
                    distanceSquared
                            > explosionRadius
                            * explosionRadius
            ) {
                continue;
            }


            double distance =
                    Math.sqrt(
                            distanceSquared
                    );


            double finalDamage;


            if (distance <= 2.0) {

                finalDamage =
                        meteorDamage
                                * 2.50;

            } else if (distance <= 4.0) {

                finalDamage =
                        meteorDamage
                                * 2.00;

            } else {

                finalDamage =
                        meteorDamage
                                * 1.25;
            }


            target.damage(
                    finalDamage,
                    caster
            );


            /*
             * 피격 대상에 추가 화염 효과.
             */
            world.spawnParticle(
                    Particle.FLAME,
                    target.getLocation()
                            .add(
                                    0.0,
                                    1.0,
                                    0.0
                            ),
                    20,
                    0.55,
                    0.75,
                    0.55,
                    0.06
            );
        }
    }


    /*
     * =========================================================
     * MAIN METEOR CLEANUP
     * =========================================================
     */
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


    /*
     * =========================================================
     * FALLING FIRE COLUMN
     * =========================================================
     */
    private void launchFallingMeteor(
            Player caster,
            Location impact,
            double damage,
            Map<UUID, Integer> hitCounts
    ) {

        World world =
                impact.getWorld();


        Location current =
                impact.clone()
                        .add(
                                0.0,
                                FALL_HEIGHT,
                                0.0
                        );


        world.playSound(
                current,
                Sound.ENTITY_BLAZE_SHOOT,
                0.8f,
                0.65f
        );


        new BukkitRunnable() {

            @Override
            public void run() {

                if (
                        !caster.isOnline()
                        || caster.isDead()
                ) {

                    cancel();
                    return;
                }


                /*
                 * 한 점이 아니라 약 3블록 길이의
                 * 세로 불기둥을 만든다.
                 *
                 * 매 tick 전체 기둥이 아래로 이동한다.
                 */
                for (
                        double columnY = 0.0;
                        columnY <= 3.0;
                        columnY += 1.0
                ) {

                    Location columnPoint =
                            current.clone()
                                    .add(
                                            0.0,
                                            columnY,
                                            0.0
                                    );


                    world.spawnParticle(
                            Particle.FLAME,
                            columnPoint,
                            8,
                            0.30,
                            0.30,
                            0.30,
                            0.025
                    );


                    world.spawnParticle(
                            Particle.CLOUD,
                            columnPoint,
                            2,
                            0.25,
                            0.20,
                            0.25,
                            0.01
                    );
                }


                world.spawnParticle(
                        Particle.LAVA,
                        current,
                        4,
                        0.30,
                        0.45,
                        0.30,
                        0.0
                );


                current.subtract(
                        0.0,
                        FALL_STEP,
                        0.0
                );


                if (
                        current.getY()
                                <= impact.getY()
                ) {

                    explode(
                            caster,
                            impact,
                            damage,
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


    /*
     * =========================================================
     * IMPACT
     * =========================================================
     */
    private void explode(
            Player caster,
            Location impact,
            double damage,
            Map<UUID, Integer> hitCounts
    ) {

        World world =
                impact.getWorld();


        /*
         * Bukkit createExplosion()을 사용하지 않는다.
         *
         * 따라서:
         * - 블록 파괴 없음
         * - 실제 화재 없음
         * - 바닐라 폭발 넉백 없음
         */
        world.spawnParticle(
                Particle.FLAME,
                impact.clone()
                        .add(
                                0.0,
                                0.8,
                                0.0
                        ),
                45,
                1.5,
                0.7,
                1.5,
                0.08
        );


        world.spawnParticle(
                Particle.LAVA,
                impact.clone()
                        .add(
                                0.0,
                                0.6,
                                0.0
                        ),
                15,
                1.6,
                0.6,
                1.6,
                0.0
        );


        world.spawnParticle(
                Particle.CLOUD,
                impact.clone()
                        .add(
                                0.0,
                                0.3,
                                0.0
                        ),
                30,
                1.6,
                0.35,
                1.6,
                0.04
        );


        world.spawnParticle(
                Particle.EXPLOSION,
                impact.clone()
                        .add(
                                0.0,
                                0.8,
                                0.0
                        ),
                4,
                1.0,
                0.4,
                1.0,
                0.0
        );


        world.playSound(
                impact,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.1f,
                0.85f
        );


        for (
                Entity nearby
                : world.getNearbyEntities(
                        impact,
                        IMPACT_RADIUS,
                        IMPACT_RADIUS,
                        IMPACT_RADIUS
                )
        ) {

            if (
                    !(nearby instanceof LivingEntity target)
            ) {
                continue;
            }


            /*
             * 플레이어에게는 피해를 주지 않는다.
             */
            if (
                    target == caster
                    || target instanceof Player
                    || target.isDead()
            ) {
                continue;
            }


            /*
             * 적대 몬스터 및 주요 보스만 공격.
             */
            if (
                    !(target instanceof Enemy)
                    && !(target instanceof EnderDragon)
                    && !(target instanceof Wither)
            ) {
                continue;
            }


            /*
             * getNearbyEntities는 박스 판정이므로
             * 실제 반경 3블록 원형 판정을 한 번 더 한다.
             */
            if (
                    target.getLocation()
                            .distanceSquared(
                                    impact
                            )
                            > IMPACT_RADIUS
                            * IMPACT_RADIUS
            ) {
                continue;
            }


            UUID targetUuid =
                    target.getUniqueId();


            int previousHits =
                    hitCounts.getOrDefault(
                            targetUuid,
                            0
                    );


            if (
                    previousHits
                            >= MAX_HITS_PER_TARGET
            ) {
                continue;
            }


            hitCounts.put(
                    targetUuid,
                    previousHits + 1
            );


            target.damage(
                    damage,
                    caster
            );


            world.spawnParticle(
                    Particle.FLAME,
                    target.getLocation()
                            .add(
                                    0.0,
                                    1.0,
                                    0.0
                            ),
                    12,
                    0.35,
                    0.5,
                    0.35,
                    0.04
            );
        }
    }
}
