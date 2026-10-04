package com.hcs.rpgcore.skill;

import java.util.List;


public final class SkillRegistry {

    private static final List<SkillDefinition>
            SKILLS =
            List.of(

                    /*
                     * =================================================
                     * WARRIOR
                     * =================================================
                     */
                    new SkillDefinition(
                            SkillIds.WARRIOR_BASH,
                            "WARRIOR",
                            "배쉬",
                            10
                    ),

                                        new SkillDefinition(
                            SkillIds.WARRIOR_WHIRLWIND,
                            "WARRIOR",
                            "휠윈드",
                            20
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_EXECUTION_SLASH,
                            "WARRIOR",
                            "익스큐션 슬래시",
                            30
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_IRON_WALL,
                            "WARRIOR",
                            "아이언 월",
                            40
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_EARTHQUAKE_SLAM,
                            "WARRIOR",
                            "어스퀘이크 슬램",
                            50
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_BATTLE_CRY,
                            "WARRIOR",
                            "배틀 크라이",
                            60
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_BERSERKER_RAGE,
                            "WARRIOR",
                            "버서커 레이지",
                            70
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_BLADE_STORM,
                            "WARRIOR",
                            "블레이드 스톰",
                            80
                    ),

                    new SkillDefinition(
                            SkillIds.WARRIOR_GATE_OF_BABYLON,
                            "WARRIOR",
                            "인피니트 블레이드",
                            90
                    ),


                    /*
                     * =================================================
                     * MAGE
                     * =================================================
                     */
                    new SkillDefinition(
                            SkillIds.MAGE_FIRE_BOLT,
                            "MAGE",
                            "파이어 볼트",
                            10
                    ),

                                        new SkillDefinition(
                            SkillIds.MAGE_FROST_NOVA,
                            "MAGE",
                            "프로스트 노바",
                            20
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_CHAIN_LIGHTNING,
                            "MAGE",
                            "체인 라이트닝",
                            30
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_MANA_SHIELD,
                            "MAGE",
                            "마나 실드",
                            40
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_BLIZZARD,
                            "MAGE",
                            "블리자드",
                            50
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_BLACK_HOLE,
                            "MAGE",
                            "블랙홀",
                            60
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_MANA_OVERLOAD,
                            "MAGE",
                            "마나 오버로드",
                            70
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_THUNDER_STORM,
                            "MAGE",
                            "썬더 스톰",
                            80
                    ),

                    new SkillDefinition(
                            SkillIds.MAGE_METEOR,
                            "MAGE",
                            "메테오 스트라이크",
                            90
                    )
            );


    private SkillRegistry() {
    }


    /*
     * =========================================================
     * ALL
     * =========================================================
     */
    public static List<SkillDefinition> all() {

        return SKILLS;
    }


    /*
     * =========================================================
     * CLASS SKILLS
     * =========================================================
     */
    public static List<SkillDefinition> forClass(
            String playerClass
    ) {

        if (
                playerClass == null
                || playerClass.isBlank()
        ) {
            return List.of();
        }


        return SKILLS.stream()
                .filter(
                        skill ->
                                skill.isClass(
                                        playerClass
                                )
                )
                .toList();
    }


    /*
     * =========================================================
     * FIND BY ID
     * =========================================================
     */
    public static SkillDefinition find(
            String skillId
    ) {

        if (
                skillId == null
                || skillId.isBlank()
        ) {
            return null;
        }


        for (
                SkillDefinition skill
                : SKILLS
        ) {

            if (
                    skill.skillId()
                            .equals(
                                    skillId
                            )
            ) {
                return skill;
            }
        }


        return null;
    }


    /*
     * =========================================================
     * UNLOCK CHECK
     * =========================================================
     */
    public static boolean isUnlocked(
            String skillId,
            String playerClass,
            int playerLevel
    ) {

        SkillDefinition skill =
                find(
                        skillId
                );


        if (skill == null) {
            return false;
        }


        if (
                !skill.isClass(
                        playerClass
                )
        ) {
            return false;
        }


        return skill.isUnlocked(
                playerLevel
        );
    }
}
