package com.hcs.rpgcore.hud;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class HudUpdateTask
        implements Runnable {

    private static final int KEEP_ALIVE_TICKS =
            20;

    private final HudService hudService;

    private int tickCounter =
            0;


    public HudUpdateTask(
            HudService hudService
    ) {

        this.hudService =
                hudService;
    }


    @Override
    public void run() {

        /*
         * =====================================================
         * O2 HUD REALTIME REFRESH
         * =====================================================
         *
         * 산소량은 매 tick 변할 수 있으므로,
         * 물속/공기 회복 중인 플레이어만 dirty 처리.
         */
        for (
                org.bukkit.entity.Player player
                : org.bukkit.Bukkit.getOnlinePlayers()
        ) {

            if (
                    player.isUnderWater()
                    || player.getRemainingAir()
                            < player.getMaximumAir()
            ) {

                hudService.requestUpdate(
                        player
                );
            }


            /*
             * 플레이어 Locator Bar는
             * 보는 방향에 따라 실시간 갱신되어야 한다.
             */
            if (
                    player.getWorld()
                            .getPlayers()
                            .size() > 1
            ) {

                hudService.requestUpdate(
                        player
                );
            }
        }



        tickCounter++;

        boolean keepAlive =
                tickCounter >= KEEP_ALIVE_TICKS;

        if (keepAlive) {
            tickCounter = 0;
        }


        for (
                Player player
                : Bukkit.getOnlinePlayers()
        ) {

            UUID uuid =
                    player.getUniqueId();

            boolean dirty =
                    hudService
                            .consumeUpdateRequest(
                                    uuid
                            );

            if (
                    !dirty
                    && !keepAlive
            ) {
                continue;
            }

            hudService.sendDisplay(
                    player,
                    keepAlive
            );
        }
    }
}
