package com.hcs.rpgcore.dungeon;

import com.hcs.rpgcore.mob.BossMobKeys;

import java.time.Duration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import org.bukkit.util.Vector;


/*
 * ============================================================
 * MINOTAUR BOSS PATTERN
 * ============================================================
 *
 * Phase 1:
 * 100% ~ 70%
 *
 * Phase 2:
 * 70% ~ 35%
 *
 * Phase 3:
 * 35% ~ 0%
 *
 * 이번 단계:
 *
 * Phase 1
 * - slam
 *
 * Phase 2
 * - slam
 *
 * Phase 3
 * - mino_slam
 * - slam
 *
 * charge / swing combo는 다음 단계에서 추가한다.
 */
public final class MinotaurBossPatternTask
        implements Runnable {

    private static final double PHASE_2_RATIO =
            0.70D;

    private static final double PHASE_3_RATIO =
            0.35D;


    private static final double PHASE_1_SPEED =
            0.28D;

    private static final double PHASE_2_SPEED =
            0.31D;

    private static final double PHASE_3_SPEED =
            0.34D;


    private static final long PHASE_1_PATTERN_MS =
            8000L;

    private static final long PHASE_2_PATTERN_MS =
            6000L;

    private static final long PHASE_3_PATTERN_MS =
            4800L;


    private static final double SLAM_RADIUS =
            5.0D;

    private static final double SLAM_DAMAGE =
            22.0D;


    private static final double MINO_SLAM_INNER_RADIUS =
            4.0D;

    private static final double MINO_SLAM_INNER_DAMAGE =
            28.0D;

    private static final double MINO_SLAM_OUTER_RADIUS =
            7.0D;

    private static final double MINO_SLAM_OUTER_DAMAGE =
            18.0D;


    private final Plugin plugin;

    private final MinotaurAnimationListener animationListener;

    private final NamespacedKey bossMobKey;


    private final Map<UUID, Integer> phaseMap =
            new HashMap<>();

    private final Map<UUID, Integer> patternIndexMap =
            new HashMap<>();

    private final Map<UUID, Long> nextPatternAt =
            new HashMap<>();


    public MinotaurBossPatternTask(
            Plugin plugin,
            MinotaurAnimationListener animationListener
    ) {

        this.plugin =
                plugin;

        this.animationListener =
                animationListener;

        this.bossMobKey =
                new NamespacedKey(
                        plugin,
                        BossMobKeys.BOSS_MOB_KEY
                );
    }


    /*
     * =========================================================
     * UPDATE
     * =========================================================
     */

    @Override
    public void run() {

        long now =
                System.currentTimeMillis();


        for (
                World world
                : plugin.getServer().getWorlds()
        ) {

            for (
                    Ravager boss
                    : world.getEntitiesByClass(
                            Ravager.class
                    )
            ) {

                if (!isMinotaur(boss)) {
                    continue;
                }


                if (
                        !boss.isValid()
                                ||
                        boss.isDead()
                ) {
                    continue;
                }


                UUID uuid =
                        boss.getUniqueId();


                int phase =
                        calculatePhase(
                                boss
                        );


                Integer previousPhase =
                        phaseMap.get(
                                uuid
                        );


                if (
                        previousPhase == null
                                ||
                        previousPhase != phase
                ) {

                    phaseMap.put(
                            uuid,
                            phase
                    );

                    /*
                     * 새 Phase는 확정된 패턴 rotation의
                     * 첫 번째 패턴부터 시작한다.
                     */
                    patternIndexMap.put(
                            uuid,
                            0
                    );


                    applyPhase(
                            boss,
                            phase,
                            previousPhase != null
                    );


                    /*
                     * 페이즈 변경과 동시에 특수기가 겹치지 않도록
                     * 2.5초 대기.
                     */
                    nextPatternAt.put(
                            uuid,
                            now + 2500L
                    );
                }


                if (
                        animationListener
                                .isCombatAnimationLocked(
                                        boss
                                )
                ) {

                    continue;
                }


                long next =
                        nextPatternAt.getOrDefault(
                                uuid,
                                now
                                        + patternInterval(
                                                phase
                                        )
                        );


                if (!nextPatternAt.containsKey(uuid)) {

                    nextPatternAt.put(
                            uuid,
                            next
                    );
                }


                if (now < next) {
                    continue;
                }


                executePattern(
                        boss,
                        phase
                );


                nextPatternAt.put(
                        uuid,
                        now
                                + patternInterval(
                                        phase
                                )
                );
            }
        }


        cleanup();
    }


    /*
     * =========================================================
     * PHASE
     * =========================================================
     */

    private int calculatePhase(
            Ravager boss
    ) {

        AttributeInstance maxHealth =
                boss.getAttribute(
                        Attribute.MAX_HEALTH
                );


        if (
                maxHealth == null
                        ||
                maxHealth.getValue()
                        <= 0.0D
        ) {

            return 1;
        }


        double ratio =
                boss.getHealth()
                        / maxHealth.getValue();


        if (ratio <= PHASE_3_RATIO) {
            return 3;
        }


        if (ratio <= PHASE_2_RATIO) {
            return 2;
        }


        return 1;
    }


    private void applyPhase(
            Ravager boss,
            int phase,
            boolean announce
    ) {

        double speed =
                switch (phase) {

                    case 3 ->
                            PHASE_3_SPEED;

                    case 2 ->
                            PHASE_2_SPEED;

                    default ->
                            PHASE_1_SPEED;
                };


        AttributeInstance attribute =
                boss.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );


        if (attribute != null) {

            attribute.setBaseValue(
                    speed
            );
        }


        if (!announce) {
            return;
        }


        if (phase == 2) {

            showPhaseSubtitle(
                    boss,
                    "미궁의 주인이 분노하기 시작합니다.",
                    NamedTextColor.RED
            );


            boss.getWorld()
                    .playSound(
                            boss.getLocation(),
                            Sound.ENTITY_RAVAGER_ROAR,
                            1.2F,
                            0.85F
                    );


            boss.getWorld()
                    .spawnParticle(
                            Particle.ANGRY_VILLAGER,
                            boss.getLocation()
                                    .clone()
                                    .add(
                                            0.0D,
                                            2.0D,
                                            0.0D
                                    ),
                            25,
                            1.2D,
                            1.0D,
                            1.2D,
                            0.0D
                    );

        } else if (phase == 3) {

            showPhaseSubtitle(
                    boss,
                    "미궁의 주인이 크게 분노하며 광폭화합니다!",
                    NamedTextColor.DARK_RED
            );


            boss.getWorld()
                    .playSound(
                            boss.getLocation(),
                            Sound.ENTITY_RAVAGER_ROAR,
                            1.5F,
                            0.55F
                    );


            boss.getWorld()
                    .spawnParticle(
                            Particle.LAVA,
                            boss.getLocation()
                                    .clone()
                                    .add(
                                            0.0D,
                                            1.0D,
                                            0.0D
                                    ),
                            45,
                            2.0D,
                            0.7D,
                            2.0D,
                            0.0D
                    );
        }
    }


    /*
     * =========================================================
     * PATTERN ROTATION
     * =========================================================
     */

    private long patternInterval(
            int phase
    ) {

        return switch (phase) {

            case 3 ->
                    PHASE_3_PATTERN_MS;

            case 2 ->
                    PHASE_2_PATTERN_MS;

            default ->
                    PHASE_1_PATTERN_MS;
        };
    }


    private void executePattern(
            Ravager boss,
            int phase
    ) {

        UUID uuid =
                boss.getUniqueId();


        int index =
                patternIndexMap.getOrDefault(
                        uuid,
                        0
                );


        /*
         * Phase 1:
         *
         * slam
         */
        if (phase == 1) {

            performSlam(
                    boss
            );


        /*
         * Phase 2:
         *
         * slam
         * -> charge
         * -> double swing
         * -> charge
         */
        } else if (phase == 2) {

            switch (index % 4) {

                case 0 ->
                        performSlam(
                                boss
                        );

                case 1 ->
                        performCharge(
                                boss,
                                false
                        );

                case 2 ->
                        performDoubleSwing(
                                boss
                        );

                default ->
                        performCharge(
                                boss,
                                false
                        );
            }


        /*
         * Phase 3:
         *
         * mino_slam
         * -> enhanced charge
         * -> swing combo
         * -> slam
         * -> enhanced charge
         */
        } else {

            switch (index % 5) {

                case 0 ->
                        performMinoSlam(
                                boss
                        );

                case 1 ->
                        performCharge(
                                boss,
                                true
                        );

                case 2 ->
                        performPhase3Combo(
                                boss
                        );

                case 3 ->
                        performSlam(
                                boss
                        );

                default ->
                        performCharge(
                                boss,
                                true
                        );
            }
        }


        patternIndexMap.put(
                uuid,
                index + 1
        );
    }


    /*
     * =========================================================
     * BULL CHARGE
     * =========================================================
     *
     * 14 ticks 동안 플레이어 위치를 조준한다.
     *
     * 14 ticks가 지난 시점의 방향을 고정하고
     * 이후에는 플레이어를 유도 추적하지 않는다.
     *
     * 따라서 플레이어가 옆으로 회피할 수 있다.
     */

    private void performCharge(
            Ravager boss,
            boolean enraged
    ) {

        Player target =
                getTargetPlayer(
                        boss
                );


        if (target == null) {
            return;
        }


        /*
         * 조준 중에는 공격/이동 상태를 잠근다.
         */
        if (
                !animationListener
                        .playSpecialAnimation(
                                boss,
                                MinotaurAnimationListener
                                        .ANIMATION_IDLE,
                                700L
                        )
        ) {
            return;
        }


        boss.setVelocity(
                new Vector(
                        0.0D,
                        0.0D,
                        0.0D
                )
        );


        Location warningLocation =
                boss.getLocation()
                        .clone();


        boss.getWorld()
                .playSound(
                        warningLocation,
                        Sound.ENTITY_RAVAGER_ROAR,
                        1.2F,
                        enraged
                                ? 0.65F
                                : 0.85F
                );


        boss.getWorld()
                .spawnParticle(
                        Particle.CLOUD,
                        warningLocation.clone()
                                .add(
                                        0.0D,
                                        0.25D,
                                        0.0D
                                ),
                        30,
                        0.8D,
                        0.2D,
                        0.8D,
                        0.03D
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> startChargeMovement(
                                boss,
                                target,
                                enraged
                        ),
                        14L
                );
    }


    private void startChargeMovement(
            Ravager boss,
            Player target,
            boolean enraged
    ) {

        if (
                !boss.isValid()
                        ||
                boss.isDead()
                        ||
                !isValidTarget(
                                boss,
                                target
                        )
        ) {
            return;
        }


        Vector direction =
                target.getLocation()
                        .toVector()
                        .subtract(
                                boss.getLocation()
                                        .toVector()
                        );


        direction.setY(
                0.0D
        );


        if (
                direction.lengthSquared()
                        < 0.0001D
        ) {
            return;
        }


        direction.normalize();


        /*
         * 이 시점 이후 방향은 고정한다.
         */
        final Vector fixedDirection =
                direction.clone();


        final double speed =
                enraged
                        ? 1.45D
                        : 1.25D;


        final double damage =
                enraged
                        ? 32.0D
                        : 28.0D;


        final int maxTicks =
                enraged
                        ? 24
                        : 20;


        animationListener
                .playSpecialAnimation(
                        boss,
                        MinotaurAnimationListener
                                .ANIMATION_RUN,
                        (
                                maxTicks
                                        + 4L
                        )
                                * 50L
                );


        new org.bukkit.scheduler.BukkitRunnable() {

            private int ticks;

            private boolean hit;


            @Override
            public void run() {

                if (
                        !boss.isValid()
                                ||
                        boss.isDead()
                ) {

                    cancel();

                    return;
                }


                /*
                 * 보스룸 중심 반경 15를 넘기지 않는다.
                 */
                Location center =
                        new Location(
                                boss.getWorld(),
                                2000138.0D,
                                188.0D,
                                2000292.0D
                        );


                if (
                        boss.getLocation()
                                .distanceSquared(
                                        center
                                )
                                > 15.0D * 15.0D
                ) {

                    boss.setVelocity(
                            new Vector(
                                    0.0D,
                                    0.0D,
                                    0.0D
                            )
                    );

                    animationListener
                            .stopSpecialAnimation(
                                    boss,
                                    MinotaurAnimationListener
                                            .ANIMATION_RUN
                            );

                    cancel();

                    return;
                }


                boss.setVelocity(
                        fixedDirection.clone()
                                .multiply(
                                        speed
                                )
                );


                if (!hit) {

                    for (
                            Player player
                            : boss.getWorld()
                                    .getPlayers()
                    ) {

                        if (
                                !isValidTarget(
                                        boss,
                                        player
                                )
                        ) {
                            continue;
                        }


                        if (
                                player.getLocation()
                                        .distanceSquared(
                                                boss.getLocation()
                                        )
                                        > 2.4D * 2.4D
                        ) {
                            continue;
                        }


                        player.damage(
                                damage
                        );


                        Vector knockback =
                                fixedDirection.clone()
                                        .multiply(
                                                enraged
                                                        ? 1.65D
                                                        : 1.40D
                                        );


                        knockback.setY(
                                enraged
                                        ? 0.55D
                                        : 0.45D
                        );


                        player.setVelocity(
                                knockback
                        );


                        hit =
                                true;


                        boss.getWorld()
                                .playSound(
                                        boss.getLocation(),
                                        Sound.ENTITY_RAVAGER_ATTACK,
                                        1.2F,
                                        0.75F
                                );

                        break;
                    }
                }


                ticks++;


                if (ticks >= maxTicks) {

                    boss.setVelocity(
                            new Vector(
                                    0.0D,
                                    0.0D,
                                    0.0D
                            )
                    );


                    animationListener
                            .stopSpecialAnimation(
                                    boss,
                                    MinotaurAnimationListener
                                            .ANIMATION_RUN
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
     * DOUBLE SWING
     * =========================================================
     *
     * swing_1:
     * 7 ticks -> 18 damage
     *
     * swing_2:
     * 12 ticks에 시작
     * 이후 9 ticks -> 22 damage
     */

    private void performDoubleSwing(
            Ravager boss
    ) {

        if (
                !animationListener
                        .playSpecialAnimation(
                                boss,
                                MinotaurAnimationListener
                                        .ANIMATION_SWING_1,
                                1800L
                        )
        ) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> damageFrontTarget(
                                boss,
                                18.0D,
                                4.5D,
                                0.45D
                        ),
                        7L
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            ||
                                    boss.isDead()
                            ) {
                                return;
                            }


                            animationListener
                                    .playSpecialAnimation(
                                            boss,
                                            MinotaurAnimationListener
                                                    .ANIMATION_SWING_2,
                                            1000L
                                    );
                        },
                        12L
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> damageFrontTarget(
                                boss,
                                22.0D,
                                4.5D,
                                0.70D
                        ),
                        21L
                );


        /*
         * swing_2는 bbmodel에서 loop.
         * 반드시 강제로 종료한다.
         */
        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    boss.isValid()
                                            &&
                                    !boss.isDead()
                            ) {

                                animationListener
                                        .stopSpecialAnimation(
                                                boss,
                                                MinotaurAnimationListener
                                                        .ANIMATION_SWING_2
                                        );
                            }
                        },
                        32L
                );
    }


    /*
     * =========================================================
     * PHASE 3 COMBO
     * =========================================================
     *
     * swing_1
     * -> swing_2
     * -> mino_slam
     *
     * 피해:
     *
     * 14 / 18 / 26
     */

    private void performPhase3Combo(
            Ravager boss
    ) {

        if (
                !animationListener
                        .playSpecialAnimation(
                                boss,
                                MinotaurAnimationListener
                                        .ANIMATION_SWING_1,
                                3000L
                        )
        ) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> damageFrontTarget(
                                boss,
                                14.0D,
                                4.5D,
                                0.40D
                        ),
                        7L
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            ||
                                    boss.isDead()
                            ) {
                                return;
                            }


                            animationListener
                                    .playSpecialAnimation(
                                            boss,
                                            MinotaurAnimationListener
                                                    .ANIMATION_SWING_2,
                                            900L
                                    );
                        },
                        12L
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> damageFrontTarget(
                                boss,
                                18.0D,
                                4.5D,
                                0.55D
                        ),
                        21L
                );


        /*
         * 28 ticks에 mino_slam 시작.
         *
         * +21 ticks 후, 즉 전체 49 ticks 시점에
         * 마지막 26 damage.
         */
        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> performComboMinoSlam(
                                boss
                        ),
                        28L
                );
    }


    private void performComboMinoSlam(
            Ravager boss
    ) {

        if (
                !boss.isValid()
                        ||
                boss.isDead()
        ) {
            return;
        }


        if (
                !animationListener
                        .playSpecialAnimation(
                                boss,
                                MinotaurAnimationListener
                                        .ANIMATION_MINO_SLAM,
                                2000L
                        )
        ) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            ||
                                    boss.isDead()
                            ) {
                                return;
                            }


                            Location center =
                                    boss.getLocation()
                                            .clone();


                            boss.getWorld()
                                    .playSound(
                                            center,
                                            Sound.ENTITY_GENERIC_EXPLODE,
                                            1.5F,
                                            0.60F
                                    );


                            boss.getWorld()
                                    .spawnParticle(
                                            Particle.EXPLOSION,
                                            center.clone()
                                                    .add(
                                                            0.0D,
                                                            0.25D,
                                                            0.0D
                                                    ),
                                            4,
                                            1.0D,
                                            0.2D,
                                            1.0D,
                                            0.0D
                                    );


                            damageRadius(
                                    boss,
                                    center,
                                    4.5D,
                                    26.0D,
                                    1.20D,
                                    0.55D
                            );
                        },
                        21L
                );
    }


    /*
     * =========================================================
     * FRONT TARGET DAMAGE
     * =========================================================
     */

    private void damageFrontTarget(
            Ravager boss,
            double damage,
            double range,
            double knockback
    ) {

        if (
                !boss.isValid()
                        ||
                boss.isDead()
        ) {
            return;
        }


        Location origin =
                boss.getLocation();


        Vector forward =
                origin.getDirection()
                        .setY(
                                0.0D
                        );


        if (
                forward.lengthSquared()
                        < 0.0001D
        ) {
            return;
        }


        forward.normalize();


        for (
                Player player
                : boss.getWorld()
                        .getPlayers()
        ) {

            if (
                    !isValidTarget(
                            boss,
                            player
                    )
            ) {
                continue;
            }


            Vector offset =
                    player.getLocation()
                            .toVector()
                            .subtract(
                                    origin.toVector()
                            );


            offset.setY(
                    0.0D
            );


            double distance =
                    offset.length();


            if (
                    distance <= 0.01D
                            ||
                    distance > range
            ) {
                continue;
            }


            Vector direction =
                    offset.clone()
                            .normalize();


            /*
             * 전방 약 120도.
             */
            if (
                    forward.dot(
                            direction
                    )
                            < 0.50D
            ) {
                continue;
            }


            player.damage(
                    damage
            );


            Vector velocity =
                    direction.multiply(
                            knockback
                    );


            velocity.setY(
                    0.25D
            );


            player.setVelocity(
                    velocity
            );
        }
    }


    private Player getTargetPlayer(
            Ravager boss
    ) {

        if (
                boss.getTarget()
                        instanceof Player player
                        &&
                isValidTarget(
                        boss,
                        player
                )
        ) {

            return player;
        }


        Player nearest =
                null;

        double nearestDistance =
                Double.MAX_VALUE;


        for (
                Player player
                : boss.getWorld()
                        .getPlayers()
        ) {

            if (
                    !isValidTarget(
                            boss,
                            player
                    )
            ) {
                continue;
            }


            double distance =
                    player.getLocation()
                            .distanceSquared(
                                    boss.getLocation()
                            );


            if (distance < nearestDistance) {

                nearest =
                        player;

                nearestDistance =
                        distance;
            }
        }


        if (nearest != null) {

            boss.setTarget(
                    nearest
            );
        }


        return nearest;
    }


    /*
     * =========================================================
     * SLAM
     * =========================================================
     *
     * bbmodel impact:
     * 21 ticks
     */

    private void performSlam(
            Ravager boss
    ) {

        if (
                !animationListener
                        .playSpecialAnimation(
                                boss,
                                MinotaurAnimationListener
                                        .ANIMATION_SLAM,
                                2000L
                        )
        ) {
            return;
        }


        boss.getWorld()
                .playSound(
                        boss.getLocation(),
                        Sound.ENTITY_RAVAGER_STEP,
                        1.0F,
                        0.65F
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            ||
                                    boss.isDead()
                            ) {
                                return;
                            }


                            Location center =
                                    boss.getLocation()
                                            .clone();


                            World world =
                                    boss.getWorld();


                            world.playSound(
                                    center,
                                    Sound.ENTITY_GENERIC_EXPLODE,
                                    1.2F,
                                    0.75F
                            );


                            world.spawnParticle(
                                    Particle.EXPLOSION,
                                    center.clone()
                                            .add(
                                                    0.0D,
                                                    0.2D,
                                                    0.0D
                                            ),
                                    3,
                                    0.8D,
                                    0.2D,
                                    0.8D,
                                    0.0D
                            );


                            world.spawnParticle(
                                    Particle.CRIT,
                                    center.clone()
                                            .add(
                                                    0.0D,
                                                    0.3D,
                                                    0.0D
                                            ),
                                    80,
                                    3.0D,
                                    0.25D,
                                    3.0D,
                                    0.1D
                            );


                            damageRadius(
                                    boss,
                                    center,
                                    SLAM_RADIUS,
                                    SLAM_DAMAGE,
                                    1.05D,
                                    0.45D
                            );
                        },
                        21L
                );
    }


    /*
     * =========================================================
     * MINO SLAM
     * =========================================================
     *
     * 21 ticks:
     * 중심 충격
     *
     * 29 ticks:
     * 외곽 충격파
     */

    private void performMinoSlam(
            Ravager boss
    ) {

        if (
                !animationListener
                        .playSpecialAnimation(
                                boss,
                                MinotaurAnimationListener
                                        .ANIMATION_MINO_SLAM,
                                2000L
                        )
        ) {
            return;
        }


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            ||
                                    boss.isDead()
                            ) {
                                return;
                            }


                            Location center =
                                    boss.getLocation()
                                            .clone();


                            World world =
                                    boss.getWorld();


                            world.playSound(
                                    center,
                                    Sound.ENTITY_GENERIC_EXPLODE,
                                    1.6F,
                                    0.55F
                            );


                            world.spawnParticle(
                                    Particle.EXPLOSION,
                                    center.clone()
                                            .add(
                                                    0.0D,
                                                    0.25D,
                                                    0.0D
                                            ),
                                    5,
                                    1.2D,
                                    0.2D,
                                    1.2D,
                                    0.0D
                            );


                            damageRadius(
                                    boss,
                                    center,
                                    MINO_SLAM_INNER_RADIUS,
                                    MINO_SLAM_INNER_DAMAGE,
                                    1.15D,
                                    0.55D
                            );
                        },
                        21L
                );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            ||
                                    boss.isDead()
                            ) {
                                return;
                            }


                            Location center =
                                    boss.getLocation()
                                            .clone();


                            World world =
                                    boss.getWorld();


                            world.playSound(
                                    center,
                                    Sound.BLOCK_BASALT_BREAK,
                                    1.5F,
                                    0.55F
                            );


                            world.spawnParticle(
                                    Particle.CLOUD,
                                    center.clone()
                                            .add(
                                                    0.0D,
                                                    0.2D,
                                                    0.0D
                                            ),
                                    160,
                                    4.5D,
                                    0.25D,
                                    4.5D,
                                    0.05D
                            );


                            damageRadius(
                                    boss,
                                    center,
                                    MINO_SLAM_OUTER_RADIUS,
                                    MINO_SLAM_OUTER_DAMAGE,
                                    1.40D,
                                    0.65D
                            );
                        },
                        29L
                );
    }


    /*
     * =========================================================
     * AREA DAMAGE
     * =========================================================
     */

    private void damageRadius(
            Ravager boss,
            Location center,
            double radius,
            double damage,
            double horizontalKnockback,
            double verticalKnockback
    ) {

        for (
                Player player
                : boss.getWorld()
                        .getPlayers()
        ) {

            if (!isValidTarget(
                    boss,
                    player
            )) {
                continue;
            }


            if (
                    player.getLocation()
                            .distanceSquared(
                                    center
                            )
                            > radius
                            * radius
            ) {

                continue;
            }


            player.damage(
                    damage
            );


            Vector knockback =
                    player.getLocation()
                            .toVector()
                            .subtract(
                                    center.toVector()
                            );


            knockback.setY(
                    0.0D
            );


            if (
                    knockback.lengthSquared()
                            > 0.0001D
            ) {

                knockback.normalize()
                        .multiply(
                                horizontalKnockback
                        );
            }


            knockback.setY(
                    verticalKnockback
            );


            player.setVelocity(
                    knockback
            );
        }
    }


    private boolean isValidTarget(
            Ravager boss,
            Player player
    ) {

        if (
                !player.isOnline()
                        ||
                player.isDead()
                        ||
                player.getWorld()
                        != boss.getWorld()
        ) {
            return false;
        }


        /*
         * 미노타우로스 보스룸은
         * 소환점을 중심으로 반경 15블록.
         *
         * 여기서는 패턴이 다른 지역의 플레이어를
         * 공격하지 못하도록 같은 제한을 둔다.
         */
        Location spawnCenter =
                new Location(
                        boss.getWorld(),
                        2000138.0D,
                        188.0D,
                        2000292.0D
                );


        return player.getLocation()
                .distanceSquared(
                        spawnCenter
                )
                <= 15.0D * 15.0D;
    }


    /*
     * =========================================================
     * PHASE SUBTITLE
     * =========================================================
     */

    private void showPhaseSubtitle(
            Ravager boss,
            String text,
            NamedTextColor color
    ) {

        Title title =
                Title.title(
                        Component.empty(),
                        Component.text(
                                text,
                                color
                        ),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofSeconds(
                                        2
                                ),
                                Duration.ZERO
                        )
                );


        for (
                Player player
                : boss.getWorld()
                        .getPlayers()
        ) {

            if (
                    isValidTarget(
                            boss,
                            player
                    )
            ) {

                player.showTitle(
                        title
                );
            }
        }
    }


    /*
     * =========================================================
     * IDENTIFICATION / CLEANUP
     * =========================================================
     */

    private boolean isMinotaur(
            Entity entity
    ) {

        String bossId =
                entity.getPersistentDataContainer()
                        .get(
                                bossMobKey,
                                PersistentDataType.STRING
                        );


        return BossMobKeys.MINOTAUR_ID
                .equals(
                        bossId
                );
    }


    private void cleanup() {

        Iterator<UUID> iterator =
                phaseMap.keySet()
                        .iterator();


        while (iterator.hasNext()) {

            UUID uuid =
                    iterator.next();


            Entity entity =
                    plugin.getServer()
                            .getEntity(
                                    uuid
                            );


            if (
                    entity == null
                            ||
                    !entity.isValid()
            ) {

                iterator.remove();

                patternIndexMap.remove(
                        uuid
                );

                nextPatternAt.remove(
                        uuid
                );
            }
        }
    }
}
