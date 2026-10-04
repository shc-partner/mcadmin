package com.hcs.rpgcore.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class VanillaExperienceListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        clearVanillaExperience(
                event.getPlayer()
        );
    }

    @EventHandler
    public void onPlayerExpChange(
            PlayerExpChangeEvent event
    ) {
        event.setAmount(0);

        clearVanillaExperience(
                event.getPlayer()
        );
    }

    private void clearVanillaExperience(
            Player player
    ) {
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);
    }
}
