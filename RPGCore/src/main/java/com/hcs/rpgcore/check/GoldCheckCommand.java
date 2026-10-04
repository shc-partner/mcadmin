package com.hcs.rpgcore.check;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/**
 * /수표 <금액>
 * /check <금액>
 */
public final class GoldCheckCommand
        implements CommandExecutor {

    private static final long MIN_AMOUNT = 1_000L;
    private static final long MAX_AMOUNT = 100_000_000L;

    private final RPGCorePlugin plugin;
    private final GoldCheckRepository repository;
    private final GoldCheckItemFactory itemFactory;
    private final ShopEconomyService economyService;

    public GoldCheckCommand(
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

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 수표를 발행할 수 있습니다.");
            return true;
        }

        if (args.length != 1 || !args[0].matches("[0-9]+")) {
            player.sendMessage("사용법: /수표 <금액>");
            player.sendMessage(
                    "발행 가능 금액: 1,000G ~ 100,000,000G"
            );
            return true;
        }

        final long amount;

        try {
            amount = Long.parseLong(args[0]);

        } catch (NumberFormatException exception) {
            player.sendMessage("올바른 금액을 입력해 주세요.");
            return true;
        }

        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            player.sendMessage(
                    "발행 가능 금액은 1,000G ~ 100,000,000G입니다."
            );
            return true;
        }

        PlayerInventory inventory = player.getInventory();

        if (inventory.firstEmpty() < 0) {
            player.sendMessage("인벤토리에 빈 칸이 필요합니다.");
            return true;
        }

        if (!economyService.has(player, amount)) {
            player.sendMessage("수표를 발행할 골드가 부족합니다.");
            return true;
        }

        UUID checkUuid = UUID.randomUUID();

        final ItemStack checkItem;

        try {
            checkItem = itemFactory.create(checkUuid, amount);

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "[GoldCheck] Item creation failed: "
                            + exception.getMessage()
            );
            player.sendMessage("수표 아이템을 생성하지 못했습니다.");
            return true;
        }

        /*
         * 수표 발행 기록을 먼저 생성한다.
         * 여기까지는 골드 차감이나 아이템 지급이 없다.
         */
        try {
            repository.createIssuing(
                    checkUuid,
                    player.getUniqueId(),
                    amount
            );

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "[GoldCheck] ISSUING insert failed: "
                            + checkUuid
                            + " error=" + exception.getMessage()
            );
            player.sendMessage("수표 발행 기록을 생성하지 못했습니다.");
            return true;
        }

        /*
         * 골드 차감 실패가 명확한 경우에만 발행 기록을 취소한다.
         * 예외가 발생해 결제 성공 여부가 불분명하면 자동 재시도하지 않는다.
         */
        final boolean withdrawn;

        try {
            withdrawn = economyService.withdraw(player, amount);

        } catch (Exception exception) {
            manualReview(
                    player,
                    checkUuid,
                    amount,
                    "Withdrawal result unknown: "
                            + exception.getMessage()
            );
            return true;
        }

        if (!withdrawn) {
            try {
                if (!repository.cancelIssuing(checkUuid)) {
                    plugin.getLogger().severe(
                            "[GoldCheck] ISSUING cancellation failed: "
                                    + checkUuid
                    );
                }

            } catch (SQLException exception) {
                plugin.getLogger().severe(
                        "[GoldCheck] ISSUING cancellation error: "
                                + checkUuid
                                + " error=" + exception.getMessage()
                );
            }

            player.sendMessage("골드 차감에 실패했습니다.");
            return true;
        }

        /*
         * 골드 차감 성공 후 수표를 발행 완료 상태로 변경한다.
         * UPDATE 응답이 불명확하면 실제 DB 상태를 다시 조회한다.
         */
        boolean issued = false;

        try {
            issued = repository.markIssued(checkUuid);

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "[GoldCheck] ISSUED transition error: "
                            + checkUuid
                            + " error=" + exception.getMessage()
            );
        }

        if (!issued) {
            try {
                GoldCheckRepository.GoldCheck record =
                        repository.find(checkUuid);

                issued = record != null
                        && GoldCheckRepository.ISSUED.equals(
                                record.status()
                        );

            } catch (SQLException exception) {
                manualReview(
                        player,
                        checkUuid,
                        amount,
                        "Cannot verify issuance: "
                                + exception.getMessage()
                );
                return true;
            }
        }

        if (!issued) {
            manualReview(
                    player,
                    checkUuid,
                    amount,
                    "Gold withdrawn but check is not ISSUED"
            );
            return true;
        }

        /*
         * 동일 서버 메인 스레드에서 지급한다.
         * 지급 실패 시 재발행하거나 환불하지 않고 거래 ID를 기록한다.
         */
        try {
            Map<Integer, ItemStack> leftovers =
                    inventory.addItem(checkItem);

            if (!leftovers.isEmpty()) {
                manualReview(
                        player,
                        checkUuid,
                        amount,
                        "Inventory could not receive the check"
                );
                return true;
            }

        } catch (Exception exception) {
            manualReview(
                    player,
                    checkUuid,
                    amount,
                    "Check delivery result unknown: "
                            + exception.getMessage()
            );
            return true;
        }

        player.sendMessage(
                String.format(
                        "%,dG 수표 1장을 발행했습니다.",
                        amount
                )
        );

        return true;
    }

    private void manualReview(
            Player player,
            UUID checkUuid,
            long amount,
            String reason
    ) {

        plugin.getLogger().severe(
                "[GoldCheck] MANUAL REVIEW REQUIRED"
                        + " check=" + checkUuid
                        + " player=" + player.getUniqueId()
                        + " amount=" + amount
                        + " reason=" + reason
        );

        player.sendMessage(
                "수표 발행 상태를 확인해야 합니다. "
                        + "관리자에게 문의해 주세요."
        );
    }
}
