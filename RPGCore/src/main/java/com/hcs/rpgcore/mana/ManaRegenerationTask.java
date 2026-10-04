package com.hcs.rpgcore.mana;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ManaRegenerationTask
        implements Runnable {

    private final ManaService manaService;

    private final double regenerationPerSecond;

    public ManaRegenerationTask(
            ManaService manaService,
            double regenerationPerSecond
    ) {
        this.manaService =
                manaService;

        this.regenerationPerSecond =
                Math.max(
                        0.0,
                        regenerationPerSecond
                );
    }

    @Override
    public void run() {

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            UUID uuid =
                    player.getUniqueId();

            if (!manaService.isInitialized(uuid)) {

                continue;
            }

            manaService.restore(
                    uuid,
                    regenerationPerSecond
            );
        }
    }
}
