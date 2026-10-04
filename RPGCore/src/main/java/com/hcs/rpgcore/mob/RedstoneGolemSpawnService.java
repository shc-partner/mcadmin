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
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class RedstoneGolemSpawnService
        implements Runnable {

    public static final String REDSTONE_GOLEM_ID =
            "redstone_golem";

    private static final String MODEL_ID =
            "redstone_golem";

    /*
     * =========================================================
     * SPAWN SETTINGS
     * =========================================================
     */

    /*
     * 운영 스폰 확률 10%.
     *
     * 스폰 체크 조건을 모두 통과한 플레이어마다
     * 10% 확률로 레드스톤 골렘 생성을 시도한다.
     */
    private static final double SPAWN_CHANCE =
            0.10D;

    private static final double MIN_DISTANCE =
            60.0D;

    private static final double MAX_DISTANCE =
            100.0D;

    private static final double NEARBY_CHECK_RANGE =
            128.0D;

    private static final int MAX_NEARBY_GOLEMS =
            2;

    /*
     * 같은 128 x 128 지역에서
     * 5분 동안 추가 생성 방지.
     */
    private static final long REGION_COOLDOWN_MS =
            5L * 60L * 1000L;

    /*
     * 필드에서 12분 이상 살아 있는 레드스톤 골렘은
     * 주변 48블록에 플레이어가 없을 때 제거한다.
     */
    private static final long MAX_IDLE_LIFETIME_MS =
            12L * 60L * 1000L;

    private static final double DESPAWN_PLAYER_RANGE =
            48.0D;

    /*
     * 시야 밖 위치를 먼저 16회 탐색하고,
     * 실패하면 일반 유효 위치를 16회 더 탐색한다.
     */
    private static final int HIDDEN_ATTEMPTS =
            16;

    private static final int FALLBACK_ATTEMPTS =
            16;

    private final Plugin plugin;
    private final NamespacedKey eliteMobKey;
    private final NamespacedKey spawnedAtKey;

    private final Map<String, Long> regionCooldown =
            new HashMap<>();

    public RedstoneGolemSpawnService(
            Plugin plugin
    ) {

        this.plugin = plugin;

        this.eliteMobKey =
                new NamespacedKey(
                        plugin,
                        EliteDemonKnightListener
                                .ELITE_MOB_KEY
                );

        this.spawnedAtKey =
                new NamespacedKey(
                        plugin,
                        "redstone_golem_spawned_at"
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

        cleanupExpiredRedstoneGolems();

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
         * 오버월드만.
         */
        if (
                world.getEnvironment()
                        != World.Environment.NORMAL
        ) {
            return;
        }

        if (
                countNearbyRedstoneGolems(
                        player
                )
                        >= MAX_NEARBY_GOLEMS
        ) {
            return;
        }

        /*
         * 운영 기준 10% 확률 판정.
         */
        if (
                ThreadLocalRandom.current()
                        .nextDouble()
                        >= SPAWN_CHANCE
        ) {
            return;
        }

        /*
         * 우선 플레이어 시야에서 가려진 위치를 찾는다.
         */
        Location spawnLocation =
                findSpawnLocation(
                        player,
                        true,
                        HIDDEN_ATTEMPTS
                );

        /*
         * 산/숲 등이 없는 넓은 평원에서도
         * 아예 스폰되지 않는 문제를 방지하기 위한 fallback.
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

        Ravager ravager =
                world.spawn(
                        spawnLocation,
                        Ravager.class,
                        CreatureSpawnEvent
                                .SpawnReason
                                .CUSTOM
                );

        configure(
                ravager
        );

        if (
                !attachBetterModel(
                        ravager
                )
        ) {

            /*
             * 모델을 찾지 못한 상태에서
             * 바닐라 Ravager만 필드에 남기지 않는다.
             */
            ravager.remove();
            return;
        }

        regionCooldown.put(
                regionKey,
                now
        );

    }

    /*
     * =========================================================
     * ADMIN TEST SPAWN
     * =========================================================
     *
     * /rpgadmin elite spawn redstone_golem 전용.
     *
     * 자연 스폰 확률, 지역 쿨다운, 주변 개체 제한은 적용하지 않고,
     * 실제 레드스톤 골렘과 동일한 설정과 BetterModel을 적용한다.
     */
    public boolean spawnForAdmin(
            Location location
    ) {

        World world =
                location.getWorld();

        if (world == null) {
            return false;
        }

        Ravager ravager =
                world.spawn(
                        location,
                        Ravager.class,
                        CreatureSpawnEvent
                                .SpawnReason
                                .CUSTOM
                );

        configure(
                ravager
        );

        if (
                !attachBetterModel(
                        ravager
                )
        ) {

            ravager.remove();

            return false;
        }

        return true;
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
             * 플레이어 주변에서 실제로 로드된 청크만 사용한다.
             *
             * 골렘 스폰 때문에 먼 청크를 강제로 로드하지 않는다.
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

            /*
             * getHighestBlockYAt()은 지면 블록의 Y를 반환한다.
             * 엔티티 발 위치는 그보다 1블록 위여야 한다.
             */
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
         * Ravager와 BetterModel이 들어갈 공간 확보.
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

        /*
         * 중간에 블록을 하나라도 만나면
         * 플레이어 시야에서 가려진 위치로 판단한다.
         */
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
            Ravager ravager
    ) {

        ravager.getPersistentDataContainer()
                .set(
                        eliteMobKey,
                        PersistentDataType.STRING,
                        REDSTONE_GOLEM_ID
                );

        ravager.getPersistentDataContainer()
                .set(
                        spawnedAtKey,
                        PersistentDataType.LONG,
                        System.currentTimeMillis()
                );

        ravager.customName(
                Component.text(
                        "[엘리트] 레드스톤 골렘",
                        NamedTextColor.RED
                )
        );

        ravager.setCustomNameVisible(
                true
        );

        /*
         * 필드 엘리트가 장시간 월드에 누적되지 않도록
         * 플레이어와 충분히 멀어지면 바닐라 거리 디스폰을 허용한다.
         *
         * 영구 보존 대상으로 지정하지 않아
         * 장기적인 엔티티 누적 위험을 줄인다.
         */
        ravager.setRemoveWhenFarAway(
                true
        );

        ravager.setPersistent(
                false
        );
    }

    /*
     * =========================================================
     * BETTERMODEL
     * =========================================================
     */
    private boolean attachBetterModel(
            Ravager ravager
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
                        (Entity) ravager
                )
        );

        return true;
    }

    /*
     * =========================================================
     * NEARBY LIMIT
     * =========================================================
     */
    private int countNearbyRedstoneGolems(
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
                            instanceof Ravager)
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
                    REDSTONE_GOLEM_ID.equals(
                            eliteId
                    )
            ) {

                count++;

                if (
                        count
                                >= MAX_NEARBY_GOLEMS
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
    private void cleanupExpiredRedstoneGolems() {

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
                    Ravager ravager
                    : world.getEntitiesByClass(
                            Ravager.class
                    )
            ) {

                String eliteId =
                        ravager.getPersistentDataContainer()
                                .get(
                                        eliteMobKey,
                                        PersistentDataType.STRING
                                );

                if (
                        !REDSTONE_GOLEM_ID.equals(
                                eliteId
                        )
                ) {
                    continue;
                }


                Long spawnedAt =
                        ravager.getPersistentDataContainer()
                                .get(
                                        spawnedAtKey,
                                        PersistentDataType.LONG
                                );

                if (spawnedAt == null) {

                    ravager.getPersistentDataContainer()
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
                                            ravager.getLocation()
                                    )
                                    <= playerRangeSquared
                    ) {

                        playerNearby =
                                true;

                        break;
                    }
                }


                if (!playerNearby) {

                    ravager.remove();
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
                        regionKey(location)
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
}
