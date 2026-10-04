package com.hcs.rpgcore.hud;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.FoodLevelChangeEvent;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public final class HudFoodListener
        implements Listener {

    private final RPGCorePlugin plugin;

    private final HudService hudService;


    public HudFoodListener(
            RPGCorePlugin plugin,
            HudService hudService
    ) {

        this.plugin =
                plugin;

        this.hudService =
                hudService;
    }


    /*
     * =========================================================
     * FOOD LEVEL CHANGE
     * =========================================================
     *
     * Minecraft foodLevel:
     * 0 ~ 20
     *
     * RPGCore HUD SP:
     * 0 ~ 100
     *
     * HudService에서:
     *
     * SP = foodLevel * 5
     *
     * 로 표시한다.
     *
     * 이 Listener는 값을 따로 저장하지 않고
     * 실제 Minecraft foodLevel이 변경된 직후
     * HUD만 다시 그리도록 요청한다.
     */
    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onFoodLevelChange(
            FoodLevelChangeEvent event
    ) {

        if (!(
                event.getEntity()
                instanceof Player player
        )) {
            return;
        }

        requestNextTick(
                player
        );
    }


    /*
     * 접속 시 현재 foodLevel 반영.
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onJoin(
            PlayerJoinEvent event
    ) {

        requestNextTick(
                event.getPlayer()
        );
    }


    /*
     * 리스폰 직후 현재 foodLevel 반영.
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        requestNextTick(
                event.getPlayer()
        );
    }


    /*
     * FoodLevelChangeEvent가 발생하는 시점에는
     * 실제 Player foodLevel 적용이 아직 완료되기 전일 수 있으므로
     * 다음 tick에서 현재 값을 읽는다.
     */
    private void requestNextTick(
            Player player
    ) {

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            hudService.requestUpdate(
                                    player
                            );
                        }
                );
    }
}
