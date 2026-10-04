package com.hcs.rpgcore.level.reward;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.item.CustomItemFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class LevelRewardService implements Listener {

    private static final String SHIELD_ID = "sturdy_shield";

    private final RPGCorePlugin plugin;
    private final LevelRewardRepository repository;
    private final CustomItemFactory itemFactory;

    private final Set<UUID> delivering = new HashSet<>();

    public LevelRewardService(
            RPGCorePlugin plugin,
            LevelRewardRepository repository
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.itemFactory = new CustomItemFactory(plugin);
    }

    /*
     * 레벨 데이터의 DB 저장이 성공한 다음 호출한다.
     *
     * 몬스터 EXP 등 비동기 작업에서도 호출 가능하다.
     * 이미 10레벨 이상이었던 경우에는 등록하지 않는다.
     */
    public void onLevelSaved(
            UUID uuid,
            int oldLevel,
            int newLevel
    ) {

        if (oldLevel >= 10 || newLevel < 10) {
            return;
        }

        try {

            repository.registerShieldReward(uuid);

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {
                        Player player = Bukkit.getPlayer(uuid);

                        if (player != null && player.isOnline()) {
                            tryDeliver(player);
                        }
                    }
            );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Lv.10 shield reward registration failed: "
                            + uuid,
                    exception
            );
        }
    }

    /*
     * 이미 등록된 미수령 보상만 지급한다.
     * 이 메서드에서 새 보상 기록을 생성하지 않는다.
     *
     * 인벤토리 API를 사용하므로 메인 스레드에서 호출한다.
     */
    public void tryDeliver(Player player) {

        UUID uuid = player.getUniqueId();

        if (!delivering.add(uuid)) {
            return;
        }

        try {

            String status =
                    repository.findShieldStatus(uuid);

            if (!"PENDING".equals(status)) {
                return;
            }

            /*
             * 인벤토리 공간이 없으면 지급 기록을 유지한다.
             */
            if (player.getInventory().firstEmpty() < 0) {

                player.sendMessage(
                        "§e[레벨 보상] 튼튼한 방패를 받을 "
                                + "인벤토리 공간이 없습니다. "
                                + "공간을 확보하고 재접속해 주세요."
                );

                return;
            }

            ItemStack shield =
                    itemFactory.create(SHIELD_ID);

            if (shield == null) {

                plugin.getLogger().severe(
                        "Lv.10 reward item not found: "
                                + SHIELD_ID
                );

                player.sendMessage(
                        "§c[레벨 보상] 튼튼한 방패를 생성하지 "
                                + "못했습니다. 관리자에게 문의해 주세요."
                );

                return;
            }

            /*
             * 실제 아이템 지급 전에 상태를 먼저 변경한다.
             * 지급 도중 종료되면 자동 중복 지급하지 않는다.
             */
            if (!repository.changeShieldStatus(
                    uuid,
                    "PENDING",
                    "GRANT_IN_FLIGHT"
            )) {
                return;
            }

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(shield);

            if (!leftovers.isEmpty()) {

                /*
                 * 방패는 수량 1의 비중첩 아이템이다.
                 * addItem이 방패를 반환했다면 지급되지 않은
                 * 것으로 보고 다시 대기 상태로 돌린다.
                 */
                repository.changeShieldStatus(
                        uuid,
                        "GRANT_IN_FLIGHT",
                        "PENDING"
                );

                player.sendMessage(
                        "§e[레벨 보상] 인벤토리 공간을 "
                                + "확보한 뒤 다시 접속해 주세요."
                );

                return;
            }

            if (!repository.changeShieldStatus(
                    uuid,
                    "GRANT_IN_FLIGHT",
                    "DELIVERED"
            )) {

                plugin.getLogger().severe(
                        "Lv.10 shield was added, but DB "
                                + "confirmation failed: " + uuid
                );

                player.sendMessage(
                        "§c[레벨 보상] 방패 지급 기록 확인이 "
                                + "필요합니다. 관리자에게 문의해 주세요."
                );

                return;
            }

            player.sendMessage(
                    "§b[레벨 보상] §fLv.10 달성 보상으로 "
                            + "§b튼튼한 방패§f를 받았습니다!"
            );

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Lv.10 shield delivery requires review: "
                            + uuid,
                    exception
            );

            player.sendMessage(
                    "§c[레벨 보상] 지급 상태 확인이 필요합니다. "
                            + "관리자에게 문의해 주세요."
            );

        } finally {

            delivering.remove(uuid);
        }
    }

    /*
     * 재접속 시 이미 등록된 미수령 보상을 확인한다.
     * 기존 10레벨 이상 플레이어에게 소급 등록하지 않는다.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {

                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null && player.isOnline()) {
                        tryDeliver(player);
                    }
                },
                40L
        );
    }
}
