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
import org.bukkit.entity.Ravager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;


/*
 * ============================================================
 * REDSTONE GOLEM ANIMATION
 * ============================================================
 *
 * 정지:
 * - redstone_golem_idle_animation_1
 *
 * 이동:
 * - redstone_golem_walk_animation_1
 *
 * 두 애니메이션만 사용한다.
 */
public final class RedstoneGolemAnimationTask
        implements Runnable {

    private static final String MODEL_ID =
            "redstone_golem";

    private static final String ANIMATION_IDLE =
            "redstone_golem_idle_animation_1";

    private static final String ANIMATION_WALK =
            "redstone_golem_walk_animation_1";

    /*
     * 아주 작은 물리 흔들림은 이동으로 판단하지 않는다.
     */
    private static final double MOVEMENT_THRESHOLD_SQUARED =
            0.0004D;

    private final Plugin plugin;
    private final NamespacedKey eliteMobKey;

    /*
     * true  = walk
     * false = idle
     */
    private final Map<UUID, Boolean> movementState =
            new HashMap<>();


    public RedstoneGolemAnimationTask(
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


    @Override
    public void run() {

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
                    Ravager ravager
                    : world.getEntitiesByClass(
                            Ravager.class
                    )
            ) {

                if (!isRedstoneGolem(ravager)) {
                    continue;
                }

                updateAnimation(
                        renderer,
                        ravager
                );
            }
        }

        cleanupRemovedEntities();
    }


    /*
     * =========================================================
     * STATE UPDATE
     * =========================================================
     */

    private void updateAnimation(
            ModelRenderer renderer,
            Ravager ravager
    ) {

        UUID uuid =
                ravager.getUniqueId();

        boolean moving =
                ravager.getVelocity()
                        .lengthSquared()
                        > MOVEMENT_THRESHOLD_SQUARED;

        Boolean previousState =
                movementState.get(
                        uuid
                );

        /*
         * 상태가 변하지 않았다면 같은 loop 애니메이션을
         * 반복해서 다시 요청하지 않는다.
         */
        if (
                previousState != null
                        && previousState == moving
        ) {
            return;
        }


        EntityTracker tracker =
                renderer.getOrCreate(
                        BukkitAdapter.adapt(
                                (Entity) ravager
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

            return;
        }


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


    /*
     * =========================================================
     * IDENTIFICATION
     * =========================================================
     */

    private boolean isRedstoneGolem(
            Entity entity
    ) {

        String eliteId =
                entity.getPersistentDataContainer()
                        .get(
                                eliteMobKey,
                                PersistentDataType.STRING
                        );

        return "redstone_golem".equals(
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
            }
        }
    }
}
