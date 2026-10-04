package com.hcs.rpgcore.enchant;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class LoggingWaveListener implements Listener {

    private static final NamespacedKey ENCHANT_KEY =
            NamespacedKey.fromString("rpgcore:logging_wave");

    private static final int[][] DIRECTIONS = {
            { 1,  0,  0},
            {-1,  0,  0},
            { 0,  1,  0},
            { 0, -1,  0},
            { 0,  0,  1},
            { 0,  0, -1}
    };

    private final JavaPlugin plugin;

    private final Set<UUID> processingPlayers =
            new HashSet<>();

    public LoggingWaveListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (processingPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if (player.isSneaking()) {
            return;
        }

        Block origin = event.getBlock();
        Material originType = origin.getType();

        if (!isLog(originType)) {
            return;
        }

        ItemStack tool =
                player.getInventory().getItemInMainHand();

        int level = getLevel(tool);

        if (level <= 0) {
            return;
        }

        int limit = switch (level) {
            case 1 -> 4;
            case 2 -> 8;
            default -> 16;
        };

        UUID playerId = player.getUniqueId();
        World originWorld = origin.getWorld();

        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();

        // 원본 블록의 정상 파괴가 끝난 다음 틱에 실행한다.
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> {

                    if (!player.isOnline()
                            || player.isDead()
                            || player.getWorld() != originWorld
                            || player.isSneaking()
                            || player.getGameMode() != GameMode.SURVIVAL) {
                        return;
                    }

                    // 원본 블록이 실제로 파괴되지 않았다면 중단한다.
                    if (originWorld.getBlockAt(
                            originX,
                            originY,
                            originZ
                    ).getType() == originType) {
                        return;
                    }

                    ItemStack currentTool =
                            player.getInventory()
                                    .getItemInMainHand();

                    if (getLevel(currentTool) <= 0) {
                        return;
                    }

                    processingPlayers.add(playerId);

                    try {
                        breakConnectedLogs(
                                player,
                                originWorld,
                                originX,
                                originY,
                                originZ,
                                originType,
                                limit
                        );
                    } finally {
                        processingPlayers.remove(playerId);
                    }
                }
        );
    }

    private void breakConnectedLogs(
            Player player,
            World world,
            int originX,
            int originY,
            int originZ,
            Material originType,
            int limit
    ) {

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(new int[]{
                originX,
                originY,
                originZ
        });

        visited.add(key(originX, originY, originZ));

        // 플레이어가 직접 파괴한 원목도 개수에 포함한다.
        int brokenCount = 1;

        while (!queue.isEmpty() && brokenCount < limit) {

            if (!player.isOnline()
                    || player.isDead()
                    || player.isSneaking()
                    || player.getWorld() != world
                    || player.getGameMode() != GameMode.SURVIVAL) {
                return;
            }

            if (getLevel(
                    player.getInventory().getItemInMainHand()
            ) <= 0) {
                return;
            }

            int[] current = queue.removeFirst();

            for (int[] direction : DIRECTIONS) {

                if (brokenCount >= limit) {
                    return;
                }

                int x = current[0] + direction[0];
                int y = current[1] + direction[1];
                int z = current[2] + direction[2];

                if (y < world.getMinHeight()
                        || y >= world.getMaxHeight()) {
                    continue;
                }

                String locationKey = key(x, y, z);

                if (!visited.add(locationKey)) {
                    continue;
                }

                // 연쇄 벌목으로 청크를 새로 로딩하지 않는다.
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                Block next = world.getBlockAt(x, y, z);

                if (next.getType() != originType) {
                    continue;
                }

                // 일반 플레이어 채굴 경로를 사용한다.
                // 보호 플러그인이 취소하면 false를 반환한다.
                boolean success = player.breakBlock(next);

                if (!success) {
                    continue;
                }

                brokenCount++;

                // 실제로 캔 블록에서만 다음 연결 원목을 탐색한다.
                queue.addLast(new int[]{x, y, z});

                if (getLevel(
                        player.getInventory().getItemInMainHand()
                ) <= 0) {
                    return;
                }
            }
        }
    }

    private int getLevel(ItemStack tool) {

        if (tool == null
                || tool.getType() == Material.AIR
                || !tool.getType().name().endsWith("_AXE")) {
            return 0;
        }

        if (ENCHANT_KEY == null) {
            return 0;
        }

        Enchantment enchantment =
                Registry.ENCHANTMENT.get(ENCHANT_KEY);

        if (enchantment == null) {
            return 0;
        }

        ItemMeta meta = tool.getItemMeta();

        if (meta == null) {
            return 0;
        }

        return meta.getEnchantLevel(enchantment);
    }

    private boolean isLog(Material material) {

        String name = material.name();

        return name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.endsWith("_STEM")
                    && material != Material.MUSHROOM_STEM
                || name.endsWith("_HYPHAE")
                || material == Material.BAMBOO_BLOCK
                || material == Material.STRIPPED_BAMBOO_BLOCK;
    }

    private String key(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }
}
