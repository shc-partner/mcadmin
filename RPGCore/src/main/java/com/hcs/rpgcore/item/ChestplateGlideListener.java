package com.hcs.rpgcore.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.hcs.rpgcore.RPGCorePlugin;


/*
 * =========================================================
 * GODFORGE DIVINE CHESTPLATE GLIDE
 * =========================================================
 *
 * Godforge Divine Chestplate를 착용한 플레이어에게
 * 엘리트라형 활공 입력을 제공한다.
 *
 * 생존 / 모험 모드:
 *
 *   점프
 *     ->
 *   공중에서 더블 점프
 *     ->
 *   활공 시작
 *
 * 실제 CHEST 슬롯의 아이템은 교체하지 않는다.
 */
public final class ChestplateGlideListener
        implements Listener, Runnable {


    private final RPGCorePlugin plugin;

    private final NamespacedKey abilityGlideKey;


    /*
     * =========================================================
     * GLIDE BOOST
     * =========================================================
     */
    private static final long BOOST_COOLDOWN_MILLIS =
            500L;

    private static final double BOOST_POWER =
            1.80D;

    private static final double MAX_BOOST_SPEED =
            3.50D;


    private final Map<UUID, Long>
            lastBoostTimes =
                    new HashMap<>();


    /*
     * 이 Listener가 allowFlight를 제어하기 시작하기 전의
     * 원래 상태를 저장한다.
     */
    private final Map<UUID, Boolean>
            previousAllowFlight =
                    new HashMap<>();


    /*
     * 현재 Godforge 흉갑 기능으로
     * 활공 중인 플레이어.
     */
    private final Set<UUID>
            activeGliders =
                    new HashSet<>();


    public ChestplateGlideListener(
            RPGCorePlugin plugin
    ) {

        this.plugin = plugin;

        this.abilityGlideKey =
                new NamespacedKey(
                        plugin,
                        "ability_glide"
                );
    }


    /*
     * =========================================================
     * FLIGHT INPUT
     * =========================================================
     *
     * allowFlight=true 상태에서는
     * 클라이언트의 더블 점프 입력이
     * PlayerToggleFlightEvent로 전달된다.
     *
     * 실제 바닐라 비행은 취소하고
     * Godforge 활공으로 전환한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onToggleFlight(
            PlayerToggleFlightEvent event
    ) {

        Player player =
                event.getPlayer();


        if (!isEligible(player)) {
            return;
        }


        UUID uuid =
                player.getUniqueId();


        if (!previousAllowFlight.containsKey(uuid)) {
            return;
        }


        /*
         * 바닐라 creative-style 비행으로
         * 전환되는 것을 막는다.
         */
        event.setCancelled(true);

        player.setFlying(false);


        /*
         * 땅에서는 활공을 시작하지 않는다.
         */
        if (player.isOnGround()) {
            return;
        }


        activeGliders.add(uuid);


        /*
         * 활공 중에는 다시 flight toggle이
         * 발생하지 않도록 임시 비활성화한다.
         */
        player.setAllowFlight(false);


        if (!player.isGliding()) {
            player.setGliding(true);
        }
    }


    /*
     * =========================================================
     * GLIDE BOOST
     * =========================================================
     *
     * 활공 중 우클릭하면 폭죽 없이 전방으로 추진한다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onGlideBoost(
            PlayerInteractEvent event
    ) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action =
                event.getAction();

        if (
                action != Action.RIGHT_CLICK_AIR
                        &&
                action != Action.RIGHT_CLICK_BLOCK
        ) {
            return;
        }

        Player player =
                event.getPlayer();

        /*
         * 글라이드 기능이 있는 RPGCore 흉갑을 착용하고
         * 실제 활공 중인 경우 모든 우클릭을
         * 폭죽형 추진 입력으로 사용한다.
         *
         * 손에 든 아이템 종류와 무관하다.
         */
        if (
                !isEligible(player)
                        ||
                player.isOnGround()
                        ||
                !player.isGliding()
        ) {
            return;
        }

        /*
         * 활공 중에는 아이템 사용 / 블록 상호작용 /
         * 실제 폭죽 사용 대신 추진 기능을 사용한다.
         */
        event.setCancelled(
                true
        );

        UUID uuid =
                player.getUniqueId();

        long now =
                System.currentTimeMillis();

        long previous =
                lastBoostTimes.getOrDefault(
                        uuid,
                        0L
                );

        if (
                now - previous
                        < BOOST_COOLDOWN_MILLIS
        ) {
            return;
        }

        lastBoostTimes.put(
                uuid,
                now
        );


        Vector direction =
                player.getEyeLocation()
                        .getDirection()
                        .normalize();

        Vector velocity =
                player.getVelocity()
                        .multiply(0.60D)
                        .add(
                                direction.multiply(
                                        BOOST_POWER
                                )
                        );

        if (
                velocity.lengthSquared()
                        > MAX_BOOST_SPEED
                        * MAX_BOOST_SPEED
        ) {

            velocity.normalize()
                    .multiply(
                            MAX_BOOST_SPEED
                    );
        }

        player.setVelocity(
                velocity
        );

        player.getWorld()
                .playSound(
                        player.getLocation(),
                        Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                        0.8F,
                        1.15F
                );
    }


    /*
     * =========================================================
     * PREVENT FORCED GLIDE STOP
     * =========================================================
     *
     * 실제 엘리트라를 착용하지 않았기 때문에
     * 서버가 활공 상태를 false로 돌리려고 할 수 있다.
     *
     * Godforge 활공 조건을 계속 만족하고 있고
     * 아직 공중이라면 glide 종료를 막는다.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onToggleGlide(
            EntityToggleGlideEvent event
    ) {

        Entity entity =
                event.getEntity();


        if (!(entity instanceof Player player)) {
            return;
        }


        if (event.isGliding()) {
            return;
        }


        UUID uuid =
                player.getUniqueId();


        if (!activeGliders.contains(uuid)) {
            return;
        }


        if (
                isEligible(player)
                && !player.isOnGround()
        ) {

            event.setCancelled(true);
        }
    }


    /*
     * =========================================================
     * RUNTIME
     * =========================================================
     */
    @Override
    public void run() {

        for (
                Player player
                : plugin.getServer().getOnlinePlayers()
        ) {

            updatePlayer(player);
        }
    }


    private void updatePlayer(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        /*
         * Godforge 흉갑을 벗었거나
         * 지원하지 않는 게임 모드가 되면
         * 즉시 기능을 해제한다.
         */
        if (!isEligible(player)) {

            stopManaging(player);

            return;
        }


        /*
         * 최초 관리 시작 시점의
         * allowFlight 상태를 보존한다.
         */
        previousAllowFlight.computeIfAbsent(
                uuid,
                ignored ->
                        player.getAllowFlight()
        );


        /*
         * 착지하면 활공 종료.
         *
         * 그 후 다시 더블 점프 입력을 받을 수 있도록
         * allowFlight를 활성화한다.
         */
        if (player.isOnGround()) {

            if (activeGliders.remove(uuid)) {

                if (player.isGliding()) {
                    player.setGliding(false);
                }
            }


            player.setFlying(false);
            player.setAllowFlight(true);

            return;
        }


        /*
         * Godforge 활공 진행 중.
         */
        if (activeGliders.contains(uuid)) {

            player.setFlying(false);
            player.setAllowFlight(false);


            /*
             * 실제 엘리트라가 없어서 서버 측에서
             * 상태가 풀리는 경우에도 다시 유지한다.
             */
            if (!player.isGliding()) {
                player.setGliding(true);
            }

            return;
        }


        /*
         * 공중이지만 아직 활공을 시작하지 않은 상태.
         *
         * 더블 점프 입력을 받을 수 있게 한다.
         */
        player.setFlying(false);
        player.setAllowFlight(true);
    }


    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */
    private void stopManaging(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        boolean wasActive =
                activeGliders.remove(uuid);


        if (
                wasActive
                && player.isGliding()
        ) {

            player.setGliding(false);
        }


        Boolean previous =
                previousAllowFlight.remove(uuid);


        if (previous == null) {
            return;
        }


        /*
         * Creative / Spectator의 기본 비행 권한은
         * Minecraft가 관리하도록 건드리지 않는다.
         */
        GameMode mode =
                player.getGameMode();

        if (
                mode == GameMode.CREATIVE
                || mode == GameMode.SPECTATOR
        ) {
            return;
        }


        player.setFlying(false);
        player.setAllowFlight(previous);
    }


    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        UUID uuid =
                player.getUniqueId();

        activeGliders.remove(uuid);
        previousAllowFlight.remove(uuid);
        lastBoostTimes.remove(uuid);
    }


    /*
     * =========================================================
     * ELIGIBILITY
     * =========================================================
     */
    private boolean isEligible(
            Player player
    ) {

        GameMode mode =
                player.getGameMode();


        if (
                mode != GameMode.SURVIVAL
                && mode != GameMode.ADVENTURE
        ) {

            return false;
        }


        return canGlide(
                player.getInventory()
                        .getChestplate()
        );
    }


    /*
     * =========================================================
     * CUSTOM ITEM CHECK
     * =========================================================
     */
    private boolean canGlide(
            ItemStack item
    ) {

        if (
                item == null
                || item.getType().isAir()
        ) {

            return false;
        }


        ItemMeta meta =
                item.getItemMeta();


        if (meta == null) {
            return false;
        }


        Byte glide =
                meta.getPersistentDataContainer()
                        .get(
                                abilityGlideKey,
                                PersistentDataType.BYTE
                        );


        return glide != null
                && glide == (byte) 1;
    }
}
