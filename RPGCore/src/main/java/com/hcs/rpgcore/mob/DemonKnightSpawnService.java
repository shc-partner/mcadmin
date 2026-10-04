package com.hcs.rpgcore.mob;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class DemonKnightSpawnService
        implements Runnable {

    private static final String MODEL_ID =
            "demon_knight";

    /*
     * =========================================================
     * SPAWN SETTINGS
     * =========================================================
     *
     * Redstone Golem과 동일한 필드 엘리트 스폰 규칙을 사용한다.
     */

    private static final double SPAWN_CHANCE =
            0.10D;

    private static final double MIN_DISTANCE =
            50.0D;

    private static final double MAX_DISTANCE =
            90.0D;

    private static final double NEARBY_CHECK_RANGE =
            128.0D;

    private static final int MAX_NEARBY_DEMON_KNIGHTS =
            2;

    /*
     * 같은 128 x 128 지역에서
     * 5분 동안 추가 생성 방지.
     */
    private static final long REGION_COOLDOWN_MS =
            5L * 60L * 1000L;

    /*
     * 필드에서 10분 이상 살아 있는 데몬 나이트는
     * 주변 48블록에 플레이어가 없을 때 제거한다.
     */
    private static final long MAX_IDLE_LIFETIME_MS =
            10L * 60L * 1000L;

    private static final double DESPAWN_PLAYER_RANGE =
            48.0D;

    /*
     * 먼저 시야 밖 위치를 16회 탐색하고,
     * 실패하면 일반 유효 위치를 16회 탐색한다.
     */
    private static final int HIDDEN_ATTEMPTS =
            16;

    private static final int FALLBACK_ATTEMPTS =
            16;


    /*
     * =========================================================
     * COMBAT SETTINGS
     * =========================================================
     */

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


    private final Plugin plugin;

    private final NamespacedKey eliteMobKey;

    private final NamespacedKey spawnedAtKey;

    private final Map<String, Long> regionCooldown =
            new HashMap<>();


    public DemonKnightSpawnService(
            Plugin plugin
    ) {

        this.plugin =
                plugin;

        this.eliteMobKey =
                new NamespacedKey(
                        plugin,
                        EliteDemonKnightListener
                                .ELITE_MOB_KEY
                );

        this.spawnedAtKey =
                new NamespacedKey(
                        plugin,
                        "demon_knight_spawned_at"
                );
    }


    /*
     * =========================================================
     * SPAWN CHECK
     * =========================================================
     */

    @Override
    public void run() {

        cleanupCooldowns();

        cleanupExpiredDemonKnights();

        for (
                Player player
                : plugin.getServer()
                        .getOnlinePlayers()
        ) {

            trySpawnForPlayer(
                    player
            );
        }
    }


    private void trySpawnForPlayer(
            Player player
    ) {

        if (
                player == null
                        || !player.isOnline()
                        || player.isDead()
        ) {
            return;
        }

        /*
         * Spectator는 필드 스폰 기준에서 제외.
         *
         * Creative는 개발 테스트를 위해 허용한다.
         */
        if (
                player.getGameMode()
                        == GameMode.SPECTATOR
        ) {
            return;
        }

        World world =
                player.getWorld();

        /*
         * 오버월드에서만 생성한다.
         */
        if (
                world.getEnvironment()
                        != World.Environment.NORMAL
        ) {
            return;
        }

        /*
         * 플레이어 주변 128블록에
         * 데몬 나이트가 2마리 이상이면 생성하지 않는다.
         */
        if (
                countNearbyDemonKnights(
                        player
                )
                        >= MAX_NEARBY_DEMON_KNIGHTS
        ) {
            return;
        }

        /*
         * 스폰 체크를 통과한 플레이어마다
         * 10% 확률로 생성을 시도한다.
         */
        if (
                ThreadLocalRandom.current()
                        .nextDouble()
                        >= SPAWN_CHANCE
        ) {
            return;
        }

        /*
         * 먼저 플레이어에게 직접 보이지 않는 위치를 탐색한다.
         */
        Location spawnLocation =
                findSpawnLocation(
                        player,
                        true,
                        HIDDEN_ATTEMPTS
                );

        /*
         * 적절한 은폐 위치를 찾지 못하면
         * 일반 유효 위치를 다시 탐색한다.
         */
        if (spawnLocation == null) {

            spawnLocation =
                    findSpawnLocation(
                            player,
                            false,
                            FALLBACK_ATTEMPTS
                    );
        }

        if (spawnLocation == null) {
            return;
        }

        String regionKey =
                regionKey(
                        spawnLocation
                );

        Long lastSpawn =
                regionCooldown.get(
                        regionKey
                );

        long now =
                System.currentTimeMillis();

        if (
                lastSpawn != null
                        && now - lastSpawn
                        < REGION_COOLDOWN_MS
        ) {
            return;
        }


        WitherSkeleton demonKnight =
                world.spawn(
                        spawnLocation,
                        WitherSkeleton.class,
                        CreatureSpawnEvent
                                .SpawnReason
                                .CUSTOM
                );

        configure(
                demonKnight
        );

        if (
                !attachBetterModel(
                        demonKnight
                )
        ) {

            /*
             * BetterModel을 붙이지 못한 바닐라
             * Wither Skeleton은 필드에 남기지 않는다.
             */
            demonKnight.remove();

            return;
        }

        regionCooldown.put(
                regionKey,
                now
        );
    }


    /*
     * =========================================================
     * LOCATION SEARCH
     * =========================================================
     */

    private Location findSpawnLocation(
            Player player,
            boolean requireHidden,
            int attempts
    ) {

        World world =
                player.getWorld();

        Location playerLocation =
                player.getLocation();

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        for (
                int attempt = 0;
                attempt < attempts;
                attempt++
        ) {

            double radius =
                    random.nextDouble(
                            MIN_DISTANCE,
                            MAX_DISTANCE
                    );

            double angle =
                    random.nextDouble(
                            0.0D,
                            Math.PI * 2.0D
                    );

            int x =
                    (int) Math.floor(
                            playerLocation.getX()
                                    + Math.cos(angle)
                                    * radius
                    );

            int z =
                    (int) Math.floor(
                            playerLocation.getZ()
                                    + Math.sin(angle)
                                    * radius
                    );

            int chunkX =
                    x >> 4;

            int chunkZ =
                    z >> 4;

            /*
             * 스폰을 위해 새로운 청크를 강제로 로드하지 않는다.
             */
            if (
                    !world.isChunkLoaded(
                            chunkX,
                            chunkZ
                    )
            ) {
                continue;
            }

            int groundY =
                    world.getHighestBlockYAt(
                            x,
                            z,
                            HeightMap
                                    .MOTION_BLOCKING_NO_LEAVES
                    );

            int spawnY =
                    groundY + 1;

            Location candidate =
                    new Location(
                            world,
                            x + 0.5D,
                            spawnY,
                            z + 0.5D
                    );

            if (
                    !isValidSurface(
                            candidate
                    )
            ) {
                continue;
            }

            if (
                    regionOnCooldown(
                            candidate
                    )
            ) {
                continue;
            }

            if (
                    requireHidden
                            && isDirectlyVisible(
                                    player,
                                    candidate
                            )
            ) {
                continue;
            }

            return candidate;
        }

        return null;
    }


    /*
     * =========================================================
     * SURFACE VALIDATION
     * =========================================================
     */

    private boolean isValidSurface(
            Location location
    ) {

        World world =
                location.getWorld();

        if (world == null) {
            return false;
        }

        int x =
                location.getBlockX();

        int y =
                location.getBlockY();

        int z =
                location.getBlockZ();

        Block ground =
                world.getBlockAt(
                        x,
                        y - 1,
                        z
                );

        Block feet =
                world.getBlockAt(
                        x,
                        y,
                        z
                );

        Block body =
                world.getBlockAt(
                        x,
                        y + 1,
                        z
                );

        Block head =
                world.getBlockAt(
                        x,
                        y + 2,
                        z
                );

        Material groundType =
                ground.getType();

        if (!groundType.isSolid()) {
            return false;
        }

        if (
                groundType == Material.WATER
                        || groundType == Material.LAVA
                        || groundType == Material.MAGMA_BLOCK
        ) {
            return false;
        }

        /*
         * Wither Skeleton 및 BetterModel이 사용할 공간.
         */
        if (
                !feet.isPassable()
                        || !body.isPassable()
                        || !head.isPassable()
        ) {
            return false;
        }

        if (
                feet.isLiquid()
                        || body.isLiquid()
                        || head.isLiquid()
        ) {
            return false;
        }

        return true;
    }


    /*
     * =========================================================
     * PLAYER VISIBILITY
     * =========================================================
     */

    private boolean isDirectlyVisible(
            Player player,
            Location target
    ) {

        Location eye =
                player.getEyeLocation();

        Location targetCenter =
                target.clone()
                        .add(
                                0.0D,
                                1.2D,
                                0.0D
                        );

        Vector direction =
                targetCenter.toVector()
                        .subtract(
                                eye.toVector()
                        );

        double distance =
                direction.length();

        if (distance <= 0.0D) {
            return true;
        }

        direction.normalize();

        return worldRayIsClear(
                eye,
                direction,
                distance
        );
    }


    private boolean worldRayIsClear(
            Location start,
            Vector direction,
            double distance
    ) {

        World world =
                start.getWorld();

        if (world == null) {
            return false;
        }

        return world.rayTraceBlocks(
                        start,
                        direction,
                        distance,
                        FluidCollisionMode.NEVER,
                        true
                )
                == null;
    }


    /*
     * =========================================================
     * CONFIGURE ENTITY
     * =========================================================
     */

    private void configure(
            WitherSkeleton entity
    ) {

        entity.getPersistentDataContainer()
                .set(
                        eliteMobKey,
                        PersistentDataType.STRING,
                        EliteDemonKnightListener
                                .DEMON_KNIGHT_ID
                );

        entity.getPersistentDataContainer()
                .set(
                        spawnedAtKey,
                        PersistentDataType.LONG,
                        System.currentTimeMillis()
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
         * 다른 필드 엘리트와 동일하게
         * 거리 기반 자연 디스폰을 허용한다.
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

    private boolean attachBetterModel(
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

            return false;
        }

        renderer.getOrCreate(
                BukkitAdapter.adapt(
                        (Entity) entity
                )
        );

        return true;
    }


    /*
     * =========================================================
     * NEARBY LIMIT
     * =========================================================
     */

    private int countNearbyDemonKnights(
            Player player
    ) {

        int count =
                0;

        for (
                Entity entity
                : player.getNearbyEntities(
                        NEARBY_CHECK_RANGE,
                        NEARBY_CHECK_RANGE,
                        NEARBY_CHECK_RANGE
                )
        ) {

            if (
                    !(entity
                            instanceof WitherSkeleton)
            ) {
                continue;
            }

            String eliteId =
                    entity.getPersistentDataContainer()
                            .get(
                                    eliteMobKey,
                                    PersistentDataType.STRING
                            );

            if (
                    EliteDemonKnightListener
                            .DEMON_KNIGHT_ID
                            .equals(
                                    eliteId
                            )
            ) {

                count++;

                if (
                        count
                                >= MAX_NEARBY_DEMON_KNIGHTS
                ) {
                    return count;
                }
            }
        }

        return count;
    }


    /*
     * =========================================================
     * EXPIRED ELITE CLEANUP
     * =========================================================
     */
    private void cleanupExpiredDemonKnights() {

        long now =
                System.currentTimeMillis();

        double playerRangeSquared =
                DESPAWN_PLAYER_RANGE
                        * DESPAWN_PLAYER_RANGE;


        for (
                World world
                : plugin.getServer()
                        .getWorlds()
        ) {

            for (
                    WitherSkeleton entity
                    : world.getEntitiesByClass(
                            WitherSkeleton.class
                    )
            ) {

                String eliteId =
                        entity.getPersistentDataContainer()
                                .get(
                                        eliteMobKey,
                                        PersistentDataType.STRING
                                );

                if (
                        !EliteDemonKnightListener
                                .DEMON_KNIGHT_ID
                                .equals(
                                        eliteId
                                )
                ) {
                    continue;
                }


                Long spawnedAt =
                        entity.getPersistentDataContainer()
                                .get(
                                        spawnedAtKey,
                                        PersistentDataType.LONG
                                );

                if (spawnedAt == null) {

                    entity.getPersistentDataContainer()
                            .set(
                                    spawnedAtKey,
                                    PersistentDataType.LONG,
                                    now
                            );

                    continue;
                }


                if (
                        now - spawnedAt
                                < MAX_IDLE_LIFETIME_MS
                ) {
                    continue;
                }


                boolean playerNearby =
                        false;

                for (
                        Player player
                        : world.getPlayers()
                ) {

                    if (
                            player.isDead()
                                    || player.getGameMode()
                                    == GameMode.SPECTATOR
                    ) {
                        continue;
                    }

                    if (
                            player.getLocation()
                                    .distanceSquared(
                                            entity.getLocation()
                                    )
                                    <= playerRangeSquared
                    ) {

                        playerNearby =
                                true;

                        break;
                    }
                }


                if (!playerNearby) {

                    entity.remove();
                }
            }
        }
    }


    /*
     * =========================================================
     * REGION COOLDOWN
     * =========================================================
     */

    private boolean regionOnCooldown(
            Location location
    ) {

        Long lastSpawn =
                regionCooldown.get(
                        regionKey(
                                location
                        )
                );

        if (lastSpawn == null) {
            return false;
        }

        return System.currentTimeMillis()
                - lastSpawn
                < REGION_COOLDOWN_MS;
    }


    private String regionKey(
            Location location
    ) {

        int regionX =
                Math.floorDiv(
                        location.getBlockX(),
                        128
                );

        int regionZ =
                Math.floorDiv(
                        location.getBlockZ(),
                        128
                );

        return location.getWorld()
                        .getUID()
                        .toString()
                + ":"
                + regionX
                + ":"
                + regionZ;
    }


    private void cleanupCooldowns() {

        long cutoff =
                System.currentTimeMillis()
                        - REGION_COOLDOWN_MS;

        regionCooldown.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        < cutoff
                );
    }


    /*
     * =========================================================
     * ATTRIBUTE
     * =========================================================
     */

    private static void setAttribute(
            WitherSkeleton entity,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                entity.getAttribute(
                        attribute
                );

        if (instance != null) {

            instance.setBaseValue(
                    value
            );
        }
    }
}
