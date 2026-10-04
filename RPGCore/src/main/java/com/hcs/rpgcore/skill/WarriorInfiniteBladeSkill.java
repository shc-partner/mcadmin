package com.hcs.rpgcore.skill;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public final class WarriorInfiniteBladeSkill {

    /*
     * =========================================================
     * BASIC
     * =========================================================
     */

    public static final double MANA_COST =
            30.0;

    public static final long COOLDOWN_MILLIS =
            45000L;


    /*
     * 기존 일반 무기 30발을
     * 베기 파동 30발로 대체한다.
     */
    private static final int NORMAL_SLASH_COUNT =
            30;

    /*
     * 기존과 동일하게
     * 한 번에 3발씩 발사.
     */
    private static final int SLASHES_PER_VOLLEY =
            3;

    /*
     * 2 tick = 0.1초.
     */
    private static final long VOLLEY_INTERVAL_TICKS =
            2L;


    /*
     * 일반 베기:
     * 총 공격력의 9%.
     *
     * 30회 모두 적중 시:
     * ATK x 0.09 x 30
     * = ATK x 2.70
     */
    private static final double NORMAL_DAMAGE_MULTIPLIER =
            0.09;

    /*
     * 마지막 대형 베기:
     * 총 공격력의 80%.
     *
     * 전체 최대 피해:
     * ATK x 2.70
     * + ATK x 0.80
     * = ATK x 3.50
     */
    private static final double FINAL_DAMAGE_MULTIPLIER =
            0.80;


    /*
     * 기존 사거리 유지.
     */
    private static final double RANGE =
            45.0;


    /*
     * 기존 투사체 속도 유지.
     */
    private static final double NORMAL_SPEED =
            2.4;

    private static final double FINAL_SPEED =
            2.8;


    /*
     * 기존 충돌 판정 크기 유지.
     */
    private static final double NORMAL_HIT_RADIUS =
            0.65;

    private static final double FINAL_HIT_RADIUS =
            1.15;


    /*
     * 플레이어 앞에서 베기 파동이 나타나는 거리.
     */
    private static final double SPAWN_FORWARD =
            2.5;


    /*
     * 일반 / 최종 베기 시각 크기.
     */
    private static final double NORMAL_SLASH_RADIUS =
            1.25;

    private static final double FINAL_SLASH_RADIUS =
            2.35;


    /*
     * 파티클.
     */
    private static final Particle.DustOptions GOLD_DUST =
            new Particle.DustOptions(
                    Color.fromRGB(
                            255,
                            190,
                            40
                    ),
                    1.25f
            );

    private static final Particle.DustOptions BRIGHT_DUST =
            new Particle.DustOptions(
                    Color.fromRGB(
                            255,
                            245,
                            170
                    ),
                    1.05f
            );


    private final RPGCorePlugin plugin;

    private final HudService hudService;

    private final WarriorWeaponAttackService
            weaponAttackService;


    public WarriorInfiniteBladeSkill(
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
         * 기존 인피니트 블레이드와 동일하게
         * 장비를 포함한 총 공격력을 사용한다.
         */
        double totalAttack =
                weaponAttackService
                        .resolveBashAttack(
                                player,
                                snapshot
                        );


        double normalDamage =
                totalAttack
                        * NORMAL_DAMAGE_MULTIPLIER;


        double finalDamage =
                totalAttack
                        * FINAL_DAMAGE_MULTIPLIER;


        /*
         * 시전 순간 방향 고정.
         */
        Location eye =
                player.getEyeLocation()
                        .clone();


        Vector forward =
                eye.getDirection()
                        .clone();


        if (
                forward.lengthSquared()
                        <= 0.0001
        ) {
            return false;
        }


        forward.normalize();


        Vector right =
                forward.clone()
                        .crossProduct(
                                new Vector(
                                        0.0,
                                        1.0,
                                        0.0
                                )
                        );


        if (
                right.lengthSquared()
                        <= 0.0001
        ) {

            right =
                    new Vector(
                            1.0,
                            0.0,
                            0.0
                    );

        } else {

            right.normalize();
        }


        Vector up =
                right.clone()
                        .crossProduct(
                                forward
                        )
                        .normalize();


        final Vector fixedForward =
                forward.clone();

        final Vector fixedRight =
                right.clone();

        final Vector fixedUp =
                up.clone();


        World world =
                player.getWorld();


        /*
         * 시전 효과.
         */
        world.playSound(
                player.getLocation(),
                Sound.BLOCK_BEACON_ACTIVATE,
                1.6f,
                1.65f
        );


        spawnCastBurst(
                eye,
                fixedForward,
                fixedRight,
                fixedUp
        );


        /*
         * =====================================================
         * MULTI SLASH VOLLEY
         * =====================================================
         *
         * 기존:
         * 검/창 30개
         *
         * 변경:
         * 베기 파동 30개
         *
         * 3발씩 0.1초 간격.
         */
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


                int fired =
                        0;


                while (
                        fired < SLASHES_PER_VOLLEY
                        && launched < NORMAL_SLASH_COUNT
                ) {

                    launchNormalSlash(
                            player,
                            eye,
                            fixedForward,
                            fixedRight,
                            fixedUp,
                            normalDamage
                    );


                    launched++;
                    fired++;
                }


                world.playSound(
                        player.getEyeLocation(),
                        Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.75f,
                        1.55f
                );


                if (
                        launched
                                >= NORMAL_SLASH_COUNT
                ) {

                    cancel();


                    /*
                     * 일반 30발 종료 후
                     * 기존과 동일하게 0.4초 뒤
                     * 마지막 강력한 공격.
                     */
                    new BukkitRunnable() {

                        @Override
                        public void run() {

                            if (
                                    !player.isOnline()
                                    || player.isDead()
                            ) {
                                return;
                            }


                            launchFinalSlash(
                                    player,
                                    eye,
                                    fixedForward,
                                    fixedRight,
                                    fixedUp,
                                    finalDamage
                            );
                        }

                    }.runTaskLater(
                            plugin,
                            8L
                    );
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                VOLLEY_INTERVAL_TICKS
        );


        return true;
    }


    /*
     * =========================================================
     * CAST BURST
     * =========================================================
     */

    private void spawnCastBurst(
            Location eye,
            Vector forward,
            Vector right,
            Vector up
    ) {

        Location center =
                eye.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                SPAWN_FORWARD
                                        )
                        );


        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        /*
         * 시전 순간 여러 개의 작은 베기 흔적이
         * 플레이어 앞에 나타난다.
         */
        for (
                int i = 0;
                i < 8;
                i++
        ) {

            double horizontal =
                    ThreadLocalRandom
                            .current()
                            .nextDouble(
                                    -1.8,
                                    1.8
                            );


            double vertical =
                    ThreadLocalRandom
                            .current()
                            .nextDouble(
                                    -0.6,
                                    1.9
                            );


            double tilt =
                    ThreadLocalRandom
                            .current()
                            .nextDouble(
                                    -0.9,
                                    0.9
                            );


            Location slashCenter =
                    center.clone()
                            .add(
                                    right.clone()
                                            .multiply(
                                                    horizontal
                                            )
                            )
                            .add(
                                    up.clone()
                                            .multiply(
                                                    vertical
                                            )
                            );


            renderSlash(
                    slashCenter,
                    right,
                    up,
                    0.65,
                    tilt,
                    false
            );
        }
    }


    /*
     * =========================================================
     * NORMAL SLASH
     * =========================================================
     */

    private void launchNormalSlash(
            Player player,
            Location eye,
            Vector forward,
            Vector right,
            Vector up,
            double damage
    ) {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        double horizontal =
                random.nextDouble(
                        -2.0,
                        2.0
                );


        double vertical =
                random.nextDouble(
                        -0.55,
                        1.85
                );


        double tilt =
                random.nextDouble(
                        -1.15,
                        1.15
                );


        Location start =
                eye.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                SPAWN_FORWARD
                                        )
                        )
                        .add(
                                right.clone()
                                        .multiply(
                                                horizontal
                                        )
                        )
                        .add(
                                up.clone()
                                        .multiply(
                                                vertical
                                        )
                        );


        launchSlashProjectile(
                player,
                start,
                forward,
                right,
                up,
                tilt,
                NORMAL_SLASH_RADIUS,
                NORMAL_SPEED,
                NORMAL_HIT_RADIUS,
                damage,
                false
        );
    }


    /*
     * =========================================================
     * FINAL SLASH
     * =========================================================
     */

    private void launchFinalSlash(
            Player player,
            Location eye,
            Vector forward,
            Vector right,
            Vector up,
            double damage
    ) {

        Location start =
                eye.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                SPAWN_FORWARD
                                        )
                        )
                        .add(
                                up.clone()
                                        .multiply(
                                                0.45
                                        )
                        );


        World world =
                player.getWorld();


        world.playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SHOOT,
                1.25f,
                0.65f
        );


        world.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.8f,
                0.65f
        );


        launchSlashProjectile(
                player,
                start,
                forward,
                right,
                up,
                0.0,
                FINAL_SLASH_RADIUS,
                FINAL_SPEED,
                FINAL_HIT_RADIUS,
                damage,
                true
        );
    }


    /*
     * =========================================================
     * FLYING SLASH
     * =========================================================
     */

    private void launchSlashProjectile(
            Player player,
            Location start,
            Vector forward,
            Vector right,
            Vector up,
            double tilt,
            double visualRadius,
            double speed,
            double hitRadius,
            double damage,
            boolean finalSlash
    ) {

        final Location current =
                start.clone();


        /*
         * 베기 면을 회전시켜
         * 30발이 전부 같은 각도로 보이지 않게 한다.
         */
        double cos =
                Math.cos(
                        tilt
                );

        double sin =
                Math.sin(
                        tilt
                );


        final Vector slashRight =
                right.clone()
                        .multiply(
                                cos
                        )
                        .add(
                                up.clone()
                                        .multiply(
                                                sin
                                        )
                        )
                        .normalize();


        final Vector slashUp =
                up.clone()
                        .multiply(
                                cos
                        )
                        .subtract(
                                right.clone()
                                        .multiply(
                                                sin
                                        )
                        )
                        .normalize();


        new BukkitRunnable() {

            private double travelled =
                    0.0;


            @Override
            public void run() {

                if (
                        !player.isOnline()
                        || player.isDead()
                        || travelled >= RANGE
                ) {

                    cancel();
                    return;
                }


                /*
                 * 한 tick 이동을 여러 sub-step으로 나눠
                 * 빠른 베기가 벽이나 몹을 관통하는 현상을 줄인다.
                 */
                int steps =
                        Math.max(
                                1,
                                (int) Math.ceil(
                                        speed / 0.55
                                )
                        );


                double stepDistance =
                        speed
                                / steps;


                for (
                        int step = 0;
                        step < steps;
                        step++
                ) {

                    current.add(
                            forward.clone()
                                    .multiply(
                                            stepDistance
                                    )
                    );


                    travelled +=
                            stepDistance;


                    if (
                            !current.getBlock()
                                    .isPassable()
                    ) {

                        spawnImpact(
                                current,
                                finalSlash
                        );

                        cancel();
                        return;
                    }


                    LivingEntity target =
                            findTarget(
                                    player,
                                    current,
                                    hitRadius
                            );


                    if (target != null) {

                        target.damage(
                                damage,
                                player
                        );


                        spawnImpact(
                                current,
                                finalSlash
                        );


                        cancel();
                        return;
                    }
                }


                renderSlash(
                        current,
                        slashRight,
                        slashUp,
                        visualRadius,
                        0.0,
                        finalSlash
                );
            }

        }.runTaskTimer(
                plugin,
                0L,
                1L
        );
    }


    /*
     * =========================================================
     * TARGET
     * =========================================================
     */

    private LivingEntity findTarget(
            Player player,
            Location center,
            double radius
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return null;
        }


        LivingEntity nearest =
                null;

        double nearestDistance =
                Double.MAX_VALUE;


        for (
                Entity entity
                        : world.getNearbyEntities(
                                center,
                                radius,
                                radius,
                                radius
                        )
        ) {

            if (
                    !(entity
                            instanceof LivingEntity living)
            ) {
                continue;
            }


            if (
                    living == player
                    || living.isDead()
                    || !living.isValid()
            ) {
                continue;
            }


            /*
             * 기존 인피니트 블레이드와 동일한
             * 적대 몬스터 계열만 공격한다.
             */
            if (
                    !(living instanceof Enemy)
                    && !(living instanceof Wither)
                    && !(living instanceof EnderDragon)
            ) {
                continue;
            }


            double distance =
                    living.getLocation()
                            .distanceSquared(
                                    center
                            );


            if (
                    distance
                            < nearestDistance
            ) {

                nearestDistance =
                        distance;

                nearest =
                        living;
            }
        }


        return nearest;
    }


    /*
     * =========================================================
     * SLASH VISUAL
     * =========================================================
     */

    private void renderSlash(
            Location center,
            Vector right,
            Vector up,
            double radius,
            double additionalTilt,
            boolean finalSlash
    ) {

        World world =
                center.getWorld();


        if (world == null) {
            return;
        }


        Vector drawRight =
                right;

        Vector drawUp =
                up;


        if (
                Math.abs(
                        additionalTilt
                ) > 0.0001
        ) {

            double cos =
                    Math.cos(
                            additionalTilt
                    );

            double sin =
                    Math.sin(
                            additionalTilt
                    );


            drawRight =
                    right.clone()
                            .multiply(
                                    cos
                            )
                            .add(
                                    up.clone()
                                            .multiply(
                                                    sin
                                            )
                            )
                            .normalize();


            drawUp =
                    up.clone()
                            .multiply(
                                    cos
                            )
                            .subtract(
                                    right.clone()
                                            .multiply(
                                                    sin
                                            )
                            )
                            .normalize();
        }


        /*
         * 약 140도짜리 초승달형 베기.
         */
        int points =
                finalSlash
                        ? 17
                        : 11;


        for (
                int i = 0;
                i < points;
                i++
        ) {

            double ratio =
                    (double) i
                            / (points - 1);


            double angle =
                    Math.toRadians(
                            -70.0
                                    + (
                                    140.0
                                            * ratio
                            )
                    );


            double x =
                    Math.cos(
                            angle
                    )
                            * radius;


            double y =
                    Math.sin(
                            angle
                    )
                            * radius;


            Location point =
                    center.clone()
                            .add(
                                    drawRight.clone()
                                            .multiply(
                                                    x
                                            )
                            )
                            .add(
                                    drawUp.clone()
                                            .multiply(
                                                    y
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
                    GOLD_DUST
            );


            if (
                    finalSlash
                    || i % 2 == 0
            ) {

                world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        BRIGHT_DUST
                );
            }
        }


        /*
         * 베기 중심에 바닐라 sweep 효과를 섞어
         * 단순한 점 궤적이 아니라 실제 참격처럼 보이게 한다.
         */
        world.spawnParticle(
                Particle.SWEEP_ATTACK,
                center,
                finalSlash
                        ? 2
                        : 1,
                finalSlash
                        ? 0.30
                        : 0.10,
                finalSlash
                        ? 0.30
                        : 0.10,
                finalSlash
                        ? 0.30
                        : 0.10,
                0.0
        );
    }


    /*
     * =========================================================
     * IMPACT
     * =========================================================
     */

    private void spawnImpact(
            Location location,
            boolean finalSlash
    ) {

        World world =
                location.getWorld();


        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.SWEEP_ATTACK,
                location,
                finalSlash
                        ? 8
                        : 3,
                finalSlash
                        ? 0.8
                        : 0.3,
                finalSlash
                        ? 0.8
                        : 0.3,
                finalSlash
                        ? 0.8
                        : 0.3,
                0.0
        );


        world.spawnParticle(
                Particle.CRIT,
                location,
                finalSlash
                        ? 28
                        : 8,
                finalSlash
                        ? 0.8
                        : 0.25,
                finalSlash
                        ? 0.8
                        : 0.25,
                finalSlash
                        ? 0.8
                        : 0.25,
                0.12
        );


        world.playSound(
                location,
                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                finalSlash
                        ? 1.4f
                        : 0.55f,
                finalSlash
                        ? 0.65f
                        : 1.25f
        );
    }
}
