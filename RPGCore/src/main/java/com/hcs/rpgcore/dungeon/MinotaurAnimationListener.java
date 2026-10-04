package com.hcs.rpgcore.dungeon;

import com.hcs.rpgcore.mob.BossMobKeys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;

import org.bukkit.NamespacedKey;
import org.bukkit.World;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageByEntityEvent;

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;


/*
 * ============================================================
 * MINOTAUR ANIMATION
 * ============================================================
 *
 * bbmodel animations:
 *
 * minotaur.idle_calm
 * minotaur.idle
 * minotaur.walking.hostile
 * minotaur.running
 * minotaur.swing_1
 * minotaur.swing_2
 * minotaur.slam
 * minotaur.mino_slam
 *
 * 일반 근접 공격:
 *
 * swing_1
 * -> 7 ticks 후 실제 피해
 *
 * swing_2
 * -> 9 ticks 후 실제 피해
 *
 * swing_2는 loop이므로 반드시 종료한다.
 */
public final class MinotaurAnimationListener
        implements Listener, Runnable {

    private static final String MODEL_ID =
            "minotaur";


    public static final String ANIMATION_IDLE_CALM =
            "minotaur.idle_calm";

    public static final String ANIMATION_IDLE =
            "minotaur.idle";

    public static final String ANIMATION_WALK =
            "minotaur.walking.hostile";

    public static final String ANIMATION_RUN =
            "minotaur.running";

    public static final String ANIMATION_SWING_1 =
            "minotaur.swing_1";

    public static final String ANIMATION_SWING_2 =
            "minotaur.swing_2";

    public static final String ANIMATION_SLAM =
            "minotaur.slam";

    public static final String ANIMATION_MINO_SLAM =
            "minotaur.mino_slam";


    private static final double MOVEMENT_THRESHOLD_SQUARED =
            0.0004D;


    /*
     * Ravager가 같은 순간 여러 번 공격 이벤트를 발생시켜도
     * 한 번의 커스텀 공격만 예약한다.
     */
    private static final long ATTACK_INTERVAL_MS =
            1100L;


    private final Plugin plugin;

    private final NamespacedKey bossMobKey;


    private final Map<UUID, String> movementState =
            new HashMap<>();

    private final Map<UUID, Long> attackAnimationUntil =
            new HashMap<>();

    private final Map<UUID, Long> lastAttackAt =
            new HashMap<>();

    private final Map<UUID, Integer> swingIndex =
            new HashMap<>();


    /*
     * 예약된 실제 피해를 boss.damage(...) 형태로 적용할 때
     * 자기 자신이 다시 공격 이벤트를 가로채는 것을 막는다.
     */
    private final Set<UUID> applyingCustomDamage =
            new HashSet<>();


    /*
     * PatternTask가 특수 공격을 재생하는 동안
     * idle/walk/run/일반 공격이 덮어쓰지 못하게 한다.
     */
    private final Map<UUID, Long> specialAnimationUntil =
            new HashMap<>();


    public MinotaurAnimationListener(
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
     * UPDATE
     * =========================================================
     */

    @Override
    public void run() {

        ModelRenderer renderer =
                BetterModel.modelOrNull(
                        MODEL_ID
                );

        if (renderer == null) {
            return;
        }


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


                UUID uuid =
                        boss.getUniqueId();


                Long specialUntil =
                        specialAnimationUntil.get(
                                uuid
                        );


                if (
                        specialUntil != null
                                &&
                        now < specialUntil
                ) {
                    continue;
                }


                if (specialUntil != null) {

                    specialAnimationUntil.remove(
                            uuid
                    );

                    movementState.remove(
                            uuid
                    );
                }


                Long attackUntil =
                        attackAnimationUntil.get(
                                uuid
                        );


                if (
                        attackUntil != null
                                &&
                        now < attackUntil
                ) {
                    continue;
                }


                EntityTracker tracker =
                        renderer.getOrCreate(
                                BukkitAdapter.adapt(
                                        (Entity) boss
                                )
                        );


                if (attackUntil != null) {

                    tracker.stopAnimation(
                            ANIMATION_SWING_1
                    );

                    tracker.stopAnimation(
                            ANIMATION_SWING_2
                    );

                    attackAnimationUntil.remove(
                            uuid
                    );

                    movementState.remove(
                            uuid
                    );
                }


                boolean moving =
                        boss.getVelocity()
                                .lengthSquared()
                                > MOVEMENT_THRESHOLD_SQUARED;


                String desired;


                if (!moving) {

                    desired =
                            healthRatio(boss)
                                    > 0.70D
                                    ? ANIMATION_IDLE_CALM
                                    : ANIMATION_IDLE;

                } else if (
                        shouldRun(
                                boss
                        )
                ) {

                    desired =
                            ANIMATION_RUN;

                } else {

                    desired =
                            ANIMATION_WALK;
                }


                String previous =
                        movementState.get(
                                uuid
                        );


                if (desired.equals(previous)) {
                    continue;
                }


                stopMovementAnimations(
                        tracker
                );


                if (
                        tracker.animate(
                                desired
                        )
                ) {

                    movementState.put(
                            uuid,
                            desired
                    );
                }
            }
        }


        cleanup();
    }


    /*
     * =========================================================
     * NORMAL ATTACK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onMinotaurAttack(
            EntityDamageByEntityEvent event
    ) {

        if (
                !(event.getDamager()
                        instanceof Ravager boss)
        ) {
            return;
        }


        if (!isMinotaur(boss)) {
            return;
        }


        UUID uuid =
                boss.getUniqueId();


        /*
         * 우리가 예약해 둔 실제 타격 이벤트.
         *
         * 이 경우에는 다시 취소하지 않는다.
         */
        if (
                applyingCustomDamage.remove(
                        uuid
                )
        ) {
            return;
        }


        if (
                !(event.getEntity()
                        instanceof Player target)
        ) {

            event.setCancelled(
                    true
            );

            return;
        }


        long now =
                System.currentTimeMillis();


        Long specialUntil =
                specialAnimationUntil.get(
                        uuid
                );


        if (
                specialUntil != null
                        &&
                now < specialUntil
        ) {

            event.setCancelled(
                    true
            );

            return;
        }


        long previousAttack =
                lastAttackAt.getOrDefault(
                        uuid,
                        0L
                );


        if (
                now - previousAttack
                        < ATTACK_INTERVAL_MS
        ) {

            event.setCancelled(
                    true
            );

            return;
        }


        /*
         * Ravager의 즉시 바닐라 피해는 취소한다.
         *
         * 실제 피해는 swing 타격 프레임에 맞춰
         * 아래 scheduler에서 다시 발생시킨다.
         */
        event.setCancelled(
                true
        );


        lastAttackAt.put(
                uuid,
                now
        );


        int index =
                swingIndex.getOrDefault(
                        uuid,
                        0
                );


        boolean firstSwing =
                index % 2 == 0;


        swingIndex.put(
                uuid,
                index + 1
        );


        EntityTracker tracker =
                getTracker(
                        boss
                );


        if (tracker == null) {
            return;
        }


        stopAllCombatAnimations(
                tracker
        );

        movementState.remove(
                uuid
        );


        String animation =
                firstSwing
                        ? ANIMATION_SWING_1
                        : ANIMATION_SWING_2;


        tracker.animate(
                animation
        );


        /*
         * swing_1 = 0.75초
         *
         * swing_2 = 약 0.917초이며 loop.
         */
        attackAnimationUntil.put(
                uuid,
                now
                        + (
                                firstSwing
                                        ? 750L
                                        : 950L
                        )
        );


        long hitDelayTicks =
                firstSwing
                        ? 7L
                        : 9L;


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> applyNormalHit(
                                boss,
                                target
                        ),
                        hitDelayTicks
                );
    }


    private void applyNormalHit(
            Ravager boss,
            Player target
    ) {

        if (
                !boss.isValid()
                        ||
                boss.isDead()
                        ||
                !target.isOnline()
                        ||
                target.isDead()
                        ||
                boss.getWorld()
                        != target.getWorld()
        ) {

            return;
        }


        /*
         * 타격 프레임이 올 때 플레이어가 이미 피했다면
         * 공격은 빗나간다.
         */
        if (
                boss.getLocation()
                        .distanceSquared(
                                target.getLocation()
                        )
                        > 4.5D * 4.5D
        ) {

            return;
        }


        applyingCustomDamage.add(
                boss.getUniqueId()
        );


        target.damage(
                50.0D,
                boss
        );
    }


    /*
     * =========================================================
     * PATTERN ANIMATION API
     * =========================================================
     */

    public boolean playSpecialAnimation(
            Ravager boss,
            String animation,
            long lockMillis
    ) {

        EntityTracker tracker =
                getTracker(
                        boss
                );


        if (tracker == null) {
            return false;
        }


        stopAllCombatAnimations(
                tracker
        );

        movementState.remove(
                boss.getUniqueId()
        );


        boolean started =
                tracker.animate(
                        animation
                );


        if (started) {

            specialAnimationUntil.put(
                    boss.getUniqueId(),
                    System.currentTimeMillis()
                            + lockMillis
            );
        }


        return started;
    }


    public void stopSpecialAnimation(
            Ravager boss,
            String animation
    ) {

        EntityTracker tracker =
                getTracker(
                        boss
                );


        if (tracker == null) {
            return;
        }


        tracker.stopAnimation(
                animation
        );


        specialAnimationUntil.remove(
                boss.getUniqueId()
        );

        movementState.remove(
                boss.getUniqueId()
        );
    }


    public boolean isCombatAnimationLocked(
            Ravager boss
    ) {

        long now =
                System.currentTimeMillis();


        Long specialUntil =
                specialAnimationUntil.get(
                        boss.getUniqueId()
                );


        if (
                specialUntil != null
                        &&
                now < specialUntil
        ) {
            return true;
        }


        Long attackUntil =
                attackAnimationUntil.get(
                        boss.getUniqueId()
                );


        return attackUntil != null
                && now < attackUntil;
    }


    /*
     * =========================================================
     * MOVEMENT
     * =========================================================
     */

    private boolean shouldRun(
            Ravager boss
    ) {

        double ratio =
                healthRatio(
                        boss
                );


        if (ratio > 0.70D) {
            return false;
        }


        if (
                !(boss.getTarget()
                        instanceof Player target)
        ) {
            return false;
        }


        if (
                target.getWorld()
                        != boss.getWorld()
        ) {
            return false;
        }


        double threshold =
                ratio <= 0.35D
                        ? 6.0D
                        : 8.0D;


        return target.getLocation()
                .distanceSquared(
                        boss.getLocation()
                )
                >= threshold
                * threshold;
    }


    private double healthRatio(
            Ravager boss
    ) {

        var maxHealth =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                );


        if (
                maxHealth == null
                        ||
                maxHealth.getValue()
                        <= 0.0D
        ) {
            return 1.0D;
        }


        return boss.getHealth()
                / maxHealth.getValue();
    }


    /*
     * =========================================================
     * TRACKER
     * =========================================================
     */

    private EntityTracker getTracker(
            Ravager boss
    ) {

        ModelRenderer renderer =
                BetterModel.modelOrNull(
                        MODEL_ID
                );


        if (renderer == null) {
            return null;
        }


        return renderer.getOrCreate(
                BukkitAdapter.adapt(
                        (Entity) boss
                )
        );
    }


    private void stopMovementAnimations(
            EntityTracker tracker
    ) {

        tracker.stopAnimation(
                ANIMATION_IDLE_CALM
        );

        tracker.stopAnimation(
                ANIMATION_IDLE
        );

        tracker.stopAnimation(
                ANIMATION_WALK
        );

        tracker.stopAnimation(
                ANIMATION_RUN
        );
    }


    private void stopAllCombatAnimations(
            EntityTracker tracker
    ) {

        stopMovementAnimations(
                tracker
        );

        tracker.stopAnimation(
                ANIMATION_SWING_1
        );

        tracker.stopAnimation(
                ANIMATION_SWING_2
        );

        tracker.stopAnimation(
                ANIMATION_SLAM
        );

        tracker.stopAnimation(
                ANIMATION_MINO_SLAM
        );
    }


    /*
     * =========================================================
     * IDENTIFICATION
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


    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    private void cleanup() {

        Iterator<UUID> iterator =
                movementState.keySet()
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

                attackAnimationUntil.remove(
                        uuid
                );

                lastAttackAt.remove(
                        uuid
                );

                swingIndex.remove(
                        uuid
                );

                specialAnimationUntil.remove(
                        uuid
                );

                applyingCustomDamage.remove(
                        uuid
                );
            }
        }
    }
}
