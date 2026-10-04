package com.hcs.rpgcore.shop;


/**
 * 품목 가격 결정 방식.
 */
public enum ShopPriceType {

    /**
     * 고정 가격.
     */
    FIXED,

    /**
     * MarketPriceService에서 가져오는 변동 가격.
     */
    MARKET
}
