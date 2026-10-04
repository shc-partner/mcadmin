package com.hcs.rpgcore.shop;

import com.hcs.rpgcore.market.MarketPriceService;


/**
 * 상점 가격 계산 전담 서비스.
 *
 * GUI와 실제 거래 처리 코드에서는
 * 가격 공식을 직접 계산하지 않고
 * 반드시 이 서비스를 사용한다.
 */
public final class ShopPriceService {

    private final MarketPriceService
            marketPriceService;


    public ShopPriceService(
            MarketPriceService marketPriceService
    ) {

        this.marketPriceService =
                marketPriceService;
    }


    /**
     * 품목 1묶음의 구매 가격.
     *
     * 예:
     * amount = 1
     * fixedBuyPrice = 10
     *
     * → 10원
     */
    public int getBuyPrice(
            ShopItemDefinition item
    ) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "item must not be null"
            );
        }


        return switch (
                item.priceType()
        ) {

            case FIXED ->
                    item.fixedBuyPrice();

            case MARKET ->
                    calculateMarketPrice(
                            item
                    );
        };
    }


    /**
     * 품목 1묶음의 판매 가격.
     */
    public int getSellPrice(
            ShopItemDefinition item
    ) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "item must not be null"
            );
        }


        return switch (
                item.priceType()
        ) {

            case FIXED ->
                    item.fixedSellPrice();

            case MARKET ->
                    calculateMarketPrice(
                            item
                    );
        };
    }


    /**
     * 여러 묶음 구매 가격.
     *
     * quantity는 "상점 클릭 횟수/묶음 수"이다.
     *
     * 실제 지급 아이템 수:
     * item.amount() * quantity
     */
    public long getTotalBuyPrice(
            ShopItemDefinition item,
            int quantity
    ) {

        validateQuantity(
                quantity
        );


        return Math.multiplyExact(
                (long)getBuyPrice(item),
                (long)quantity
        );
    }


    /**
     * 여러 묶음 판매 가격.
     */
    public long getTotalSellPrice(
            ShopItemDefinition item,
            int quantity
    ) {

        validateQuantity(
                quantity
        );


        return Math.multiplyExact(
                (long)getSellPrice(item),
                (long)quantity
        );
    }


    /**
     * MARKET 가격.
     *
     * 기본 시장가격 × 품질/등급 배율.
     *
     * 예:
     *
     * 일반:
     * 20 × 1.0 = 20
     *
     * 고급:
     * 20 × 1.5 = 30
     *
     * 최상급:
     * 20 × 2.5 = 50
     */
    /**
     * 다음 시장 날짜까지 남은 시간(초).
     * 실제 시세 날짜와 같은 MarketPriceService 기준을 사용한다.
     */
    public long getSecondsUntilNextMarketDay() {
        return marketPriceService.getSecondsUntilNextMarketDay();
    }


    private int calculateMarketPrice(
            ShopItemDefinition item
    ) {

        int basePrice =
                marketPriceService.getPrice(
                        item.marketId(),
                        item.marketItemId()
                );


        long calculated =
                Math.round(
                        basePrice
                                * item.marketMultiplier()
                );


        if (calculated < 0L) {
            return 0;
        }


        if (calculated > Integer.MAX_VALUE) {

            throw new IllegalStateException(
                    "Calculated shop price is too large: "
                            + item.entryId()
            );
        }


        return (int)calculated;
    }


    private void validateQuantity(
            int quantity
    ) {

        if (quantity <= 0) {

            throw new IllegalArgumentException(
                    "quantity must be greater than 0"
            );
        }
    }
}
