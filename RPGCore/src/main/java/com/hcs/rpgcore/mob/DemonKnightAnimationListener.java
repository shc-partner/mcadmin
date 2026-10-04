package com.hcs.rpgcore.mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;


/*
 * ============================================================
 * DEMON KNIGHT ANIMATION
 * ============================================================
 *
 * 정지:
 * - idle
 *
 * 이동:
 * - walk
 *
 * 공격:
 * - 1타: hammer_attack_1
 * - 2타: hammer_attack_2
 * - 3타: hammer_attack_3
 * - 이후 반복
 *
 * 이번 단계에서는 run / guard / shield 공격 / death는 사용하지 않는다.
 */
public final class DemonKnightAnimationListener
        implements Listener, Runnable {

    private static final String MODEL_ID =
            "demon_knight";

    private static final String ANIMATION_IDLE =
            "idle";

    private static final String ANIMATION_WALK =
            "walk";

    private static final String ANIMATION_ATTACK_1 =
            "hammer_attack_1";

    private static final String ANIMATION_ATTACK_2 =
            "hammer_attack_2";

    private static final String ANIMATION_ATTACK_3 =
            "hammer_attack_3";

    /*
     * bbmodel 기준 애니메이션 길이.
     *
     * hammer_attack_1 = 1.25 seconds
     * hammer_attack_2 = 1.25 seconds
     * hammer_attack_3 = 2.25 seconds
     */
    private static final long ATTACK_1_DURATION_MS =
            1250L;

    private static final long ATTACK_2_DURATION_MS =
            1250L;

    private static final long ATTACK_3_DURATION_MS =
            2250L;

    /*
     * 아주 작은 물리 흔들림은 이동으로 판단하지 않는다.
     */
    private static final double MOVEMENT_THRESHOLD_SQUARED =
            0.0004D;

    private final Plugin plugin;
    private final NamespacedKey eliteMobKey;

    /*
     * 개체별 공격 순서.
     *
     * 1 -> 2 -> 3 -> 1 반복.
     */
    private final Map<UUID, Integer> attackCounts =
            new HashMap<>();

    /*
     * 공격 애니메이션이 끝나는 시각.
     *
     * 공격 중에는 idle / walk를 재생하지 않는다.
     */
    private final Map<UUID, Long> attackAnimationUntil =
            new HashMap<>();

    /*
     * 이동 상태.
     *
     * true  = walk
     * false = idle
     */
    private final Map<UUID, Boolean> movementState =
            new HashMap<>();


    public DemonKnightAnimationListener(
            Plugin plugin
    ) {

        this.plugin = plugin;

        this.eliteMobKey =
                new NamespacedKey(
                        plugin,
                        EliteDemonKnightListener
                                .ELITE_MOB_KEY
                );
    }


    /*
     * =========================================================
     * IDLE / WALK
     * =========================================================
     */

    @Override
    public void run() {

        long now =
                System.currentTimeMillis();

        ModelRenderer renderer =
                BetterModel.modelOrNull(
                        MODEL_ID
                );

        if (renderer == null) {
            return;
        }


        for (
                World world
                : plugin.getServer().getWorlds()
        ) {

            for (
                    WitherSkeleton entity
                    : world.getEntitiesByClass(
                            WitherSkeleton.class
                    )
            ) {

                if (!isDemonKnight(entity)) {
                    continue;
                }

                UUID uuid =
                        entity.getUniqueId();

                Long attackUntil =
                        attackAnimationUntil.get(
                                uuid
                        );

                /*
                 * 공격 애니메이션 중에는
                 * idle / walk를 재생하지 않는다.
                 */
                if (
                        attackUntil != null
                                && now < attackUntil
                ) {
                    continue;
                }

                if (attackUntil != null) {

                    attackAnimationUntil.remove(
                            uuid
                    );

                    /*
                     * 공격 종료 후 현재 상태 애니메이션을
                     * 다시 선택할 수 있도록 초기화한다.
                     */
                    movementState.remove(
                            uuid
                    );
                }


                boolean moving =
                        entity.getVelocity()
                                .lengthSquared()
                                > MOVEMENT_THRESHOLD_SQUARED;

                Boolean previous =
                        movementState.get(
                                uuid
                        );

                if (
                        previous != null
                                && previous == moving
                ) {
                    continue;
                }


                EntityTracker tracker =
                        renderer.getOrCreate(
                                BukkitAdapter.adapt(
                                        (Entity) entity
                                )
                        );


                if (moving) {

                    tracker.stopAnimation(
                            ANIMATION_IDLE
                    );

                    if (
                            tracker.animate(
                                    ANIMATION_WALK
                            )
                    ) {

                        movementState.put(
                                uuid,
                                true
                        );
                    }

                } else {

                    tracker.stopAnimation(
                            ANIMATION_WALK
                    );

                    if (
                            tracker.animate(
                                    ANIMATION_IDLE
                            )
                    ) {

                        movementState.put(
                                uuid,
                                false
                        );
                    }
                }
            }
        }

        cleanupRemovedEntities();
    }


    /*
     * =========================================================
     * ATTACK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onDemonKnightAttack(
            EntityDamageByEntityEvent event
    ) {

        if (
                !(event.getDamager()
                        instanceof WitherSkeleton attacker)
        ) {
            return;
        }

        if (!isDemonKnight(attacker)) {
            return;
        }


        EntityTracker tracker =
                getTracker(
                        attacker
                );

        if (tracker == null) {
            return;
        }


        UUID uuid =
                attacker.getUniqueId();

        int attackCount =
                attackCounts.getOrDefault(
                        uuid,
                        0
                )
                        + 1;

        if (attackCount > 3) {
            attackCount = 1;
        }

        attackCounts.put(
                uuid,
                attackCount
        );


        /*
         * 이동 애니메이션이 공격과 겹치지 않도록 중지한다.
         */
        tracker.stopAnimation(
                ANIMATION_IDLE
        );

        tracker.stopAnimation(
                ANIMATION_WALK
        );

        /*
         * 직전 공격 애니메이션이 남아 있을 경우
         * 새로운 공격과 겹치지 않도록 정리한다.
         */
        stopAttackAnimations(
                tracker
        );

        movementState.remove(
                uuid
        );


        long duration;

        if (attackCount == 1) {

            tracker.animate(
                    ANIMATION_ATTACK_1
            );

            duration =
                    ATTACK_1_DURATION_MS;

        } else if (attackCount == 2) {

            tracker.animate(
                    ANIMATION_ATTACK_2
            );

            duration =
                    ATTACK_2_DURATION_MS;

        } else {

            tracker.animate(
                    ANIMATION_ATTACK_3
            );

            duration =
                    ATTACK_3_DURATION_MS;
        }


        attackAnimationUntil.put(
                uuid,
                System.currentTimeMillis()
                        + duration
        );
    }


    /*
     * =========================================================
     * TRACKER
     * =========================================================
     */

    private EntityTracker getTracker(
            WitherSkeleton entity
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
                        (Entity) entity
                )
        );
    }


    private void stopAttackAnimations(
            EntityTracker tracker
    ) {

        tracker.stopAnimation(
                ANIMATION_ATTACK_1
        );

        tracker.stopAnimation(
                ANIMATION_ATTACK_2
        );

        tracker.stopAnimation(
                ANIMATION_ATTACK_3
        );
    }


    /*
     * =========================================================
     * IDENTIFICATION
     * =========================================================
     */

    private boolean isDemonKnight(
            Entity entity
    ) {

        String eliteId =
                entity.getPersistentDataContainer()
                        .get(
                                eliteMobKey,
                                PersistentDataType.STRING
                        );

        return EliteDemonKnightListener
                .DEMON_KNIGHT_ID
                .equals(
                        eliteId
                );
    }


    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    private void cleanupRemovedEntities() {

        Iterator<UUID> iterator =
                attackCounts.keySet()
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

                attackAnimationUntil.remove(
                        uuid
                );

                movementState.remove(
                        uuid
                );
            }
        }


        /*
         * 아직 공격한 적 없는 개체도 movementState에는
         * 존재할 수 있으므로 별도로 정리한다.
         */
        movementState.keySet()
                .removeIf(
                        uuid -> {

                            Entity entity =
                                    plugin.getServer()
                                            .getEntity(
                                                    uuid
                                            );

                            return entity == null
                                    || !entity.isValid();
                        }
                );
    }
}
