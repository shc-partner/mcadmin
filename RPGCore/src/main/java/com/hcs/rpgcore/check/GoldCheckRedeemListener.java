package com.hcs.rpgcore.check;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * RPGCore 골드 수표 우클릭 환전.
 *
 * 실제 금액 및 상태는 MariaDB에서 검증한다.
 * 골드 지급 여부가 불명확하면 자동 재시도하지 않는다.
 */
public final class GoldCheckRedeemListener implements Listener {

    private final RPGCorePlugin plugin;
    private final GoldCheckRepository repository;
    private final GoldCheckItemFactory itemFactory;
    private final ShopEconomyService economyService;

    public GoldCheckRedeemListener(
            RPGCorePlugin plugin,
            GoldCheckRepository repository,
            GoldCheckItemFactory itemFactory,
            ShopEconomyService economyService
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.itemFactory = itemFactory;
        this.economyService = economyService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        EquipmentSlot hand = event.getHand();

        if (hand == null) {
            return;
        }

        ItemStack item = event.getItem();
        UUID checkUuid = itemFactory.readCheckUuid(item);

        if (checkUuid == null) {
            return;
        }

        // 수표를 우클릭했으면 일반 아이템 사용은 막는다.
        event.setCancelled(true);

        Player player = event.getPlayer();

        /*
         * 수표는 한 장씩 발행된다.
         * 동일 UUID를 가진 아이템이 스택된 경우 환전을 거부한다.
         */
        if (item == null || item.getAmount() != 1) {
            player.sendMessage(
                    "수표는 한 장씩 환전해야 합니다."
            );
            return;
        }

        final GoldCheckRepository.GoldCheck check;

        try {
            check = repository.find(checkUuid);

        } catch (SQLException exception) {
            logError(
                    "수표 조회 실패: " + checkUuid,
                    exception
            );

            player.sendMessage(
                    "수표 정보를 조회하지 못했습니다."
            );
            return;
        }

        if (check == null) {
            player.sendMessage(
                    "발행 기록이 없는 수표입니다."
            );
            return;
        }

        if (!GoldCheckRepository.ISSUED.equals(
                check.status()
        )) {
            player.sendMessage(
                    "이미 환전됐거나 확인이 필요한 수표입니다."
            );
            return;
        }

        long amount = check.amountGold();

        if (amount <= 0L || amount > 100_000_000L) {
            manualReview(
                    player,
                    checkUuid,
                    "잘못된 수표 금액: " + amount
            );
            return;
        }

        /*
         * DB에서 ISSUED -> REDEEMING을 선점한다.
         * 같은 UUID를 가진 복제 수표의 중복 환전을 방지한다.
         */
        final boolean claimed;

        try {
            claimed = repository.claimRedemption(
                    checkUuid,
                    player.getUniqueId()
            );

        } catch (SQLException exception) {
            /*
             * UPDATE가 실제 반영됐는지 알 수 없으므로
             * 골드를 지급하거나 선점을 해제하지 않는다.
             */
            logError(
                    "환전 선점 결과 불명확: " + checkUuid,
                    exception
            );

            manualReview(
                    player,
                    checkUuid,
                    "환전 선점 결과 불명확"
            );
            return;
        }

        if (!claimed) {
            player.sendMessage(
                    "이미 환전 중이거나 사용할 수 없는 수표입니다."
            );
            return;
        }

        /*
         * Vault 입금에 성공한 경우에만 환전 완료 처리한다.
         */
        final boolean deposited;

        try {
            deposited = economyService.deposit(
                    player,
                    amount
            );

        } catch (Exception exception) {
            /*
             * 입금이 실제로 성공했을 가능성이 있으므로
             * REDEEMING 상태를 유지하고 관리자 확인을 요청한다.
             */
            logError(
                    "골드 지급 결과 불명확: " + checkUuid,
                    exception
            );

            manualReview(
                    player,
                    checkUuid,
                    "골드 지급 결과 불명확"
            );
            return;
        }

        if (!deposited) {

            /*
             * Vault가 입금 실패를 명확히 반환한 경우에만
             * 다음 환전을 위해 선점을 해제한다.
             */
            try {
                if (!repository.releaseRedemption(
                        checkUuid,
                        player.getUniqueId()
                )) {
                    manualReview(
                            player,
                            checkUuid,
                            "입금 실패 후 선점 해제 실패"
                    );
                    return;
                }

            } catch (SQLException exception) {
                logError(
                        "환전 선점 해제 실패: " + checkUuid,
                        exception
                );

                manualReview(
                        player,
                        checkUuid,
                        "입금 실패 후 DB 상태 변경 오류"
                );
                return;
            }

            player.sendMessage(
                    "골드 지급에 실패했습니다. 잠시 후 다시 시도하세요."
            );
            return;
        }

        /*
         * 입금은 성공했다.
         * 이제 DB를 REDEEMED로 변경한다.
         */
        boolean redeemed = false;

        try {
            redeemed = repository.markRedeemed(
                    checkUuid,
                    player.getUniqueId()
            );

        } catch (SQLException exception) {
            logError(
                    "환전 완료 상태 변경 실패: " + checkUuid,
                    exception
            );
        }

        /*
         * UPDATE 응답이 불명확하면 현재 DB 상태를 재조회한다.
         */
        if (!redeemed) {
            try {
                GoldCheckRepository.GoldCheck current =
                        repository.find(checkUuid);

                redeemed = current != null
                        && GoldCheckRepository.REDEEMED.equals(
                                current.status()
                        )
                        && player.getUniqueId().equals(
                                current.redeemedByUuid()
                        );

            } catch (SQLException exception) {
                logError(
                        "환전 완료 상태 확인 실패: " + checkUuid,
                        exception
                );
            }
        }

        if (!redeemed) {
            /*
             * 이미 골드가 입금됐으므로 자동 환불하거나
             * 수표를 재사용 가능 상태로 돌리지 않는다.
             */
            manualReview(
                    player,
                    checkUuid,
                    "골드는 지급됐으나 DB 환전 완료 확인 실패"
            );
            return;
        }

        /*
         * DB 환전 완료 후 손에 든 수표 한 장을 회수한다.
         */
        ItemStack currentItem;

        if (hand == EquipmentSlot.HAND) {
            currentItem =
                    player.getInventory().getItemInMainHand();
        } else {
            currentItem =
                    player.getInventory().getItemInOffHand();
        }

        UUID currentUuid =
                itemFactory.readCheckUuid(currentItem);

        if (!checkUuid.equals(currentUuid)
                || currentItem.getAmount() != 1) {

            manualReview(
                    player,
                    checkUuid,
                    "환전 완료 후 수표 회수 대상 불일치"
            );
            return;
        }

        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(
                    new ItemStack(Material.AIR)
            );
        } else {
            player.getInventory().setItemInOffHand(
                    new ItemStack(Material.AIR)
            );
        }

        player.sendMessage(
                String.format(
                        "수표를 환전하여 %,dG를 받았습니다.",
                        amount
                )
        );
    }

    private void manualReview(
            Player player,
            UUID checkUuid,
            String reason
    ) {

        plugin.getLogger().severe(
                "[GoldCheck] MANUAL REVIEW REQUIRED"
                        + " check=" + checkUuid
                        + " player=" + player.getUniqueId()
                        + " reason=" + reason
        );

        player.sendMessage(
                "수표 처리 상태를 확인해야 합니다. "
                        + "관리자에게 문의해 주세요."
        );
    }

    private void logError(
            String message,
            Exception exception
    ) {
        plugin.getLogger().log(
                Level.SEVERE,
                "[GoldCheck] " + message,
                exception
        );
    }
}
