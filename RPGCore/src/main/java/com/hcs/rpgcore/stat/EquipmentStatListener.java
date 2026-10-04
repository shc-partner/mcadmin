package com.hcs.rpgcore.stat;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.hud.HudService;
import com.hcs.rpgcore.hud.HudSnapshot;
import com.hcs.rpgcore.mana.ManaService;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;


public final class EquipmentStatListener
        implements Listener {

    private final RPGCorePlugin plugin;

    private final HudService hudService;

    private final StatService statService;

    private final PlayerStatApplier playerStatApplier;

    private final ManaService manaService;

    private final Set<UUID> pendingPlayers =
            ConcurrentHashMap.newKeySet();


    public EquipmentStatListener(
            RPGCorePlugin plugin,
            HudService hudService,
            StatService statService,
            PlayerStatApplier playerStatApplier,
            ManaService manaService
    ) {

        this.plugin =
                plugin;

        this.hudService =
                hudService;

        this.statService =
                statService;

        this.playerStatApplier =
                playerStatApplier;

        this.manaService =
                manaService;
    }


    @EventHandler
    public void onItemHeld(
            PlayerItemHeldEvent event
    ) {

        scheduleRefresh(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onSwapHand(
            PlayerSwapHandItemsEvent event
    ) {

        scheduleRefresh(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(
                event.getWhoClicked()
                instanceof Player player
        )) {
            return;
        }

        scheduleRefresh(
                player,
                1L
        );
    }


    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!(
                event.getWhoClicked()
                instanceof Player player
        )) {
            return;
        }

        scheduleRefresh(
                player,
                1L
        );
    }


    @EventHandler
    public void onInteract(
            PlayerInteractEvent event
    ) {

        if (!event.hasItem()) {
            return;
        }

        scheduleRefresh(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onDropItem(
            PlayerDropItemEvent event
    ) {

        scheduleRefresh(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onItemBreak(
            PlayerItemBreakEvent event
    ) {

        scheduleRefresh(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        scheduleRefresh(
                event.getPlayer(),
                1L
        );
    }


    @EventHandler
    public void onJoin(
            PlayerJoinEvent event
    ) {

        /*
         * HUD profile / DB 캐시 생성 이후 적용될 수 있도록
         * 접속 시에는 조금 늦게 재계산한다.
         */
        scheduleRefresh(
                event.getPlayer(),
                30L
        );
    }


    /*
     * =============================================================
     * REFRESH
     * =============================================================
     */
    private void scheduleRefresh(
            Player player,
            long delay
    ) {

        UUID uuid =
                player.getUniqueId();

        if (!pendingPlayers.add(
                uuid
        )) {
            return;
        }

        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            pendingPlayers.remove(
                                    uuid
                            );

                            Player onlinePlayer =
                                    Bukkit.getPlayer(
                                            uuid
                                    );

                            if (
                                    onlinePlayer == null
                                    || !onlinePlayer.isOnline()
                            ) {
                                return;
                            }


                            /*
                             * 1.
                             * HUD Snapshot을 현재 장비 기준으로 갱신.
                             */
                            hudService.refreshEquipmentStats(
                                    onlinePlayer
                            );


                            /*
                             * 2.
                             * HUD에 저장된 현재 RPG 레벨 확보.
                             */
                            HudSnapshot snapshot =
                                    hudService.getProfile(
                                            uuid
                                    );

                            if (snapshot == null) {
                                return;
                            }


                            /*
                             * 3.
                             * HUD와 동일한 StatService로
                             * 현재 최종 능력치를 다시 계산.
                             */
                            PlayerStats stats =
                                    statService.calculate(
                                            snapshot.level(),
                                            snapshot.playerClass(),
                                            onlinePlayer
                                    );


                            /*
                             * 4.
                             * 실제 Minecraft ATTACK_DAMAGE 적용.
                             */
                            playerStatApplier.apply(
                                    onlinePlayer,
                                    stats
                            );


                            /*
                             * 장비 변경에 따른 최대 MANA 적용.
                             *
                             * preserveRatio=true:
                             * 장착/해제 전 현재 MP 비율을 유지한다.
                             */
                            manaService.updateMaximumMana(
                                    uuid,
                                    stats.maxMana(),
                                    true
                            );
                        },
                        delay
                );
    }
}
