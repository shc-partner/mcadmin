package com.hcs.rpgcore.combat;

import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class MonsterFallDamageListener implements Listener {

    @EventHandler
    public void onMonsterFallDamage(EntityDamageEvent event) {

        if (event.getCause()
                != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        event.setCancelled(true);
    }
}
