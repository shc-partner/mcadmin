package com.hcs.rpgcore.level;

import com.hcs.rpgcore.player.PlayerData;

public final class LevelService {

    private final int maximumLevel;

    public LevelService(int maximumLevel) {

        this.maximumLevel =
                Math.max(
                        1,
                        maximumLevel
                );
    }

    /**
     * 서버의 최대 RPG 레벨을 반환한다.
     */
    public int getMaximumLevel() {

        return maximumLevel;
    }

    /**
     * 현재 레벨에서 다음 레벨로 올라가기 위해 필요한 EXP를 반환한다.
     *
     * Lv.99 이상에서는 더 이상 레벨업할 수 없으므로 0을 반환한다.
     */
    public long getRequiredExperience(int level) {

        if (level >= maximumLevel) {
            return 0L;
        }

        if (level < 1) {
            level = 1;
        }

        /*
         * Lv.1 ~ Lv.9
         *
         * Lv.1 -> 2 : 100
         * Lv.2 -> 3 : 200
         * ...
         * Lv.9 -> 10 : 900
         */
        if (level < 10) {

            return 100L
                    + ((long) (level - 1) * 100L);
        }

        /*
         * Lv.10 ~ Lv.19
         *
         * Lv.10 -> 11 : 1,000
         * Lv.11 -> 12 : 1,150
         * ...
         */
        if (level < 20) {

            return 1000L
                    + ((long) (level - 10) * 150L);
        }

        /*
         * Lv.20 ~ Lv.29
         */
        if (level < 30) {

            return 2500L
                    + ((long) (level - 20) * 250L);
        }

        /*
         * Lv.30 ~ Lv.39
         */
        if (level < 40) {

            return 5000L
                    + ((long) (level - 30) * 400L);
        }

        /*
         * Lv.40 ~ Lv.49
         */
        if (level < 50) {

            return 9000L
                    + ((long) (level - 40) * 700L);
        }

        /*
         * Lv.50 ~ Lv.59
         */
        if (level < 60) {

            return 16000L
                    + ((long) (level - 50) * 1200L);
        }

        /*
         * Lv.60 ~ Lv.69
         */
        if (level < 70) {

            return 28000L
                    + ((long) (level - 60) * 2000L);
        }

        /*
         * Lv.70 ~ Lv.79
         */
        if (level < 80) {

            return 48000L
                    + ((long) (level - 70) * 3500L);
        }

        /*
         * Lv.80 ~ Lv.89
         */
        if (level < 90) {

            return 80000L
                    + ((long) (level - 80) * 6000L);
        }

        /*
         * Lv.90 ~ Lv.98
         *
         * Lv.90 -> 91 : 140,000
         * ...
         * Lv.98 -> 99 : 220,000
         */
        return 140000L
                + ((long) (level - 90) * 10000L);
    }

    /**
     * 플레이어에게 RPG EXP를 지급하고
     * 필요한 경우 한 번에 여러 레벨을 올린다.
     */
    public LevelUpResult addExperience(
            PlayerData playerData,
            long gainedExperience
    ) {

        int oldLevel =
                playerData.getLevel();

        /*
         * 혹시 DB 등에 잘못된 레벨이 들어있어도
         * 정상 범위로 보정한다.
         */
        int currentLevel =
                Math.max(
                        1,
                        Math.min(
                                maximumLevel,
                                oldLevel
                        )
                );

        long currentExperience =
                Math.max(
                        0L,
                        playerData.getExperience()
                );

        /*
         * 이미 최대 레벨이라면
         * 추가 EXP를 저장하지 않는다.
         */
        if (currentLevel >= maximumLevel) {

            playerData.setLevel(
                    maximumLevel
            );

            playerData.setExperience(
                    0L
            );

            return new LevelUpResult(
                    oldLevel,
                    maximumLevel,
                    0L,
                    false
            );
        }

        /*
         * 음수 또는 0 EXP는 무시한다.
         */
        if (gainedExperience <= 0L) {

            playerData.setLevel(
                    currentLevel
            );

            playerData.setExperience(
                    currentExperience
            );

            return new LevelUpResult(
                    oldLevel,
                    currentLevel,
                    currentExperience,
                    false
            );
        }

        /*
         * long overflow 방지.
         */
        long newExperience;

        if (Long.MAX_VALUE - currentExperience
                < gainedExperience) {

            newExperience =
                    Long.MAX_VALUE;

        } else {

            newExperience =
                    currentExperience
                            + gainedExperience;
        }

        /*
         * EXP가 충분하면 반복적으로 레벨업한다.
         *
         * 예:
         * Lv.1 EXP 0
         * +5000 EXP
         *
         * 한 번의 호출로 여러 레벨 상승 가능.
         */
        while (currentLevel < maximumLevel) {

            /*
             * =====================================================
             * Lv.10 1차 전직 잠금
             * =====================================================
             *
             * Lv.10에 도달한 뒤 직업이 NONE이면
             * EXP는 계속 누적하지만 Lv.11 이상으로
             * 상승하지 않는다.
             */
            if (
                    currentLevel == 10
                    && !hasSelectedClass(playerData)
            ) {
                break;
            }


            long requiredExperience =
                    getRequiredExperience(
                            currentLevel
                    );

            if (requiredExperience <= 0L) {
                break;
            }

            if (newExperience
                    < requiredExperience) {

                break;
            }

            newExperience -=
                    requiredExperience;

            currentLevel++;
        }

        /*
         * Lv.99에 도달한 순간
         * 잔여 EXP는 모두 제거한다.
         */
        if (currentLevel >= maximumLevel) {

            currentLevel =
                    maximumLevel;

            newExperience =
                    0L;
        }

        playerData.setLevel(
                currentLevel
        );

        playerData.setExperience(
                newExperience
        );

        boolean leveledUp =
                currentLevel > oldLevel;

        return new LevelUpResult(
                oldLevel,
                currentLevel,
                newExperience,
                leveledUp
        );
    }

    /**
     * 플레이어가 1차 직업을 선택했는지 확인한다.
     */
    private boolean hasSelectedClass(
            PlayerData playerData
    ) {

        String playerClass =
                playerData.getPlayerClass();

        if (playerClass == null) {
            return false;
        }

        playerClass =
                playerClass.trim();

        return !playerClass.isEmpty()
                && !"NONE".equalsIgnoreCase(
                        playerClass
                );
    }

}
