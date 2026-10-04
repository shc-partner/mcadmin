package com.hcs.rpgcore.mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.EntityTracker;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;


/*
 * ============================================================
 * WARBRINGER ANIMATION
 * ============================================================
 *
 * 정지:
 * - 별도 애니메이션 없음
 * - hover 사용 안 함
 *
 * 이동:
 * - pursue
 *
 * 공격:
 * - 1타: sweep
 * - 2타: sweep
 * - 3타: heavy_cleave
 * - 이후 반복
 */
public final class WarbringerAnimationListener
        implements Listener, Runnable {

    private static final String MODEL_ID =
            "visage_of_war";

    private static final String ANIMATION_PURSUE =
            "animation.visage_of_war.pursue";

    private static final String ANIMATION_SWEEP =
            "animation.visage_of_war.sweep";

    private static final String ANIMATION_HEAVY_CLEAVE =
            "animation.visage_of_war.heavy_cleave";

    /*
     * bbmodel 기준:
     *
     * sweep        = 1.3 seconds
     * heavy_cleave = 3.0 seconds
     */
    private static final long SWEEP_DURATION_MS =
            1300L;

    private static final long HEAVY_CLEAVE_DURATION_MS =
            3000L;

    /*
     * 아주 작은 물리 흔들림만으로 pursue가 시작되지 않도록
     * 실제 이동 여부 판단에 최소 velocity를 둔다.
     */
    private static final double MOVEMENT_THRESHOLD_SQUARED =
            0.0004D;

    private final Plugin plugin;

    /*
     * 개체별 공격 횟수.
     *
     * 1, 2 -> sweep
     * 3    -> heavy_cleave
     */
    private final Map<UUID, Integer> attackCounts =
            new HashMap<>();

    /*
     * 공격 애니메이션이 끝나기 전까지
     * pursue가 다시 재생되지 않도록 한다.
     */
    private final Map<UUID, Long> attackAnimationUntil =
            new HashMap<>();

    /*
     * pursue를 이미 재생 중인 개체.
     *
     * 매 런타임 tick마다 같은 loop 애니메이션을
     * 다시 요청하지 않도록 관리한다.
     */
    private final Map<UUID, Boolean> pursuing =
            new HashMap<>();


    public WarbringerAnimationListener(
            Plugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * =========================================================
     * MOVEMENT ANIMATION
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
                    Ravager ravager
                    : world.getEntitiesByClass(
                            Ravager.class
                    )
            ) {

                if (!isWarbringer(ravager)) {
                    continue;
                }

                UUID uuid =
                        ravager.getUniqueId();

                /*
                 * 공격 애니메이션 중에는
                 * pursue를 재생하지 않는다.
                 */
                Long attackUntil =
                        attackAnimationUntil.get(
                                uuid
                        );

                if (
                        attackUntil != null
                                && now < attackUntil
                ) {

                    stopPursueIfNecessary(
                            ravager
                    );

                    continue;
                }

                if (attackUntil != null) {

                    attackAnimationUntil.remove(
                            uuid
                    );
                }


                boolean moving =
                        ravager.getVelocity()
                                .lengthSquared()
                                > MOVEMENT_THRESHOLD_SQUARED;

                if (moving) {

                    startPursueIfNecessary(
                            ravager
                    );

                } else {

                    stopPursueIfNecessary(
                            ravager
                    );
                }
            }
        }

        cleanupRemovedEntities();
    }


    /*
     * =========================================================
     * ATTACK ANIMATION
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onWarbringerAttack(
            EntityDamageByEntityEvent event
    ) {

        if (
                !(event.getDamager()
                        instanceof Ravager ravager)
        ) {
            return;
        }

        if (!isWarbringer(ravager)) {
            return;
        }

        UUID uuid =
                ravager.getUniqueId();

        int attackCount =
                attackCounts.getOrDefault(
                        uuid,
                        0
                )
                        + 1;

        /*
         * 1 -> 2 -> 3 -> 1 반복.
         */
        if (attackCount > 3) {
            attackCount = 1;
        }

        attackCounts.put(
                uuid,
                attackCount
        );


        EntityTracker tracker =
                getTracker(
                        ravager
                );

        if (tracker == null) {
            return;
        }


        /*
         * 이동 loop가 공격 애니메이션과 겹치지 않도록
         * 먼저 pursue를 중지한다.
         */
        tracker.stopAnimation(
                ANIMATION_PURSUE
        );

        pursuing.remove(
                uuid
        );


        if (attackCount == 3) {

            tracker.animate(
                    ANIMATION_HEAVY_CLEAVE
            );

            attackAnimationUntil.put(
                    uuid,
                    System.currentTimeMillis()
                            + HEAVY_CLEAVE_DURATION_MS
            );

            return;
        }


        tracker.animate(
                ANIMATION_SWEEP
        );

        attackAnimationUntil.put(
                uuid,
                System.currentTimeMillis()
                        + SWEEP_DURATION_MS
        );
    }


    /*
     * =========================================================
     * PURSUE
     * =========================================================
     */

    private void startPursueIfNecessary(
            Ravager ravager
    ) {

        UUID uuid =
                ravager.getUniqueId();

        if (
                pursuing.getOrDefault(
                        uuid,
                        false
                )
        ) {
            return;
        }

        EntityTracker tracker =
                getTracker(
                        ravager
                );

        if (tracker == null) {
            return;
        }

        if (
                tracker.animate(
                        ANIMATION_PURSUE
                )
        ) {

            pursuing.put(
                    uuid,
                    true
            );
        }
    }


    private void stopPursueIfNecessary(
            Ravager ravager
    ) {

        UUID uuid =
                ravager.getUniqueId();

        if (
                !pursuing.getOrDefault(
                        uuid,
                        false
                )
        ) {
            return;
        }

        EntityTracker tracker =
                getTracker(
                        ravager
                );

        if (tracker != null) {

            tracker.stopAnimation(
                    ANIMATION_PURSUE
            );
        }

        pursuing.remove(
                uuid
        );
    }


    /*
     * =========================================================
     * TRACKER
     * =========================================================
     */

    private EntityTracker getTracker(
            Ravager ravager
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
                        (Entity) ravager
                )
        );
    }


    /*
     * =========================================================
     * IDENTIFICATION
     * =========================================================
     */

    private boolean isWarbringer(
            Entity entity
    ) {

        String eliteId =
                entity.getPersistentDataContainer()
                        .get(
                                new org.bukkit.NamespacedKey(
                                        plugin,
                                        EliteDemonKnightListener
                                                .ELITE_MOB_KEY
                                ),
                                PersistentDataType.STRING
                        );

        return WarbringerSpawnService
                .WARBRINGER_ID
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

                pursuing.remove(
                        uuid
                );
            }
        }
    }
}
