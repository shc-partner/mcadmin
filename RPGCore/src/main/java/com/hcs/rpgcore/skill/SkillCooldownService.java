package com.hcs.rpgcore.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillCooldownService {

    private final Map<String, Long> cooldownMap =
            new ConcurrentHashMap<>();


    /*
     * =========================================================
     * COOLDOWN CHECK
     * =========================================================
     */
    public boolean isOnCooldown(
            UUID uuid,
            String skillId
    ) {

        return getRemainingMillis(
                uuid,
                skillId
        ) > 0L;
    }


    /*
     * =========================================================
     * REMAINING TIME
     * =========================================================
     */
    public long getRemainingMillis(
            UUID uuid,
            String skillId
    ) {

        String key =
                createKey(
                        uuid,
                        skillId
                );


        Long expiresAt =
                cooldownMap.get(
                        key
                );


        if (expiresAt == null) {
            return 0L;
        }


        long remaining =
                expiresAt
                        - System.currentTimeMillis();


        if (remaining <= 0L) {

            cooldownMap.remove(
                    key
            );

            return 0L;
        }


        return remaining;
    }


    /*
     * =========================================================
     * START COOLDOWN
     * =========================================================
     */
    public void startCooldown(
            UUID uuid,
            String skillId,
            long cooldownMillis
    ) {

        long safeCooldown =
                Math.max(
                        0L,
                        cooldownMillis
                );


        cooldownMap.put(
                createKey(
                        uuid,
                        skillId
                ),
                System.currentTimeMillis()
                        + safeCooldown
        );
    }


    /*
     * =========================================================
     * CLEAR PLAYER
     * =========================================================
     */
    public void clearPlayer(
            UUID uuid
    ) {

        String prefix =
                uuid.toString()
                        + ":";


        cooldownMap.keySet()
                .removeIf(
                        key -> key.startsWith(
                                prefix
                        )
                );
    }


    public void clear() {

        cooldownMap.clear();
    }


    private String createKey(
            UUID uuid,
            String skillId
    ) {

        return uuid
                + ":"
                + skillId;
    }
}
