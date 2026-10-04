package com.hcs.rpgcore.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class SkillOffenseBuffService {

    /*
     * =========================================================
     * BERSERKER RAGE
     * =========================================================
     */

    public static final double BERSERKER_ATTACK_MULTIPLIER =
            1.25;

    public static final double BERSERKER_MOVE_SPEED_MULTIPLIER =
            1.20;

    public static final double BERSERKER_INCOMING_DAMAGE_MULTIPLIER =
            1.10;


    /*
     * =========================================================
     * MANA OVERLOAD
     * =========================================================
     */

    public static final double MANA_OVERLOAD_DAMAGE_MULTIPLIER =
            1.30;

    public static final double MANA_OVERLOAD_COST_MULTIPLIER =
            1.25;


    /*
     * UUID -> expire time millis
     */
    private final Map<UUID, Long> berserkerExpireMap =
            new ConcurrentHashMap<>();

    private final Map<UUID, Long> manaOverloadExpireMap =
            new ConcurrentHashMap<>();


    /*
     * =========================================================
     * ACTIVATE
     * =========================================================
     */
    public void activateBerserkerRage(
            UUID uuid,
            long durationMillis
    ) {

        if (uuid == null) {
            return;
        }


        long safeDuration =
                Math.max(
                        0L,
                        durationMillis
                );


        berserkerExpireMap.put(
                uuid,
                System.currentTimeMillis()
                        + safeDuration
        );
    }


    public void activateManaOverload(
            UUID uuid,
            long durationMillis
    ) {

        if (uuid == null) {
            return;
        }


        long safeDuration =
                Math.max(
                        0L,
                        durationMillis
                );


        manaOverloadExpireMap.put(
                uuid,
                System.currentTimeMillis()
                        + safeDuration
        );
    }


    /*
     * =========================================================
     * ACTIVE CHECK
     * =========================================================
     */
    public boolean isBerserkerRageActive(
            UUID uuid
    ) {

        return isActive(
                berserkerExpireMap,
                uuid
        );
    }


    public boolean isManaOverloadActive(
            UUID uuid
    ) {

        return isActive(
                manaOverloadExpireMap,
                uuid
        );
    }


    private boolean isActive(
            Map<UUID, Long> map,
            UUID uuid
    ) {

        if (uuid == null) {
            return false;
        }


        Long expireTime =
                map.get(
                        uuid
                );


        if (expireTime == null) {
            return false;
        }


        if (
                expireTime
                        <= System.currentTimeMillis()
        ) {

            map.remove(
                    uuid
            );

            return false;
        }


        return true;
    }


    /*
     * =========================================================
     * DAMAGE MULTIPLIER
     * =========================================================
     */

    public double applyWarriorAttackMultiplier(
            UUID uuid,
            double damage
    ) {

        double safeDamage =
                Math.max(
                        0.0,
                        damage
                );


        if (
                !isBerserkerRageActive(
                        uuid
                )
        ) {
            return safeDamage;
        }


        return safeDamage
                * BERSERKER_ATTACK_MULTIPLIER;
    }


    public double applyMageDamageMultiplier(
            UUID uuid,
            double damage
    ) {

        double safeDamage =
                Math.max(
                        0.0,
                        damage
                );


        if (
                !isManaOverloadActive(
                        uuid
                )
        ) {
            return safeDamage;
        }


        return safeDamage
                * MANA_OVERLOAD_DAMAGE_MULTIPLIER;
    }


    /*
     * =========================================================
     * MANA COST MULTIPLIER
     * =========================================================
     */
    public double applyMageManaCostMultiplier(
            UUID uuid,
            double manaCost
    ) {

        double safeCost =
                Math.max(
                        0.0,
                        manaCost
                );


        if (
                !isManaOverloadActive(
                        uuid
                )
        ) {
            return safeCost;
        }


        return safeCost
                * MANA_OVERLOAD_COST_MULTIPLIER;
    }


    /*
     * =========================================================
     * INCOMING DAMAGE MULTIPLIER
     * =========================================================
     */
    public double applyIncomingDamageMultiplier(
            UUID uuid,
            double damage
    ) {

        double safeDamage =
                Math.max(
                        0.0,
                        damage
                );


        if (
                !isBerserkerRageActive(
                        uuid
                )
        ) {
            return safeDamage;
        }


        return safeDamage
                * BERSERKER_INCOMING_DAMAGE_MULTIPLIER;
    }


    /*
     * =========================================================
     * REMAINING TIME
     * =========================================================
     */
    public long getBerserkerRemainingMillis(
            UUID uuid
    ) {

        return getRemainingMillis(
                berserkerExpireMap,
                uuid
        );
    }


    public long getManaOverloadRemainingMillis(
            UUID uuid
    ) {

        return getRemainingMillis(
                manaOverloadExpireMap,
                uuid
        );
    }


    private long getRemainingMillis(
            Map<UUID, Long> map,
            UUID uuid
    ) {

        if (uuid == null) {
            return 0L;
        }


        Long expireTime =
                map.get(
                        uuid
                );


        if (expireTime == null) {
            return 0L;
        }


        long remaining =
                expireTime
                        - System.currentTimeMillis();


        if (remaining <= 0L) {

            map.remove(
                    uuid
            );

            return 0L;
        }


        return remaining;
    }


    /*
     * =========================================================
     * CLEAR
     * =========================================================
     */
    public void remove(
            UUID uuid
    ) {

        if (uuid == null) {
            return;
        }


        berserkerExpireMap.remove(
                uuid
        );

        manaOverloadExpireMap.remove(
                uuid
        );
    }


    public void clear() {

        berserkerExpireMap.clear();

        manaOverloadExpireMap.clear();
    }
}
