package com.hcs.rpgcore.dungeon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;


/*
 * =========================================================
 * RED DRAGON INFERNO RIFT HAZARD
 * =========================================================
 *
 * 레드 드래곤 전용 맵 고유 패턴.
 *
 * - 10초마다 발동
 * - 보스 중심 7 ~ 9블록 거리에서 위치 선정
 * - 4 x 4 위험 지형 생성
 * - 3초 동안 경고
 * - 이후 폭발
 * - 낮은 피해 + 화상
 * - 폭발 직후 원래 블록 복구
 * - 보스 클리어 / 실패 / 리셋 시 즉시 전체 복구
 */
public final class RedDragonInfernoRiftHazard {

    /*
     * 10초.
     */
    private static final long ATTACK_INTERVAL_TICKS =
            200L;


    /*
     * 위험 지형 유지 시간.
     *
     * 3초.
     */
    private static final long WARNING_TICKS =
            60L;


    private static final double MIN_DISTANCE =
            7.0D;

    private static final double MAX_DISTANCE =
            9.0D;


    /*
     * 4 x 4.
     */
    private static final int AREA_SIZE =
            4;


    /*
     * 포지셔닝 제한이 핵심이므로
     * 피해량은 낮게 유지한다.
     */
    private static final double EXPLOSION_DAMAGE =
            50.0D;


    /*
     * 3초 화상.
     */
    private static final int FIRE_TICKS =
            60;


    private final JavaPlugin plugin;

    private final LivingEntity boss;

    private final Set<UUID> participants;

    private final BooleanSupplier combatActive;


    private BukkitTask attackTask;


    /*
     * 현재 아직 폭발하지 않은 균열.
     *
     * stop() 호출 시 전부 복구해야 한다.
     */
    private final List<RiftInstance> activeRifts =
            new ArrayList<>();


    public RedDragonInfernoRiftHazard(
            JavaPlugin plugin,
            LivingEntity boss,
            Set<UUID> participants,
            BooleanSupplier combatActive
    ) {

        this.plugin =
                plugin;

        this.boss =
                boss;

        this.participants =
                participants;

        this.combatActive =
                combatActive;
    }


    /*
     * =========================================================
     * START / STOP
     * =========================================================
     */

    public void start() {

        stop();

        attackTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::tryCreateRift,
                                ATTACK_INTERVAL_TICKS,
                                ATTACK_INTERVAL_TICKS
                        );
    }


    public void stop() {

        if (attackTask != null) {

            attackTask.cancel();

            attackTask =
                    null;
        }


        for (
                RiftInstance rift
                : new ArrayList<>(
                        activeRifts
                )
        ) {

            cleanupRift(
                    rift
            );
        }


        activeRifts.clear();
    }


    /*
     * =========================================================
     * CREATE
     * =========================================================
     */

    private void tryCreateRift() {

        if (!combatActive.getAsBoolean()) {
            return;
        }


        if (
                boss == null
                        ||
                !boss.isValid()
                        ||
                boss.isDead()
        ) {

            return;
        }


        Location bossLocation =
                boss.getLocation()
                        .clone();

        World world =
                bossLocation.getWorld();

        if (world == null) {
            return;
        }


        double angle =
                ThreadLocalRandom.current()
                        .nextDouble(
                                0.0D,
                                Math.PI * 2.0D
                        );

        double distance =
                ThreadLocalRandom.current()
                        .nextDouble(
                                MIN_DISTANCE,
                                MAX_DISTANCE
                        );


        double targetX =
                bossLocation.getX()
                        + Math.cos(
                                angle
                        )
                        * distance;

        double targetZ =
                bossLocation.getZ()
                        + Math.sin(
                                angle
                        )
                        * distance;


        RiftInstance rift =
                createRiftArea(
                        world,
                        targetX,
                        bossLocation.getY(),
                        targetZ
                );

        if (rift == null) {
            return;
        }


        activeRifts.add(
                rift
        );


        playWarningStart(
                rift
        );


        rift.warningTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                () ->
                                        playWarningTick(
                                                rift
                                        ),
                                0L,
                                10L
                        );


        rift.explosionTask =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () ->
                                        explodeRift(
                                                rift
                                        ),
                                WARNING_TICKS
                        );
    }


    /*
     * =========================================================
     * TERRAIN
     * =========================================================
     */

    private RiftInstance createRiftArea(
            World world,
            double centerX,
            double searchY,
            double centerZ
    ) {

        Map<Block, BlockData> originals =
                new LinkedHashMap<>();


        int baseX =
                (int) Math.floor(
                        centerX
                );

        int baseZ =
                (int) Math.floor(
                        centerZ
                );


        /*
         * 4 x 4:
         *
         * -1, 0, 1, 2
         */
        for (
                int dx = -1;
                dx <= 2;
                dx++
        ) {

            for (
                    int dz = -1;
                    dz <= 2;
                    dz++
            ) {

                int x =
                        baseX + dx;

                int z =
                        baseZ + dz;


                Block ground =
                        findGroundBlock(
                                world,
                                x,
                                (int) Math.floor(
                                        searchY
                                ),
                                z
                        );

                if (ground == null) {

                    restoreBlocks(
                            originals
                    );

                    return null;
                }


                /*
                 * 기반암은 절대 변경하지 않는다.
                 */
                if (
                        ground.getType()
                                == Material.BEDROCK
                ) {

                    restoreBlocks(
                            originals
                    );

                    return null;
                }


                originals.put(
                        ground,
                        ground.getBlockData()
                                .clone()
                );
            }
        }


        /*
         * 정확히 4 x 4가 만들어졌을 때만 적용한다.
         */
        if (
                originals.size()
                        != AREA_SIZE
                        * AREA_SIZE
        ) {

            restoreBlocks(
                    originals
            );

            return null;
        }


        double totalY =
                0.0D;


        for (
                Block block
                : originals.keySet()
        ) {

            totalY +=
                    block.getY();

            block.setType(
                    Material.MAGMA_BLOCK,
                    false
            );
        }


        double centerY =
                totalY
                        / originals.size()
                        + 1.0D;


        Location center =
                new Location(
                        world,
                        baseX + 0.5D,
                        centerY,
                        baseZ + 0.5D
                );


        return new RiftInstance(
                center,
                originals
        );
    }


    private Block findGroundBlock(
            World world,
            int x,
            int startY,
            int z
    ) {

        int topY =
                Math.min(
                        world.getMaxHeight() - 2,
                        startY + 3
                );


        int bottomY =
                world.getMinHeight() + 1;


        for (
                int y = topY;
                y >= bottomY;
                y--
        ) {

            Block block =
                    world.getBlockAt(
                            x,
                            y,
                            z
                    );

            Block above =
                    world.getBlockAt(
                            x,
                            y + 1,
                            z
                    );


            if (
                    !block.getType()
                            .isSolid()
            ) {

                continue;
            }


            /*
             * 플레이어가 설 수 있는 표면만 사용한다.
             */
            if (!above.isPassable()) {
                continue;
            }


            return block;
        }


        return null;
    }


    /*
     * =========================================================
     * WARNING
     * =========================================================
     */

    private void playWarningStart(
            RiftInstance rift
    ) {

        World world =
                rift.center
                        .getWorld();

        if (world == null) {
            return;
        }


        world.playSound(
                rift.center,
                Sound.BLOCK_LAVA_POP,
                1.5F,
                0.55F
        );


        world.spawnParticle(
                Particle.FLAME,
                rift.center,
                40,
                1.5D,
                0.15D,
                1.5D,
                0.02D
        );


        world.spawnParticle(
                Particle.SMOKE,
                rift.center,
                30,
                1.5D,
                0.15D,
                1.5D,
                0.03D
        );
    }


    private void playWarningTick(
            RiftInstance rift
    ) {

        if (!combatActive.getAsBoolean()) {

            cleanupRift(
                    rift
            );

            return;
        }


        World world =
                rift.center
                        .getWorld();

        if (world == null) {
            return;
        }


        world.spawnParticle(
                Particle.FLAME,
                rift.center,
                14,
                1.4D,
                0.10D,
                1.4D,
                0.01D
        );


        world.spawnParticle(
                Particle.SMOKE,
                rift.center,
                8,
                1.4D,
                0.10D,
                1.4D,
                0.01D
        );
    }


    /*
     * =========================================================
     * EXPLOSION
     * =========================================================
     */

    private void explodeRift(
            RiftInstance rift
    ) {

        if (
                !activeRifts.contains(
                        rift
                )
        ) {

            return;
        }


        if (!combatActive.getAsBoolean()) {

            cleanupRift(
                    rift
            );

            return;
        }


        World world =
                rift.center
                        .getWorld();

        if (world == null) {

            cleanupRift(
                    rift
            );

            return;
        }


        world.playSound(
                rift.center,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.8F,
                0.65F
        );


        world.spawnParticle(
                Particle.EXPLOSION,
                rift.center,
                4,
                1.0D,
                0.2D,
                1.0D,
                0.0D
        );


        world.spawnParticle(
                Particle.FLAME,
                rift.center,
                90,
                1.8D,
                0.6D,
                1.8D,
                0.08D
        );


        world.spawnParticle(
                Particle.LAVA,
                rift.center,
                30,
                1.6D,
                0.3D,
                1.6D,
                0.0D
        );


        damagePlayersInsideRift(
                rift
        );


        cleanupRift(
                rift
        );
    }


    private void damagePlayersInsideRift(
            RiftInstance rift
    ) {

        if (rift.originalBlocks.isEmpty()) {
            return;
        }


        int minX =
                Integer.MAX_VALUE;

        int maxX =
                Integer.MIN_VALUE;

        int minZ =
                Integer.MAX_VALUE;

        int maxZ =
                Integer.MIN_VALUE;


        for (
                Block block
                : rift.originalBlocks
                        .keySet()
        ) {

            minX =
                    Math.min(
                            minX,
                            block.getX()
                    );

            maxX =
                    Math.max(
                            maxX,
                            block.getX()
                    );

            minZ =
                    Math.min(
                            minZ,
                            block.getZ()
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            block.getZ()
                    );
        }


        for (
                UUID uuid
                : participants
        ) {

            Player player =
                    plugin.getServer()
                            .getPlayer(
                                    uuid
                            );

            if (
                    player == null
                            ||
                    !player.isOnline()
                            ||
                    player.isDead()
            ) {

                continue;
            }


            if (
                    player.getWorld()
                            != rift.center
                            .getWorld()
            ) {

                continue;
            }


            Location location =
                    player.getLocation();


            double x =
                    location.getX();

            double z =
                    location.getZ();


            if (
                    x < minX
                            ||
                    x >= maxX + 1.0D
                            ||
                    z < minZ
                            ||
                    z >= maxZ + 1.0D
            ) {

                continue;
            }


            /*
             * 특수 패턴 자체 피해량을 유지하기 위해
             * boss를 damage source로 넘기지 않는다.
             */
            player.damage(
                    EXPLOSION_DAMAGE
            );


            player.setFireTicks(
                    Math.max(
                            player.getFireTicks(),
                            FIRE_TICKS
                    )
            );
        }
    }


    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    private void cleanupRift(
            RiftInstance rift
    ) {

        if (rift == null) {
            return;
        }


        if (rift.warningTask != null) {

            rift.warningTask.cancel();

            rift.warningTask =
                    null;
        }


        if (rift.explosionTask != null) {

            rift.explosionTask.cancel();

            rift.explosionTask =
                    null;
        }


        restoreBlocks(
                rift.originalBlocks
        );


        activeRifts.remove(
                rift
        );
    }


    private void restoreBlocks(
            Map<Block, BlockData> originals
    ) {

        for (
                Map.Entry<Block, BlockData> entry
                : originals.entrySet()
        ) {

            Block block =
                    entry.getKey();

            BlockData original =
                    entry.getValue();


            if (
                    block == null
                            ||
                    original == null
            ) {

                continue;
            }


            block.setBlockData(
                    original,
                    false
            );
        }
    }


    /*
     * =========================================================
     * INSTANCE
     * =========================================================
     */

    private static final class RiftInstance {

        private final Location center;

        private final Map<Block, BlockData> originalBlocks;

        private BukkitTask warningTask;

        private BukkitTask explosionTask;


        private RiftInstance(
                Location center,
                Map<Block, BlockData> originalBlocks
        ) {

            this.center =
                    center;

            this.originalBlocks =
                    originalBlocks;
        }
    }
}
