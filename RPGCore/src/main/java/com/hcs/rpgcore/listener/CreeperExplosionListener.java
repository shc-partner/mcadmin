package com.hcs.rpgcore.listener;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class CreeperExplosionListener implements Listener {

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper)) {
            return;
        }

        // 크리퍼 폭발 자체와 엔티티 피해/넉백은 유지하고
        // 블록 파괴만 제거한다.
        event.blockList().clear();
    }
}
