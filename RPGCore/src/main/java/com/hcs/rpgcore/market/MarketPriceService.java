package com.hcs.rpgcore.market;

import com.hcs.rpgcore.RPGCorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


/**
 * RPGCore 공통 시장 가격 서비스.
 *
 * 현재 구현:
 *
 * - Minecraft 1일 = 24,000 tick
 * - 날짜가 바뀔 때 새로운 가격 사용
 * - DB에 날짜별 가격 영구 저장
 * - 서버 재시작 후에도 같은 날짜 가격 유지
 * - 작물별 독립 가격
 *
 * 다른 시장(광물, 생선, 재료 등)에서도
 * marketId만 다르게 지정하여 재사용할 수 있다.
 */
public final class MarketPriceService {

    public static final int MIN_PRICE = 11;
    public static final int MAX_PRICE = 50;

    private static final long TICKS_PER_DAY =
            24000L;


    private final RPGCorePlugin plugin;

    private final MarketPriceRepository repository;

    private final Map<PriceKey, Integer>
            currentPriceCache =
            new ConcurrentHashMap<>();


    private long cachedDay =
            Long.MIN_VALUE;


    public MarketPriceService(
            RPGCorePlugin plugin,
            MarketPriceRepository repository
    ) {

        this.plugin = plugin;
        this.repository = repository;
    }


    /**
     * 현재 Minecraft 시장 날짜.
     *
     * 기본 world를 우선 사용하고,
     * 없으면 서버의 첫 번째 world를 사용한다.
     */
    public long getCurrentMarketDay() {

        World world =
                Bukkit.getWorld(
                        "world"
                );


        if (world == null) {

            if (Bukkit.getWorlds().isEmpty()) {

                throw new IllegalStateException(
                        "시장 날짜 계산에 사용할 월드가 없습니다."
                );
            }

            world =
                    Bukkit.getWorlds().get(0);
        }


        return Math.floorDiv(
                world.getFullTime(),
                TICKS_PER_DAY
        );
    }


    /**
     * 다음 시세 변경까지 남은 Minecraft tick.
     */
    public long getTicksUntilNextMarketDay() {

        World world =
                Bukkit.getWorld(
                        "world"
                );


        if (world == null) {

            if (Bukkit.getWorlds().isEmpty()) {
                return TICKS_PER_DAY;
            }

            world =
                    Bukkit.getWorlds().get(0);
        }


        long tickInDay =
                Math.floorMod(
                        world.getFullTime(),
                        TICKS_PER_DAY
                );


        long remaining =
                TICKS_PER_DAY
                        - tickInDay;


        if (remaining <= 0L) {
            return TICKS_PER_DAY;
        }


        return remaining;
    }


    /**
     * 다음 갱신까지 남은 시간을 초 단위로 표시할 때 사용.
     *
     * Minecraft 정상 20 TPS 기준.
     */
    public long getSecondsUntilNextMarketDay() {

        long ticks =
                getTicksUntilNextMarketDay();

        return (ticks + 19L) / 20L;
    }


    /**
     * 현재 날짜의 시장 가격.
     */
    public int getPrice(
            String marketId,
            String itemId
    ) {

        validateId(
                marketId,
                "marketId"
        );

        validateId(
                itemId,
                "itemId"
        );


        long marketDay =
                getCurrentMarketDay();


        refreshCacheDay(
                marketDay
        );


        PriceKey key =
                new PriceKey(
                        marketDay,
                        marketId,
                        itemId
                );


        return currentPriceCache.computeIfAbsent(
                key,
                ignored ->
                        repository.getOrCreatePrice(
                                marketDay,
                                marketId,
                                itemId,
                                this::rollBasePrice
                        )
        );
    }


    /**
     * 가장 최근의 과거 시세.
     *
     * 기록이 없으면 null.
     */
    public Integer getPreviousPrice(
            String marketId,
            String itemId
    ) {

        long marketDay =
                getCurrentMarketDay();


        return repository.findPreviousPrice(
                marketDay,
                marketId,
                itemId
        );
    }


    /**
     * 현재가 - 이전가.
     *
     * 이전 기록이 없으면 0.
     */
    public int getPriceChange(
            String marketId,
            String itemId
    ) {

        int current =
                getPrice(
                        marketId,
                        itemId
                );


        Integer previous =
                getPreviousPrice(
                        marketId,
                        itemId
                );


        if (previous == null) {
            return 0;
        }


        return current - previous;
    }


    /**
     * 목표:
     *
     * 최저 11
     * 최고 50
     * 장기 평균 약 19.6
     *
     * 분포:
     *
     * 11~15 : 20%
     * 16~19 : 35%
     * 20~24 : 35%
     * 25~34 :  8%
     * 35~50 :  2%
     *
     * 40~50원대의 고가 시세는
     * 의도적으로 희귀하게 발생한다.
     */
    private int rollBasePrice() {

        ThreadLocalRandom random =
                ThreadLocalRandom.current();


        int roll =
                random.nextInt(
                        100
                );


        if (roll < 20) {

            return random.nextInt(
                    11,
                    16
            );
        }


        if (roll < 55) {

            return random.nextInt(
                    16,
                    20
            );
        }


        if (roll < 90) {

            return random.nextInt(
                    20,
                    25
            );
        }


        if (roll < 98) {

            return random.nextInt(
                    25,
                    35
            );
        }


        return random.nextInt(
                35,
                51
        );
    }


    private synchronized void refreshCacheDay(
            long marketDay
    ) {

        if (cachedDay == marketDay) {
            return;
        }


        currentPriceCache.clear();

        cachedDay =
                marketDay;


        plugin.getLogger().info(
                "Market day changed: "
                        + marketDay
        );
    }


    private void validateId(
            String value,
            String name
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            throw new IllegalArgumentException(
                    name
                            + " must not be blank"
            );
        }
    }


    private record PriceKey(
            long marketDay,
            String marketId,
            String itemId
    ) {
    }
}
