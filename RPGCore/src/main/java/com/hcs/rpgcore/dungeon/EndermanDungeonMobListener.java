package com.hcs.rpgcore.dungeon;

import io.papermc.paper.event.entity.EntityMoveEvent;

import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public final class EndermanDungeonMobListener
        implements Listener {

    private static final double MAX_VANILLA_HEALTH =
            1024.0D;


    /*
     * =========================================================
     * NATURAL SPAWN BLOCK
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onNaturalSpawn(
            CreatureSpawnEvent event
    ) {

        if (
                event.getSpawnReason()
                        != CreatureSpawnEvent.SpawnReason.NATURAL
        ) {
            return;
        }

        if (
                !dungeonService.isInsideBoundary(
                        event.getLocation()
                )
        ) {
            return;
        }

        event.setCancelled(true);
    }

    /*
     * =========================================================
     * DUNGEON MOVEMENT BOUNDARY
     * =========================================================
     *
     * 몬스터 전투 영역:
     *
     * X block -10000370 ~ -10000353
     * Z block -9999931 ~ -9999914
     *
     * 플레이어 이동에는 적용하지 않는다.
     */
    private static final double BOUNDARY_MIN_X =
            -10000370.0D;

    private static final double BOUNDARY_MAX_X =
            -10000352.0D;

    private static final double BOUNDARY_MIN_Z =
            -9999931.0D;

    private static final double BOUNDARY_MAX_Z =
            -9999913.0D;

    private final EndermanDungeonService dungeonService;


    public EndermanDungeonMobListener(
            EndermanDungeonService dungeonService
    ) {

        this.dungeonService =
                dungeonService;
    }


    /*
     * =========================================================
     * DEATH
     * =========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onEntityDeath(
            EntityDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        if (!dungeonService.isDungeonMob(entity)) {
            return;
        }

        /*
         * 던전 전용 몬스터는 바닐라 드롭을 남기지 않는다.
         */
        event.getDrops().clear();

        /*
         * 바닐라 녹색 EXP Orb 제거.
         *
         * RPGCore 자체 EXP는 여기에서 건드리지 않는다.
         */
        event.setDroppedExp(0);

        dungeonService.onDungeonMobDeath(
                entity
        );
    }


    /*
     * =========================================================
     * DUNGEON FORCED HOSTILITY
     * =========================================================
     *
     * 던전 Enderman / Boss는
     * activeParticipant만 공격한다.
     *
     * 타겟을 잃으면 다음 tick에
     * 가장 가까운 참가자를 다시 지정한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onDungeonEndermanTarget(
            EntityTargetLivingEntityEvent event
    ) {

        if (
                !(event.getEntity()
                        instanceof Enderman enderman)
        ) {
            return;
        }

        if (!dungeonService
                .isDungeonMob(enderman)) {

            return;
        }


        /*
         * 정상적인 activeParticipant 타겟이면
         * 그대로 허용한다.
         */
        if (
                event.getTarget()
                        instanceof Player player
                && dungeonService
                        .isActiveParticipant(player)
        ) {
            return;
        }


        /*
         * 던전 외부 플레이어나
         * 다른 LivingEntity를 공격하지 못하게 한다.
         */
        if (event.getTarget() != null) {

            event.setCancelled(true);
        }


        /*
         * 타겟 소실 / 사망 / 이탈 시
         * 다음 tick에 가장 가까운 참가자로 재지정한다.
         */
        dungeonService
                .scheduleDungeonRetarget(
                        enderman
                );
    }


    /*
     * =========================================================
     * MOVEMENT BOUNDARY
     * =========================================================
     *
     * 던전 엔더맨이 전투 영역 밖으로
     * 걸어서 나가는 것 자체를 차단한다.
     *
     * 일반 월드 엔더맨에는 영향을 주지 않는다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityMove(
            EntityMoveEvent event
    ) {

        if (!(event.getEntity()
                instanceof Enderman enderman)) {

            return;
        }


        if (!dungeonService
                .isDungeonMob(enderman)) {

            return;
        }


        /*
         * 방향만 회전한 경우에는 처리하지 않는다.
         */
        if (!event.hasChangedPosition()) {
            return;
        }


        double x =
                event.getTo().getX();

        double z =
                event.getTo().getZ();


        boolean inside =
                x >= BOUNDARY_MIN_X
                        && x < BOUNDARY_MAX_X
                        && z >= BOUNDARY_MIN_Z
                        && z < BOUNDARY_MAX_Z;


        if (inside) {
            return;
        }


        /*
         * 경계를 넘어가려는 이동 자체를 취소한다.
         *
         * 따라서 몹이 밖으로 나간 뒤 되돌아오는 것이 아니라
         * 애초에 경계선을 통과하지 못한다.
         */
        event.setCancelled(
                true
        );
    }


    /*
     * =========================================================
     * TELEPORT BLOCK
     * =========================================================
     *
     * 일반 월드 엔더맨에는 영향을 주지 않는다.
     *
     * EndermanDungeonService의 PDC가 붙은
     * 엔더맨만 순간이동을 차단한다.
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityTeleport(
            EntityTeleportEvent event
    ) {

        if (!(event.getEntity()
                instanceof Enderman enderman)) {

            return;
        }

        if (!dungeonService
                .isDungeonMob(enderman)) {

            return;
        }

        event.setCancelled(true);
    }


    /*
     * =========================================================
     * BLOCK PICKUP / PLACE BLOCK
     * =========================================================
     *
     * 엔더맨이 던전 건축물을 뜯거나
     * 다른 곳에 블록을 배치하지 못하게 한다.
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityChangeBlock(
            EntityChangeBlockEvent event
    ) {

        if (!(event.getEntity()
                instanceof Enderman enderman)) {

            return;
        }

        if (!dungeonService
                .isDungeonMob(enderman)) {

            return;
        }

        event.setCancelled(true);
    }


    /*
     * =========================================================
     * EFFECTIVE HEALTH
     * =========================================================
     *
     * Paper Entity 실제 HP 최대값 대응.
     *
     * 예:
     *
     * Wave 1
     * RPG HP 900
     * 실제 HP 900
     * 피해 보정 없음
     *
     * Wave 2
     * RPG HP 1100
     * 실제 HP 1024
     * 피해 x (1024 / 1100)
     *
     * Wave 3
     * RPG HP 1350
     * 실제 HP 1024
     * 피해 x (1024 / 1350)
     *
     * Boss
     * RPG HP 30000
     * 실제 HP 1024
     * 피해 x (1024 / 30000)
     */

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onDungeonMobDamage(
            EntityDamageEvent event
    ) {

        if (!(event.getEntity()
                instanceof LivingEntity entity)) {

            return;
        }

        if (!dungeonService
                .isDungeonMob(entity)) {

            return;
        }

        double effectiveHealth =
                dungeonService
                        .getEffectiveHealth(entity);

        if (effectiveHealth <= 0.0D) {
            return;
        }

        /*
         * RPG HP가 1024 이하라면
         * 실제 HP와 RPG HP가 동일하므로
         * 피해 보정이 필요 없다.
         */
        if (effectiveHealth
                <= MAX_VANILLA_HEALTH) {

            return;
        }

        double damageScale =
                MAX_VANILLA_HEALTH
                        / effectiveHealth;

        event.setDamage(
                event.getDamage()
                        * damageScale
        );
    }
}
