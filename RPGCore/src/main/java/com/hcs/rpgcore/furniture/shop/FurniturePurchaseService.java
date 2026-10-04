package com.hcs.rpgcore.furniture.shop;

import com.hcs.rpgcore.RPGCorePlugin;
import com.hcs.rpgcore.shop.ShopEconomyService;
import com.hcs.rpgcore.shop.ShopItemDefinition;
import com.hcs.rpgcore.shop.ShopItemProvider;
import com.hcs.rpgcore.shop.ShopItemSource;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * NPC 25 가구 상점 구매 처리.
 *
 * 상품 및 가격: MariaDB
 * 아이템 생성: ShopItemProvider
 * 골드 처리: ShopEconomyService
 *
 * 현재 구매 지원: CRAFTENGINE
 * ITEMSADDER는 추후 아이템 생성기 연동 후 활성화.
 */
public final class FurniturePurchaseService {

    private final RPGCorePlugin plugin;
    private final FurnitureShopRepository shopRepository;
    private final FurniturePurchaseRepository purchaseRepository;
    private final ShopEconomyService economyService;
    private final ShopItemProvider itemProvider;

    /*
     * 같은 플레이어의 구매 중복 실행 방지.
     * Bukkit 메인 스레드에서만 접근한다.
     */
    private final Set<UUID> purchasing = new HashSet<>();

    public FurniturePurchaseService(
            RPGCorePlugin plugin,
            FurnitureShopRepository shopRepository,
            FurniturePurchaseRepository purchaseRepository,
            ShopEconomyService economyService,
            ShopItemProvider itemProvider
    ) {
        this.plugin = plugin;
        this.shopRepository = shopRepository;
        this.purchaseRepository = purchaseRepository;
        this.economyService = economyService;
        this.itemProvider = itemProvider;
    }

    /**
     * DB 상품 ID로 가구 1개 구매.
     */
    public boolean purchase(
            Player player,
            String shopItemId
    ) {

        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Furniture purchase must run on the server main thread."
            );
        }

        UUID playerUuid = player.getUniqueId();

        if (!purchasing.add(playerUuid)) {
            player.sendMessage("이미 구매를 처리하고 있습니다.");
            return false;
        }

        try {
            return purchaseLocked(player, shopItemId);

        } finally {
            purchasing.remove(playerUuid);
        }
    }

    private boolean purchaseLocked(
            Player player,
            String shopItemId
    ) {

        final FurnitureShopRepository.FurnitureItem product;

        try {
            product = shopRepository.findEnabledByShopItemId(
                    shopItemId
            );

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Product lookup failed: "
                            + exception.getMessage()
            );

            player.sendMessage(
                    "가구 상품 정보를 불러오지 못했습니다."
            );

            return false;
        }

        if (product == null) {
            player.sendMessage(
                    "현재 판매하지 않는 가구입니다."
            );
            return false;
        }

        final ShopItemSource itemSource;

        try {
            itemSource =
                    ShopItemSource.valueOf(
                            product.provider()
                                    .trim()
                                    .toUpperCase()
                    );

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Unsupported provider: "
                            + product.provider()
                            + " shopItemId="
                            + product.shopItemId()
            );

            player.sendMessage(
                    "아직 구매를 지원하지 않는 가구입니다."
            );

            return false;
        }

        if (
                itemSource != ShopItemSource.CRAFTENGINE
                        && itemSource != ShopItemSource.ITEMSADDER
        ) {
            player.sendMessage(
                    "아직 구매를 지원하지 않는 가구입니다."
            );

            return false;
        }

        long price = product.priceGold();

        if (price <= 0L || price > Integer.MAX_VALUE) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Invalid price: "
                            + product.shopItemId()
                            + " price=" + price
            );

            player.sendMessage(
                    "상품 가격 설정을 확인해야 합니다."
            );

            return false;
        }

        /*
         * 결제 전에 실제 설치 가능한 CraftEngine 아이템 생성.
         */
        final ItemStack item;

        try {
            ShopItemDefinition definition =
                    ShopItemDefinition.fixedBuy(
                            product.shopItemId(),
                            product.displayName(),
                            itemSource,
                            product.providerItemId(),
                            0,
                            1,
                            (int) price
                    );

            item = itemProvider.create(
                    player,
                    definition
            );

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Item creation failed: "
                            + product.providerItemId()
                            + " error=" + exception.getMessage()
            );

            player.sendMessage(
                    "가구 아이템을 생성하지 못했습니다."
            );

            return false;
        }

        if (item == null || item.getType().isAir()) {
            player.sendMessage(
                    "가구 아이템 정보가 올바르지 않습니다."
            );

            return false;
        }

        /*
         * 가구 1개가 들어갈 빈 슬롯을 확보한다.
         * 스택 가능한 경우에도 보수적으로 빈 슬롯을 요구한다.
         */
        PlayerInventory inventory = player.getInventory();

        if (inventory.firstEmpty() < 0) {
            player.sendMessage(
                    "인벤토리에 빈 칸이 필요합니다."
            );

            return false;
        }

        if (!economyService.has(player, price)) {
            player.sendMessage(
                    "골드가 부족합니다. 필요 금액: "
                            + price + "G"
            );

            return false;
        }

        UUID transactionId = UUID.randomUUID();

        /*
         * 결제에 앞서 거래 ID와 가격을 DB에 기록한다.
         */
        try {
            purchaseRepository.createPending(
                    transactionId,
                    player.getUniqueId(),
                    product
            );

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Purchase record creation failed: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );

            player.sendMessage(
                    "구매 기록을 생성하지 못했습니다."
            );

            return false;
        }

        /*
         * 결제 실패 시 아이템은 지급하지 않는다.
         */
        boolean withdrawn;

        try {
            withdrawn = economyService.withdraw(
                    player,
                    price
            );

        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Withdraw exception. "
                            + "Transaction requires review: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );

            player.sendMessage(
                    "결제 상태를 확인해야 합니다. 관리자에게 문의해 주세요."
            );

            return false;
        }

        if (!withdrawn) {
            transitionOrLog(
                    transactionId,
                    FurniturePurchaseRepository.PENDING,
                    FurniturePurchaseRepository.CANCELLED
            );

            player.sendMessage(
                    "골드 결제에 실패했습니다."
            );

            return false;
        }

        /*
         * 결제 후 DB 기록이 PAID인지 확인한다.
         *
         * UPDATE 응답이 불명확하면 실제 상태를 재조회한다.
         * DB 상태를 확인할 수 없을 때는 자동 환불하지 않는다.
         */
        if (!confirmPaidOrResolve(
                player,
                transactionId,
                price
        )) {
            return false;
        }

        /*
         * 지급 실패 시 거래 전 인벤토리로 복구할 수 있도록
         * 지급 직전의 storage inventory를 복사한다.
         */
        ItemStack[] before = cloneStorage(
                inventory.getStorageContents()
        );

        try {
            Map<Integer, ItemStack> leftovers =
                    inventory.addItem(item);

            if (!leftovers.isEmpty()) {
                inventory.setStorageContents(before);

                refund(
                        player,
                        transactionId,
                        price,
                        FurniturePurchaseRepository.PAID
                );

                return false;
            }

        } catch (Exception exception) {
            inventory.setStorageContents(before);

            plugin.getLogger().severe(
                    "[FurnitureShop] Delivery failed: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );

            refund(
                    player,
                    transactionId,
                    price,
                    FurniturePurchaseRepository.PAID
            );

            return false;
        }

        /*
         * 아이템 지급이 끝난 뒤에는 기록 오류가 나더라도
         * 자동 환불하거나 아이템을 다시 지급하지 않는다.
         */
        try {
            if (!purchaseRepository.transition(
                    transactionId,
                    FurniturePurchaseRepository.PAID,
                    FurniturePurchaseRepository.DELIVERED
            )) {
                plugin.getLogger().severe(
                        "[FurnitureShop] Item delivered but "
                                + "DELIVERED transition failed: "
                                + transactionId
                );
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Delivered item requires "
                            + "transaction review: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );
        }

        player.sendMessage(
                product.displayName()
                        + " 구매 완료! -"
                        + price + "G"
        );

        return true;
    }

    private boolean confirmPaidOrResolve(
            Player player,
            UUID transactionId,
            long price
    ) {

        try {
            if (purchaseRepository.transition(
                    transactionId,
                    FurniturePurchaseRepository.PENDING,
                    FurniturePurchaseRepository.PAID
            )) {
                return true;
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[FurnitureShop] PAID transition error: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );
        }

        /*
         * UPDATE 결과가 불명확하므로 DB의 실제 상태를 확인한다.
         */
        final String status;

        try {
            status = purchaseRepository.findStatus(
                    transactionId
            );

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "[FurnitureShop] PAYMENT STATUS UNKNOWN - "
                            + "MANUAL REVIEW REQUIRED: "
                            + transactionId
                            + " player=" + player.getUniqueId()
                            + " amount=" + price
                            + " error=" + exception.getMessage()
            );

            player.sendMessage(
                    "결제 상태를 확인할 수 없습니다. "
                            + "관리자에게 문의해 주세요."
            );

            return false;
        }

        if (FurniturePurchaseRepository.PAID.equals(status)) {
            return true;
        }

        if (FurniturePurchaseRepository.PENDING.equals(status)) {

            refund(
                    player,
                    transactionId,
                    price,
                    FurniturePurchaseRepository.PENDING
            );

            return false;
        }

        plugin.getLogger().severe(
                "[FurnitureShop] UNEXPECTED PAYMENT STATUS - "
                        + "MANUAL REVIEW REQUIRED: "
                        + transactionId
                        + " status=" + status
                        + " player=" + player.getUniqueId()
                        + " amount=" + price
        );

        player.sendMessage(
                "결제 기록 확인이 필요합니다. "
                        + "관리자에게 문의해 주세요."
        );

        return false;
    }

    private void refund(
            Player player,
            UUID transactionId,
            long price,
            String expectedStatus
    ) {

        boolean refunded;

        try {
            refunded = economyService.deposit(
                    player,
                    price
            );

        } catch (Exception exception) {
            refunded = false;

            plugin.getLogger().severe(
                    "[FurnitureShop] Refund exception: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );
        }

        transitionOrLog(
                transactionId,
                expectedStatus,
                refunded
                        ? FurniturePurchaseRepository.REFUNDED
                        : FurniturePurchaseRepository.REFUND_FAILED
        );

        if (refunded) {
            player.sendMessage(
                    "가구 지급에 실패하여 "
                            + price + "G를 환불했습니다."
            );

        } else {
            plugin.getLogger().severe(
                    "[FurnitureShop] MANUAL REFUND REQUIRED: "
                            + transactionId
                            + " player=" + player.getUniqueId()
                            + " amount=" + price
            );

            player.sendMessage(
                    "환불을 확인해야 합니다. 관리자에게 문의해 주세요."
            );
        }
    }

    private void transitionOrLog(
            UUID transactionId,
            String expected,
            String next
    ) {

        try {
            if (!purchaseRepository.transition(
                    transactionId,
                    expected,
                    next
            )) {
                plugin.getLogger().severe(
                        "[FurnitureShop] Unexpected transaction status: "
                                + transactionId
                                + " expected=" + expected
                                + " next=" + next
                );
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "[FurnitureShop] Transaction status update failed: "
                            + transactionId
                            + " error=" + exception.getMessage()
            );
        }
    }

    private ItemStack[] cloneStorage(
            ItemStack[] contents
    ) {

        ItemStack[] copy = new ItemStack[contents.length];

        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null
                    ? null
                    : contents[i].clone();
        }

        return copy;
    }
}
