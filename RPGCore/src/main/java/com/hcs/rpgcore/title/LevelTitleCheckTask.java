package com.hcs.rpgcore.title;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.player.PlayerData;
import com.hcs.rpgcore.player.PlayerRepository;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 온라인 플레이어의 RPGCore DB 레벨을 확인한다.
 *
 * 접속 후 최초 검사: 약 5초
 * 이후 검사: 1분 간격
 *
 * 레벨 칭호 획득 및 중복 지급 방지는
 * LevelTitleUnlockService에 위임한다.
 */
public final class LevelTitleCheckTask {

    private final RPGCorePlugin plugin;
    private final PlayerRepository playerRepository;
    private final LevelTitleUnlockService unlockService;

    private final Set<UUID> loading =
            ConcurrentHashMap.newKeySet();

    private BukkitTask task;

    public LevelTitleCheckTask(
            RPGCorePlugin plugin,
            PlayerRepository playerRepository,
            LevelTitleUnlockService unlockService
    ) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;
        this.unlockService = unlockService;
    }

    public void start() {
        if (task != null) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::checkOnlinePlayers,
                100L,
                1200L
        );

        plugin.getLogger().info(
                "[PlayerTitle] 레벨 칭호 자동 획득 검사 시작"
        );
    }

    private void checkOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (!loading.add(uuid)) {
                continue;
            }

            Bukkit.getScheduler().runTaskAsynchronously(
                    plugin,
                    () -> checkPlayerLevel(uuid)
            );
        }
    }

    private void checkPlayerLevel(UUID uuid) {
        try {
            PlayerData data =
                    playerRepository.findPlayer(uuid);

            if (data == null || !plugin.isEnabled()) {
                return;
            }

            unlockService.checkLevel(
                    uuid,
                    data.getLevel()
            );

        } catch (SQLException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "[PlayerTitle] 레벨 칭호 검사 실패: "
                            + uuid,
                    exception
            );

        } finally {
            loading.remove(uuid);
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        loading.clear();
    }
}
