package com.hcs.rpgcore.shop;

import com.hcs.rpgcore.RPGCorePlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;


/**
 * RPGCore 상점용 Vault 경제 서비스.
 *
 * 현재 서버의 실제 Economy provider는
 * Vault를 통해 EternalEconomy가 제공한다.
 *
 * 상점 코드가 EternalEconomy 구현에
 * 직접 의존하지 않도록 Vault만 사용한다.
 */
public final class ShopEconomyService {

    private final RPGCorePlugin plugin;

    private Economy economy;


    public ShopEconomyService(
            RPGCorePlugin plugin
    ) {

        this.plugin = plugin;

        loadEconomy();
    }


    /**
     * Vault Economy provider 로드.
     */
    private void loadEconomy() {

        RegisteredServiceProvider<Economy>
                registration =
                plugin.getServer()
                        .getServicesManager()
                        .getRegistration(
                                Economy.class
                        );


        if (registration == null) {

            throw new IllegalStateException(
                    "Vault Economy provider를 찾을 수 없습니다."
            );
        }


        this.economy =
                registration.getProvider();


        if (this.economy == null) {

            throw new IllegalStateException(
                    "Vault Economy provider가 null입니다."
            );
        }


        plugin.getLogger().info(
                "Shop economy provider: "
                        + economy.getName()
        );
    }


    public Economy getEconomy() {
        return economy;
    }


    public double getBalance(
            Player player
    ) {

        validatePlayer(
                player
        );


        return Math.max(
                0.0D,
                economy.getBalance(
                        player
                )
        );
    }


    public long getWholeBalance(
            Player player
    ) {

        return (long)Math.floor(
                getBalance(
                        player
                )
        );
    }


    /**
     * 현재 잔액이 필요한 금액 이상인지 확인.
     */
    public boolean has(
            Player player,
            long amount
    ) {

        validateAmount(
                amount
        );


        return economy.has(
                player,
                (double)amount
        );
    }


    /**
     * 구매 금액 차감.
     *
     * 성공 여부만 반환한다.
     */
    public boolean withdraw(
            Player player,
            long amount
    ) {

        validatePlayer(
                player
        );

        validateAmount(
                amount
        );


        if (amount == 0L) {
            return true;
        }


        EconomyResponse response =
                economy.withdrawPlayer(
                        player,
                        (double)amount
                );


        if (!response.transactionSuccess()) {

            plugin.getLogger().warning(
                    "Shop withdraw failed: "
                            + player.getName()
                            + " amount="
                            + amount
                            + " error="
                            + response.errorMessage
            );

            return false;
        }


        return true;
    }


    /**
     * 판매 금액 지급.
     */
    public boolean deposit(
            Player player,
            long amount
    ) {

        validatePlayer(
                player
        );

        validateAmount(
                amount
        );


        if (amount == 0L) {
            return true;
        }


        EconomyResponse response =
                economy.depositPlayer(
                        player,
                        (double)amount
                );


        if (!response.transactionSuccess()) {

            plugin.getLogger().warning(
                    "Shop deposit failed: "
                            + player.getName()
                            + " amount="
                            + amount
                            + " error="
                            + response.errorMessage
            );

            return false;
        }


        return true;
    }


    private void validatePlayer(
            Player player
    ) {

        if (player == null) {

            throw new IllegalArgumentException(
                    "player must not be null"
            );
        }
    }


    private void validateAmount(
            long amount
    ) {

        if (amount < 0L) {

            throw new IllegalArgumentException(
                    "amount must not be negative"
            );
        }


        /*
         * Vault Economy API는 double을 사용하지만
         * RPGCore 화폐는 정수 단위로 운영한다.
         *
         * double의 정수 정밀도 한계를 넘는 금액은
         * 거래하지 않는다.
         */
        if (amount > 9_000_000_000_000_000L) {

            throw new IllegalArgumentException(
                    "amount is too large"
            );
        }
    }
}
