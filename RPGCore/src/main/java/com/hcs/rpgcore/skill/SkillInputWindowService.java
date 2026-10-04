package com.hcs.rpgcore.skill;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class SkillInputWindowService {

    private static final long INPUT_WINDOW_MILLIS =
            1000L;

    private final ConcurrentHashMap<UUID, Long>
            expirationMap =
                    new ConcurrentHashMap<>();


    /*
     * =========================================================
     * OPEN WINDOW
     * =========================================================
     */
    public void open(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return;
        }

        expirationMap.put(
                playerUuid,
                System.currentTimeMillis()
                        + INPUT_WINDOW_MILLIS
        );
    }


    /*
     * =========================================================
     * ACTIVE
     * =========================================================
     */
    public boolean isActive(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return false;
        }

        Long expiration =
                expirationMap.get(
                        playerUuid
                );

        if (expiration == null) {
            return false;
        }

        if (
                System.currentTimeMillis()
                        > expiration
        ) {

            expirationMap.remove(
                    playerUuid
            );

            return false;
        }

        return true;
    }


    /*
     * =========================================================
     * CONSUME
     * =========================================================
     */
    public boolean consume(
            UUID playerUuid
    ) {

        if (!isActive(playerUuid)) {
            return false;
        }

        expirationMap.remove(
                playerUuid
        );

        return true;
    }


    public void clear(
            UUID playerUuid
    ) {

        if (playerUuid == null) {
            return;
        }

        expirationMap.remove(
                playerUuid
        );
    }
}
