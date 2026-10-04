package com.hcs.rpgcore.mana;

import com.hcs.rpgcore.runtime.PlayerRuntimeData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManaService {

    private final Map<UUID, PlayerRuntimeData> runtimeDataMap =
            new ConcurrentHashMap<>();

    /*
     * 플레이어 런타임 MANA 초기화.
     *
     * 접속 시:
     *
     * currentMana = maximumMana
     */
    public PlayerRuntimeData initialize(
            UUID uuid,
            double maximumMana
    ) {

        double safeMaximum =
                Math.max(
                        0.0,
                        maximumMana
                );

        PlayerRuntimeData runtimeData =
                new PlayerRuntimeData(
                        uuid,
                        safeMaximum,
                        safeMaximum
                );

        runtimeDataMap.put(
                uuid,
                runtimeData
        );

        return runtimeData;
    }

    public boolean isInitialized(
            UUID uuid
    ) {

        return runtimeDataMap.containsKey(
                uuid
        );
    }

    public PlayerRuntimeData getRuntimeData(
            UUID uuid
    ) {

        return runtimeDataMap.get(
                uuid
        );
    }

    public double getCurrentMana(
            UUID uuid
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            return 0.0;
        }

        return runtimeData.getCurrentMana();
    }

    public double getMaximumMana(
            UUID uuid
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            return 0.0;
        }

        return runtimeData.getMaximumMana();
    }

    /*
     * MANA 소비.
     *
     * 충분하면 true.
     * 부족하면 false.
     */
    public boolean consume(
            UUID uuid,
            double amount
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            return false;
        }

        return runtimeData.consumeMana(
                amount
        );
    }

    /*
     * MANA 회복.
     */
    public void restore(
            UUID uuid,
            double amount
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            return;
        }

        runtimeData.restoreMana(
                amount
        );
    }

    /*
     * 현재 MANA 직접 설정.
     */
    public void setCurrentMana(
            UUID uuid,
            double amount
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            return;
        }

        runtimeData.setCurrentMana(
                amount
        );
    }

    /*
     * 최대 MANA 변경.
     *
     * preserveRatio = true
     *
     * 기존:
     * 50 / 100
     *
     * 최대 MANA가 200으로 증가하면:
     * 100 / 200
     *
     * 비율 50% 유지.
     */
    public void updateMaximumMana(
            UUID uuid,
            double newMaximumMana,
            boolean preserveRatio
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            initialize(
                    uuid,
                    newMaximumMana
            );

            return;
        }

        runtimeData.setMaximumMana(
                newMaximumMana,
                preserveRatio
        );
    }

    /*
     * MANA 완전 회복.
     */
    public void refill(
            UUID uuid
    ) {

        PlayerRuntimeData runtimeData =
                runtimeDataMap.get(
                        uuid
                );

        if (runtimeData == null) {

            return;
        }

        runtimeData.refillMana();
    }

    /*
     * 플레이어 로그아웃.
     */
    public void remove(
            UUID uuid
    ) {

        runtimeDataMap.remove(
                uuid
        );
    }

    /*
     * 서버 종료.
     */
    public void clear() {

        runtimeDataMap.clear();
    }
}
