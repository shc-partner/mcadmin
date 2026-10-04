package com.hcs.rpgcore.mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;


/*
 * ============================================================
 * FALLEN ANGEL BOSS PATTERN
 * ============================================================
 *
 * Phase 1: 100% ~ 70%
 * Phase 2:  70% ~ 35%
 * Phase 3:  35% ~ 0%
 *
 * 일반 공격 애니메이션은 FallenAngelAnimationListener가 담당한다.
 *
 * 이 클래스는:
 * - 페이즈
 * - 광폭화 연출
 * - 특수 공격
 * - 공중 띄우기
 * - 페이즈별 이동속도
 *
 * 를 담당한다.
 */
public final class FallenAngelBossPatternTask
        implements Listener, Runnable {

    private static final double PHASE_2_RATIO =
            0.70D;

    private static final double PHASE_3_RATIO =
            0.35D;


    /*
     * 기본 이동속도 0.24에서 시작한다.
     *
     * 광폭화 후에도 기존 0.30보다 낮은 값을 유지한다.
     */
    private static final double PHASE_1_SPEED =
            0.24D;

    private static final double PHASE_2_SPEED =
            0.26D;

    private static final double PHASE_3_SPEED =
            0.28D;


    private static final long PHASE_1_PATTERN_MS =
            7000L;

    private static final long PHASE_2_PATTERN_MS =
            6000L;

    private static final long PHASE_3_PATTERN_MS =
            5000L;


    /*
     * 특수 공격 피해량.
     *
     * 일반 근접 공격 45보다 낮게 두고
     * 범위/공중제어 효과 자체에 의미를 둔다.
     */
    private static final double FRONTAL_SLASH_DAMAGE =
            18.0D;

    private static final double SHOCKWAVE_DAMAGE =
            20.0D;

    private static final double DIVINE_STRIKE_DAMAGE =
            22.0D;

    private static final double DARK_BURST_DAMAGE =
            26.0D;


    private static final double FRONTAL_SLASH_RANGE =
            5.0D;

    private static final double SHOCKWAVE_RADIUS =
            5.5D;

    private static final double DARK_BURST_RADIUS =
            6.0D;


    private final Plugin plugin;

    private final NamespacedKey bossMobKey;


    private final Map<UUID, Integer> phaseMap =
            new HashMap<>();

    private final Map<UUID, Integer> patternIndexMap =
            new HashMap<>();

    private final Map<UUID, Long> nextPatternAt =
            new HashMap<>();

    /*
     * 정상 근접 공격 중 몇 번째 공격인지 저장한다.
     *
     * 세 번째 실제 타격마다 플레이어를 띄운다.
     */
    private final Map<UUID, Integer> normalHitCount =
            new HashMap<>();


    public FallenAngelBossPatternTask(
            Plugin plugin
    ) {

        this.plugin =
                plugin;

        this.bossMobKey =
                new NamespacedKey(
                        plugin,
                        BossMobKeys.BOSS_MOB_KEY
                );
    }


    /*
     * =========================================================
     * MAIN UPDATE
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
                    WitherSkeleton boss
                    : world.getEntitiesByClass(
                            WitherSkeleton.class
                    )
            ) {

                if (!isFallenAngel(boss)) {
                    continue;
                }

                if (
                        !boss.isValid()
                                || boss.isDead()
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
                                || previousPhase != phase
                ) {

                    phaseMap.put(
                            uuid,
                            phase
                    );

                    applyPhase(
                            boss,
                            phase,
                            previousPhase != null
                    );

                    /*
                     * 페이즈 전환 직후 특수 공격이
                     * 즉시 겹치지 않도록 잠깐 시간을 준다.
                     */
                    nextPatternAt.put(
                            uuid,
                            now + 2500L
                    );
                }


                long next =
                        nextPatternAt.getOrDefault(
                                uuid,
                                now + patternInterval(
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
            WitherSkeleton boss
    ) {

        AttributeInstance maxHealth =
                boss.getAttribute(
                        Attribute.MAX_HEALTH
                );

        if (
                maxHealth == null
                        || maxHealth.getValue()
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
            WitherSkeleton boss,
            int phase,
            boolean announce
    ) {

        double movementSpeed =
                switch (phase) {

                    case 3 ->
                            PHASE_3_SPEED;

                    case 2 ->
                            PHASE_2_SPEED;

                    default ->
                            PHASE_1_SPEED;
                };


        AttributeInstance speed =
                boss.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );

        if (speed != null) {

            speed.setBaseValue(
                    movementSpeed
            );
        }


        if (!announce) {
            return;
        }


        World world =
                boss.getWorld();

        Location location =
                boss.getLocation()
                        .clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        );


        if (phase == 2) {

            world.spawnParticle(
                    Particle.REVERSE_PORTAL,
                    location,
                    120,
                    1.5D,
                    1.0D,
                    1.5D,
                    0.08D
            );

            world.spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    location,
                    60,
                    1.1D,
                    1.0D,
                    1.1D,
                    0.04D
            );

            world.playSound(
                    location,
                    Sound.ENTITY_WITHER_SPAWN,
                    1.2F,
                    1.25F
            );

            broadcastToNearbyPlayers(
                    boss,
                    Component.text(
                            "타락한 천사가 분노하기 시작합니다.",
                            NamedTextColor.LIGHT_PURPLE
                    )
            );

        } else if (phase == 3) {

            world.spawnParticle(
                    Particle.PORTAL,
                    location,
                    220,
                    2.2D,
                    1.4D,
                    2.2D,
                    0.20D
            );

            world.spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    location,
                    100,
                    1.6D,
                    1.2D,
                    1.6D,
                    0.07D
            );

            world.playSound(
                    location,
                    Sound.ENTITY_ENDER_DRAGON_GROWL,
                    1.5F,
                    0.70F
            );

            broadcastToNearbyPlayers(
                    boss,
                    Component.text(
                            "타락한 천사가 크게 분노하며 광폭화합니다!",
                            NamedTextColor.RED
                    )
            );
        }
    }


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


    /*
     * =========================================================
     * PATTERN ROTATION
     * =========================================================
     */

    private void executePattern(
            WitherSkeleton boss,
            int phase
    ) {

        UUID uuid =
                boss.getUniqueId();

        int index =
                patternIndexMap.getOrDefault(
                        uuid,
                        0
                );


        if (phase == 1) {

            performFrontalSlash(
                    boss
            );

            index++;

        } else if (phase == 2) {

            if (index % 2 == 0) {

                performShockwave(
                        boss,
                        false
                );

            } else {

                performDivineStrike(
                        boss
                );
            }

            index++;

        } else {

            switch (index % 3) {

                case 0 ->
                        performFrontalSlash(
                                boss
                        );

                case 1 ->
                        performShockwave(
                                boss,
                                true
                        );

                default ->
                        performDarkBurst(
                                boss
                        );
            }

            index++;
        }


        patternIndexMap.put(
                uuid,
                index
        );
    }


    /*
     * =========================================================
     * PATTERN 1
     * FALLEN SLASH
     * =========================================================
     */

    private void performFrontalSlash(
            WitherSkeleton boss
    ) {

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


        World world =
                boss.getWorld();

        world.playSound(
                origin,
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.4F,
                0.65F
        );

        world.spawnParticle(
                Particle.SWEEP_ATTACK,
                origin.clone()
                        .add(
                                forward.clone()
                                        .multiply(
                                                2.0D
                                        )
                        )
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                10,
                1.4D,
                0.6D,
                1.4D,
                0.0D
        );

        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                origin.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                45,
                1.8D,
                0.8D,
                1.8D,
                0.08D
        );


        for (
                Player player
                : world.getPlayers()
        ) {

            if (!isValidTarget(boss, player)) {
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
                            || distance
                            > FRONTAL_SLASH_RANGE
            ) {
                continue;
            }


            Vector direction =
                    offset.clone()
                            .normalize();

            /*
             * 정면 약 120도.
             */
            if (
                    forward.dot(
                            direction
                    ) < 0.50D
            ) {
                continue;
            }


            player.damage(
                    FRONTAL_SLASH_DAMAGE
            );

            direction
                    .multiply(
                            0.70D
                    )
                    .setY(
                            0.35D
                    );

            player.setVelocity(
                    direction
            );
        }
    }


    /*
     * =========================================================
     * PATTERN 2
     * FALLEN SHOCKWAVE
     * =========================================================
     */

    private void performShockwave(
            WitherSkeleton boss,
            boolean enraged
    ) {

        Location center =
                boss.getLocation();

        World world =
                boss.getWorld();


        world.spawnParticle(
                Particle.PORTAL,
                center.clone()
                        .add(
                                0.0D,
                                1.0D,
                                0.0D
                        ),
                enraged
                        ? 220
                        : 150,
                SHOCKWAVE_RADIUS,
                0.8D,
                SHOCKWAVE_RADIUS,
                0.15D
        );

        world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                center.clone()
                        .add(
                                0.0D,
                                0.4D,
                                0.0D
                        ),
                enraged
                        ? 90
                        : 55,
                2.5D,
                0.35D,
                2.5D,
                0.04D
        );

        world.playSound(
                center,
                Sound.ENTITY_ENDERMAN_SCREAM,
                1.4F,
                enraged
                        ? 0.55F
                        : 0.75F
        );


        for (
                Player player
                : world.getPlayers()
        ) {

            if (!isValidTarget(boss, player)) {
                continue;
            }

            if (
                    player.getLocation()
                            .distanceSquared(
                                    center
                            )
                            > SHOCKWAVE_RADIUS
                            * SHOCKWAVE_RADIUS
            ) {
                continue;
            }


            player.damage(
                    SHOCKWAVE_DAMAGE
                            + (
                                    enraged
                                            ? 4.0D
                                            : 0.0D
                            )
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
                                enraged
                                        ? 1.45D
                                        : 1.15D
                        );
            }

            knockback.setY(
                    enraged
                            ? 0.75D
                            : 0.58D
            );

            player.setVelocity(
                    knockback
            );
        }
    }


    /*
     * =========================================================
     * PATTERN 3
     * DIVINE STRIKE
     * =========================================================
     *
     * 현재 타겟 위치에 경고 이펙트를 보여준 뒤
     * 짧은 지연 후 폭발한다.
     */

    private void performDivineStrike(
            WitherSkeleton boss
    ) {

        Player target =
                getTargetPlayer(
                        boss
                );

        if (target == null) {
            return;
        }


        Location targetLocation =
                target.getLocation()
                        .clone();

        World world =
                targetLocation.getWorld();

        if (world == null) {
            return;
        }


        /*
         * 공격 전 경고.
         */
        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                targetLocation.clone()
                        .add(
                                0.0D,
                                0.15D,
                                0.0D
                        ),
                75,
                1.5D,
                0.08D,
                1.5D,
                0.02D
        );

        world.playSound(
                targetLocation,
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                0.9F,
                1.45F
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            || boss.isDead()
                            ) {
                                return;
                            }

                            World impactWorld =
                                    targetLocation.getWorld();

                            if (impactWorld == null) {
                                return;
                            }


                            impactWorld.spawnParticle(
                                    Particle.SOUL_FIRE_FLAME,
                                    targetLocation.clone()
                                            .add(
                                                    0.0D,
                                                    0.4D,
                                                    0.0D
                                            ),
                                    90,
                                    1.8D,
                                    0.5D,
                                    1.8D,
                                    0.06D
                            );

                            impactWorld.spawnParticle(
                                    Particle.REVERSE_PORTAL,
                                    targetLocation.clone()
                                            .add(
                                                    0.0D,
                                                    0.8D,
                                                    0.0D
                                            ),
                                    110,
                                    1.8D,
                                    0.8D,
                                    1.8D,
                                    0.10D
                            );

                            impactWorld.playSound(
                                    targetLocation,
                                    Sound.ENTITY_GENERIC_EXPLODE,
                                    1.1F,
                                    1.20F
                            );


                            for (
                                    Player player
                                    : impactWorld.getPlayers()
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
                                                        targetLocation
                                                )
                                                > 2.5D * 2.5D
                                ) {
                                    continue;
                                }


                                player.damage(
                                        DIVINE_STRIKE_DAMAGE
                                );

                                Vector velocity =
                                        player.getVelocity();

                                velocity.setY(
                                        0.72D
                                );

                                player.setVelocity(
                                        velocity
                                );
                            }
                        },
                        14L
                );
    }


    /*
     * =========================================================
     * PATTERN 4
     * DARK BURST
     * =========================================================
     */

    private void performDarkBurst(
            WitherSkeleton boss
    ) {

        Location center =
                boss.getLocation()
                        .clone();


        World world =
                boss.getWorld();


        /*
         * 경고 이펙트.
         */
        world.spawnParticle(
                Particle.REVERSE_PORTAL,
                center.clone()
                        .add(
                                0.0D,
                                0.15D,
                                0.0D
                        ),
                160,
                DARK_BURST_RADIUS,
                0.10D,
                DARK_BURST_RADIUS,
                0.04D
        );

        world.playSound(
                center,
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                1.2F,
                0.65F
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !boss.isValid()
                                            || boss.isDead()
                            ) {
                                return;
                            }


                            World impactWorld =
                                    boss.getWorld();

                            impactWorld.spawnParticle(
                                    Particle.PORTAL,
                                    center.clone()
                                            .add(
                                                    0.0D,
                                                    1.0D,
                                                    0.0D
                                            ),
                                    300,
                                    DARK_BURST_RADIUS,
                                    1.2D,
                                    DARK_BURST_RADIUS,
                                    0.25D
                            );

                            impactWorld.spawnParticle(
                                    Particle.SOUL_FIRE_FLAME,
                                    center.clone()
                                            .add(
                                                    0.0D,
                                                    0.6D,
                                                    0.0D
                                            ),
                                    130,
                                    3.0D,
                                    0.6D,
                                    3.0D,
                                    0.08D
                            );

                            impactWorld.playSound(
                                    center,
                                    Sound.ENTITY_ENDER_DRAGON_GROWL,
                                    1.4F,
                                    0.55F
                            );


                            for (
                                    Player player
                                    : impactWorld.getPlayers()
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
                                                        center
                                                )
                                                > DARK_BURST_RADIUS
                                                * DARK_BURST_RADIUS
                                ) {
                                    continue;
                                }


                                player.damage(
                                        DARK_BURST_DAMAGE
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
                                                    1.45D
                                            );
                                }

                                knockback.setY(
                                        0.90D
                                );

                                player.setVelocity(
                                        knockback
                                );
                            }
                        },
                        20L
                );
    }


    /*
     * =========================================================
     * NORMAL ATTACK LAUNCH
     * =========================================================
     *
     * 실제로 성공한 일반 근접 공격 세 번째마다
     * 플레이어를 공중에 띄운다.
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onNormalBossHit(
            EntityDamageByEntityEvent event
    ) {

        if (
                !(event.getDamager()
                        instanceof WitherSkeleton boss)
        ) {
            return;
        }

        if (!isFallenAngel(boss)) {
            return;
        }

        if (
                !(event.getEntity()
                        instanceof Player player)
        ) {
            return;
        }


        UUID uuid =
                boss.getUniqueId();

        int count =
                normalHitCount.getOrDefault(
                        uuid,
                        0
                )
                        + 1;

        if (count >= 3) {

            count =
                    0;

            Vector knockback =
                    player.getLocation()
                            .toVector()
                            .subtract(
                                    boss.getLocation()
                                            .toVector()
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
                                0.55D
                        );
            }

            knockback.setY(
                    0.62D
            );

            player.setVelocity(
                    knockback
            );


            player.getWorld()
                    .spawnParticle(
                            Particle.REVERSE_PORTAL,
                            player.getLocation()
                                    .clone()
                                    .add(
                                            0.0D,
                                            1.0D,
                                            0.0D
                                    ),
                            35,
                            0.7D,
                            0.7D,
                            0.7D,
                            0.06D
                    );

            player.getWorld()
                    .playSound(
                            player.getLocation(),
                            Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                            1.0F,
                            0.70F
                    );
        }


        normalHitCount.put(
                uuid,
                count
        );
    }


    /*
     * =========================================================
     * TARGET
     * =========================================================
     */

    private Player getTargetPlayer(
            WitherSkeleton boss
    ) {

        if (
                boss.getTarget()
                        instanceof Player player
                        && isValidTarget(
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

            if (
                    distance
                            < nearestDistance
            ) {

                nearest =
                        player;

                nearestDistance =
                        distance;
            }
        }

        return nearest;
    }


    private boolean isValidTarget(
            WitherSkeleton boss,
            Player player
    ) {

        return player != null
                && player.isOnline()
                && !player.isDead()
                && player.getWorld()
                == boss.getWorld();
    }


    /*
     * =========================================================
     * IDENTIFICATION
     * =========================================================
     */

    private boolean isFallenAngel(
            Entity entity
    ) {

        String bossId =
                entity.getPersistentDataContainer()
                        .get(
                                bossMobKey,
                                PersistentDataType.STRING
                        );

        return BossMobKeys.FALLEN_ANGEL_ID
                .equals(
                        bossId
                );
    }


    /*
     * =========================================================
     * MESSAGE
     * =========================================================
     */

    private void broadcastToNearbyPlayers(
            WitherSkeleton boss,
            Component message
    ) {

        for (
                Player player
                : boss.getWorld()
                        .getPlayers()
        ) {

            if (
                    player.getLocation()
                            .distanceSquared(
                                    boss.getLocation()
                            )
                            <= 64.0D * 64.0D
            ) {

                player.sendMessage(
                        message
                );
            }
        }
    }


    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

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
                            || !entity.isValid()
            ) {

                iterator.remove();

                patternIndexMap.remove(
                        uuid
                );

                nextPatternAt.remove(
                        uuid
                );

                normalHitCount.remove(
                        uuid
                );
            }
        }
    }
}
