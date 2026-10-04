package com.hcs.rpgcore.stat;

import com.hcs.rpgcore.player.PlayerData;
import org.bukkit.entity.Player;

public final class StatService {

    private final EquipmentStatReader equipmentStatReader;


    public StatService(
            EquipmentStatReader equipmentStatReader
    ) {

        this.equipmentStatReader =
                equipmentStatReader;
    }


    

    

    



    /*
     * =========================================================
     * FINAL STATS
     * =========================================================
     *
     * PlayerData만 주어진 경우 장비를 제외한
     * 레벨 + 직업 기본 스탯을 반환한다.
     */
    public PlayerStats calculate(
            PlayerData playerData
    ) {

        return calculateBase(
                playerData
        );
    }


    /*
     * =========================================================
     * FINAL STATS WITH EQUIPMENT
     * =========================================================
     *
     * 장비는 ATTACK / DEFENSE / MAX MANA / 피해감소를 반영한다.
     *
     * MAX HEALTH는 장비에 의해 증가하지 않는다.
     *
     * MAX MANA는 주손 아이템의 max-mana만 반영한다.
     */
    public PlayerStats calculate(
            PlayerData playerData,
            Player player
    ) {

        PlayerStats base =
                calculateBase(
                        playerData
                );


        EquipmentStats equipment =
                equipmentStatReader.read(
                        player
                );


        return new PlayerStats(
                base.maxHealth(),
                base.attack()
                        + equipment.attack(),
                base.defense()
                        + equipment.defense(),
                base.maxMana()
                        + equipment.maxMana(),
                equipment.damageReductionFlat()
        );
    }


    /*
     * =========================================================
     * FINAL STATS FROM LEVEL + EQUIPMENT
     * =========================================================
     *
     * 직업 정보가 없는 계산용.
     */
    public PlayerStats calculate(
            int level,
            Player player
    ) {

        int safeLevel =
                clampLevel(
                        level
                );


        PlayerStats base =
                new PlayerStats(
                        calculateBaseMaxHealth(
                                safeLevel
                        ),
                        calculateBaseAttack(
                                safeLevel
                        ),
                        calculateBaseDefense(
                                safeLevel
                        ),
                        calculateBaseMaxMana(
                                safeLevel
                        ),
                        0.0
                );


        EquipmentStats equipment =
                equipmentStatReader.read(
                        player
                );


        return new PlayerStats(
                base.maxHealth(),
                base.attack()
                        + equipment.attack(),
                base.defense()
                        + equipment.defense(),
                base.maxMana()
                        + equipment.maxMana(),
                equipment.damageReductionFlat()
        );
    }


    /*
     * =========================================================
     * CLASS-AWARE LEVEL + EQUIPMENT
     * =========================================================
     *
     * level
     * + class growth
     * + equipment attack / defense
     *
     * 장비 MAX HP 증가는 적용하지 않는다.
     *
     * MAX MP는 주손 아이템의 max-mana만 적용한다.
     */
    public PlayerStats calculate(
            int level,
            String playerClass,
            Player player
    ) {

        PlayerStats base =
                calculateBase(
                        level,
                        playerClass
                );


        EquipmentStats equipment =
                equipmentStatReader.read(
                        player
                );


        return new PlayerStats(
                base.maxHealth(),
                base.attack()
                        + equipment.attack(),
                base.defense()
                        + equipment.defense(),
                base.maxMana()
                        + equipment.maxMana(),
                equipment.damageReductionFlat()
        );
    }


/*
 * =========================================================
 * CLASS-AWARE BASE STATS
 * =========================================================
 *
 * level + class만으로 장비 제외 기본 스탯을 계산한다.
 */
public PlayerStats calculateBase(
        int level,
        String playerClass
) {

    int safeLevel =
            clampLevel(
                    level
            );


    double maxHealth =
            calculateBaseMaxHealth(
                    safeLevel
            );


    double attack =
            calculateBaseAttack(
                    safeLevel,
                    playerClass
            );


    double defense =
            calculateBaseDefense(
                    safeLevel
            );


    double maxMana =
            calculateBaseMaxMana(
                    safeLevel
            );


    if (
            playerClass != null
            && "WARRIOR".equalsIgnoreCase(
                    playerClass
            )
    ) {

        maxHealth =
                calculateWarriorMaxHealth(
                        safeLevel
                );

        maxMana =
                calculateWarriorMaxMana(
                        safeLevel
                );
    }


    if (
            playerClass != null
            && "MAGE".equalsIgnoreCase(
                    playerClass
            )
    ) {

        maxHealth =
                calculateMageMaxHealth(
                        safeLevel
                );

        maxMana =
                calculateMageMaxMana(
                        safeLevel
                );
    }


    return new PlayerStats(
            maxHealth,
            attack,
            defense,
            maxMana,
            0.0
    );
}



    /*
     * =========================================================
     * BASE STATS
     * =========================================================
     *
     * 레벨에 따른 캐릭터 기본 능력치.
     *
     * 장비 스탯은 이 메서드에서 계산하지 않는다.
     */
    public PlayerStats calculateBase(
        PlayerData playerData
) {

    return calculateBase(
            playerData.getLevel(),
            playerData.getPlayerClass()
    );
}


    /*
     * =========================================================
     * BASE MAX HEALTH
     * =========================================================
     *
     * NOVICE
     *
     * Lv.1  = 20
     * Lv.10 = 100
     *
     * Lv.10 이후 직업 미선택 상태에서는
     * 초보자 최종 HP 100을 유지한다.
     */
    public double calculateBaseMaxHealth(
            int level
    ) {

        int safeLevel =
                clampLevel(
                        level
                );

        int noviceLevel =
                Math.min(
                        safeLevel,
                        10
                );

        double progress =
                (noviceLevel - 1)
                        / 9.0;

        return Math.round(
                20.0
                        + 80.0
                        * Math.pow(
                                progress,
                                1.15
                        )
        );
    }


    /*
     * =========================================================
     * BASE ATTACK
     * =========================================================
     */
    public double calculateBaseAttack(
            int level
    ) {

        int safeLevel =
                clampLevel(
                        level
                );


        /*
         * =====================================================
         * NOVICE / NO CLASS
         * =====================================================
         *
         * Lv.1  = 1
         * Lv.10 = 2
         *
         * Lv.10 이후에도 직업 정보가 없는 경우
         * 기본 공격력 2를 유지한다.
         */

        int noviceLevel =
                Math.min(
                        safeLevel,
                        10
                );


        return Math.round(
                1.0
                        + (
                                (noviceLevel - 1)
                                        / 9.0
                        )
        );
    }


    /*
     * =========================================================
     * CLASS-AWARE BASE ATTACK
     * =========================================================
     *
     * NOVICE
     * Lv.1  = 1
     * Lv.10 = 2
     *
     * WARRIOR
     * Lv.10 이후 레벨당 +0.20
     *
     * MAGE
     * Lv.10 이후 레벨당 +0.10
     *
     * 모든 결과는 Math.round()로 정수 처리한다.
     * =========================================================
     */

    public double calculateBaseAttack(
            int level,
            String playerClass
    ) {

        int safeLevel =
                clampLevel(
                        level
                );


        /*
         * Lv.1 ~ Lv.9 또는 직업 미선택 상태.
         */
        if (
                safeLevel < 10
                        ||
                playerClass == null
                        ||
                playerClass.isBlank()
        ) {

            return calculateBaseAttack(
                    safeLevel
            );
        }


        /*
         * =====================================================
         * WARRIOR
         * =====================================================
         *
         * Lv.10 = 2
         * Lv.20 = 4
         * Lv.40 = 8
         * Lv.70 = 14
         * Lv.99 = 20
         */

        if (
                "WARRIOR".equalsIgnoreCase(
                        playerClass
                )
        ) {

            return Math.round(
                    2.0
                            + (
                                    (safeLevel - 10)
                                            * 0.20
                            )
            );
        }


        /*
         * =====================================================
         * MAGE
         * =====================================================
         *
         * Lv.10 = 2
         * Lv.20 = 3
         * Lv.40 = 5
         * Lv.70 = 8
         * Lv.99 = 11
         */

        if (
                "MAGE".equalsIgnoreCase(
                        playerClass
                )
        ) {

            return Math.round(
                    2.0
                            + (
                                    (safeLevel - 10)
                                            * 0.10
                            )
            );
        }


        /*
         * 알 수 없는 직업은 초보자 규칙.
         */
        return calculateBaseAttack(
                safeLevel
        );
    }


    /*
     * =========================================================
     * BASE DEFENSE
     * =========================================================
     */
    public double calculateBaseDefense(
            int level
    ) {

        int safeLevel =
                clampLevel(
                        level
                );

        /*
         * 2레벨마다 +1
         *
         * Lv.1  = 1
         * Lv.2  = 1
         * Lv.3  = 2
         * ...
         * Lv.99 = 50
         */
        return 1.0
                + (
                        (safeLevel - 1)
                        / 2
                );
    }


    /*
     * =========================================================
     * BASE MAX MANA
     * =========================================================
     *
     * NOVICE
     *
     * Lv.1 ~ Lv.10 = 5
     *
     * 직업 미선택 상태에서는 최대 MANA 5를 유지한다.
     */
    public double calculateBaseMaxMana(
            int level
    ) {

        clampLevel(
                level
        );

        return 5.0;
    }


    /*
     * =========================================================
     * LEVEL CLAMP
     * =========================================================
     */
    private int clampLevel(
            int level
    ) {

        return Math.max(
                1,
                Math.min(
                        99,
                        level
                )
        );
    }


/*
 * =========================================================
 * CLASS HP / MP GROWTH
 * =========================================================
 *
 * Lv.10 전직 기준
 *
 * Lv.10
 * WARRIOR = HP 100 / MP 5
 * MAGE    = HP 100 / MP 30
 *
 * Lv.99
 * WARRIOR = HP 1000 / MP 300
 * MAGE    = HP 650  / MP 1000
 */


/*
 * =========================================================
 * CLASS PROGRESS
 * =========================================================
 *
 * Lv.10 = 0.0
 * Lv.99 = 1.0
 */
private double calculateClassProgress(
        int level
) {

    int safeLevel =
            clampLevel(
                    level
            );

    if (safeLevel <= 10) {
        return 0.0;
    }

    return (safeLevel - 10)
            / 89.0;
}


/*
 * =========================================================
 * WARRIOR MAX HEALTH
 * =========================================================
 */
private double calculateWarriorMaxHealth(
        int level
) {

    int safeLevel =
            clampLevel(
                    level
            );

    if (safeLevel <= 10) {
        return calculateBaseMaxHealth(
                safeLevel
        );
    }

    double progress =
            calculateClassProgress(
                    safeLevel
            );

    return Math.round(
            100.0
                    + 900.0
                    * Math.pow(
                            progress,
                            1.30
                    )
    );
}


/*
 * =========================================================
 * WARRIOR MAX MANA
 * =========================================================
 */
private double calculateWarriorMaxMana(
        int level
) {

    int safeLevel =
            clampLevel(
                    level
            );

    if (safeLevel <= 10) {
        return 5.0;
    }

    double progress =
            calculateClassProgress(
                    safeLevel
            );

    return Math.round(
            5.0
                    + 295.0
                    * Math.pow(
                            progress,
                            1.30
                    )
    );
}


/*
 * =========================================================
 * MAGE MAX HEALTH
 * =========================================================
 */
private double calculateMageMaxHealth(
        int level
) {

    int safeLevel =
            clampLevel(
                    level
            );

    if (safeLevel <= 10) {
        return calculateBaseMaxHealth(
                safeLevel
        );
    }

    double progress =
            calculateClassProgress(
                    safeLevel
            );

    return Math.round(
            100.0
                    + 550.0
                    * Math.pow(
                            progress,
                            1.30
                    )
    );
}


/*
 * =========================================================
 * MAGE MAX MANA
 * =========================================================
 */
private double calculateMageMaxMana(
        int level
) {

    int safeLevel =
            clampLevel(
                    level
            );

    /*
     * 전직 전에는 MP 5.
     *
     * 마법사 전직이 가능한 Lv.10부터
     * 기본 MP 30으로 시작한다.
     */
    if (safeLevel < 10) {
        return 5.0;
    }

    double progress =
            calculateClassProgress(
                    safeLevel
            );

    return Math.round(
            30.0
                    + 970.0
                    * Math.pow(
                            progress,
                            1.30
                    )
    );
}


/*
 * =========================================================
 * CLASS GROWTH BONUS
 * =========================================================
 *
 * Lv.10 전직 기준.
 *
 * Lv.10 자체는 보너스 0.
 * Lv.10 -> 11부터 해당 구간의 증가량을 누적한다.
 */


/*
 * =========================================================
 * WARRIOR ATTACK BONUS
 * =========================================================
 */
private double calculateWarriorAttackBonus(
        int level
) {

    int safeLevel =
            clampLevel(
                    level
            );

    double bonus =
            0.0;


    for (
            int currentLevel = 10;
            currentLevel < safeLevel;
            currentLevel++
    ) {

        bonus +=
                getWarriorAttackGrowth(
                        currentLevel
                );
    }


    return bonus;
}


/*
 * =========================================================
 * WARRIOR HP BONUS
 * =========================================================
 */
/*
 * =========================================================
 * MAGE MP BONUS
 * =========================================================
 */
/*
 * =========================================================
 * WARRIOR ATTACK PER LEVEL
 * =========================================================
 */
private double getWarriorAttackGrowth(
        int level
) {

    if (level < 20) {
        return 1.0;
    }

    if (level < 30) {
        return 2.0;
    }

    if (level < 40) {
        return 3.0;
    }

    if (level < 50) {
        return 4.0;
    }

    if (level < 60) {
        return 6.0;
    }

    if (level < 70) {
        return 8.0;
    }

    if (level < 80) {
        return 11.0;
    }

    if (level < 90) {
        return 15.0;
    }

    return 20.0;
}


/*
 * =========================================================
 * WARRIOR HP PER LEVEL
 * =========================================================
 */
/*
 * =========================================================
 * MAGE MP PER LEVEL
 * =========================================================
 */
}
