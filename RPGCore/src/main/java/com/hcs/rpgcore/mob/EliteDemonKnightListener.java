package com.hcs.rpgcore.mob;


import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public final class EliteDemonKnightListener
        implements Listener {

    public static final String ELITE_MOB_KEY =
            "elite_mob";

    public static final String DEMON_KNIGHT_ID =
            "demon_knight";

    private static final double MAX_HEALTH =
            500.0D;

    private static final double ATTACK_DAMAGE =
            30.0D;

    private static final double ARMOR =
            15.0D;

    private static final double MOVEMENT_MULTIPLIER =
            1.10D;

    private static final double KNOCKBACK_RESISTANCE =
            0.50D;

    private static final String MODEL_ID =
            "demon_knight";

    private final Plugin plugin;
    private final NamespacedKey eliteMobKey;

    public EliteDemonKnightListener(
            Plugin plugin
    ) {

        this.plugin = plugin;

        this.eliteMobKey =
                new NamespacedKey(
                        plugin,
                        ELITE_MOB_KEY
                );
    }

    /*
     * =========================================================
     * ADMIN TEST SPAWN
     * =========================================================
     *
     * /rpgadmin elite spawn demon_knight 전용.
     *
     * 필드 스폰 확률, 거리, 지역 쿨다운은 적용하지 않고,
     * 실제 데몬 나이트와 동일한 설정과 BetterModel을 적용한다.
     */
    public void spawnForAdmin(
            Location location
    ) {

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        WitherSkeleton demonKnight =
                world.spawn(
                        location,
                        WitherSkeleton.class,
                        CreatureSpawnEvent
                                .SpawnReason
                                .CUSTOM
                );

        configureDemonKnight(
                demonKnight
        );

        attachBetterModel(
                demonKnight
        );
    }

    /*
     * =========================================================
     * CONFIGURE ELITE
     * =========================================================
     */
    private void configureDemonKnight(
            WitherSkeleton entity
    ) {

        /*
         * PDC 식별자.
         *
         * EXP / 드롭 / 특수 로직에서
         * 일반 Wither Skeleton과 구분한다.
         */
        entity.getPersistentDataContainer()
                .set(
                        eliteMobKey,
                        PersistentDataType.STRING,
                        DEMON_KNIGHT_ID
                );

        entity.customName(
                Component.text(
                        "[엘리트] 데몬 나이트",
                        NamedTextColor.DARK_RED
                )
        );

        entity.setCustomNameVisible(
                true
        );

        setAttribute(
                entity,
                Attribute.MAX_HEALTH,
                MAX_HEALTH
        );

        entity.setHealth(
                MAX_HEALTH
        );

        setAttribute(
                entity,
                Attribute.ATTACK_DAMAGE,
                ATTACK_DAMAGE
        );

        setAttribute(
                entity,
                Attribute.ARMOR,
                ARMOR
        );

        setAttribute(
                entity,
                Attribute.KNOCKBACK_RESISTANCE,
                KNOCKBACK_RESISTANCE
        );

        AttributeInstance movement =
                entity.getAttribute(
                        Attribute.MOVEMENT_SPEED
                );

        if (movement != null) {

            movement.setBaseValue(
                    movement.getBaseValue()
                            * MOVEMENT_MULTIPLIER
            );
        }

        /*
         * 필드 엘리트가 장시간 월드에 누적되지 않도록
         * 플레이어와 충분히 멀어지면 바닐라 거리 디스폰을 허용한다.
         *
         * 영구 보존 대상으로 지정하지 않아
         * 장기적인 엔티티 누적 위험을 줄인다.
         */
        entity.setRemoveWhenFarAway(
                true
        );

        entity.setPersistent(
                false
        );
    }

    /*
     * =========================================================
     * BETTERMODEL
     * =========================================================
     */
    private void attachBetterModel(
            WitherSkeleton entity
    ) {

        ModelRenderer renderer =
                BetterModel.modelOrNull(
                        MODEL_ID
                );

        if (renderer == null) {

            plugin.getLogger()
                    .warning(
                            "BetterModel model not found: "
                                    + MODEL_ID
                    );

            return;
        }

        renderer.getOrCreate(
                BukkitAdapter.adapt(
                        (Entity) entity
                )
        );
    }

    /*
     * =========================================================
     * PREVENT DEMON KNIGHT COMBUSTION
     * =========================================================
     *
     * 데몬 나이트가 낮의 햇빛 등으로 연소 상태가 되는 것을 막는다.
     *
     * 일반 엔티티의 연소에는 영향을 주지 않는다.
     */
    @EventHandler(
            priority = EventPriority.NORMAL,
            ignoreCancelled = true
    )
    public void onDemonKnightCombust(
            EntityCombustEvent event
    ) {

        if (!isDemonKnight(event.getEntity())) {
            return;
        }

        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
    }

    /*
     * =========================================================
     * REMOVE WITHER DEBUFF
     * =========================================================
     *
     * Wither Skeleton의 근접 AI는 유지하되,
     * 공격으로 발생하는 WITHER 효과는 제거한다.
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

        if (
                !(event.getEntity()
                        instanceof LivingEntity target)
        ) {
            return;
        }

        /*
         * 바닐라 Wither Skeleton이 피해 처리 과정에서
         * WITHER를 부여한 직후 제거한다.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () ->
                                target.removePotionEffect(
                                        PotionEffectType.WITHER
                                )
                );
    }

    /*
     * =========================================================
     * DEMON KNIGHT VANILLA DROP
     * =========================================================
     *
     * 엘리트 데몬 나이트의 바닐라 Wither Skeleton 드롭은 제거한다.
     * RPG 전용 보상 시스템에서 필요한 보상을 별도로 처리한다.
     */
    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onDemonKnightDeath(
            EntityDeathEvent event
    ) {

        if (!isDemonKnight(event.getEntity())) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    /*
     * =========================================================
     * IDENTIFICATION
     * =========================================================
     */
    public boolean isDemonKnight(
            Entity entity
    ) {

        String value =
                entity.getPersistentDataContainer()
                        .get(
                                eliteMobKey,
                                PersistentDataType.STRING
                        );

        return DEMON_KNIGHT_ID.equals(
                value
        );
    }

    private static void setAttribute(
            LivingEntity entity,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                entity.getAttribute(
                        attribute
                );

        if (instance == null) {
            return;
        }

        instance.setBaseValue(
                value
        );
    }
}
