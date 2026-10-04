package com.hcs.rpgcore.shop;


/**
 * 상점에 표시되는 하나의 거래 품목.
 *
 * entryId:
 *     RPGCore 상점 내부 식별자
 *
 * itemId:
 *     실제 아이템 공급원에서 사용하는 ID
 *
 * 예:
 *     source = CRAFTENGINE
 *     itemId = customcrops:tomato_seeds
 */
public record ShopItemDefinition(

        String entryId,

        String displayName,

        ShopItemSource source,

        String itemId,

        int slot,

        int amount,

        ShopPriceType priceType,

        int fixedBuyPrice,

        int fixedSellPrice,

        String marketId,

        String marketItemId,

        double marketMultiplier
) {

    public ShopItemDefinition {

        if (
                entryId == null
                        || entryId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "entryId must not be blank"
            );
        }


        if (
                displayName == null
                        || displayName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "displayName must not be blank"
            );
        }


        if (source == null) {
            throw new IllegalArgumentException(
                    "source must not be null"
            );
        }


        if (
                itemId == null
                        || itemId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "itemId must not be blank"
            );
        }


        if (
                slot < 0
                        || slot >= 54
        ) {
            throw new IllegalArgumentException(
                    "slot must be between 0 and 53"
            );
        }


        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "amount must be greater than 0"
            );
        }


        if (priceType == null) {
            throw new IllegalArgumentException(
                    "priceType must not be null"
            );
        }


        if (
                fixedBuyPrice < 0
                        || fixedSellPrice < 0
        ) {
            throw new IllegalArgumentException(
                    "fixed prices must not be negative"
            );
        }


        if (
                priceType == ShopPriceType.MARKET
                        && (
                                marketId == null
                                        || marketId.isBlank()
                                        || marketItemId == null
                                        || marketItemId.isBlank()
                        )
        ) {

            throw new IllegalArgumentException(
                    "MARKET price requires "
                            + "marketId and marketItemId"
            );
        }


        if (marketMultiplier <= 0.0D) {
            throw new IllegalArgumentException(
                    "marketMultiplier must be greater than 0"
            );
        }
    }


    /**
     * 고정가격 구매 품목 생성용.
     */
    public static ShopItemDefinition fixedBuy(

            String entryId,

            String displayName,

            ShopItemSource source,

            String itemId,

            int slot,

            int amount,

            int buyPrice
    ) {

        return new ShopItemDefinition(
                entryId,
                displayName,
                source,
                itemId,
                slot,
                amount,
                ShopPriceType.FIXED,
                buyPrice,
                0,
                null,
                null,
                1.0D
        );
    }


    /**
     * 고정가격 판매 품목 생성용.
     */
    public static ShopItemDefinition fixedSell(

            String entryId,

            String displayName,

            ShopItemSource source,

            String itemId,

            int slot,

            int amount,

            int sellPrice
    ) {

        return new ShopItemDefinition(
                entryId,
                displayName,
                source,
                itemId,
                slot,
                amount,
                ShopPriceType.FIXED,
                0,
                sellPrice,
                null,
                null,
                1.0D
        );
    }


    /**
     * 시장가격 판매 품목 생성용.
     */
    public static ShopItemDefinition marketSell(

            String entryId,

            String displayName,

            ShopItemSource source,

            String itemId,

            int slot,

            int amount,

            String marketId,

            String marketItemId,

            double multiplier
    ) {

        return new ShopItemDefinition(
                entryId,
                displayName,
                source,
                itemId,
                slot,
                amount,
                ShopPriceType.MARKET,
                0,
                0,
                marketId,
                marketItemId,
                multiplier
        );
    }
}
