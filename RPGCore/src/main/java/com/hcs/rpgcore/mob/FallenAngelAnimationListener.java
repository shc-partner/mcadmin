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
 * FALLEN ANGEL ANIMATION
 * ============================================================
 *
 * 정지:
 * - animation.fallen angel.idle
 *
 * 이동:
 * - animation.fallen angel.walk
 *
 * 공격:
 * - animation.fallen angel.attack
 *
 * pose / pose2 / speed는 현재 사용하지 않는다.
 */
public final class FallenAngelAnimationListener
        implements Listener, Runnable {

    private static final String MODEL_ID =
            "fallen_angel";

    private static final String ANIMATION_IDLE =
            "animation.fallen angel.idle";

    private static final String ANIMATION_WALK =
            "animation.fallen angel.walk";

    private static final String ANIMATION_ATTACK =
            "animation.fallen angel.attack";

    private static final String ANIMATION_SPEED =
            "animation.fallen angel.speed";

    /*
     * bbmodel 기준 attack 길이 = 0.5초.
     *
     * attack은 loop 애니메이션이므로
     * 이 시간이 지나면 반드시 중지한다.
     */
    private static final long ATTACK_DURATION_MS =
            500L;

    /*
     * 실제 근접 피해 최소 간격.
     *
     * Wither Skeleton AI가 이보다 빠르게 공격을 시도하더라도
     * Fallen Angel의 실제 피해는 최소 1초 간격으로만 허용한다.
     */
    private static final long ATTACK_INTERVAL_MS =
            1000L;

    private static final double MOVEMENT_THRESHOLD_SQUARED =
            0.0004D;


    private final Plugin plugin;

    private final NamespacedKey bossMobKey;

    /*
     * 마지막으로 실제 피해가 허용된 시각을
     * Fallen Angel 엔티티 PDC에 저장한다.
     */
    private final NamespacedKey attackCooldownKey;


    /*
     * 현재 기본 이동 애니메이션.
     *
     * idle / walk / speed 중 하나를 저장한다.
     */
    private final Map<UUID, String> movementState =
            new HashMap<>();

    /*
     * 공격 애니메이션 종료 시각.
     */
    private final Map<UUID, Long> attackAnimationUntil =
            new HashMap<>();


    public FallenAngelAnimationListener(
            Plugin plugin
    ) {

        this.plugin =
                plugin;

        this.bossMobKey =
                new NamespacedKey(
                        plugin,
                        BossMobKeys.BOSS_MOB_KEY
                );

        this.attackCooldownKey =
                new NamespacedKey(
                        plugin,
                        "fallen_angel_last_attack_ms"
                );
    }


    /*
     * =========================================================
     * IDLE / WALK / ATTACK END
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
                    WitherSkeleton boss
                    : world.getEntitiesByClass(
                            WitherSkeleton.class
                    )
            ) {

                if (!isFallenAngel(boss)) {
                    continue;
                }

                UUID uuid =
                        boss.getUniqueId();

                Long attackUntil =
                        attackAnimationUntil.get(
                                uuid
                        );

                /*
                 * 공격 애니메이션 재생 중.
                 *
                 * attack이 loop이므로 종료 시각 전에는
                 * idle / walk가 개입하지 않는다.
                 */
                if (
                        attackUntil != null
                                && now < attackUntil
                ) {
                    continue;
                }


                EntityTracker tracker =
                        renderer.getOrCreate(
                                BukkitAdapter.adapt(
                                        (Entity) boss
                                )
                        );


                /*
                 * 공격 시간이 끝났다면 loop attack을
                 * 반드시 중지한다.
                 */
                if (attackUntil != null) {

                    tracker.stopAnimation(
                            ANIMATION_ATTACK
                    );

                    attackAnimationUntil.remove(
                            uuid
                    );

                    /*
                     * 현재 상태에 맞는 idle/walk를
                     * 다시 선택하도록 초기화한다.
                     */
                    movementState.remove(
                            uuid
                    );
                }


                boolean moving =
                        boss.getVelocity()
                                .lengthSquared()
                                > MOVEMENT_THRESHOLD_SQUARED;

                String desiredAnimation;

                if (!moving) {

                    desiredAnimation =
                            ANIMATION_IDLE;

                } else if (
                        shouldUseSpeedAnimation(
                                boss
                        )
                ) {

                    desiredAnimation =
                            ANIMATION_SPEED;

                } else {

                    desiredAnimation =
                            ANIMATION_WALK;
                }


                String previous =
                        movementState.get(
                                uuid
                        );

                if (
                        desiredAnimation.equals(
                                previous
                        )
                ) {
                    continue;
                }


                tracker.stopAnimation(
                        ANIMATION_IDLE
                );

                tracker.stopAnimation(
                        ANIMATION_WALK
                );

                tracker.stopAnimation(
                        ANIMATION_SPEED
                );


                if (
                        tracker.animate(
                                desiredAnimation
                        )
                ) {

                    movementState.put(
                            uuid,
                            desiredAnimation
                    );
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
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onFallenAngelAttack(
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


        /*
         * =====================================================
         * ACTUAL DAMAGE INTERVAL
         * =====================================================
         *
         * 애니메이션만 느리게 보이게 하는 것이 아니라
         * 실제 근접 피해 발생 빈도를 최소 1초로 제한한다.
         */
        long now =
                System.currentTimeMillis();

        Long lastAttack =
                boss.getPersistentDataContainer()
                        .get(
                                attackCooldownKey,
                                PersistentDataType.LONG
                        );

        if (
                lastAttack != null
                        && now - lastAttack
                        < ATTACK_INTERVAL_MS
        ) {

            event.setCancelled(
                    true
            );

            return;
        }

        boss.getPersistentDataContainer()
                .set(
                        attackCooldownKey,
                        PersistentDataType.LONG,
                        now
                );


        EntityTracker tracker =
                getTracker(
                        boss
                );

        if (tracker == null) {
            return;
        }


        UUID uuid =
                boss.getUniqueId();


        /*
         * 이동/정지 애니메이션과 공격이 겹치지 않도록 한다.
         */
        tracker.stopAnimation(
                ANIMATION_IDLE
        );

        tracker.stopAnimation(
                ANIMATION_WALK
        );

        tracker.stopAnimation(
                ANIMATION_SPEED
        );

        /*
         * attack은 loop이므로
         * 기존 공격 loop가 남아 있으면 먼저 정리한다.
         */
        tracker.stopAnimation(
                ANIMATION_ATTACK
        );

        movementState.remove(
                uuid
        );


        tracker.animate(
                ANIMATION_ATTACK
        );

        attackAnimationUntil.put(
                uuid,
                System.currentTimeMillis()
                        + ATTACK_DURATION_MS
        );
    }


    /*
     * =========================================================
     * SPEED ANIMATION
     * =========================================================
     *
     * HP 70% 이하부터 사용한다.
     *
     * 플레이어와 8블록 이상 떨어진 상태에서 실제 이동 중이면
     * walk 대신 speed 모션을 사용한다.
     */
    private boolean shouldUseSpeedAnimation(
            WitherSkeleton boss
    ) {

        org.bukkit.attribute.AttributeInstance maxHealth =
                boss.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                );

        if (
                maxHealth == null
                        || maxHealth.getValue()
                        <= 0.0D
        ) {
            return false;
        }

        double healthRatio =
                boss.getHealth()
                        / maxHealth.getValue();

        if (healthRatio > 0.70D) {
            return false;
        }


        if (
                !(boss.getTarget()
                        instanceof org.bukkit.entity.Player target)
        ) {
            return false;
        }

        if (
                target.getWorld()
                        != boss.getWorld()
        ) {
            return false;
        }


        return target.getLocation()
                .distanceSquared(
                        boss.getLocation()
                )
                >= 8.0D * 8.0D;
    }


    /*
     * =========================================================
     * TRACKER
     * =========================================================
     */

    private EntityTracker getTracker(
            WitherSkeleton boss
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
     * CLEANUP
     * =========================================================
     */

    private void cleanupRemovedEntities() {

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
                            || !entity.isValid()
            ) {

                iterator.remove();

                attackAnimationUntil.remove(
                        uuid
                );
            }
        }


        attackAnimationUntil.keySet()
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
