package com.hcs.rpgcore.listener;

import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public final class EndermanBlockProtectionListener implements Listener {

    @EventHandler
    public void onEndermanChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Enderman)) {
            return;
        }

        // Prevent Endermen from picking up or placing blocks.
        // Movement, attacks and teleportation remain unchanged.
        event.setCancelled(true);
    }
}
